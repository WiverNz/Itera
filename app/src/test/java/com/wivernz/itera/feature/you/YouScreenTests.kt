package com.wivernz.itera.feature.you

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wivernz.itera.analytics.ExportRange
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class YouScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedPermissionCoachReadOnlyTimeAndResetConfirmation() {
        val events = mutableListOf<YouUiEvent>()
        compose.setContent {
            IteraTheme { YouScreen(YouUiState(loading = false), events::add, true) }
        }
        compose.onNodeWithText("Time format").performScrollTo().assertHasNoClickAction()
        compose.onNodeWithText("24-hour").assertExists()
        compose.onNodeWithText("Notifications are off for Itera").performScrollTo().assertExists()
        compose.onNodeWithText(
            "AI feedback on explanations",
            substring = true
        ).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Reset program").performScrollTo().performClick()
        compose.onNodeWithText("Delete your training history", substring = true).assertExists()
        compose.onNodeWithText("Cancel").assertExists().performClick()
        assertTrue(events.isEmpty())
        compose.onNodeWithText("Erase everything").performScrollTo().performClick()
        compose.onNodeWithText("Delete all local training history", substring = true).assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
class ExportSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rangesDestinationsPrivacyAndNoTargetMessage() {
        val ranges = mutableListOf<ExportRange>()
        val destinations = mutableListOf<ExportDestination>()
        compose.setContent {
            IteraTheme {
                ExportSheet(ExportUiState(noTarget = true), ranges::add, destinations::add, {})
            }
        }
        compose.onNodeWithText("This file contains your notes and reflections.").assertExists()
        compose.onNodeWithText("Last 30 days").assertExists()
        compose.onNodeWithText("Last 12 months").performClick()
        compose.onNodeWithText("Everything").performClick()
        compose.onNodeWithText("No app can share this file. Save to a file instead.").assertExists()
        compose.onNodeWithText("Share").performClick()
        compose.onNodeWithText("Save to a file").performClick()
        assertEquals(listOf(ExportRange.YEAR, ExportRange.ALL), ranges)
        assertEquals(listOf(ExportDestination.SHARE, ExportDestination.SAVE), destinations)
    }
}
