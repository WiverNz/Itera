@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import com.wivernz.itera.domain.training.RefreshAvailabilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private class Catalog(val techniques: Map<String, Technique>, val curriculumDays: Int)

private data class Local(
    val now: LocalTime,
    val failed: Boolean,
    val pending: Set<Long>,
    val logged: TodayTarget.LogPractice?
)

/**
 * Today (docs/ux/02-screen-specs-today.md). Ensures today's plan once in `init`; the header renders from the clock
 * and the stored program day before it resolves. Availability is re-evaluated on every emission and on resume.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val ensureToday: EnsureTodayPlanUseCase,
    private val plans: TrainingPlanRepository,
    catalog: TechniqueCatalogRepository,
    private val reviews: ReviewRepository,
    preferences: PreferencesRepository,
    private val refresh: RefreshAvailabilityUseCase,
    private val complete: CompleteActivityUseCase,
    private val analytics: Analytics,
    private val clock: Clock
) : ViewModel() {
    private val dayId = MutableStateFlow<Long?>(null)
    private val local = MutableStateFlow(Local(LocalTime.now(clock), false, emptySet(), null))
    private val undo = mutableMapOf<Long, Job>()

    private val day = dayId.flatMapLatest { id -> id?.let(plans::observeDay) ?: flowOf(null) }
        .onEach { it?.let(::refreshIfNeeded) }
        .catch { emit(null) }

    private val reviewTopics = day.mapLatest { day ->
        day?.activities.orEmpty()
            .filter { it.source == ActivitySource.REVIEW }
            .mapNotNull { it.reviewItemId }
            .mapNotNull { id ->
                reviews.item(id)?.takeIf { it.topicId != null }?.let { id to it.prompt }
            }
            .toMap()
    }.catch { emit(emptyMap()) }

    private val catalogRefresh = MutableStateFlow(0)
    private val catalogFlow = catalogRefresh.flatMapLatest {
        flow {
            emit(
                Catalog(
                    catalog.catalog().associateBy {
                        it.id.value
                    },
                    catalog.curriculum().days.size
                )
            )
        }.catch { emit(Catalog(emptyMap(), 0)) }
    }

    val state: StateFlow<TodayUiState> = combine(
        combine(day, reviewTopics) { d, t -> d to t },
        catalogFlow,
        preferences.preferences,
        local
    ) { (d, topics), cat, prefs, l ->
        mapToUiState(
            TodayInput(
                day = d,
                techniques = cat.techniques,
                curriculumDays = cat.curriculumDays,
                preferences = prefs,
                now = l.now,
                reviewTopics = topics,
                pendingPractice = l.pending,
                failed = l.failed,
                zone = clock.zone,
                loggedPractice = l.logged
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), TodayUiState())

    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.TODAY))
        ensure()
    }

    /** "Try again" on the error state. */
    fun retry() = ensure()

    /** ON_RESUME: greeting thresholds, day rollover and lazy availability. */
    fun onResume() {
        catalogRefresh.value++
        local.update { it.copy(now = LocalTime.now(clock)) }
        viewModelScope.launch {
            val current = dayId.value?.let { plans.day(it) }
            if (current == null || current.date != LocalDate.now(clock)) {
                ensure()
            } else {
                refreshIfNeeded(current)
            }
        }
    }

    /** Practice prompts complete in place; the commit waits out the undo window, so undo writes nothing. */
    fun logPractice(target: TodayTarget.LogPractice) {
        if (target.activityId in local.value.pending) return
        local.update { it.copy(pending = it.pending + target.activityId, logged = target) }
        undo[target.activityId] = viewModelScope.launch {
            delay(UNDO_WINDOW)
            commitPractice(target.activityId)
        }
    }

    fun undoPractice(activityId: Long) {
        undo.remove(activityId)?.cancel()
        local.update { it.copy(pending = it.pending - activityId, logged = null) }
    }

    fun practiceMessageShown() {
        local.update { it.copy(logged = null) }
    }

    private suspend fun commitPractice(activityId: Long) {
        undo.remove(activityId)
        val activity = plans.activity(activityId)
        complete(activityId, ActivityResult.Template(emptyMap())).onSuccess {
            activity?.let { a ->
                AnalyticsTechnique.of(a.techniqueId.value)?.let {
                    analytics.track(Event.PracticeLogged(it))
                }
            }
        }
        local.update { it.copy(pending = it.pending - activityId) }
    }

    private fun ensure() {
        local.update { it.copy(failed = false) }
        viewModelScope.launch {
            try {
                dayId.value = ensureToday().id
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                // Keep any stored checklist visible under the error card.
                dayId.value = runCatching { plans.dayByDate(LocalDate.now(clock))?.id }.getOrNull()
                    ?: dayId.value
                local.update { it.copy(failed = true) }
            }
        }
    }

    private fun refreshIfNeeded(day: TrainingDay) {
        if (day.date != LocalDate.now(clock)) return
        val waiting = day.activities.any {
            it.state == ActivityState.SCHEDULED || it.state == ActivityState.SNOOZED
        }
        if (waiting) viewModelScope.launch { runCatching { refresh(day.date) } }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
        val UNDO_WINDOW = 5.seconds
    }
}
