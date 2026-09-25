@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.today

import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueDefaults
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.feature.planActivity
import com.wivernz.itera.feature.trainingDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun technique(id: String, skill: Skill, introDay: Int?) = Technique(
    TechniqueId(id), "name:$id", "short:$id", "", skill, introDay, ExerciseType.TEMPLATE, 5, false,
    emptyList(), null, TechniqueDefaults()
)

val TECHNIQUES: Map<String, Technique> = listOf(
    technique("two_minute_rule", Skill.HABITS, 1),
    technique("pomodoro", Skill.FOCUS, 2),
    technique("feynman_technique", Skill.LEARNING, 6),
    technique("pareto_principle", Skill.PLANNING, 9),
    technique("daily_reflection", Skill.REFLECTION, null)
).associateBy { it.id.value }

fun program(
    state: ActivityState = ActivityState.AVAILABLE,
    id: Long = 1,
    technique: String = "two_minute_rule"
) = planActivity(id, technique, ActivitySource.PROGRAM, ExerciseType.TEMPLATE, state)

fun focus(state: ActivityState = ActivityState.AVAILABLE, id: Long = 2) = planActivity(
    id, "pomodoro", ActivitySource.FOCUS_SUGGESTION, ExerciseType.FOCUS_TIMER, state,
    title = "25-minute focus session", subtitle = "Suggested around 11:00", minutes = 25, optional = true
)

fun reflection(state: ActivityState = ActivityState.SCHEDULED, id: Long = 3) = planActivity(
    id,
    "daily_reflection",
    ActivitySource.REFLECTION,
    ExerciseType.REFLECTION,
    state,
    title = "Daily reflection",
    subtitle = "21:00 · 2 min",
    minutes = 2
)

fun review(state: ActivityState = ActivityState.AVAILABLE, id: Long = 4) = planActivity(
    id,
    "feynman_technique",
    ActivitySource.REVIEW,
    ExerciseType.REVIEW,
    state,
    order = 1,
    reviewItemId = 9
)

class TodayMapperTest {
    private fun map(
        activities: List<com.wivernz.itera.domain.model.PlanActivity>,
        now: LocalTime = LocalTime.of(10, 0),
        status: TrainingDayStatus = TrainingDayStatus.IN_PROGRESS,
        programDay: Int = 1,
        prefs: UserPreferences = UserPreferences(onboardingCompleted = true),
        pending: Set<Long> = emptySet(),
        carryOver: String? = null,
        failed: Boolean = false
    ) = mapToUiState(
        TodayInput(
            day = trainingDay(activities, programDay, status, carryOver),
            techniques = TECHNIQUES,
            curriculumDays = 14,
            preferences = prefs,
            now = now,
            reviewTopics = mapOf(9L to "Redis persistence"),
            pendingPractice = pending,
            failed = failed
        )
    )

    @Test fun greetingThresholds() {
        assertEquals(Greeting.MORNING, greetingFor(LocalTime.of(11, 59)))
        assertEquals(Greeting.AFTERNOON, greetingFor(LocalTime.of(12, 0)))
        assertEquals(Greeting.AFTERNOON, greetingFor(LocalTime.of(17, 59)))
        assertEquals(Greeting.EVENING, greetingFor(LocalTime.of(18, 0)))
    }

    @Test fun heroPriority1ExerciseNotDoneWithNewBadge() {
        val hero = map(listOf(program(), focus(), reflection())).hero as TodayHero.Exercise
        assertEquals("two_minute_rule", hero.techniqueId)
        assertEquals(Skill.HABITS, hero.skill)
        assertTrue(hero.isNew)
        assertEquals(TodayTarget.Exercise(1, "two_minute_rule"), hero.target)
    }

    @Test fun practiceDayPastIntroHasNoNewBadge() {
        val hero = map(listOf(program(), reflection()), programDay = 16).hero as TodayHero.Exercise
        assertFalse(hero.isNew)
    }

    @Test fun heroPriority1bCombinationDay() {
        val parent =
            planActivity(
                1,
                "eisenhower_matrix",
                ActivitySource.COMBINATION,
                ExerciseType.COMBINATION,
                minutes = 70
            )
        val step =
            planActivity(
                5,
                "eisenhower_matrix",
                ActivitySource.COMBINATION,
                ExerciseType.EISENHOWER
            )
        val state = map(listOf(parent, step, reflection()), programDay = 14)
        val hero = state.hero as TodayHero.Combination
        assertTrue(hero.curriculum)
        assertEquals(70, hero.minutes)
        // steps belong to the chain; only the parent is a row
        assertEquals(listOf(1L, 3L), state.steps.map { it.activityId })
    }

    @Test fun heroPriority2DueReviewBeatsFocus() {
        val hero = map(
            listOf(program(ActivityState.COMPLETED), review(), focus(), reflection())
        ).hero
        hero as TodayHero.Review
        assertEquals("Redis persistence", hero.topic)
        assertEquals(TodayTarget.Review(4), hero.target)
    }

    @Test fun heroPriority3FocusBeforeSixOnly() {
        val activities =
            listOf(program(ActivityState.COMPLETED), focus(), reflection(ActivityState.AVAILABLE))
        assertTrue(map(activities, LocalTime.of(17, 59)).hero is TodayHero.Focus)
        assertTrue(map(activities, LocalTime.of(18, 0)).hero is TodayHero.Reflection)
    }

