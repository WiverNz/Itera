package com.wivernz.itera.feature

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.TestLogger
import com.wivernz.itera.data.focus.DataStoreFocusTimerRepository
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.coach.NoOpCoachFeedbackProvider
import com.wivernz.itera.domain.focus.FocusServiceLauncher
import com.wivernz.itera.domain.focus.FocusSessionController
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.feature.exercise.combination.CombinationViewModel
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerViewModel
import com.wivernz.itera.feature.exercise.feynman.FeynmanViewModel
import com.wivernz.itera.feature.exercise.habitstack.HabitStackViewModel
import com.wivernz.itera.feature.exercise.premortem.PremortemViewModel
import com.wivernz.itera.feature.exercise.review.ReviewViewModel
import com.wivernz.itera.feature.exercise.runner.ExerciseResultViewModel
import com.wivernz.itera.feature.exercise.runner.ExerciseRunnerViewModel
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.focus.FocusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

/** A DataStore held in memory, for the real focus-timer repository. */
class MemoryStore<T>(initial: T) : DataStore<T> {
    val state = MutableStateFlow(initial)
    override val data = state
    override suspend fun updateData(transform: suspend (t: T) -> T): T =
        transform(state.value).also { state.value = it }
}

class RecordingLauncher : FocusServiceLauncher {
    val updates = mutableListOf<FocusTimerState?>()
    override fun update(state: FocusTimerState?) {
        updates += state
    }
}

/** The real controller over an in-memory `focus_timer.pb`. */
class FocusRig(val h: EngineHarness) {
    val store = MemoryStore<FocusTimerState?>(null)
    val timers = DataStoreFocusTimerRepository(store, h.clock, TestLogger())
    val launcher = RecordingLauncher()
    val controller = FocusSessionController(
        timers, h.plans, h.start, h.abandon, h.complete, launcher, h.analytics, h.clock,
        TestLogger()
    )
}

fun EngineHarness.sessionDeps() =
    ExerciseSessionDeps(plans, catalog, start, abandon, complete, saveDraft, analytics)

private fun args(activityId: Long) = SavedStateHandle(mapOf("activityId" to activityId))

private fun <T : androidx.lifecycle.ViewModel> tracked(vm: T): T =
    MainDispatcherRule.current?.track(vm) ?: vm

fun EngineHarness.runnerViewModel(activityId: Long) = tracked(
    ExerciseRunnerViewModel(
        args(
            activityId
        ),
        plans, catalog, start, snooze, abandon, complete, saveDraft, analytics, clock
    )
)

fun EngineHarness.resultViewModel(activityId: Long) = tracked(
    ExerciseResultViewModel(args(activityId), plans, catalog, reviews, observeTechniques)
)

fun EngineHarness.focusViewModel(rig: FocusRig, activityId: Long, minutes: Int, technique: String) =
    tracked(
        FocusViewModel(
            SavedStateHandle(
                mapOf("activityId" to activityId, "minutes" to minutes, "technique" to technique)
            ),
            rig.controller,
            plans,
            analytics,
            clock,
            TestLogger()
        )
    )

fun EngineHarness.eisenhowerViewModel(
    activityId: Long,
    saved: SavedStateHandle = args(activityId)
) = tracked(EisenhowerViewModel(saved, sessionDeps()))

fun EngineHarness.feynmanViewModel(activityId: Long) = tracked(
    FeynmanViewModel(args(activityId), sessionDeps(), topics, reviews, NoOpCoachFeedbackProvider())
)

fun EngineHarness.reviewViewModel(activityId: Long) =
    tracked(ReviewViewModel(args(activityId), sessionDeps(), reviews, submitReview, clock))

fun EngineHarness.premortemViewModel(activityId: Long) =
    tracked(PremortemViewModel(args(activityId), sessionDeps(), states, clock))

fun EngineHarness.habitStackViewModel(activityId: Long) =
    tracked(HabitStackViewModel(args(activityId), sessionDeps(), prefs))

fun EngineHarness.combinationViewModel(activityId: Long) = tracked(
    CombinationViewModel(args(activityId), plans, catalog, start, complete, saveDraft, clock)
)

/** Unlocks [technique] and adds an available MANUAL activity for it to today's plan. */
fun EngineHarness.plant(
    technique: String,
    type: ExerciseType,
    source: ActivitySource = ActivitySource.MANUAL
): Long = runBlocking {
    val today = ensureToday()
    states.unlock(TechniqueId(technique), clock.instant(), 1)
    plans.insertActivity(
        today.id,
        PlannedActivity(
            techniqueId = TechniqueId(technique),
            exerciseType = type,
            source = source,
            dayPart = DayPart.MORNING,
            orderIndex = today.activities.size + 10,
            copyKey = "activity_program",
            copyArgs = linkedMapOf("technique" to technique, "minutes" to 5),
            estimatedMinutes = 5,
            optional = true,
            state = ActivityState.AVAILABLE,
            scheduledAt = null
        )
    )
}

/** Trains whole days until the plan for [programDay] is today's. */
fun EngineHarness.reachProgramDay(programDay: Int) = runBlocking {
    while (programDay() < programDay) {
        trainFullDay()
        nextMorning()
    }
    ensureToday()
}
