@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.training.AddManualPracticeUseCase
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.exercise.runner.bodyFor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PracticeEntry(
    val id: Long,
    val date: LocalDate,
    val summary: PracticeSummary,
    val note: String?
)
sealed interface PracticeSummary {
    data class Review(val grade: com.wivernz.itera.domain.model.RecallGrade) : PracticeSummary
    data class Text(val value: String) : PracticeSummary
    data class Count(val kind: PracticeKind, val count: Int) : PracticeSummary
    data class Topic(val title: String) : PracticeSummary
    data class Habit(val anchor: String, val habit: String) : PracticeSummary
}
enum class PracticeKind { MINUTES, TASKS, REASONS, STEPS, ITEMS }
fun practiceSummary(result: ActivityResult?, fallback: String): PracticeSummary = when (result) {
    is ActivityResult.Focus -> PracticeSummary.Count(
        PracticeKind.MINUTES,
        result.actualSeconds / 60
    )
    is ActivityResult.Eisenhower -> PracticeSummary.Count(
        PracticeKind.TASKS,
        result.items.count {
            it.quadrant !=
                Quadrant.UNSORTED
        }
    )
    is ActivityResult.Feynman -> PracticeSummary.Topic(result.topicTitle)
    is ActivityResult.Premortem -> PracticeSummary.Count(PracticeKind.REASONS, result.reasons.size)
    is ActivityResult.HabitStack -> PracticeSummary.Habit(result.anchor, result.habit)
    is ActivityResult.Combination -> PracticeSummary.Count(
        PracticeKind.STEPS,
        result.stepResults.size
    )
    is ActivityResult.Template -> {
        val items = result.values.values.filterIsInstance<BlockValue.Items>()
        if (items.isNotEmpty()) {
            PracticeSummary.Count(
                PracticeKind.ITEMS,
                items.sumOf { v ->
                    v.items.count { it.done }
                }
            )
        } else {
            PracticeSummary.Text(
                result.values.values.filterIsInstance<BlockValue.Text>().firstOrNull {
                    it.text.isNotBlank()
                }?.text
                    ?: fallback
            )
        }
    }
    is ActivityResult.Reflection -> PracticeSummary.Text(
        result.tomorrowChange?.takeIf {
            it.isNotBlank()
        } ?: fallback
    )
    is ActivityResult.Review -> PracticeSummary.Review(result.grade)
    null -> PracticeSummary.Text(fallback)
}
data class TechniqueDetailUiState(
    val technique: Technique? = null,
    val progress: TechniqueProgress? = null,
    val related: List<Technique> = emptyList(),
    val history: List<PracticeEntry> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val historyFailed: Boolean = false,
    val busy: Boolean = false,
    val practiceFailed: Boolean = false
)
sealed interface TechniqueDetailUiEvent {
    data object Practice : TechniqueDetailUiEvent
    data class Related(val id: TechniqueId) : TechniqueDetailUiEvent
    data object Retry : TechniqueDetailUiEvent
    data object Returned : TechniqueDetailUiEvent
}
sealed interface TechniqueDetailEffect {
    data class Practice(val activityId: Long, val technique: String, val body: ExerciseBody) :
        TechniqueDetailEffect
    data class Related(val id: TechniqueId) : TechniqueDetailEffect
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TechniqueDetailViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val catalog: TechniqueCatalogRepository,
    progress: ObserveTechniqueProgressUseCase,
    history: ProgressRepository,
    private val addPractice: AddManualPracticeUseCase,
    analytics: Analytics
) : ViewModel() {
    private val id = TechniqueId(checkNotNull(saved.get<String>("technique")))
    private val retry = MutableStateFlow(0)
    private val local = MutableStateFlow(false to false)
    private val channel = Channel<TechniqueDetailEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    private var pendingId: Long? = saved["pendingPractice"]
    private var departed = false
    val state = retry.flatMapLatest {
        combine(
            flow { emit(catalog.catalog()) },
            progress.of(id),
            history.observeRecentPractice(id).map {
                it to false
            }.catch { emit(emptyList<HistoryEntry>() to true) },
            local
        ) { techniques, facts, recent, action ->
            val technique = techniques.firstOrNull { it.id == id && !it.retired }
            TechniqueDetailUiState(
                technique, facts,
                technique?.relatedTechniqueIds.orEmpty().mapNotNull { related ->
                    techniques.firstOrNull {
                        it.id ==
                            related &&
                            !it.retired
                    }
                },
                recent.first.sortedWith(
                    compareByDescending<HistoryEntry> {
                        it.activity.completedAt
                    }.thenByDescending { it.activity.id }
                ).take(5).map {
                    PracticeEntry(
                        it.activity.id,
                        it.date,
                        practiceSummary(it.activity.result, technique?.name.orEmpty()),
                        it.activity.note
                    )
                },
                loading = false, failed = technique == null, historyFailed = recent.second,
                busy = action.first, practiceFailed = action.second
            )
        }.catch { emit(TechniqueDetailUiState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TechniqueDetailUiState())
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.TECHNIQUE))
    }
    fun onEvent(event: TechniqueDetailUiEvent) {
        when (event) {
            TechniqueDetailUiEvent.Retry -> retry.value++
            TechniqueDetailUiEvent.Returned -> if (departed) {
                departed = false
                pendingId = null
                saved["pendingPractice"] = null
                local.value = false to false
            }
            is TechniqueDetailUiEvent.Related -> viewModelScope.launch {
                channel.send(TechniqueDetailEffect.Related(event.id))
            }
            TechniqueDetailUiEvent.Practice -> practice()
        }
    }
    private fun practice() {
        val technique = state.value.technique ?: return
        if (state.value.progress?.unlocked != true || local.value.first) return
        local.value = true to false
        viewModelScope.launch {
            try {
                val activityId = pendingId ?: addPractice(id).getOrThrow().also {
                    pendingId = it
                    saved["pendingPractice"] = it
                }
                val body = bodyFor(technique.exerciseType, technique).let {
                    if (it ==
                        ExerciseBody.Generic
                    ) {
                        ExerciseBody.Template
                    } else {
                        it
                    }
                }
                departed = true
                channel.send(TechniqueDetailEffect.Practice(activityId, id.value, body))
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                local.value =
                    false to true
            }
        }
    }
}
