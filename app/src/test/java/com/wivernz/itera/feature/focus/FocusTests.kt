@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.focus

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.core.navigation.FocusSession
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.data.focus.DataStoreFocusTimerRepository
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.focus.FocusEnd
import com.wivernz.itera.domain.focus.FocusOutcome
import com.wivernz.itera.domain.focus.FocusRestoreResult
import com.wivernz.itera.domain.focus.FocusTimer
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.feature.FocusRig
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.MemoryStore
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.focusViewModel
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

private val T0: Instant = Instant.parse("2026-03-28T09:00:00Z")
private fun started(minutes: Int = 25) =
    FocusTimer.start(7, TechniqueId("pomodoro"), "Outline the roadmap", minutes, T0)

@RunWith(RobolectricTestRunner::class)
class FocusTimerStateTest {
    @Test fun remainingAtStartMidAndZero() {
        val s = started()
        assertEquals(1500, FocusTimer.remainingSeconds(s, T0))
        assertEquals(900, FocusTimer.remainingSeconds(s, T0.plusSeconds(600)))
        assertEquals(1, FocusTimer.remainingSeconds(s, T0.plusMillis(1_499_500)))
        assertEquals(0, FocusTimer.remainingSeconds(s, T0.plusSeconds(1500)))
        assertEquals(0, FocusTimer.remainingSeconds(s, T0.plusSeconds(5000)))
        assertTrue(FocusTimer.isFinished(s, T0.plusSeconds(1500)))
    }

    @Test fun pauseResumeShiftsEndsAtByExactlyThePause() {
        val paused = FocusTimer.pause(started(), T0.plusSeconds(100))
        assertEquals(1400, FocusTimer.remainingSeconds(paused, T0.plusSeconds(700)))
        assertFalse(FocusTimer.isFinished(paused, T0.plusSeconds(99_999)))
        val resumed = FocusTimer.resume(paused, T0.plusSeconds(700))
        assertEquals(T0.plusSeconds(2100), resumed.endsAt)
        assertEquals(600_000, resumed.accumulatedPauseMs)
        assertEquals(1400, FocusTimer.remainingSeconds(resumed, T0.plusSeconds(700)))
        assertEquals(resumed, FocusTimer.resume(resumed, T0.plusSeconds(800)))
    }

    @Test fun plusFiveIsAdditiveAndRepeatable() {
        val twice = FocusTimer.extend(FocusTimer.extend(started()))
        assertEquals(600, twice.extendedSeconds)
        assertEquals(2100, FocusTimer.totalSeconds(twice))
        assertEquals(2100, FocusTimer.remainingSeconds(twice, T0))
    }

    @Test fun sessionsUnderAMinuteRecordNothing() {
        assertEquals(FocusOutcome.TooShort, FocusTimer.outcome(started(), T0.plusSeconds(30)))
        assertEquals(
            FocusOutcome.Record(60, false),
            FocusTimer.outcome(started(), T0.plusSeconds(60))
        )
    }

    @Test fun naturalFinishAndEarlyEndSeconds() {
        val s = FocusTimer.extend(started())
        assertEquals(FocusOutcome.Record(1800, true), FocusTimer.outcome(s, T0.plusSeconds(1800)))
        val paused = FocusTimer.resume(
            FocusTimer.pause(started(), T0.plusSeconds(300)),
            T0.plusSeconds(500)
        )
        assertEquals(
            FocusOutcome.Record(400, false),
            FocusTimer.outcome(paused, T0.plusSeconds(600))
        )
    }

    @Test fun clockJumps() {
        val s = started()
        // backwards: still running against the original end instant, logged by the caller
        assertTrue(FocusTimer.jumpedBackwards(T0.plusSeconds(600), T0.plusSeconds(400)))
        assertFalse(FocusTimer.isFinished(s, T0.minusSeconds(3600)))
        assertEquals(1500, FocusTimer.remainingSeconds(s, T0.minusSeconds(3600)).coerceAtMost(1500))
        // forwards past endsAt: completes naturally with clamped seconds
        assertEquals(FocusOutcome.Record(1500, true), FocusTimer.outcome(s, T0.plusSeconds(90_000)))
    }

    @Test fun everyRestoreBranchAndTheDayOldDiscard() = runBlocking {
        val clock = FakeClock(T0)
        val repo =
            DataStoreFocusTimerRepository(MemoryStore<FocusTimerState?>(null), clock, TestLogger())
        assertEquals(FocusTimerRestore.None, repo.restore())
        repo.save(started())
        assertEquals(FocusTimerRestore.Running(started()), repo.restore())
        val paused = FocusTimer.pause(started(), T0)
        repo.save(paused)
        assertEquals(FocusTimerRestore.Paused(paused), repo.restore())
        repo.save(started())
        clock.advance(Duration.ofMinutes(30))
        assertEquals(FocusTimerRestore.AutoComplete(started()), repo.restore())
        clock.advance(Duration.ofHours(24))
        assertEquals(FocusTimerRestore.Discarded, repo.restore())
        assertNull(repo.restore().takeIf { it != FocusTimerRestore.None })
    }
}

