@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.runner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.exercise.template.CompletionGate
import com.wivernz.itera.feature.exercise.template.TemplateActions
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.resultViewModel
import com.wivernz.itera.feature.runnerViewModel
import com.wivernz.itera.feature.subscribe
import com.wivernz.itera.feature.todayViewModel
import java.time.Duration
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val EngineHarness.twoMinuteId: Long
    get() = runBlocking { ensureToday() }.activities.first {
        it.techniqueId.value ==
            "two_minute_rule"
    }.id

private fun ExerciseRunnerViewModel.addTask(label: String) {
    val block = state.value.blocks.filterIsInstance<ExerciseBlock.Checklist>().single()
    setPendingItem(block.key, label)
    addItem(block.key, block)
}

@RunWith(RobolectricTestRunner::class)
class ExerciseRunnerViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun snoozeTimeRule() {
        assertEquals(LocalTime.of(11, 0), snoozeTimeFor(LocalTime.of(9, 0)))
        assertEquals(LocalTime.of(12, 0), snoozeTimeFor(LocalTime.of(9, 15)))
        assertEquals(LocalTime.of(10, 0), snoozeTimeFor(LocalTime.of(7, 59)))
        assertEquals(LocalTime.of(20, 0), snoozeTimeFor(LocalTime.of(18, 30)))
        assertEquals(LocalTime.of(20, 0), snoozeTimeFor(LocalTime.of(19, 45)))
        assertNull(snoozeTimeFor(LocalTime.of(20, 0)))
        assertNull(snoozeTimeFor(LocalTime.of(23, 10)))
    }

    @Test fun loadsByIdAndOffersTheComputedSnooze() {
        val vm = h.runnerViewModel(h.twoMinuteId)
        val state = vm.state.await { !it.loading }
        assertEquals("two_minute_rule", state.techniqueId)
        assertEquals(ExerciseBody.Template, state.body)
        assertEquals(LocalTime.of(10, 0), state.snoozeAt) // harness clock is 08:00
    }

    @Test fun unknownActivityIsAnErrorState() {
        assertTrue(h.runnerViewModel(9_999).state.await { !it.loading }.missing)
    }

    @Test fun startOpensTheBodyAndMovesToInProgress() {
        val id = h.twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.onPrimary()
        assertEquals(
            ExerciseRunnerEffect.OpenBody(id, "two_minute_rule", ExerciseBody.Template),
            vm.effects.awaitFirst()
        )
        assertEquals(ActivityState.IN_PROGRESS, runBlocking { h.plans.activity(id) }!!.state)
    }

    @Test fun snoozeLeavesTheActivitySnoozed() {
        val id = h.twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.onSnooze()
        assertEquals(ExerciseRunnerEffect.Close, vm.effects.awaitFirst())
        val activity = runBlocking { h.plans.activity(id) }!!
        assertEquals(ActivityState.SNOOZED, activity.state)
        assertTrue("snooze:$id" in h.reminders.calls)
    }

    @Test fun draftIsSavedAndRestoredExactly() {
        val id = h.twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.onPrimary()
        vm.effects.awaitFirst()
        vm.addTask("Reply to Anna")
        h.clock.advance(Duration.ofSeconds(40))
        vm.toggleItem("tasks", "1")
        vm.flushDraft()
        val draft = eventually { runBlocking { h.plans.draft(id) } as? ActivityResult.Template }
        val item = (draft.values["tasks"] as BlockValue.Items).items.single()
        assertEquals(ChecklistItem("1", "Reply to Anna", true, 40), item)
        val again = h.runnerViewModel(id).state.await { !it.loading }
        assertEquals(draft.values["tasks"], again.values["tasks"])
    }

    @Test fun requireCheckedGatesThenCompletesWithStopwatches() {
        val id = h.twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.addTask("Reply to Anna")
        vm.addTask("Pay the bill")
        h.clock.advance(Duration.ofSeconds(30))
        vm.toggleItem("tasks", "1")
        assertEquals(CompletionGate.TickMore(1), vm.state.value.gate)
        vm.finish() // gated: nothing happens
        h.clock.advance(Duration.ofSeconds(100))
        vm.toggleItem("tasks", "2")
        assertEquals(CompletionGate.Ready, vm.state.value.gate)
        vm.finish()
        assertEquals(
            ExerciseRunnerEffect.ShowResult(id, "two_minute_rule"),
            vm.effects.awaitFirst()
        )
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Template
        val items = (result.values["tasks"] as BlockValue.Items).items
        assertEquals(listOf(30, 130), items.map { it.elapsedSeconds })
        assertTrue(items.all { it.done })
    }

    @Test fun requireBlocksAndAlwaysComplete() {
        val spaced = h.plant("spaced_repetition", ExerciseType.TEMPLATE)
        val vm = h.runnerViewModel(spaced)
        assertEquals(CompletionGate.FillBlocks, vm.state.await { !it.loading }.gate)
        vm.setText("item", "Mitochondria make ATP")
        vm.finish()
        assertTrue(vm.effects.awaitFirst() is ExerciseRunnerEffect.ShowResult)
        assertEquals(1, runBlocking { h.reviews.active() }.size)

        val generic = h.plant("five_second_rule", ExerciseType.TEMPLATE)
        val gvm = h.runnerViewModel(generic)
        assertEquals(ExerciseBody.Generic, gvm.state.await { !it.loading }.body)
        gvm.onPrimary()
        assertEquals(
            ExerciseRunnerEffect.ShowResult(generic, "five_second_rule"),
            gvm.effects.awaitFirst()
        )
        assertEquals(ActivityState.COMPLETED, runBlocking { h.plans.activity(generic) }!!.state)
    }

    @Test fun ratingAndNotePersistOnChange() {
        val id = h.plant("five_second_rule", ExerciseType.TEMPLATE)
        runBlocking { h.complete(id, ActivityResult.Template(emptyMap())) }
        val vm = h.resultViewModel(id)
        vm.state.await { !it.loading }
        vm.setDifficulty(Difficulty.HARD)
        vm.setNote("Felt silly, worked")
        vm.flush()
        val stored =
            eventually { runBlocking { h.plans.activity(id) }!!.takeIf { it.note != null } }
        assertEquals(Difficulty.HARD, stored.difficulty)
        val back = h.resultViewModel(id).state.await {
            !it.loading && it.level != MasteryLevel.NONE
        }
        assertEquals(Difficulty.HARD, back.difficulty)
        assertEquals("Felt silly, worked", back.note)
        assertEquals(MasteryLevel.MET, back.level)
    }
}

