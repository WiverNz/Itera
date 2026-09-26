@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.combination

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.feature.FocusRig
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.combinationViewModel
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerActions
import com.wivernz.itera.feature.focus.FocusEffect
import com.wivernz.itera.feature.focusViewModel
import com.wivernz.itera.feature.reachProgramDay
import com.wivernz.itera.feature.reduceMotion
import java.time.Duration
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

private val TrainingDay.parent get() = activities.single {
    it.exerciseType ==
        ExerciseType.COMBINATION
}

/** Runs Eisenhower (choosing "Q4 roadmap") and 80/20 (choosing "Define the bets") inline. */
private fun CombinationViewModel.sortAndPick() {
    state.await { !it.loading && it.current?.kind == StepKind.Eisenhower }
    setEntry("Q4 roadmap\nFix login bug\nVendor survey\nTidy bookmarks")
    onPrimary()
    state.await { it.board.items.isNotEmpty() }
    listOf(Quadrant.DO_NOW, Quadrant.DO_NOW, Quadrant.DELEGATE, Quadrant.DROP).forEach(::place)
    choose("1")
    onPrimary()
    val pick = state.await { it.current?.kind is StepKind.PickOne }
    assertEquals("Q4 roadmap", pick.subject)
    setOption(0, "Define the bets")
    setOption(1, "Collect requests")
    chooseOption(0)
    onPrimary()
}

@RunWith(RobolectricTestRunner::class)
class CombinationViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun day14ChainProgressesCarriesForwardAndResumes() {
        val day = h.reachProgramDay(14)
        val vm = h.combinationViewModel(day.parent.id)
        val first = vm.state.await { !it.loading }
        assertEquals(
            listOf("eisenhower_matrix", "pareto_principle", "deep_work", "daily_reflection"),
            first.steps.map {
                it.techniqueId
            }
        )
        assertEquals(
            listOf(ChainState.NOW, ChainState.NEXT, ChainState.NEXT, ChainState.NEXT),
            first.steps.map {
                it.state
            }
        )
        assertEquals(
            first.steps.take(3).sumOf {
                if (it.kind is StepKind.Focus) 50 else 10
            },
            first.totalMinutes
        )
        assertTrue(first.curriculum)
        assertTrue(!first.anyDone)

        vm.sortAndPick()
        val focus = vm.state.await { it.current?.kind is StepKind.Focus }
        assertEquals("Define the bets", focus.subject)
        assertTrue(focus.anyDone)

        // a new view model (process death) resumes at the same step
        val resumed = h.combinationViewModel(day.parent.id).state.await { !it.loading }
        assertEquals("deep_work", resumed.current!!.techniqueId)
        assertEquals(StepSummary.Sorted("Q4 roadmap"), resumed.steps[0].summary)
        assertEquals(StepSummary.Picked("Define the bets"), resumed.steps[1].summary)

        vm.onPrimary()
        val open = vm.effects.awaitFirst() as CombinationEffect.OpenFocus
        assertEquals(50, open.minutes)
        // the reflection step never completes here
        assertEquals(StepKind.Reflection, vm.state.value.steps.last().kind)
        assertEquals(ChainState.NEXT, vm.state.value.steps.last().state)
    }
}

