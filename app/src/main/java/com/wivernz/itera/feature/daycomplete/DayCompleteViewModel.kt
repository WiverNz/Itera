package com.wivernz.itera.feature.daycomplete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the day actually contained, in the order the summary names it. */
enum class DayPartKind { EXERCISE, REVIEW, FOCUS, PRACTICE, REFLECTION }

/** One arc of the ring: a row of the day, filled when it was completed. */
data class RingSegment(val skill: Skill, val done: Boolean)

sealed interface TomorrowPreview {
    data class Technique(val techniqueId: String, val name: String, val skill: Skill) :
        TomorrowPreview
    data object Combination : TomorrowPreview

    /** Past the authored curriculum: tomorrow is a practice day chosen when it is generated. */
    data object Practice : TomorrowPreview
}

data class DayCompleteUiState(
    val loading: Boolean = true,
    val programDay: Int = 1,
    val segments: List<RingSegment> = emptyList(),
    val done: List<DayPartKind> = emptyList(),
    val reflectionSkipped: Boolean = false,
    val change: String? = null,
    val tomorrow: TomorrowPreview = TomorrowPreview.Practice,
    val morningTime: LocalTime = LocalTime.of(8, 30)
) {
    val completedCount: Int get() = segments.count { it.done }

    /** "Exercise, focus, reflection" - the prototype's full-day line - only when that is literally true. */
    val fullDay: Boolean
        get() = !reflectionSkipped &&
            done == listOf(DayPartKind.EXERCISE, DayPartKind.FOCUS, DayPartKind.REFLECTION)
}

/**
 * Day complete (docs/ux/02-screen-specs-exercise.md section 6). Reads the curriculum for tomorrow's preview and
 * never generates tomorrow's plan. Arriving records `last_seen_day_complete` and cancels the day's reminders.
 */
@HiltViewModel
class DayCompleteViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val preferences: PreferencesRepository,
    private val reminders: ReminderScheduler,
    analytics: Analytics
) : ViewModel() {
    private val dayId: Long = checkNotNull(saved.get<Long>(ARG_DAY))
    private val mutable = MutableStateFlow(DayCompleteUiState())
    val state: StateFlow<DayCompleteUiState> = mutable.asStateFlow()

    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.DAY_COMPLETE))
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val day = plans.day(dayId) ?: return
        val prefs = preferences.preferences.first()
        val techniques = runCatching { catalog.catalog() }.getOrDefault(emptyList())
        val curriculum = runCatching { catalog.curriculum() }.getOrNull()
        mutable.value = summarize(day, techniques, curriculum, prefs.morningTime)
        if (prefs.lastSeenDayComplete != day.date) {
            preferences.update { it.copy(lastSeenDayComplete = day.date) }
        }
        day.activities.filter { it.state in OPEN_STATES }.forEach {
            reminders.cancelForActivity(it.id)
        }
    }

    companion object {
        const val ARG_DAY = "dayId"
        private val OPEN_STATES = setOf(
            ActivityState.SCHEDULED,
            ActivityState.AVAILABLE,
            ActivityState.IN_PROGRESS,
            ActivityState.SNOOZED
        )

        /** Pure summary of a finished day. */
        fun summarize(
            day: TrainingDay,
            techniques: List<Technique>,
            curriculum: Curriculum?,
            morningTime: LocalTime
        ): DayCompleteUiState {
            val byId = techniques.associateBy { it.id.value }
            val rows = day.activities
                .filter { it.state != ActivityState.EXPIRED && !it.isCombinationStep }
                .sortedBy { it.orderIndex }
            val reflection = rows.firstOrNull { it.source == ActivitySource.REFLECTION }
            val done = rows.filter { it.state == ActivityState.COMPLETED }
                .map(::kindOf).distinct().sortedBy { it.ordinal }
            return DayCompleteUiState(
                loading = false,
                programDay = day.programDay,
                segments = rows.map {
                    RingSegment(
                        skillOf(it, byId),
                        it.state == ActivityState.COMPLETED
                    )
                },
                done = done,
                reflectionSkipped = reflection?.state == ActivityState.SKIPPED,
                change = (reflection?.result as? ActivityResult.Reflection)?.tomorrowChange
                    ?.takeIf { it.isNotBlank() },
                tomorrow = tomorrow(day.programDay + 1, curriculum, byId),
                morningTime = morningTime
            )
        }

        /** Reads `curriculum.days[programDay]`, the already-advanced day. */
        fun tomorrow(
            programDay: Int,
            curriculum: Curriculum?,
            byId: Map<String, Technique>
        ): TomorrowPreview {
            val entry =
                curriculum?.days?.getOrNull(programDay - 1) ?: return TomorrowPreview.Practice
            if (entry.combination.isNotEmpty()) return TomorrowPreview.Combination
            val technique =
                entry.newTechniqueId?.value?.let(byId::get) ?: return TomorrowPreview.Practice
            return TomorrowPreview.Technique(technique.id.value, technique.name, technique.skill)
        }

        private fun kindOf(activity: PlanActivity): DayPartKind = when {
            activity.source == ActivitySource.REFLECTION -> DayPartKind.REFLECTION
            activity.source == ActivitySource.REVIEW -> DayPartKind.REVIEW
            activity.source == ActivitySource.FOCUS_SUGGESTION -> DayPartKind.FOCUS
            activity.source == ActivitySource.PROGRAM ||
                activity.source == ActivitySource.COMBINATION -> DayPartKind.EXERCISE
            activity.exerciseType == ExerciseType.FOCUS_TIMER -> DayPartKind.FOCUS
            else -> DayPartKind.PRACTICE
        }

        private fun skillOf(activity: PlanActivity, byId: Map<String, Technique>): Skill =
            when (activity.source) {
                ActivitySource.FOCUS_SUGGESTION -> Skill.FOCUS
                ActivitySource.REFLECTION -> Skill.REFLECTION
                else -> byId[activity.techniqueId.value]?.skill ?: Skill.HABITS
            }
    }
}