@RunWith(RobolectricTestRunner::class)
class ExerciseRunnerScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val intro = ExerciseRunnerUiState(
        loading = false,
        techniqueId = "two_minute_rule",
        name = "2-minute rule",
        why = "Small tasks are cheap to finish.",
        task = "Find two tasks.",
        body = ExerciseBody.Template,
        snoozeAt = LocalTime.of(12, 0)
    )

    @Test fun introShowsTheComputedSnoozeTime() {
        compose.setContent { IteraTheme { ExerciseIntroScreen(intro, {}, {}, {}) } }
        compose.onNodeWithText("Start exercise").assertExists()
        compose.onNodeWithText("Not now — remind me at 12:00", substring = true).assertExists()
    }

    @Test fun genericIntroSaysIDidIt() {
        compose.setContent {
            IteraTheme {
                ExerciseIntroScreen(intro.copy(body = ExerciseBody.Generic, snoozeAt = null), {
                }, {}, {})
            }
        }
        compose.onNodeWithText("I did it").assertExists()
        compose.onNodeWithText("Not now").assertExists()
    }

    @Test fun primaryIsGatedWithAStateDescription() {
        val block = ExerciseBlock.Checklist("tasks", "Find two quick tasks", 2, 5, true, "Add")
        var state by mutableStateOf(
            intro.copy(
                blocks = listOf(block),
                values = mapOf(
                    "tasks" to BlockValue.Items(listOf(ChecklistItem("1", "a", true, 3)))
                ),
                gate = CompletionGate.TickMore(1)
            )
        )
        compose.setContent { IteraTheme { ExerciseRunScreen(state, NoTemplateActions, {}, {}) } }
        compose.onNodeWithTag("RunPrimary").assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Tick 1 more task to finish"
                )
            )
        compose.onNodeWithText("1 / 1").assertExists()
        state = state.copy(gate = CompletionGate.Ready)
        compose.onNodeWithTag("RunPrimary").assertIsEnabled()
    }

    @Test fun resultRatingPersistsAcrossBackAndForward() {
        var result by mutableStateOf(
            ExerciseResultUiState(loading = false, name = "2-minute rule", level = MasteryLevel.MET)
        )
        var shown by mutableStateOf(true)
        compose.setContent {
            IteraTheme {
                if (shown) {
                    ExerciseResultScreen(result, {
                        result = result.copy(difficulty = it)
                    }, {}, {})
                }
            }
        }
        compose.onNodeWithTag("Feel_EASY").performClick()
        shown = false
        compose.waitForIdle()
        shown = true
        compose.onNodeWithTag("Feel_EASY").assertIsSelected()
        compose.onNodeWithText(
            "Practice it on 1 more day to reach Practiced.",
            substring = true
        ).assertDoesNotExist()
    }
}

