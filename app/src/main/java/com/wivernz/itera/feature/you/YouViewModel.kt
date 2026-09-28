package com.wivernz.itera.feature.you

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.analytics.SettingKey
import com.wivernz.itera.analytics.SettingValue
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.core.voice.VoiceModelStore
import com.wivernz.itera.domain.demo.DemoDataLoader
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.LearningTopicRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.training.EraseAllDataUseCase
import com.wivernz.itera.domain.training.ResetProgramUseCase
import com.wivernz.itera.domain.voice.VoiceLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class YouUiState(
    val preferences: UserPreferences = UserPreferences(),
    val topics: List<LearningTopic> = emptyList(),
    val permission: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val demoAvailable: Boolean = false,
    /** Milestone 013: each language's offline speech model, and whether Play can download one. */
    val voiceModels: Map<VoiceLanguage, VoiceModelState> = emptyMap(),
    val voiceModelInfo: Map<VoiceLanguage, VoiceModelInfo> = emptyMap(),
    val modelDownloadAvailable: Boolean = false
)
sealed interface YouUiEvent {
    data class Name(val text: String) : YouUiEvent
    data class Change(val key: SettingKey, val value: SettingValue, val minute: Int = 0) :
        YouUiEvent
    data class Permission(val granted: Boolean) : YouUiEvent

    /** ADR-0022: allow or revoke the system speech recogniser. Not tracked. */
    data class SystemRecognition(val allowed: Boolean) : YouUiEvent

    /** ADR-0022: the consented recognition app, or null to turn it off. Not tracked. */
    data class Recognizer(val id: String?) : YouUiEvent

    /** Milestone 013: offline speech model actions for one language. Never started automatically. */
    data class DownloadModel(val language: VoiceLanguage) : YouUiEvent

    data class ImportModel(val uri: String) : YouUiEvent

    data class RemoveModel(val language: VoiceLanguage) : YouUiEvent

    data class RecheckModel(val language: VoiceLanguage) : YouUiEvent
    data class Topic(val id: Long?, val title: String) : YouUiEvent
    data class Archive(val id: Long) : YouUiEvent
    data class Open(val route: ScreenRoute) : YouUiEvent
    data class Reset(val erase: Boolean) : YouUiEvent
    data object Demo : YouUiEvent
    data object Retry : YouUiEvent
}
sealed interface YouEffect {
    data class ResetDone(val erased: Boolean) : YouEffect
    data object DemoDone : YouEffect

