package com.wivernz.itera.domain

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.feature.progress.ProgressScreen
import com.wivernz.itera.feature.progress.ProgressUiState
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Five calendar days with no use (D-14 end to end): the program day is unchanged, the missed curriculum day is
 * offered, the review that fell due during the gap is due once, and Progress shows the gap as untrained days
 * with the "missed days don't reset anything" note and no warning.
 */
@RunWith(RobolectricTestRunner::class)
class GapIntegrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fiveUnusedDaysChangeNothingButTheCalendar() = runTest {
        val h = EngineHarness()
        try {
            // Days 1-6, ending with the Feynman exercise, whose review falls due the next day.
            repeat(6) {
                val day = h.trainFullDay()
                assertEquals(TrainingDayStatus.COMPLETE, day.status)
                h.nextMorning()
            }
            val lastTrained = h.today.minusDays(1)
            val review = h.reviews.active().single()
            assertEquals(lastTrained.plusDays(1), review.dueOn)
            assertEquals(7, h.programDay())

            // Five calendar days pass without the app being opened; return on the sixth morning.
            h.at(LocalTime.of(8, 0), lastTrained.plusDays(6))
            assertEquals(7, h.programDay())

            val today = h.ensureToday()
            assertEquals("the program does not move while the user is away", 7, h.programDay())
            assertEquals(7, today.programDay)
            val primary = today.activities.first()
            assertEquals(ActivitySource.PROGRAM, primary.source)
            assertEquals(
                "the missed curriculum day is offered",
                "two_list_strategy",
                primary.techniqueId.value
            )
            val reviews = today.activities.filter { it.exerciseType == ExerciseType.REVIEW }
            assertEquals("an overdue review is due once, not once per missed day", 1, reviews.size)
            assertEquals(review.id, reviews.single().reviewItemId)
            assertEquals(listOf(review.id), h.reviews.observeDue(h.today).first().map { it.id })
            assertEquals(listOf(review.id), h.reviews.active().map { it.id })

            // Nothing was generated for the unused days.
            val gap = (1..5).map { lastTrained.plusDays(it.toLong()) }
            gap.forEach { date -> assertEquals(null, h.plans.dayByDate(date)) }

            val summary = h.observeProgress().first()
            val dots = summary.dayDots.associateBy { it.date }
            gap.forEach { date ->
                assertEquals("$date is shown untrained", false, dots.getValue(date).trained)
            }
            assertTrue((1..6).all { dots.getValue(lastTrained.minusDays(it.toLong() - 1)).trained })
            assertEquals(6, summary.trainedDays)

            val startedOn = h.prefs.state.value.programStartedOn
            compose.setContent {
                IteraTheme {
                    ProgressScreen(
                        ProgressUiState(
                            summary,
                            startedOn,
                            false
                        ),
                        {
                        },
                        {},
                        {}
                    )
                }
            }
            compose.onNodeWithText(
                "Missed days don’t reset anything. The program simply continues where you left off."
            ).assertExists()
            listOf("streak", "missed", "behind", "warning").forEach { word ->
                compose.onNodeWithText(word, substring = true, ignoreCase = true).let {
                    // only the reassurance sentence may mention missed days
                    if (word == "missed") it.assertExists() else it.assertDoesNotExist()
                }
            }
        } finally {
            h.close()
        }
    }
}
