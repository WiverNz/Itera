@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.eisenhower

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.eisenhowerViewModel
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
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

private const val TASKS = "Fix login bug\nOutline Q4 roadmap\nVendor survey\n\nTidy bookmarks\nBook dentist"

@RunWith(RobolectricTestRunner::class)
class EisenhowerViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun entryNeedsFourTasksAndKeepsSeven() {
        assertEquals(7, EisenhowerBoard.lines((1..9).joinToString("\n") { "t$it" }).size)
        val vm = h.eisenhowerViewModel(h.plant("eisenhower_matrix", ExerciseType.EISENHOWER))
        vm.state.await { !it.loading }
        vm.setEntry("a\nb\nc")
        assertEquals(1, vm.state.value.entryMissing)
        vm.startSorting()
        assertTrue(vm.state.value.entering)
        vm.setEntry(TASKS)
        vm.startSorting()
        val sorting = vm.state.value
        assertEquals(5, sorting.board.items.size)
        assertEquals("1", sorting.board.selectedId)
    }

    @Test fun placementAutoAdvancesAndTheLastPlacementClearsSelection() {
        var board = EisenhowerBoard.fromEntry("a\nb\nc\nd")
        board = board.place(Quadrant.DO_NOW)
        assertEquals("2", board.selectedId)
        board = board.place(Quadrant.SCHEDULE).place(Quadrant.DELEGATE)
        assertEquals("4", board.selectedId)
        board = board.place(Quadrant.DROP)
        assertNull(board.selectedId)
        assertTrue(board.allSorted)
        assertEquals(EisenhowerGate.CHOOSE, board.gate)
    }

    @Test fun replacementMovesATaskAndDropsAStaleChoice() {
        var board = EisenhowerBoard.fromEntry("a\nb\nc\nd")
        repeat(4) { board = board.place(Quadrant.DO_NOW) }
        board = board.choose("2")
        assertEquals(EisenhowerGate.READY, board.gate)
        board = board.select("2").place(Quadrant.DROP)
        assertEquals(Quadrant.DROP, board.items.first { it.id == "2" }.quadrant)
        assertNull(board.chosenId)
        assertEquals(EisenhowerGate.CHOOSE, board.gate)
        assertEquals(board, board.choose("2")) // only Do-now tasks can be chosen
    }

    @Test fun finishPersistsEveryItemAndTheChoice() {
        val id = h.plant("eisenhower_matrix", ExerciseType.EISENHOWER)
        val vm = h.eisenhowerViewModel(id)
        vm.state.await { !it.loading }
        vm.setEntry("a\nb\nc\nd")
        vm.startSorting()
        listOf(
            Quadrant.DO_NOW,
            Quadrant.SCHEDULE,
            Quadrant.DELEGATE,
            Quadrant.DROP
        ).forEach(vm::place)
        assertTrue(vm.state.value.board.allSorted)
        vm.choose("1")
        vm.finish()
        assertEquals(EisenhowerEffect.ShowResult(id, "eisenhower_matrix"), vm.effects.awaitFirst())
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Eisenhower
        assertEquals("1", result.chosenItemId)
        assertEquals(4, result.items.size)
    }

    @Test fun midSortDraftRestoresTheExactBoard() {
        val id = h.plant("eisenhower_matrix", ExerciseType.EISENHOWER)
        val saved = SavedStateHandle(mapOf("activityId" to id))
        val vm = h.eisenhowerViewModel(id, saved)
        vm.state.await { !it.loading }
        vm.setEntry(TASKS)
        vm.startSorting()
        vm.place(Quadrant.SCHEDULE)
        vm.select("4")
        vm.flushDraft()
        eventually {
            (runBlocking { h.plans.draft(id) } as? ActivityResult.Eisenhower)?.takeIf { d ->
                d.items.any {
                    it.quadrant ==
                        Quadrant.SCHEDULE
                }
            }
        }
        val restored = h.eisenhowerViewModel(id, saved).state.await { !it.loading }
        assertEquals(vm.state.value.board, restored.board)
        assertEquals(false, restored.entering)
    }
}

@RunWith(RobolectricTestRunner::class)
class EisenhowerScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private fun show(
        state: EisenhowerUiState,
        actions: EisenhowerActions = EisenhowerActions({
        }, {}, {}),
        scale: Float = 1f
    ) = compose.setContent {
        CompositionLocalProvider(
            LocalDensity provides Density(LocalDensity.current.density, scale)
        ) {
            IteraTheme { EisenhowerScreen(state, {}, {}, actions, {}, {}) }
        }
    }

    private val sorting =
        EisenhowerUiState(
            loading = false,
            name = "Eisenhower matrix",
            entering = false,
            board = EisenhowerBoard.fromEntry("Fix bug\nRoadmap\nSurvey\nBookmarks")
        )

    @Test fun entryStepIsGated() {
        show(EisenhowerUiState(loading = false, entryText = "a\nb"))
        compose.onNodeWithTag("EisenhowerEntry").assertExists()
        compose.onNodeWithTag("EisenhowerPrimary")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Add 2 more tasks to continue"
                )
            )
    }

    @Test fun selectThenPlaceWithTheHint() {
        var placed: Quadrant? = null
        show(sorting, EisenhowerActions({}, { placed = it }, {}))
        compose.onNodeWithText("Now tap a square").assertExists()
        compose.onNodeWithTag("Quadrant_SCHEDULE").performClick()
        compose.runOnIdle { assertEquals(Quadrant.SCHEDULE, placed) }
    }

    @Test fun quadrantsAreDisabledWithAReasonWhenNothingIsSelected() {
        show(sorting.copy(board = sorting.board.copy(selectedId = null)))
        compose.onNodeWithText("Tap a task").assertExists()
        compose.onNodeWithTag("Quadrant_DO_NOW")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Pick a task first"
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test fun allSortedMessage() {
        var board = sorting.board
        repeat(4) { board = board.place(Quadrant.DO_NOW) }
        show(sorting.copy(board = board))
        compose.onNodeWithTag("EisenhowerAllSorted").assertExists()
    }

    @Test fun keepsTheTwoByTwoAtDoubleFontScale() {
        show(sorting, scale = 2f)
        listOf("DO_NOW", "SCHEDULE", "DELEGATE", "DROP").forEach {
            compose.onNodeWithTag("Quadrant_$it").assertExists()
        }
    }
}