@RunWith(RobolectricTestRunner::class)
class FocusViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()
    private val rig = FocusRig(h)

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    private fun pomodoro() = h.plant("pomodoro", ExerciseType.FOCUS_TIMER)

    @Test fun sheetIsSeededFromTheTechnique() {
        val state = h.focusViewModel(rig, pomodoro(), 50, "deep_work").state.await { !it.loading }
        assertEquals(50, state.minutes)
        assertEquals(listOf(15, 25, 50), state.minuteOptions)
        assertNull(state.timer)
    }

    @Test fun startAndEachControl() {
        val id = pomodoro()
        val vm = h.focusViewModel(rig, id, 25, "pomodoro")
        vm.state.await { !it.loading }
        vm.setTask("Outline the roadmap")
        vm.setMinutes(15)
        vm.start()
        assertEquals(900, vm.state.await { it.timer != null }.timer!!.totalSeconds)
        assertEquals(ActivityState.IN_PROGRESS, runBlocking { h.plans.activity(id) }!!.state)
        assertTrue(rig.launcher.updates.last()!!.pausedAt == null)
        vm.extend()
        assertEquals(
            1200,
            vm.state.await {
                it.timer?.totalSeconds == 1200
            }.timer!!.remainingSeconds
        )
        vm.togglePause()
        vm.state.await { it.timer?.paused == true }
        assertTrue(rig.launcher.updates.last()!!.pausedAt != null)
        h.clock.advance(Duration.ofMinutes(3))
        vm.togglePause()
        assertEquals(1200, vm.state.await { it.timer?.paused == false }.timer!!.remainingSeconds)
    }

    @Test fun earlyEndConfirmsThenRecords() {
        val id = pomodoro()
        val vm = h.focusViewModel(rig, id, 25, "pomodoro")
        vm.state.await { !it.loading }
        vm.setTask("Report")
        vm.start()
        vm.state.await { it.timer != null }
        h.clock.advance(Duration.ofMinutes(10))
        vm.requestEnd()
        assertTrue(vm.state.value.confirmingEnd)
        vm.dismissEnd()
        assertFalse(vm.state.value.confirmingEnd)
        vm.requestEnd()
        vm.confirmEnd()
        assertEquals(
            FocusEffect.Completed(id, "pomodoro", chainStep = false, natural = false),
            vm.effects.awaitFirst()
        )
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Focus
        assertEquals(600, result.actualSeconds)
        assertFalse(result.completedNaturally)
        assertEquals(600L, runBlocking { h.db.focusSessionDao().totalSeconds().first() })
        assertNull(rig.launcher.updates.last())
    }

    @Test fun underAMinuteWritesNothing() {
        val id = pomodoro()
        val vm = h.focusViewModel(rig, id, 25, "pomodoro")
        vm.state.await { !it.loading }
        vm.setTask("Report")
        vm.start()
        vm.state.await { it.timer != null }
        h.clock.advance(Duration.ofSeconds(30))
        vm.confirmEnd()
        assertEquals(FocusEffect.TooShort, vm.effects.awaitFirst())
        assertEquals(ActivityState.AVAILABLE, runBlocking { h.plans.activity(id) }!!.state)
        assertEquals(0L, runBlocking { h.db.focusSessionDao().totalSeconds().first() })
    }

    @Test fun naturalFinishCompletesOnTheTick() {
        val id = pomodoro()
        val vm = h.focusViewModel(rig, id, 25, "pomodoro")
        vm.state.await { !it.loading }
        vm.setTask("Report")
        vm.start()
        vm.state.await { it.timer != null }
        h.clock.advance(Duration.ofMinutes(26))
        assertEquals(
            FocusEffect.Completed(id, "pomodoro", chainStep = false, natural = true),
            vm.effects.awaitFirst()
        )
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Focus
        assertEquals(1500, result.actualSeconds)
        assertTrue(result.completedNaturally)
    }
}

