@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.demo.DemoDataLoader
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.onboarding.CompleteOnboardingUseCase
import com.wivernz.itera.feature.daycomplete.DayCompleteViewModel
import com.wivernz.itera.feature.onboarding.OnboardingViewModel
import com.wivernz.itera.feature.reflection.ReflectionViewModel
import com.wivernz.itera.feature.today.TodayViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Main is one dedicated thread, as on a device: continuations hop back to it after Room work, so no view-model
 * code ends up blocking a Room thread.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = newSingleThreadContext("test-main")
    private val store = ViewModelStore()
    private var count = 0

    /** View models created in a test are cleared before the database closes and Main is reset. */
    fun <T : ViewModel> track(vm: T): T = vm.also { store.put("vm${count++}", it) }

    fun clearViewModels() {
        store.clear()
        // let cancelled continuations drain off the main thread before anything is torn down
        runBlocking { kotlinx.coroutines.withContext(dispatcher) { } }
        Thread.sleep(DRAIN_MILLIS)
        runBlocking { kotlinx.coroutines.withContext(dispatcher) { } }
    }

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
        current = this
    }
    override fun finished(description: Description) {
        clearViewModels()
        current = null
        Dispatchers.resetMain()
        dispatcher.close()
    }

    companion object {
        @Volatile var current: MainDispatcherRule? = null
    }
}

private fun <T : ViewModel> tracked(vm: T): T = MainDispatcherRule.current?.track(vm) ?: vm

/** Waits in real time for a state the view model reaches asynchronously. */
fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = runBlocking {
    withTimeout(AWAIT_MILLIS) { first(predicate) }
}

fun <T> Flow<T>.awaitFirst(): T = runBlocking { withTimeout(AWAIT_MILLIS) { first() } }

/** Keeps a `WhileSubscribed` state flow hot for the duration of a test. */
fun <T> StateFlow<T>.subscribe(): () -> Unit {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    scope.launch { collect { } }
    return { scope.cancel() }
}

fun <T> eventually(block: suspend () -> T?): T = runBlocking {
    withTimeout(AWAIT_MILLIS) {
        var value: T? = block()
        while (value == null) {
            kotlinx.coroutines.delay(POLL_MILLIS)
            value = block()
        }
        value
    }
}

fun reduceMotion() {
    Settings.Global.putFloat(
        ApplicationProvider.getApplicationContext<Application>().contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        0f
    )
}

fun EngineHarness.completeOnboarding() =
    CompleteOnboardingUseCase(prefs, unlock, ensureToday, reminders, analytics, TestLogger(), clock)

fun EngineHarness.onboardingViewModel(
    saved: SavedStateHandle = SavedStateHandle(),
    demo: DemoDataLoader? = null
) = OnboardingViewModel(
    saved,
    completeOnboarding(),
    prefs,
    catalog,
    Optional.ofNullable(demo),
    analytics
)

fun EngineHarness.todayViewModel() = tracked(
    TodayViewModel(ensureToday, plans, catalog, reviews, prefs, refresh, complete, analytics, clock)
)

fun EngineHarness.reflectionViewModel(activityId: Long) = tracked(
    ReflectionViewModel(
        SavedStateHandle(mapOf(ReflectionViewModel.ARG_ACTIVITY to activityId)),
        plans,
        catalog,
        progress,
        complete,
        skip,
        saveDraft,
        analytics
    )
)

fun EngineHarness.dayCompleteViewModel(dayId: Long) = tracked(
    DayCompleteViewModel(
        SavedStateHandle(mapOf(DayCompleteViewModel.ARG_DAY to dayId)),
        plans,
        catalog,
        prefs,
        reminders,
        analytics
    )
)

/** Plan-row fixture for pure mapper and screen tests. */
fun planActivity(
    id: Long,
    technique: String,
    source: ActivitySource,
    type: ExerciseType,
    state: ActivityState = ActivityState.AVAILABLE,
    order: Int = id.toInt(),
    title: String = technique,
    subtitle: String = "Now · 5 min",
    minutes: Int = 5,
    optional: Boolean = false,
    note: String? = null,
    result: ActivityResult? = null,
    reviewItemId: Long? = null,
    weeklyLookBack: Boolean = false,
    snoozedUntil: Instant? = null
) = PlanActivity(
    id = id,
    trainingDayId = 1,
    techniqueId = TechniqueId(technique),
    exerciseType = type,
    source = source,
    orderIndex = order,
    dayPart = when (source) {
        ActivitySource.REFLECTION -> DayPart.EVENING
        ActivitySource.FOCUS_SUGGESTION, ActivitySource.PRACTICE_PROMPT -> DayPart.DAYTIME
        else -> DayPart.MORNING
    },
    title = title,
    subtitle = subtitle,
    instruction = "",
    estimatedMinutes = minutes,
    state = state,
    optional = optional,
    scheduledAt = LocalTime.of(8, 30),
    snoozedUntil = snoozedUntil,
    startedAt = null,
    completedAt = null,
    durationSeconds = null,
    difficulty = null,
    note = note,
    result = result,
    reviewItemId = reviewItemId,
    weeklyLookBack = weeklyLookBack
)

fun trainingDay(
    activities: List<PlanActivity>,
    programDay: Int = 1,
    status: TrainingDayStatus = TrainingDayStatus.PLANNED,
    carryOver: String? = null,
    date: LocalDate = LocalDate.of(2026, 3, 28)
) = TrainingDay(1, programDay, date, status, carryOver, activities, null)

private const val AWAIT_MILLIS = 20_000L
private const val DRAIN_MILLIS = 100L
private const val POLL_MILLIS = 20L
