package com.wivernz.itera.feature.progress

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.progress.SkillLevels
import com.wivernz.itera.feature.reduceMotion
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

fun emptyProgress(): ProgressUiState {
    val date = LocalDate.of(2026, 9, 26)
    return ProgressUiState(
        SkillLevels.summarize(date, date, emptyList(), {
            null
        }, 0, 0, 0),
        date,
        false
    )
}

@RunWith(RobolectricTestRunner::class)
class ProgressScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun motion() = reduceMotion()

    @Test fun firstDayIsHonestAndBothLinksWork() {
        var history = false
        var library = false
        compose.setContent {
            IteraTheme {
                ProgressScreen(emptyProgress(), { history = true }, {
                    library =
                        true
                }, {})
            }
        }
        compose.onNodeWithText("0 of the last 1 days").assertExists()
        compose.onAllNodesWithText("0 practices", substring = true).assertCountEquals(5)
        compose.onNodeWithText("History").performScrollTo().performClick()
        compose.onNodeWithText("Library").performScrollTo().performClick()
        assertTrue(history && library)
    }

    @Test fun skeletonRetainsHeaderAndReassurance() {
        compose.setContent { IteraTheme { ProgressScreen(ProgressUiState(), {}, {}, {}) } }
        compose.onNodeWithText("Progress").assertExists()
        compose.onNodeWithText("Missed days don’t reset anything.", substring = true).assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
class ProgressHonestyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun renderedSemanticsHaveNoRewardsOrPressure() {
        compose.setContent { IteraTheme { ProgressScreen(emptyProgress(), {}, {}, {}) } }
        val tree = compose.onRoot(useUnmergedTree = true).printToString()
        listOf("streak", "in a row", "XP", "points", "% complete").forEach {
            assertFalse(it, tree.contains(it, ignoreCase = true))
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun boundedCalendarScrollsToRestDay() {
        val date = LocalDate.of(2026, 9, 26)
        val days =
            historyDays(YearMonth.from(date), date.minusDays(20), date, emptyList(), emptyMap())
        compose.setContent {
            IteraTheme {
                HistoryScreen(HistoryUiState(YearMonth.from(date), date, days, loading = false), {
                }, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
        compose.onNodeWithText("No training logged in September 2026.").assertExists()
        compose.onNodeWithContentDescription(
            "Sunday, September 6, 2026, activities: 0. "
        ).performClick()
        compose.onNodeWithText("Sunday, September 6, 2026").assertIsDisplayed()
        compose.onAllNodesWithText("Rest day. No training logged.").onLast().assertIsDisplayed()
    }
}
