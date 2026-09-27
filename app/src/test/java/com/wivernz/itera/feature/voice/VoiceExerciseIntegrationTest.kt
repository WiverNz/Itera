package com.wivernz.itera.feature.voice

import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceItem
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.combinationViewModel
import com.wivernz.itera.feature.eisenhowerViewModel
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.exercise.runner.ExerciseRunnerViewModel
import com.wivernz.itera.feature.exercise.template.CompletionGate
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.premortemViewModel
import com.wivernz.itera.feature.reachProgramDay
import com.wivernz.itera.feature.runnerViewModel
import com.wivernz.itera.feature.today.TodayUiState
import com.wivernz.itera.feature.today.TodayVoiceHost
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Voice uses each exercise's own actions and gates; it never searches beyond the active list. */
@RunWith(RobolectricTestRunner::class)
class VoiceExerciseIntegrationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    private val twoMinuteId: Long
        get() = runBlocking { h.ensureToday() }.activities.first {
            it.techniqueId.value == "two_minute_rule"
        }.id

    private fun VoiceCommandHost.run(command: VoiceCommand): Any = runBlocking {
        when (val plan = planVoice(command)) {
            is VoicePlan.Run -> executeVoice(plan.action)
            else -> plan
        }
    }

    private fun ExerciseRunnerViewModel.items() =
        (state.value.values["tasks"] as BlockValue.Items).items

    @Test fun checklistAddAndCompleteUseTheListsOwnActions() {
        val id = twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.enterRun()
        assertEquals(
            setOf(
                VoiceCommandKind.ADD_ITEM,
                VoiceCommandKind.COMPLETE_ITEM,
                VoiceCommandKind.COMPLETE_CURRENT_EXERCISE
            ),
            vm.voiceCommands
        )
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.Added("Reply to Anna")),
            vm.run(VoiceCommand.AddItem("Reply to Anna "))
        )
        vm.setPendingItem("tasks", "typed, not yet added")
        vm.run(VoiceCommand.AddItem("Pay the bill"))
        assertEquals(listOf("Reply to Anna", "Pay the bill"), vm.items().map { it.label })
        assertEquals(
            "the add field keeps what was typed",
            "typed, not yet added",
            vm.state.value.pendingItems["tasks"]
        )
        // stopwatch semantics are unchanged: voice-added items start now and run
        h.clock.advance(Duration.ofSeconds(40))
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.Completed("Reply to Anna")),
            vm.run(VoiceCommand.CompleteItem("reply to anna"))
        )
        assertTrue(vm.items().first().done)
        assertEquals(40, vm.items().first().elapsedSeconds)
        // already-done items are never matched again
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NoMatch("reply to anna")),
            vm.planVoice(VoiceCommand.CompleteItem("reply to anna"))
        )
        // the draft goes through the normal autosave
        vm.flushDraft()
        eventually {
            (runBlocking { h.plans.draft(id) } as? ActivityResult.Template)?.values?.get("tasks")
                ?.let { it as BlockValue.Items }?.items?.takeIf { it.size == 2 }
        }
    }

    @Test fun duplicateAndPartialLabelsNeedAChoiceAndStaleTargetsAreRejected() {
        val vm = h.runnerViewModel(twoMinuteId)
        vm.state.await { !it.loading }
        listOf("Call Anna", "Water plants", "Call Anna").forEach {
            vm.run(VoiceCommand.AddItem(it))
        }
        val plan = vm.planVoice(VoiceCommand.CompleteItem("call anna"))
        assertEquals(
            VoicePlan.Choose(
                listOf(VoiceItem("1", "Call Anna", 1), VoiceItem("3", "Call Anna", 3))
            ),
            plan
        )
        assertEquals(
            VoicePlan.Choose(listOf(VoiceItem("2", "Water plants", 2))),
            vm.planVoice(VoiceCommand.CompleteItem("plants"))
        )
        // ticked by touch before the voice confirmation: nothing changes
        vm.toggleItem("tasks", "3")
        val stale =
            runBlocking {
                vm.executeVoice(VoiceAction.CompleteItem(VoiceItem("3", "Call Anna", 3)))
            }
        assertEquals(VoiceOutcome.Rejected(VoiceRejection.Stale), stale)
        assertEquals(listOf(false, false, true), vm.items().map { it.done })
    }

    @Test fun exerciseCompletionKeepsEveryGate() {
        val id = twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.enterRun()
        vm.run(VoiceCommand.AddItem("One"))
        vm.run(VoiceCommand.AddItem("Two"))
        assertEquals(
            VoicePlan.Reject(VoiceRejection.ExerciseNotReady),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        vm.run(VoiceCommand.CompleteItem("one"))
        vm.run(VoiceCommand.CompleteItem("two"))
        assertEquals(CompletionGate.Ready, vm.state.value.gate)
        assertEquals(
            VoicePlan.Confirm(VoiceAction.CompleteExercise),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        assertEquals(ActivityState.IN_PROGRESS, runBlocking { h.plans.activity(id) }!!.state)
        runBlocking { vm.executeVoice(VoiceAction.CompleteExercise) }
        eventually {
            runBlocking { h.plans.activity(id) }?.takeIf {
                it.state ==
                    ActivityState.COMPLETED
            }
        }
    }

    @Test fun eisenhowerAddsTasksButHasNoItemCompletion() {
        val vm = h.eisenhowerViewModel(h.plant("eisenhower_matrix", ExerciseType.EISENHOWER))
        vm.state.await { !it.loading }
        assertFalse(VoiceCommandKind.COMPLETE_ITEM in vm.voiceCommands)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NotHere),
            vm.planVoice(VoiceCommand.CompleteItem("x"))
        )
        vm.setEntry("Taxes")
        vm.run(VoiceCommand.AddItem("Book dentist"))
        assertEquals("Taxes\nBook dentist", vm.state.value.entryText)
        repeat(5) { vm.run(VoiceCommand.AddItem("Task $it")) }
        assertEquals(7, vm.state.value.entryCount)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.ListFull),
            vm.planVoice(VoiceCommand.AddItem("eighth"))
        )
        assertEquals(
            VoicePlan.Reject(VoiceRejection.ExerciseNotReady),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        vm.startSorting()
        vm.state.await { !it.entering }
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NoList),
            vm.planVoice(VoiceCommand.AddItem("late"))
        )
        // sorting is not silently completed by voice either
        assertEquals(
            VoicePlan.Reject(VoiceRejection.ExerciseNotReady),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
    }

    @Test fun premortemAddsReasonsAndCompletesWithoutAddingTheAction() {
        val id = h.plant("premortem", ExerciseType.PREMORTEM)
        val vm = h.premortemViewModel(id)
        vm.state.await { !it.loading }
        vm.setNewReason("half typed")
        vm.run(VoiceCommand.AddItem("Scope kept growing"))
        assertEquals(listOf("Scope kept growing"), vm.state.value.reasons.map { it.text })
        assertEquals("half typed", vm.state.value.newReason)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.ExerciseNotReady),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        vm.setProject("Launch")
        vm.run(VoiceCommand.AddItem("Nobody tested it"))
        vm.run(VoiceCommand.AddItem("Backend dev left"))
        vm.setAction("Book a test day")
        assertEquals(
            VoicePlan.Confirm(VoiceAction.CompleteExercise, VoiceConfirmNote.MITIGATION_NOT_ADDED),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        runBlocking { vm.executeVoice(VoiceAction.CompleteExercise) }
        val done = eventually {
            runBlocking { h.plans.activity(id) }?.takeIf {
                it.state ==
                    ActivityState.COMPLETED
            }
        }
        assertFalse((done.result as ActivityResult.Premortem).mitigationAddedToToday)
    }

    @Test fun combinationNeverCompletesItsChainByVoice() {
        val day = h.reachProgramDay(14)
        val vm = h.combinationViewModel(
            day.activities.single {
                it.exerciseType ==
                    ExerciseType.COMBINATION
            }.id
        )
        vm.state.await { !it.loading }
        assertFalse(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE in vm.voiceCommands)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NotHere),
            vm.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
        assertTrue(VoiceCommandKind.ADD_ITEM in vm.voiceCommands)
        vm.run(VoiceCommand.AddItem("Quarterly plan"))
        assertEquals("Quarterly plan", vm.state.value.entryText)
    }

    @Test fun reviewOffersOnlyTheRecommendation() {
        val host = NoVoiceCommands.withRecommendation(confirm = true) { }
        assertEquals(setOf(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION), host.voiceCommands)
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NotHere),
            host.planVoice(VoiceCommand.CompleteCurrentExercise)
        )
    }

    @Test fun recommendationOnTodayCreatesNothing() {
        var opened = 0
        val host = TodayVoiceHost({ TodayUiState(loading = false) }) { _, _ -> opened++ }
        assertEquals(
            VoiceOutcome.Done(VoiceFeedback.Recommendation),
            host.run(VoiceCommand.ShowCurrentRecommendation)
        )
        assertEquals(
            VoicePlan.Reject(VoiceRejection.NotHere),
            host.planVoice(VoiceCommand.AddItem("x"))
        )
        assertEquals(0, opened)
    }
}
