@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.daycomplete

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.dayCompleteViewModel
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.planActivity
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.subscribe
import com.wivernz.itera.feature.today.TodayHero
import com.wivernz.itera.feature.todayViewModel
import com.wivernz.itera.feature.trainingDay
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val reflectionResult = ActivityResult.Reflection(
    "a",
    emptyList(),
    null,
    emptyList(),
    "Start at 8"
)

private fun day(
    reflectionState: ActivityState,
    change: String? = "Start at 8",
    focusDone: Boolean = true
) = trainingDay(
    listOf(
        planActivity(
            1,
            "two_minute_rule",
            ActivitySource.PROGRAM,
            ExerciseType.TEMPLATE,
            ActivityState.COMPLETED
        ),
        planActivity(
            2,
            "pomodoro",
            ActivitySource.FOCUS_SUGGESTION,
            ExerciseType.FOCUS_TIMER,
            if (focusDone) ActivityState.COMPLETED else ActivityState.AVAILABLE,
            optional = true
        ),
        planActivity(
            3,
            "daily_reflection",
            ActivitySource.REFLECTION,
            ExerciseType.REFLECTION,
            reflectionState,
            result = if (reflectionState == ActivityState.COMPLETED) {
                reflectionResult.copy(tomorrowChange = change)
            } else {
                null
            }
        )
    ),
    status = TrainingDayStatus.COMPLETE
)

@RunWith(RobolectricTestRunner::class)
class DayCompleteViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    private val techniques get() = runBlocking { h.catalog.catalog() }
    private val curriculum get() = runBlocking { h.catalog.curriculum() }

    @Test fun fullyCompletedDay() {
        val state = DayCompleteViewModel.summarize(
            day(ActivityState.COMPLETED),
            techniques,
            curriculum,
            LocalTime.of(8, 30)
        )
        assertTrue(state.fullDay)
        assertEquals(3, state.completedCount)
        assertEquals("Start at 8", state.change)
        assertEquals(
            listOf(Skill.HABITS, Skill.FOCUS, Skill.REFLECTION),
            state.segments.map {
                it.skill
            }
        )
    }

    @Test fun skippedReflectionIsNamedHonestly() {
        val state = DayCompleteViewModel.summarize(
            day(ActivityState.SKIPPED),
            techniques,
            curriculum,
            LocalTime.of(8, 30)
        )
        assertFalse(state.fullDay)
        assertTrue(state.reflectionSkipped)
        assertEquals(listOf(DayPartKind.EXERCISE, DayPartKind.FOCUS), state.done)
        assertNull(state.change)
    }

    @Test fun blankChangeOmitsTheCard() {
        val state = DayCompleteViewModel.summarize(
            day(ActivityState.COMPLETED, change = "  "),
            techniques,
            curriculum,
            LocalTime.of(8, 30)
        )
        assertNull(state.change)
    }

    @Test fun tomorrowPreviewReadsTheCurriculum() {
        val byId = techniques.associateBy { it.id.value }
        fun preview(programDay: Int) =
            DayCompleteViewModel.tomorrow(programDay + 1, curriculum, byId)
        assertEquals("pomodoro", (preview(1) as TomorrowPreview.Technique).techniqueId)
        assertEquals(TomorrowPreview.Combination, preview(13))
        assertEquals(TomorrowPreview.Practice, preview(14))
        assertEquals(TomorrowPreview.Practice, preview(20))
    }

    @Test fun arrivalRecordsLastSeenAndCancelsOpenReminders() {
        val trained = runBlocking { h.trainFullDay() }
        val vm = h.dayCompleteViewModel(trained.id)
        val state = vm.state.await { !it.loading }
        assertEquals(1, state.programDay)
        eventually { h.prefs.state.value.lastSeenDayComplete }
        assertEquals(trained.date, h.prefs.state.value.lastSeenDayComplete)
        // tomorrow's plan was not generated tonight
        assertEquals(1, runBlocking { h.eventNames() }.count { it == "plan_generated" })
    }
}

@RunWith(RobolectricTestRunner::class)
class DayCompleteScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    @Test fun rendersAllElementsAndGoodNightNavigates() {
        var night = 0
        val state = DayCompleteUiState(
            loading = false,
            programDay = 1,
            segments = listOf(Skill.HABITS, Skill.FOCUS, Skill.REFLECTION).map {
                RingSegment(it, true)
            },
            done = listOf(DayPartKind.EXERCISE, DayPartKind.FOCUS, DayPartKind.REFLECTION),
            change = "Start at 8",
            tomorrow = TomorrowPreview.Technique("pomodoro", "Pomodoro", Skill.FOCUS),
            morningTime = LocalTime.of(8, 30)
        )
        compose.setContent { IteraTheme { DayCompleteScreen(state) { night++ } } }
        compose.onNodeWithText("3/3").assertExists()
        compose.onNodeWithText("Day 1 complete").assertExists()
        compose.onNodeWithText(
            "Exercise, focus, reflection. That’s a full training day."
        ).assertExists()
        compose.onNodeWithText("“Start at 8”").assertExists()
        compose.onNodeWithText("Tomorrow · Day 2").assertExists()
        compose.onNodeWithText("Pomodoro").assertExists()
        compose.onNodeWithText("Unlocks at 8:30 AM").assertExists()
        compose.onNodeWithText("Good night").performClick()
        assertEquals(1, night)
    }

    @Test fun changeCardAbsentAndSummaryHonestWhenReflectionSkipped() {
        val state = DayCompleteUiState(
            loading = false,
            programDay = 3,
            segments = listOf(
                RingSegment(Skill.PLANNING, true),
                RingSegment(Skill.REFLECTION, false)
            ),
            done = listOf(DayPartKind.EXERCISE),
            reflectionSkipped = true
        )
        compose.setContent { IteraTheme { DayCompleteScreen(state) {} } }
        compose.onNodeWithTag("ChangeCard").assertDoesNotExist()
        compose.onNodeWithText("Done today: exercise. Reflection skipped tonight.").assertExists()
        compose.onNodeWithText("1/2").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
class DayCompleteIntegrationTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun fullDayThenGoodNightLeavesAQuietTodayAndAdvancesOnce() {
        val h = EngineHarness()
        val trained = runBlocking { h.trainFullDay() }
        assertEquals(TrainingDayStatus.COMPLETE, trained.status)
        h.dayCompleteViewModel(trained.id).state.await { !it.loading }
        eventually { h.prefs.state.value.lastSeenDayComplete }
        // "Good night" returns to Today; later the same evening it is still quiet
        h.at(LocalTime.of(22, 0))
        val today = h.todayViewModel()
        val stop = today.state.subscribe()
        val hero = today.state.await { it.hero != null }.hero as TodayHero.DayDone
        assertTrue(hero.quiet)
        assertEquals(1, hero.programDay)
        assertEquals(2, h.prefs.state.value.currentProgramDay)
        assertEquals(1, runBlocking { h.eventNames() }.count { it == "program_day_advanced" })
        stop()
        main.clearViewModels()
        h.close()
    }
}
