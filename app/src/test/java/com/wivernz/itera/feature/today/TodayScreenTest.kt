@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.feature.planActivity
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.trainingDay
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Day 1, midday, Day 2 and Day 9 fixtures, rendered from the pure mapper. */
object TodayFixtures {
    private fun state(
        activities: List<PlanActivity>,
        now: LocalTime,
        programDay: Int = 1,
        status: TrainingDayStatus = TrainingDayStatus.IN_PROGRESS,
        carryOver: String? = null,
        prefs: UserPreferences = UserPreferences(onboardingCompleted = true),
        pending: Set<Long> = emptySet(),
        failed: Boolean = false
    ) = mapToUiState(
        TodayInput(
            trainingDay(activities, programDay, status, carryOver),
            TECHNIQUES,
            14,
            prefs,
            now,
            pendingPractice = pending,
            failed = failed
        )
    )

    val day1 =
        state(listOf(program(), focus(ActivityState.SCHEDULED), reflection()), LocalTime.of(8, 40))
    val midday =
        state(listOf(program(ActivityState.COMPLETED), focus(), reflection()), LocalTime.of(12, 30))
    val prompt = planActivity(
        2,
        "two_minute_rule",
        ActivitySource.PRACTICE_PROMPT,
        ExerciseType.TEMPLATE,
        optional = true,
        title = "2-minute rule",
        subtitle = "Tap to log when you use it"
    )
    fun day2(pending: Set<Long> = emptySet()) = state(
        listOf(program(technique = "pomodoro"), prompt, reflection()),
        LocalTime.of(9, 0),
        programDay = 2,
        carryOver = "Start the focus block before email",
        pending = pending
    )
    val day9 = state(
        listOf(program(technique = "pareto_principle"), review(), reflection()),
        LocalTime.of(9, 0),
        programDay = 9
    )
    val done = state(
        listOf(
            program(ActivityState.COMPLETED),
            focus(ActivityState.COMPLETED),
            reflection(ActivityState.COMPLETED)
        ),
        LocalTime.of(21, 30),
        status = TrainingDayStatus.COMPLETE,
        prefs = UserPreferences(lastSeenDayComplete = LocalDate.of(2026, 3, 28))
    )
    val error = state(listOf(program(), focus(), reflection()), LocalTime.of(9, 0), failed = true)
}

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule val compose = createComposeRule()
    private val targets = mutableListOf<TodayTarget>()
    private var retries = 0

    @Before fun setup() = reduceMotion()

    private fun show(state: TodayUiState) = compose.setContent {
        IteraTheme { TodayScreen(state, { targets += it }, { retries++ }) }
    }

    @Test fun day1RendersHeroStepsAndCounter() {
        show(TodayFixtures.day1)
        compose.onNodeWithText("Good morning").assertExists()
        compose.onNodeWithText("Day 1").assertExists()
        compose.onNodeWithText("TODAY’S TRAINING").assertExists()
        compose.onNodeWithText("New").assertExists()
        compose.onNodeWithText("Start today’s exercise").performClick()
        assertEquals(TodayTarget.Exercise(1, "two_minute_rule"), targets.single())
        compose.onAllNodesWithTag("Step").fetchSemanticsNodes().let { assertEquals(3, it.size) }
        compose.onNodeWithTag("TodayCounter").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("0 of 3 done")
            )
        )
        compose.onNodeWithTag("CarryOver").assertDoesNotExist()
    }

    @Test fun middayOffersTheFocusBlock() {
        show(TodayFixtures.midday)
        compose.onNodeWithText("Good afternoon").assertExists()
        compose.onNodeWithText("NEXT UP · OPTIONAL").assertExists()
        compose.onNodeWithText("Start focus session").performClick()
        assertEquals(TodayTarget.Focus(2, 25, "pomodoro"), targets.single())
        compose.onNodeWithText("1 / 3").assertExists()
    }

    @Test fun day2ShowsCarryOverAndPracticePromptCompletesInPlaceWithUndo() {
        var state by mutableStateOf(TodayFixtures.day2())
        var undone: Long? = null
        compose.setContent {
            IteraTheme {
                TodayScreen(
                    state,
                    { target ->
                        if (target is TodayTarget.LogPractice) {
                            state =
                                TodayFixtures.day2(
                                    setOf(target.activityId)
                                ).copy(loggedPractice = target)
                        }
                    },
                    {},
                    onUndoPractice = {
                        undone = it
                        state = TodayFixtures.day2()
                    }
                )
            }
        }
        compose.onNodeWithTag("CarryOver").assertExists()
        compose.onNodeWithText(
            "Start the focus block before email",
            substring = true
        ).assertExists()
        compose.onNodeWithText("2-minute rule").performScrollTo().performClick()
        compose.onNodeWithText("1 / 3").assertExists()
        compose.onNodeWithText("Undo").performClick()
        compose.waitUntil { undone == 2L }
        compose.onNodeWithText("0 / 3").assertExists()
    }

    @Test fun day9ShowsReviewAsFourthRowAndHeroAfterExercise() {
        show(TodayFixtures.day9)
        compose.onNodeWithText("Day 9").assertExists()
        compose.onNodeWithText("0 / 3").assertExists()
        compose.onAllNodesWithTag("Step").fetchSemanticsNodes().let { assertEquals(3, it.size) }
    }

    @Test fun errorStateRendersAndTryAgainFires() {
        show(TodayFixtures.error)
        compose.onNodeWithTag("TodayError").assertExists()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
        compose.onAllNodesWithTag("Step").fetchSemanticsNodes().let { assertEquals(3, it.size) }
    }

    @Test fun quietDayCompleteHasNoPrimaryButton() {
        show(TodayFixtures.done)
        compose.onNodeWithText("Training done for today").assertExists()
        compose.onNodeWithTag("HeroAction").assertDoesNotExist()
        compose.onNodeWithText("Wrap up the day").assertDoesNotExist()
    }

    @Test fun heroMergesIntoOneNodeWithTheButtonAsChild() {
        show(TodayFixtures.day1)
        compose.onNodeWithTag(
            "Hero"
        ).assert(hasText("Today’s training", substring = true, ignoreCase = true))
        compose.onNodeWithTag("HeroAction", useUnmergedTree = true).assertExists()
    }

    @Test fun scheduledReflectionHeroIsDisabled() {
        val evening = mapToUiState(
            TodayInput(
                trainingDay(
                    listOf(program(ActivityState.COMPLETED), reflection(ActivityState.SCHEDULED))
                ),
                TECHNIQUES,
                14,
                UserPreferences(),
                LocalTime.of(18, 30)
            )
        )
        show(evening)
        compose.onNodeWithText("Start reflection").assertIsNotEnabled()
    }
}
