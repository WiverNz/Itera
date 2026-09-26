@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.reduceMotion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainScreenTest : TrainTestBase() {
    @get:Rule val compose = createComposeRule()

    @Before fun motion() = reduceMotion()

    @Test fun threeNodeStatesAndLibraryNavigation(): Unit = runBlocking {
        val events = mutableListOf<TrainUiEvent>()
        val nodes = trainNodes(9, h.catalog.curriculum(), h.catalog.catalog(), emptyList())
        compose.setContent {
            IteraTheme {
                TrainScreen(
                    TrainUiState(programDay = 9, week = 2, nodes = nodes, loading = false),
                    events::add
                )
            }
        }
        compose.onNodeWithTag("TrainDay8").assertHasClickAction().performClick()
        compose.onNodeWithTag("TrainDay9").assertHasClickAction().performClick()
        compose.onNodeWithTag("TrainDay10").assertHasNoClickAction().assertIsNotEnabled()
        compose.onNodeWithTag("TrainLibrary").performScrollTo().performClick()
        assertEquals(
            listOf(TrainUiEvent.OpenDay(8), TrainUiEvent.OpenDay(9), TrainUiEvent.Library),
            events
        )
    }

    @Test fun dueCardNavigatesAndShowsMoreCount() {
        var event: TrainUiEvent? = null
        val item =
            ReviewItem(
                1,
                TechniqueId(
                    "spaced_repetition"
                ),
                null, "", 1, "", 0, h.today, null, ReviewState.DUE
            )
        compose.setContent {
            IteraTheme {
                TrainScreen(TrainUiState(loading = false, review = item, moreReviews = 2), {
                    event =
                        it
                })
            }
        }
        compose.onNodeWithTag("TrainReview").performScrollTo().performClick()
        compose.onNodeWithText("+2 more").assertExists()
        assertEquals(TrainUiEvent.Review, event)
    }

    @Test fun dayAfterCurriculumHasNoTotalAndScrollDoesNotResetOnRecomposition(): Unit = runBlocking {
        var state by mutableStateOf(
            TrainUiState(
                programDay = 20,
                week = 3,
                loading = false,
                nodes = trainNodes(20, h.catalog.curriculum(), h.catalog.catalog(), emptyList())
            )
        )
        compose.setContent { IteraTheme { TrainScreen(state, {}) } }
        compose.onNodeWithTag("TrainDay20").assertExists()
        compose.onNodeWithTag("Train").performScrollToIndex(0)
        compose.onAllNodesWithText("Day 20").assertCountEquals(2)
        compose.onNodeWithText("Day 20 of 14").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(unlockedCount = 14) }
        compose.onNodeWithText("Your program, one technique at a time.").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest : TrainTestBase() {
    @get:Rule val compose = createComposeRule()

    @Before fun motion() = reduceMotion()

    @Test fun fourteenRowsStickyFilterAndLockedNavigation(): Unit = runBlocking {
        val rows = libraryRows(h.catalog.catalog(), h.observeTechniques().awaitFirst(), null)
        var selected: TechniqueId? = null
        compose.setContent {
            IteraTheme {
                LibraryScreen(LibraryUiState(rows), {
                    if (it is LibraryUiEvent.Open) {
                        selected =
                            it.id
                    }
                }, {})
            }
        }
        rows.forEach { row ->
            compose.onNodeWithTag(
                "Library"
            ).performScrollToNode(hasTestTag("Technique-${row.technique.id.value}"))
        }
        compose.onNodeWithTag("LibraryFilters").assertIsDisplayed()
        compose.onNodeWithTag("Technique-premortem").performScrollTo().performClick()
        assertEquals(TechniqueId("premortem"), selected)
        val title = rows.first { it.technique.id.value == "premortem" }.technique.name
        compose.onNodeWithContentDescription("$title, locked, unlocks on day 12").assertExists()
    }

    @Test fun filterIsSingleSelectAndIncludesLockedTechniques(): Unit = runBlocking {
        val catalog = h.catalog.catalog()
        var state by mutableStateOf(LibraryUiState(libraryRows(catalog, emptyList(), null)))
        compose.setContent {
            IteraTheme {
                LibraryScreen(state, {
                    if (it is LibraryUiEvent.Filter) {
                        state =
                            state.copy(
                                filter = it.skill,
                                rows = libraryRows(catalog, emptyList(), it.skill)
                            )
                    }
                }, {})
            }
        }
        compose.onNodeWithText("Learning").performClick().assertIsSelected()
        compose.onNodeWithText("All").assertIsNotSelected()
        compose.onNodeWithTag("Technique-spaced_repetition").assertExists()
        compose.onNodeWithTag("Technique-pomodoro").assertDoesNotExist()
    }
}

@RunWith(RobolectricTestRunner::class)
class TechniqueDetailScreenTest : TrainTestBase() {
    @get:Rule val compose = createComposeRule()

    @Before fun motion() = reduceMotion()

    @Test fun lockedExplanationReadableAndPracticeDisabledAndHistoryHidden(): Unit = runBlocking {
        val technique = h.catalog.technique(TechniqueId("premortem"))!!
        compose.setContent {
            IteraTheme {
                TechniqueDetailScreen(
                    TechniqueDetailUiState(
                        technique = technique,
                        loading = false
                    ),
                    {
                    },
                    {}
                )
            }
        }
        compose.onNodeWithText(technique.explanation).assertExists()
        compose.onNodeWithTag("PracticeNow").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("TechniqueHistory").assertDoesNotExist()
    }

    @Test fun unlockedPracticeAndRelatedEmitTheirEvents(): Unit = runBlocking {
        val technique = h.catalog.technique(TechniqueId("two_minute_rule"))!!
        val facts = h.observeTechniques().awaitFirst().first { it.techniqueId == technique.id }
        val related = h.catalog.technique(technique.relatedTechniqueIds.first())!!
        val events = mutableListOf<TechniqueDetailUiEvent>()
        compose.setContent {
            IteraTheme {
                TechniqueDetailScreen(
                    TechniqueDetailUiState(
                        technique = technique,
                        progress = facts,
                        related = listOf(related),
                        loading = false
                    ),
                    events::add,
                    {
                    }
                )
            }
        }
        compose.onNodeWithTag("PracticeNow").performScrollTo().performClick()
        compose.onNodeWithText(related.name).performScrollTo().performClick()
        assertEquals(
            listOf(TechniqueDetailUiEvent.Practice, TechniqueDetailUiEvent.Related(related.id)),
            events
        )
        compose.onNodeWithTag("TechniqueHistory").assertExists()
    }
}
