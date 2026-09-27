package com.wivernz.itera.feature.voice

import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.TestLogger
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.feature.FocusRig
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.focus.FocusViewModel
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.today.TodayHero
import com.wivernz.itera.feature.today.TodayStep
import com.wivernz.itera.feature.today.TodayTarget
import com.wivernz.itera.feature.today.TodayUiState
import com.wivernz.itera.feature.today.TodayVoiceHost
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Voice focus commands go through the same view-model actions and the one timer controller as touch. */
@RunWith(RobolectricTestRunner::class)
class VoiceFocusIntegrationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()
    private val rig = FocusRig(h)

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    private fun vm(id: Long, seed: Int = 25, requested: Int = 0) = main.track(
        FocusViewModel(
            SavedStateHandle(
                mapOf(
                    FocusViewModel.ARG_ACTIVITY to id,
                    FocusViewModel.ARG_MINUTES to seed,
                    FocusViewModel.ARG_TECHNIQUE to "pomodoro",
                    FocusViewModel.ARG_REQUESTED to requested
                )
            ),
            rig.controller,
            h.plans,
            h.analytics,
            h.clock,
            TestLogger()
        )
    ).also { it.state.await { s -> !s.loading } }

    private fun run(vm: FocusViewModel, command: VoiceCommand): Any = runBlocking {
        when (val plan = vm.planVoice(command)) {
            is VoicePlan.Run -> vm.executeVoice(plan.action)
            else -> plan
        }
    }

    private fun pomodoro() = h.plant("pomodoro", ExerciseType.FOCUS_TIMER)

    @Test fun defaultStartKeepsTheSuggestedLength() {
        val vm = vm(pomodoro())
        vm.setTask("Draft the brief")
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.FocusStarted),
            run(vm, VoiceCommand.StartFocus(null))
        )
        assertEquals(1500, vm.state.await { it.timer != null }.timer!!.totalSeconds)
    }

    @Test fun specifiedStartUsesAnOfferedLength() {
        val vm = vm(pomodoro())
        vm.setTask("Review notes")
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.FocusStarted),
            run(vm, VoiceCommand.StartFocus(50))
        )
        assertEquals(3000, vm.state.await { it.timer != null }.timer!!.totalSeconds)
    }

    @Test fun unavailableDurationsAreExplainedNeverRounded() {
        val vm = vm(pomodoro())
        vm.setTask("Plan")
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusDuration(40, listOf(15, 25, 50))),
            vm.planVoice(VoiceCommand.StartFocus(40))
        )
        assertEquals(25, vm.state.value.minutes)
        assertNull(vm.state.value.timer)
        assertNull(runBlocking { rig.controller.current() })
    }

    @Test fun missingTaskOpensTheSetupWithTheParsedLength() {
        val vm = vm(pomodoro())
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.FocusSetup(15)),
            run(vm, VoiceCommand.StartFocus(15))
        )
        assertEquals(15, vm.state.value.minutes)
        assertNull(runBlocking { rig.controller.current() })
    }

    @Test fun routeRequestedLengthsPreselectOrExplain() {
        assertEquals(50, vm(pomodoro(), requested = 50).state.value.minutes)
        val unsupported = vm(pomodoro(), requested = 40).state.value
        assertEquals(25, unsupported.minutes)
        assertEquals(40, unsupported.unsupportedMinutes)
    }

    @Test fun pauseResumeAndConfirmedEndKeepTheTimerRules() {
        val id = pomodoro()
        val vm = vm(id)
        vm.setTask("Deep work")
        vm.start()
        vm.state.await { it.timer != null }
        assertEquals(
            setOf(
                VoiceCommandKind.PAUSE_FOCUS,
                VoiceCommandKind.RESUME_FOCUS,
                VoiceCommandKind.END_FOCUS
            ),
            vm.voiceCommands
        )
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusActive),
            vm.planVoice(VoiceCommand.StartFocus(25))
        )
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusNotPaused),
            vm.planVoice(VoiceCommand.ResumeFocus)
        )
        assertEquals(VoiceOutcome.Done(VoiceFeedback.FocusPaused), run(vm, VoiceCommand.PauseFocus))
        vm.state.await { it.timer?.paused == true }
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusAlreadyPaused),
            vm.planVoice(VoiceCommand.PauseFocus)
        )
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.FocusResumed),
            run(vm, VoiceCommand.ResumeFocus)
        )
        vm.state.await { it.timer?.paused == false }

        // EndFocus only raises the existing confirmation; nothing ends until it is confirmed
        assertEquals(VoiceOutcome.Done(VoiceFeedback.Handover), run(vm, VoiceCommand.EndFocus))
        assertTrue(vm.state.value.confirmingEnd)
        assertEquals(ActivityState.IN_PROGRESS, runBlocking { h.plans.activity(id) }!!.state)
        // under a minute: the existing discard rule, nothing recorded
        vm.confirmEnd()
        eventually {
            runBlocking { h.plans.activity(id) }?.takeIf {
                it.state ==
                    ActivityState.AVAILABLE
            }
        }
        assertNull(runBlocking { rig.controller.current() })
    }

    @Test fun confirmedEndAfterAMinuteRecordsTheSession() {
        val id = pomodoro()
        val vm = vm(id)
        vm.setTask("Write")
        vm.start()
        vm.state.await { it.timer != null }
        h.clock.advance(Duration.ofMinutes(3))
        run(vm, VoiceCommand.EndFocus)
        vm.confirmEnd()
        eventually {
            runBlocking { h.plans.activity(id) }?.takeIf {
                it.state ==
                    ActivityState.COMPLETED
            }
        }
    }

    @Test fun aNaturalFinishBeforeConfirmationMakesTheVoiceActionStale() {
        val vm = vm(pomodoro())
        vm.setTask("Sprint")
        vm.start()
        vm.state.await { it.timer != null }
        val plan = vm.planVoice(VoiceCommand.PauseFocus) as VoicePlan.Run
        h.clock.advance(Duration.ofMinutes(26))
        runBlocking { rig.controller.finishIfElapsed() }
        assertEquals(
            VoiceOutcome.Rejected(VoiceRejection.Stale),
            runBlocking {
                vm.executeVoice(plan.action)
            }
        )
        assertNull(runBlocking { rig.controller.current() })
    }

    @Test fun commandsWithoutASessionChangeNothing() {
        val vm = vm(pomodoro())
        assertEquals(setOf(VoiceCommandKind.START_FOCUS), vm.voiceCommands)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusNotRunning),
            vm.planVoice(VoiceCommand.PauseFocus)
        )
        assertEquals(
            VoicePlan.Reject(VoiceRejection.FocusNotRunning),
            vm.planVoice(VoiceCommand.EndFocus)
        )
        assertTrue(rig.launcher.updates.isEmpty())
    }

    // ------------------------------------------------------------------ Today's focus entry

    private val focus = TodayTarget.Focus(9, 30, "pomodoro")

    private fun today(hero: TodayHero?, steps: List<TodayStep> = emptyList()) =
        TodayUiState(hero = hero, steps = steps, loading = false)

    @Test fun todayStartsOnlyFromItsActionableFocusEntry() {
        val opened = mutableListOf<Pair<TodayTarget.Focus, Int>>()
        var state = today(TodayHero.Focus(30, enabled = true, target = focus))
        val host = TodayVoiceHost({ state }) { t, m -> opened += t to m }
        assertEquals(
            VoicePlan.Run(VoiceAction.StartFocus(25)),
            host.planVoice(VoiceCommand.StartFocus(25))
        )
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.Handover),
            runBlocking {
                host.executeVoice(VoiceAction.StartFocus(25))
            }
        )
        runBlocking { host.executeVoice(VoiceAction.StartFocus(null)) }
        assertEquals(listOf(focus to 25, focus to 0), opened)

        state = today(TodayHero.Focus(30, enabled = false, target = focus))
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NoFocusEntry),
            host.planVoice(VoiceCommand.StartFocus(null))
        )
        state = today(null).copy(loading = true)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NoFocusEntry),
            host.planVoice(VoiceCommand.StartFocus(null))
        )
        assertEquals(2, opened.size)
    }
}