@RunWith(RobolectricTestRunner::class)
class FocusScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private var extended = 0
    private var toggled = 0
    private var ended = 0

    private fun show(state: FocusUiState) = compose.setContent {
        IteraTheme(dark = false) {
            NightSurface {
                FocusScreen(state, true, {
                }, {}, {}, {}, { extended++ }, { toggled++ }, { ended++ }, {}, {})
            }
        }
    }

    @Test fun controlsRenderAndFire() {
        show(FocusUiState(loading = false, timer = FocusTimerUi("Outline", 1200, 1500, false)))
        compose.onNodeWithText("20:00").assertExists()
        compose.onNodeWithText("Itera notifications paused").assertExists()
        compose.onNodeWithTag("FocusRing").assertExists()
        compose.onNodeWithTag("FocusExtend").performClick()
        compose.onNodeWithTag("FocusToggle").performClick()
        compose.onNodeWithTag("FocusEnd").performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1), listOf(extended, toggled, ended)) }
    }

    @Test fun endConfirmationAppears() {
        show(
            FocusUiState(
                loading = false,
                timer = FocusTimerUi("Outline", 1200, 1500, false),
                confirmingEnd = true
            )
        )
        compose.onNodeWithText("End session?").assertExists()
    }

    @Test fun readoutAnnouncesOnlyAtThresholds() {
        show(FocusUiState(loading = false, timer = FocusTimerUi("Outline", 1200, 1500, false)))
        compose.onNodeWithTag(
            "FocusReadout"
        ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
        compose.onNodeWithTag(
            "FocusAnnouncement"
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("")))
    }

    @Test fun readoutAnnouncesFiveMinutes() {
        show(FocusUiState(loading = false, timer = FocusTimerUi("Outline", 299, 1500, false)))
        compose.onNodeWithTag(
            "FocusAnnouncement"
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("5 minutes left")
            )
        )
    }

    @Test fun setupStartsDisabledWithoutATask() {
        show(FocusUiState(loading = false))
        compose.onNodeWithTag("FocusSetup").assertExists()
        compose.onNodeWithTag(
            "FocusStart"
        ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test fun screenIsDarkUnderALightTheme() {
        var dark = false
        compose.setContent {
            IteraTheme(dark = false) {
                NightSurface {
                    dark = com.wivernz.itera.core.designsystem.theme.Itera.colors.isDark
                }
            }
        }
        compose.runOnIdle { assertTrue(dark) }
    }
}

@RunWith(RobolectricTestRunner::class)
class FocusTimerServiceTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun serviceStartsWithTheTimerAndStopsOnPauseAndEnd() {
        val launcher = AndroidFocusServiceLauncher(app, TestLogger())
        val running = started()
        launcher.update(running)
        val start = shadowOf(app).nextStartedService
        assertEquals(FocusSessionService::class.java.name, start.component!!.className)
        assertEquals(running.activityId, FocusNotificationSnapshot.from(start)!!.activityId)
        launcher.update(FocusTimer.pause(running, T0))
        assertEquals(
            FocusSessionService::class.java.name,
            shadowOf(app).nextStoppedService.component!!.className
        )
        launcher.update(null)
        assertEquals(
            FocusSessionService::class.java.name,
            shadowOf(app).nextStoppedService.component!!.className
        )
    }

    @Test fun notificationUsesAChronometerAndDeepLinksToTheSession() {
        val snapshot = FocusNotificationSnapshot.of(started())
        val notification = FocusNotifications.running(app, snapshot)
        assertEquals(snapshot.endsAtMillis, notification.`when`)
        assertTrue(notification.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(
            notification.extras.getBoolean(android.app.Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        )
        assertEquals(2, notification.actions.size)
        val intent: Intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(
            FocusSession(7, 25, "pomodoro"),
            RouteCodec.decode(intent.getStringExtra(RouteCodec.EXTRA)!!)
        )
    }

    @Test fun processDeathRestoresWithinTolerance() = runBlocking {
        val rig = FocusRig(h)
        val id = h.plant("pomodoro", ExerciseType.FOCUS_TIMER)
        rig.controller.start(id, TechniqueId("pomodoro"), "Report", 25).getOrThrow()
        h.clock.advance(Duration.ofMinutes(10))
        // a new controller over the same stored file stands in for the relaunched process
        val relaunched = FocusRig(h).also { it.store.state.value = rig.store.state.value }
        val restored = relaunched.controller.restore() as FocusRestoreResult.Attach
        assertEquals(900, FocusTimer.remainingSeconds(restored.state, h.clock.instant()), 2)
        assertTrue(relaunched.launcher.updates.single() != null)
    }

    @Test fun elapsedWhileDeadAutoCompletes() = runBlocking {
        val rig = FocusRig(h)
        val id = h.plant("pomodoro", ExerciseType.FOCUS_TIMER)
        rig.controller.start(id, TechniqueId("pomodoro"), "Report", 25).getOrThrow()
        h.clock.advance(Duration.ofMinutes(40))
        val finished = rig.controller.restore() as FocusRestoreResult.Finished
        assertEquals(FocusEnd.Completed(id, TechniqueId("pomodoro"), true), finished.end)
        assertEquals(ActivityState.COMPLETED, h.plans.activity(id)!!.state)
        assertNull(rig.controller.current())
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) = assertTrue(
        "$actual not within $tolerance of $expected",
        kotlin.math.abs(expected - actual) <= tolerance
    )
}