    data class ModelImported(val result: VoiceModelImport) : YouEffect
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class YouViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val topics: LearningTopicRepository,
    private val reminders: ReminderScheduler,
    private val reset: ResetProgramUseCase,
    private val erase: EraseAllDataUseCase,
    private val demo: Optional<DemoDataLoader>,
    private val analytics: Analytics,
    private val voiceModels: VoiceModelStore
) : ViewModel() {
    private val local = MutableStateFlow(YouUiState(demoAvailable = demo.isPresent))
    private val refresh = MutableStateFlow(0)
    private val channel = Channel<YouEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    val state = refresh.flatMapLatest {
        combine(
            preferences.preferences,
            topics.observeTopics(),
            local,
            voiceModels.states,
            voiceModels.downloadAvailable
        ) { prefs, rows, ui, models, downloadable ->
            ui.copy(
                preferences = prefs,
                topics = rows.filterNot { it.archived },
                loading = false,
                voiceModels = models,
                voiceModelInfo = VoiceLanguage.entries.associateWith(voiceModels::info),
                modelDownloadAvailable = downloadable
            )
        }.catch { emit(local.value.copy(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.YOU))
    }

    fun onEvent(event: YouUiEvent) {
        if (local.value.busy) return
        if (event ==
            YouUiEvent.Retry
        ) {
            local.update { it.copy(failed = false) }
            refresh.value++
            return
        }
        if (event is YouUiEvent.Reset ||
            event == YouUiEvent.Demo
        ) {
            local.update { it.copy(busy = true) }
        }
        viewModelScope.launch {
            try {
                when (event) {
                    is YouUiEvent.Name -> preferences.update { it.copy(displayName = event.text) }
                    is YouUiEvent.Change -> {
                        preferences.update { changePreference(it, event, local.value.permission) }
                        analytics.track(Event.SettingChanged(event.key, event.value))
                        if (event.key in reminderKeys) reminders.rescheduleAll()
                    }
                    is YouUiEvent.Permission -> local.update { it.copy(permission = event.granted) }
                    is YouUiEvent.SystemRecognition -> preferences.update {
                        it.copy(systemRecognitionAllowed = event.allowed)
                    }
                    is YouUiEvent.Recognizer -> preferences.update {
                        it.copy(selectedRecognizer = event.id)
                    }
                    is YouUiEvent.DownloadModel -> voiceModels.download(event.language)
                    is YouUiEvent.ImportModel ->
                        channel.send(YouEffect.ModelImported(voiceModels.import(event.uri)))
                    is YouUiEvent.RemoveModel -> voiceModels.remove(event.language)
                    is YouUiEvent.RecheckModel -> voiceModels.revalidate(event.language)
                    is YouUiEvent.Topic -> if (event.title.isNotBlank()) {
                        if (event.id ==
                            null
                        ) {
                            topics.add(event.title)
                        } else {
                            topics.rename(event.id, event.title)
                        }
                    }
                    is YouUiEvent.Archive -> topics.archive(event.id)
                    is YouUiEvent.Open -> analytics.track(Event.ScreenViewed(event.route))
                    is YouUiEvent.Reset -> {
                        if (event.erase) erase() else reset()
                        channel.send(YouEffect.ResetDone(event.erase))
                    }
                    YouUiEvent.Demo -> {
                        demo.orElse(null)?.load()
                        channel.send(YouEffect.DemoDone)
                    }
                    YouUiEvent.Retry -> Unit
                }
                local.update { it.copy(failed = false) }
            } catch (
                e: CancellationException
            ) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                local.update { it.copy(failed = true) }
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }
}

private val reminderKeys =
    setOf(
        SettingKey.MORNING_TIME,
        SettingKey.EVENING_TIME,
        SettingKey.NOTIFY_MORNING,
        SettingKey.NOTIFY_FOCUS,
        SettingKey.NOTIFY_REVIEWS,
        SettingKey.NOTIFY_EVENING
    )
fun changePreference(
    p: UserPreferences,
    event: YouUiEvent.Change,
    permission: Boolean
): UserPreferences {
    val value = event.value
    return when (event.key) {
        SettingKey.MORNING_TIME -> p.copy(
            morningTime = java.time.LocalTime.of((value as SettingValue.Hour).hour, event.minute)
        )
        SettingKey.EVENING_TIME -> p.copy(
            eveningTime = java.time.LocalTime.of((value as SettingValue.Hour).hour, event.minute)
        )
        SettingKey.TIME_BUDGET -> p.copy(timeBudget = (value as SettingValue.Budget).budget)
        SettingKey.PACE -> p.copy(pace = (value as SettingValue.Pace).pace)
        SettingKey.THEME -> p.copy(theme = (value as SettingValue.Theme).theme)
        SettingKey.FOCUS_AREAS -> {
            val skill = (value as SettingValue.Area).skill
            p.copy(
                focusAreas = if (skill in
                    p.focusAreas
                ) {
                    p.focusAreas - skill
                } else if (p.focusAreas.size <
                    2
                ) {
                    p.focusAreas + skill
                } else {
                    p.focusAreas
                }
            )
        }
        SettingKey.NOTIFY_MORNING -> p.copy(
            notifyMorning =
            permission && (value as SettingValue.Toggle).enabled
        )
        SettingKey.NOTIFY_FOCUS -> p.copy(
            notifyFocus =
            permission && (value as SettingValue.Toggle).enabled
        )
        SettingKey.NOTIFY_REVIEWS -> p.copy(
            notifyReviews =
            permission && (value as SettingValue.Toggle).enabled
        )
        SettingKey.NOTIFY_EVENING -> p.copy(
            notifyEvening =
            permission && (value as SettingValue.Toggle).enabled
        )
        SettingKey.LANGUAGE -> p
    }
}