private object NoTemplateActions : TemplateActions {
    override fun onText(key: String, text: String) = Unit
    override fun onPendingItem(key: String, text: String) = Unit
    override fun onAddItem(block: ExerciseBlock.Checklist) = Unit
    override fun onToggleItem(key: String, itemId: String) = Unit
    override fun onChoiceOption(key: String, index: Int, text: String) = Unit
    override fun onChoose(key: String, index: Int) = Unit
    override fun onListItem(key: String, primary: Boolean, index: Int, text: String) = Unit
    override fun onToggleChip(key: String, option: String, max: Int) = Unit
    override fun onChipCustom(key: String, text: String) = Unit
}

@RunWith(RobolectricTestRunner::class)
class ExerciseCompletionIntegrationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun twoMinuteRuleFromTodayThroughReflectionIntoDayTwo() {
        val today = h.todayViewModel()
        val stop = today.state.subscribe()
        val id = h.twoMinuteId
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        vm.onPrimary()
        vm.effects.awaitFirst()
        vm.addTask("Reply to Anna")
        vm.addTask("Pay the bill")
        vm.toggleItem("tasks", "1")
        vm.toggleItem("tasks", "2")
        vm.finish()
        vm.effects.awaitFirst()
        assertEquals(ActivityState.COMPLETED, runBlocking { h.plans.activity(id) }!!.state)
        assertEquals(MasteryLevel.MET, runBlocking { h.mastery("two_minute_rule") })
        val filled = today.state.await { s ->
            s.steps.any {
                it.activityId == id &&
                    it.state == StepState.Done
            }
        }
        assertEquals(1, filled.completedCount)

        // reflection closes Day 1, Day 2 opens with the carry-over banner
        h.at(LocalTime.of(20, 30))
        val day = runBlocking {
            h.refresh()
            h.ensureToday()
        }
        runBlocking {
            day.activities.filter {
                it.state != ActivityState.COMPLETED && it.optional
            }.forEach { h.skip(it.id) }
            val reflection = day.activities.single { it.exerciseType == ExerciseType.REFLECTION }
            h.complete(
                reflection.id,
                ActivityResult.Reflection(
                    "tasks",
                    emptyList(),
                    null,
                    emptyList(),
                    "Email after focus"
                )
            ).getOrThrow()
        }
        assertEquals(TrainingDayStatus.COMPLETE, runBlocking { h.plans.day(day.id) }!!.status)
        h.nextMorning()
        val day2 = runBlocking { h.ensureToday() }
        assertEquals(2, day2.programDay)
        assertEquals("Email after focus", day2.carryOverIntent)
        stop()
    }
}