@RunWith(RobolectricTestRunner::class)
class CombinationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private fun step(id: String, state: ChainState, kind: StepKind, summary: StepSummary? = null) =
        ChainStepUi(1, id, id, Skill.PLANNING, "prompt $id", "hint $id", state, kind, summary)

    private var closes = 0

    private fun show(state: CombinationUiState) = compose.setContent {
        IteraTheme {
            CombinationScreen(
                state,
                CombinationActions({
                }, EisenhowerActions({}, {}, {}), { _, _ -> }, {}, {}, { closes++ })
            )
        }
    }

    private val pickBlock = ExerciseBlock.PickOne("steps", "Which part?", 3, emptyList())
    private val mid = CombinationUiState(
        loading = false,
        programDay = 14,
        totalMinutes = 70,
        steps = listOf(
            step(
                "eisenhower_matrix",
                ChainState.DONE,
                StepKind.Eisenhower,
                StepSummary.Sorted("Q4 roadmap")
            ),
            step("pareto_principle", ChainState.NOW, StepKind.PickOne(pickBlock)),
            step("deep_work", ChainState.NEXT, StepKind.Focus(50)),
            step("daily_reflection", ChainState.NEXT, StepKind.Reflection)
        ),
        subject = "Q4 roadmap",
        choice = BlockValue.Choice(listOf("Define the bets", "", ""), 0)
    )

    @Test fun threeCardStatesAndThePrimaryNamesTheNextStep() {
        show(mid)
        compose.onNodeWithTag("Step_eisenhower_matrix_DONE").assertExists()
        compose.onNodeWithTag("Step_pareto_principle_NOW").assertExists()
        compose.onNodeWithTag("Step_deep_work_NEXT").assertExists()
        compose.onNodeWithText("Most important: Q4 roadmap").assertExists()
        compose.onNodeWithText("Continue to deep_work").assertExists()
        compose.onNodeWithText("70 min").assertExists()
    }

    @Test fun closingWithOneStepDoneConfirms() {
        show(mid)
        compose.onNodeWithTag("Combination").assertExists()
        compose.onNodeWithText("Day 14 · Combination").assertExists()
        compose.onNode(androidx.compose.ui.test.hasContentDescription("Close")).performClick()
        compose.onNodeWithText("Leave the chain?").assertExists()
        compose.runOnIdle { assertEquals(0, closes) }
    }

    @Test fun resumedChainLandsOnTheFocusStep() {
        show(
            mid.copy(
                steps = listOf(
                    mid.steps[0],
                    mid.steps[1].copy(
                        state = ChainState.DONE,
                        summary = StepSummary.Picked("Define the bets")
                    ),
                    mid.steps[2].copy(state = ChainState.NOW),
                    mid.steps[3]
                ),
                subject = "Define the bets"
            )
        )
        compose.onNodeWithText("Start deep_work").assertExists()
        compose.onNodeWithText("For: Define the bets").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
class CombinationIntegrationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun fullDay14ChainGrantsIntegrated() {
        val day = h.reachProgramDay(14)
        val vm = h.combinationViewModel(day.parent.id)
        vm.sortAndPick()
        vm.state.await { it.current?.kind is StepKind.Focus }
        vm.onPrimary()
        val open = vm.effects.awaitFirst() as CombinationEffect.OpenFocus

        // the full-screen timer round trip
        val rig = FocusRig(h)
        val focus = h.focusViewModel(rig, open.activityId, open.minutes, open.techniqueId)
        assertEquals("Define the bets", focus.state.await { !it.loading }.task)
        focus.start()
        focus.state.await { it.timer != null }
        h.clock.advance(Duration.ofMinutes(51))
        val done = focus.effects.awaitFirst() as FocusEffect.Completed
        assertTrue(done.chainStep)

        val finished = vm.state.await { it.chainDone }
        assertNull(finished.current)
        val parent = eventually(day.parent.id)
        val stored = runBlocking { h.plans.day(day.id) }!!
        val chain = stored.activities.filter { it.source == ActivitySource.COMBINATION }
        // one parent plus one row per step; the reflection step folds into the evening reflection (004)
        assertEquals(4, chain.size)
        assertTrue(chain.all { it.state == ActivityState.COMPLETED })
        assertEquals(3, (parent.result as ActivityResult.Combination).stepResults.size)
        assertEquals(
            "Define the bets",
            (
                stored.activities.single {
                    it.techniqueId.value ==
                        "deep_work"
                }.result as ActivityResult.Focus
                ).taskLabel
        )
        listOf("eisenhower_matrix", "pareto_principle", "deep_work").forEach {
            assertEquals(it, MasteryLevel.INTEGRATED, runBlocking { h.mastery(it) })
        }
        // the reflection is completed in the evening, not by the chain
        assertTrue(
            stored.activities.single { it.source == ActivitySource.REFLECTION }.state !=
                ActivityState.COMPLETED
        )
    }

    private fun eventually(id: Long) = com.wivernz.itera.feature.eventually {
        runBlocking { h.plans.activity(id) }?.takeIf { it.state == ActivityState.COMPLETED }
    }
}