    @Test fun scheduledFocusHeroIsShownButNotActionable() {
        val hero = map(
            listOf(program(ActivityState.COMPLETED), focus(ActivityState.SCHEDULED), reflection())
        )
            .hero as TodayHero.Focus
        assertFalse(hero.enabled)
    }

    @Test fun heroPriority4Reflection() {
        val hero = map(
            listOf(
                program(ActivityState.COMPLETED),
                focus(ActivityState.COMPLETED),
                reflection(ActivityState.AVAILABLE)
            )
        ).hero as TodayHero.Reflection
        assertTrue(hero.enabled)
        assertEquals(TodayTarget.Reflection(3), hero.target)
    }

    @Test fun heroPriority5DayDoneThenQuiet() {
        val done = listOf(
            program(ActivityState.COMPLETED),
            focus(ActivityState.COMPLETED),
            reflection(ActivityState.COMPLETED)
        )
        val loud = map(done, status = TrainingDayStatus.COMPLETE).hero as TodayHero.DayDone
        assertFalse(loud.quiet)
        val quiet = map(
            done,
            status = TrainingDayStatus.COMPLETE,
            prefs = UserPreferences(lastSeenDayComplete = LocalDate.of(2026, 3, 28))
        ).hero as TodayHero.DayDone
        assertTrue(quiet.quiet)
    }

    @Test fun completedDayOffersNoFocusEvenBeforeSix() {
        val hero = map(
            listOf(program(ActivityState.COMPLETED), focus(), reflection(ActivityState.COMPLETED)),
            LocalTime.of(10, 0),
            TrainingDayStatus.COMPLETE
        ).hero
        assertTrue(hero is TodayHero.DayDone)
    }

    @Test fun sevenRowStates() {
        val snoozeEnd = Instant.parse("2026-03-28T11:00:00Z")
        val rows = map(
            listOf(
                program(ActivityState.COMPLETED, id = 1).copy(note = "Two emails"),
                planActivity(
                    2,
                    "pomodoro",
                    ActivitySource.MANUAL,
                    ExerciseType.FOCUS_TIMER,
                    ActivityState.IN_PROGRESS
                ),
                planActivity(
                    3,
                    "two_minute_rule",
                    ActivitySource.PRACTICE_PROMPT,
                    ExerciseType.TEMPLATE,
                    ActivityState.AVAILABLE
                ),
                planActivity(
                    4,
                    "pareto_principle",
                    ActivitySource.MANUAL,
                    ExerciseType.TEMPLATE,
                    ActivityState.SNOOZED,
                    snoozedUntil = snoozeEnd
                ),
                focus(ActivityState.SKIPPED, id = 5),
                planActivity(
                    6,
                    "pareto_principle",
                    ActivitySource.MANUAL,
                    ExerciseType.TEMPLATE,
                    ActivityState.EXPIRED
                ),
                reflection(ActivityState.SCHEDULED, id = 7)
            )
        ).steps.associateBy { it.activityId }
        assertEquals(6, rows.size) // EXPIRED is not shown
        assertEquals(StepState.Done, rows.getValue(1).state)
        assertEquals(StepSubtitle.Note("Two emails"), rows.getValue(1).subtitle)
        assertNull(rows.getValue(1).target)
        assertEquals(StepState.Now, rows.getValue(2).state)
        assertEquals(TodayTarget.LogPractice(3, "two_minute_rule"), rows.getValue(3).target)
        assertEquals(StepState.Next, rows.getValue(4).state)
        assertEquals(StepSubtitle.Snoozed(LocalTime.of(11, 0)), rows.getValue(4).subtitle)
        assertEquals(StepState.Done, rows.getValue(5).state)
        assertEquals(StepSubtitle.Skipped, rows.getValue(5).subtitle)
        assertNull(rows.getValue(5).accent)
        assertEquals(StepState.Next, rows.getValue(7).state)
        assertNull(rows.getValue(7).target) // not reachable before the evening
    }

    @Test fun counterCountsOptionalRowsAndReviews() {
        val normal = map(listOf(program(ActivityState.COMPLETED), focus(), reflection()))
        assertEquals(1, normal.completedCount)
        assertEquals(3, normal.totalCount)
        val withReview = map(listOf(program(), review(), focus(), reflection()))
        assertEquals(0 to 4, withReview.completedCount to withReview.totalCount)
    }

    @Test fun pendingPracticeCountsAsDone() {
        val prompt =
            planActivity(
                2,
                "two_minute_rule",
                ActivitySource.PRACTICE_PROMPT,
                ExerciseType.TEMPLATE
            )
        val state = map(listOf(program(), prompt, reflection()), pending = setOf(2))
        assertEquals(1, state.completedCount)
        assertEquals(StepState.Done, state.steps[1].state)
    }

    @Test fun carryOverAndErrorAndLoading() {
        assertEquals(
            "Start at 8",
            map(listOf(program(), reflection()), carryOver = "Start at 8").carryOver
        )
        assertNull(map(listOf(program(), reflection()), carryOver = " ").carryOver)
        val failed = map(listOf(program(), reflection()), failed = true)
        assertNull(failed.hero)
        assertTrue(failed.error)
        assertEquals(2, failed.steps.size) // stored checklist still renders
        val loading = mapToUiState(
            TodayInput(null, TECHNIQUES, 14, UserPreferences(currentProgramDay = 4), LocalTime.NOON)
        )
        assertTrue(loading.loading)
        assertEquals(4, loading.programDay)
        assertEquals(Greeting.AFTERNOON, loading.greeting)
    }
}
