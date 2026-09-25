@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.domain.training

import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.repository.encodeCopyArgs
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.id
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.domain.testCatalogRepository
import com.wivernz.itera.domain.unlock.UnlockRules
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenerateDailyPlanUseCaseTest {
    private val repo = testCatalogRepository()
    private val catalog: List<Technique> = runBlocking { repo.catalog() }
    private val curriculum = runBlocking { repo.curriculum() }
    private val morning = LocalTime.of(8, 30)
    private val eleven = LocalTime.of(11, 0)
    private val evening = LocalTime.of(21, 0)

    private fun inputs(
        day: Int,
        prefs: UserPreferences = UserPreferences(),
        due: List<DueReview> = emptyList(),
        mastery: Map<TechniqueId, MasteryLevel>? = null,
        lastUsed: Map<TechniqueId, LocalDate> = emptyMap(),
        unlocked: Set<TechniqueId> = UnlockRules.unlockedSet(catalog, day)
    ) = PlanInputs(
        programDay = day,
        curriculum = curriculum,
        catalog = catalog,
        unlocked = unlocked,
        dueReviews = due,
        preferences = prefs,
        // everything introduced before today has been met
        mastery = mastery ?: catalog.associate {
            it.id to
                if ((it.introDay ?: 0) < day &&
                    it.id in unlocked
                ) {
                    MasteryLevel.MET
                } else {
                    MasteryLevel.NONE
                }
        },
        lastUsedOn = lastUsed
    )

    private fun reflection(order: Int, lookBack: Boolean = false) = PlannedActivity(
        id(
            "daily_reflection"
        ),
        ExerciseType.REFLECTION, ActivitySource.REFLECTION, DayPart.EVENING, order,
        "activity_reflection",
        linkedMapOf<String, Any>(
            "technique" to "daily_reflection",
            "minutes" to 2,
            "eveningTime" to "21:00"
        )
            .apply { if (lookBack) put("weeklyLookBack", true) },
        2, false, ActivityState.SCHEDULED, evening
    )

    private fun program(tech: String, minutes: Int, type: ExerciseType) = PlannedActivity(
        id(tech), type, ActivitySource.PROGRAM, DayPart.MORNING, 0, "activity_program",
        linkedMapOf(
            "technique" to tech,
            "minutes" to minutes
        ),
        minutes, false, ActivityState.AVAILABLE, morning
    )

    private fun review(itemId: Long, stage: Int, due: LocalDate) = ReviewItem(
        itemId,
        id(
            "feynman_technique"
        ),
        1, "Redis persistence", 3, "answer", stage, due, null, ReviewState.SCHEDULED
    )

    @Test fun day1() {
        val plan = PlanComposer.compose(inputs(1))
        assertEquals(
            listOf(
                program("two_minute_rule", 5, ExerciseType.TEMPLATE),
                PlannedActivity(
                    id(
                        "pomodoro"
                    ),
                    ExerciseType.FOCUS_TIMER, ActivitySource.FOCUS_SUGGESTION, DayPart.DAYTIME, 1,
                    "activity_focus_generic",
                    linkedMapOf("technique" to "pomodoro", "minutes" to 25, "time" to "11:00"),
                    25, true, ActivityState.SCHEDULED, eleven
                ),
                reflection(2)
            ),
            plan.activities
        )
        assertEquals(
            "{\"technique\":\"pomodoro\",\"minutes\":25,\"time\":\"11:00\"}",
            encodeCopyArgs(plan.activities[1].copyArgs)
        )
        // counter "0 / 3" counts every activity; day completion counts the two required ones
        assertEquals(3, plan.activities.size)
        assertEquals(2, plan.activities.count { !it.optional })
    }

    @Test fun day2() {
        val plan = PlanComposer.compose(inputs(2))
        assertEquals(
            listOf(
                program("pomodoro", 30, ExerciseType.FOCUS_TIMER),
                PlannedActivity(
                    id(
                        "two_minute_rule"
                    ),
                    ExerciseType.TEMPLATE, ActivitySource.PRACTICE_PROMPT, DayPart.DAYTIME, 1,
                    "activity_practice", linkedMapOf("technique" to "two_minute_rule"),
                    5, true, ActivityState.SCHEDULED, eleven
                ),
                reflection(2)
            ),
            plan.activities
        )
    }

    @Test fun day9() {
        val today = LocalDate.of(2026, 4, 5)
        val plan = PlanComposer.compose(inputs(9, due = listOf(DueReview(review(7, 1, today), 6))))
        assertEquals(
            listOf(
                program("pareto_principle", 10, ExerciseType.TEMPLATE),
                PlannedActivity(
                    id(
                        "feynman_technique"
                    ),
                    ExerciseType.REVIEW, ActivitySource.REVIEW, DayPart.MORNING, 1,
                    "activity_review",
                    linkedMapOf(
                        "technique" to "feynman_technique",
                        "minutes" to 5,
                        "sourceDay" to 6
                    ),
                    5, false, ActivityState.AVAILABLE, morning, reviewItemId = 7
                ),
                reflection(2)
            ),
            plan.activities
        )
    }

    @Test fun day14() {
        val plan = PlanComposer.compose(inputs(14))
        val step = { tech: String, minutes: Int, type: ExerciseType, n: Int ->
            PlannedActivity(
                id(tech), type, ActivitySource.COMBINATION, DayPart.MORNING, n, "activity_program",
                linkedMapOf("technique" to tech, "minutes" to minutes, "step" to n), minutes, false,
                ActivityState.AVAILABLE, morning
            )
        }
        assertEquals(
            listOf(
                PlannedActivity(
                    id(
                        "eisenhower_matrix"
                    ),
                    ExerciseType.COMBINATION, ActivitySource.COMBINATION, DayPart.MORNING, 0,
                    "activity_combination",
                    linkedMapOf(
                        "minutes" to 70,
                        "steps" to "eisenhower_matrix,pareto_principle,deep_work,daily_reflection",
                        "curriculumDay" to 14
                    ),
                    70, false, ActivityState.AVAILABLE, morning
                ),
                step("eisenhower_matrix", 10, ExerciseType.EISENHOWER, 1),
                step("pareto_principle", 10, ExerciseType.TEMPLATE, 2),
                step("deep_work", 50, ExerciseType.FOCUS_TIMER, 3),
                reflection(4, lookBack = true)
            ),
            plan.activities
        )
        assertTrue(plan.combinationDay)
        assertTrue(plan.weeklyLookBack)
    }

    @Test fun deterministicAcross100Runs() {
        val input = inputs(9, due = listOf(DueReview(review(7, 1, LocalDate.of(2026, 4, 5)), 6)))
        val first = PlanComposer.compose(input)
        repeat(100) {
            val again = PlanComposer.compose(input)
            assertEquals(first, again)
            assertEquals(
                first.activities.map { encodeCopyArgs(it.copyArgs) },
                again.activities.map { encodeCopyArgs(it.copyArgs) }
            )
        }
    }

    @Test fun reviewCapPerPaceAndOverflowRemainsDue() {
        val date = LocalDate.of(2026, 4, 5)
        val due = (1L..5L).map { DueReview(review(it, 0, date), 6) }
        val caps = mapOf(
            ProgramPace.GENTLE to 1,
            ProgramPace.STANDARD to 2,
            ProgramPace.INTENSE to 3
        )
        caps.forEach { (pace, cap) ->
            val plan = PlanComposer.compose(inputs(10, UserPreferences(pace = pace), due))
            val reviews = plan.activities.filter { it.source == ActivitySource.REVIEW }
            assertEquals(cap, reviews.size)
            assertEquals((1L..cap).toList(), reviews.map { it.reviewItemId })
            // a due review displaces the optional secondary
            assertTrue(plan.activities.none { it.optional })
            assertEquals(cap + 1, PlanComposer.reviewCap(pace, TimeBudget.LONG))
        }
    }

    @Test fun focusSuggestionTruthTable() {
        val both = setOf(id("pomodoro"), id("deep_work"))
        assertTrue(PlanComposer.focusSuggestionAllowed(both, TimeBudget.STANDARD, false))
        assertTrue(
            PlanComposer.focusSuggestionAllowed(setOf(id("deep_work")), TimeBudget.LONG, false)
        )
        assertFalse(PlanComposer.focusSuggestionAllowed(emptySet(), TimeBudget.STANDARD, false))
        assertFalse(PlanComposer.focusSuggestionAllowed(both, TimeBudget.SHORT, false))
        assertFalse(PlanComposer.focusSuggestionAllowed(both, TimeBudget.STANDARD, true))
        assertEquals(
            id("pomodoro"),
            PlanComposer.focusTechnique(setOf(id("pomodoro")), UserPreferences())
        )
        assertEquals(id("pomodoro"), PlanComposer.focusTechnique(both, UserPreferences()))
        assertEquals(
            id("deep_work"),
            PlanComposer.focusTechnique(both, UserPreferences(timeBudget = TimeBudget.LONG))
        )
        assertEquals(
            id("deep_work"),
            PlanComposer.focusTechnique(both, UserPreferences(pace = ProgramPace.INTENSE))
        )
    }

    @Test fun budgetAndFocusBlockRules() {
        // SHORT budget: no focus suggestion, a practice prompt instead
        val short = PlanComposer.compose(inputs(3, UserPreferences(timeBudget = TimeBudget.SHORT)))
        assertTrue(short.activities.none { it.source == ActivitySource.FOCUS_SUGGESTION })
        assertEquals(1, short.activities.count { it.source == ActivitySource.PRACTICE_PROMPT })
        // Day 1 with a SHORT budget: nothing optional qualifies
        assertTrue(
            PlanComposer.compose(
                inputs(1, UserPreferences(timeBudget = TimeBudget.SHORT))
            ).activities.none {
                it.optional
            }
        )
        // Day 3 standard: Pomodoro focus suggestion
        val day3 = PlanComposer.compose(inputs(3))
        assertEquals("activity_focus", day3.activities.single { it.optional }.copyKey)
        // A PROGRAM focus-timer technique (Deep Work, Day 8) suppresses the focus suggestion
        val day8 = PlanComposer.compose(inputs(8))
        assertTrue(day8.activities.none { it.source == ActivitySource.FOCUS_SUGGESTION })
        assertEquals(ActivitySource.PRACTICE_PROMPT, day8.activities.single { it.optional }.source)
        // Weekly look-back on Day 7 and every seventh day after
        assertTrue(PlanComposer.compose(inputs(7)).weeklyLookBack)
        assertFalse(PlanComposer.compose(inputs(8)).weeklyLookBack)
        assertTrue(
            PlanComposer.compose(inputs(21, mastery = allAt(MasteryLevel.PRACTICED))).weeklyLookBack
        )
    }

    private fun allAt(level: MasteryLevel) = catalog.associate { it.id to level }

    @Test fun practiceTechniqueTiebreaks() {
        val day = 13
        val base = allAt(MasteryLevel.APPLIED)
        val exclude = setOf(id("one_percent_improvement"))
        // 1. lowest non-NONE mastery
        val levels =
            base + (id("deep_work") to MasteryLevel.MET) +
                (id("pomodoro") to MasteryLevel.PRACTICED)
        assertEquals(
            id("deep_work"),
            PlanComposer.practiceTechnique(inputs(day, mastery = levels), exclude)?.id
        )
        // 2. oldest last use (never used first)
        val used =
            catalog.associate { it.id to LocalDate.of(2026, 4, 1) } +
                (id("premortem") to LocalDate.of(2026, 3, 1))
        assertEquals(
            id("premortem"),
            PlanComposer.practiceTechnique(
                inputs(day, mastery = base, lastUsed = used),
                exclude
            )?.id
        )
        // 3. focus-area membership
        val same = catalog.associate { it.id to LocalDate.of(2026, 4, 1) }
        val learning = UserPreferences(focusAreas = setOf(Skill.LEARNING))
        assertEquals(
            id("feynman_technique"),
            PlanComposer.practiceTechnique(
                inputs(day, learning, mastery = base, lastUsed = same),
                exclude
            )?.id
        )
        // 4. lowest intro day
        assertEquals(
            id("two_minute_rule"),
            PlanComposer.practiceTechnique(
                inputs(day, mastery = base, lastUsed = same),
                exclude
            )?.id
        )
        // every candidate excluded: nothing
        assertNull(
            PlanComposer.practiceTechnique(
                inputs(day, mastery = allAt(MasteryLevel.INTEGRATED)),
                emptySet()
            )
        )
        assertNull(PlanComposer.practiceTechnique(inputs(1), setOf(id("two_minute_rule"))))
    }

    @Test fun day15PlusForEveryPace() {
        ProgramPace.entries.forEach { pace ->
            for (day in 15..30) {
                val plan = PlanComposer.compose(
                    inputs(
                        day,
                        UserPreferences(pace = pace),
                        mastery = allAt(MasteryLevel.PRACTICED)
                    )
                )
                val primary = plan.activities.filter { isPrimary(it.source, it.exerciseType) }
                assertEquals("day $day", 1, primary.size)
                val combination = (day - 14) % 3 == 0
                assertEquals("day $day", combination, plan.combinationDay)
                if (combination) {
                    assertEquals(ExerciseType.COMBINATION, primary.single().exerciseType)
                } else {
                    assertEquals(ActivitySource.PROGRAM, primary.single().source)
                }
                assertEquals(1, plan.activities.count { it.source == ActivitySource.REFLECTION })
                assertTrue(plan.activities.count { it.optional } <= 1)
            }
        }
    }

    @Test fun generatedCombinationWith2And3And5Eligible() {
        fun eligible(vararg ids: String) = catalog.associate {
            it.id to
                if (it.id.value in ids) MasteryLevel.PRACTICED else MasteryLevel.MET
        }
        // 2 eligible: falls back to a single practice
        val two = PlanComposer.compose(
            inputs(17, mastery = eligible("eisenhower_matrix", "pomodoro"))
        )
        assertFalse(two.combinationDay)
        assertEquals(ActivitySource.PROGRAM, two.activities.first().source)
        // 3 from three different skills: Planning -> Focus/Learning -> Reflection
        val three = PlanComposer.compose(
            inputs(17, mastery = eligible("pareto_principle", "feynman_technique", "premortem"))
        )
        assertTrue(three.combinationDay)
        assertEquals(
            listOf("pareto_principle", "feynman_technique", "premortem"),
            three.activities.filter {
                it.source == ActivitySource.COMBINATION &&
                    it.exerciseType != ExerciseType.COMBINATION
            }
                .map { it.techniqueId.value }
        )
        // 3 eligible but two share a skill: no valid chain
        assertNull(
            PlanComposer.generatedCombination(
                inputs(17, mastery = eligible("pomodoro", "deep_work", "premortem"))
            )
        )
        // 5 eligible: focus areas first, then intro day; the reflection step folds into the evening
        val five =
            eligible(
                "eisenhower_matrix",
                "pareto_principle",
                "pomodoro",
                "feynman_technique",
                "daily_reflection"
            )
        assertEquals(
            listOf("eisenhower_matrix", "pomodoro", "daily_reflection"),
            PlanComposer.generatedCombination(inputs(17, mastery = five))?.map { it.id.value }
        )
        val learning = UserPreferences(focusAreas = setOf(Skill.LEARNING))
        assertEquals(
            listOf("eisenhower_matrix", "feynman_technique", "daily_reflection"),
            PlanComposer.generatedCombination(inputs(17, learning, mastery = five))?.map {
                it.id.value
            }
        )
    }

    @Test fun noLockedTechniqueAcross1000RandomDays() {
        val random = java.util.Random(4)
        val levels = MasteryLevel.entries
        repeat(1000) {
            val day = 1 + random.nextInt(40)
            val prefs = UserPreferences(
                timeBudget = TimeBudget.entries[random.nextInt(3)],
                pace = ProgramPace.entries[random.nextInt(3)],
                focusAreas = Skill.entries.filter { random.nextBoolean() }.toSet(),
                morningTime = LocalTime.of(5 + random.nextInt(8), 0)
            )
            val unlocked = UnlockRules.unlockedSet(catalog, day)
            val mastery = catalog.associate {
                it.id to
                    if (it.id in
                        unlocked
                    ) {
                        levels[random.nextInt(levels.size)]
                    } else {
                        MasteryLevel.NONE
                    }
            }
            val due = listOf(DueReview(review(1, 0, LocalDate.of(2026, 4, 1)), 6))
            val plan = PlanComposer.compose(inputs(day, prefs, due, mastery))
            // Day 1's generic focus block is attached to Pomodoro by design and grants nothing
            plan.activities.filterNot { it.copyKey == "activity_focus_generic" }.forEach {
                assertTrue("day $day ${it.techniqueId}", it.techniqueId in unlocked)
            }
            assertEquals(1, plan.activities.count { isPrimary(it.source, it.exerciseType) })
            assertEquals(1, plan.activities.count { it.source == ActivitySource.REFLECTION })
            assertTrue(plan.activities.count { it.optional } <= 1)
            assertEquals(plan.activities.indices.toList(), plan.activities.map { it.orderIndex })
        }
    }

    @Test fun useCaseIsIdempotentAndStableForTheDay() = runTest {
        val h = EngineHarness()
        try {
            val first = h.generate(h.today)
            val second = h.generate(h.today)
            assertEquals(first, second)
            assertEquals(1, h.db.trainingDayDao().before(h.today.plusDays(1).toEpochDay()).size)
            // preferences change mid-day: the plan is not regenerated
            h.prefs.update { it.copy(timeBudget = TimeBudget.SHORT, pace = ProgramPace.INTENSE) }
            assertEquals(first, h.generate(h.today))
            assertEquals(
                listOf("plan:${first.id}"),
                h.reminders.calls.filter {
                    it.startsWith("plan")
                }
            )
        } finally {
            h.close()
        }
    }

    @Test fun previousDayIsRolledOverBeforeGenerating() = runTest {
        val h = EngineHarness()
        try {
            val day1 = h.ensureToday()
            h.nextMorning()
            h.ensureToday()
            val rolled = checkNotNull(h.plans.day(day1.id))
            assertEquals(TrainingDayStatus.ABANDONED, rolled.status)
            assertTrue(rolled.activities.all { it.state == ActivityState.EXPIRED })
            assertEquals(1, h.programDay())
        } finally {
            h.close()
        }
    }

    @Test fun olderGeneratorVersionIsRegeneratedOnlyWhenUntouched() = runTest {
        val h = EngineHarness()
        try {
            h.db.trainingDayDao().insert(
                TrainingDayEntity(
                    programDay = 1,
                    date = h.today.toEpochDay(),
                    status = "PLANNED",
                    generatorVersion = 0,
                    createdAt = 0
                )
            )
            val regenerated = h.generate(h.today)
            assertEquals(3, regenerated.activities.size)
            assertEquals(GENERATOR_VERSION, h.plans.generatorVersion(regenerated.id))
        } finally {
            h.close()
        }
    }

    @Test fun day1GenericFocusGrantsPomodoroNothing() = runTest {
        val h = EngineHarness()
        try {
            val day = h.ensureToday()
            val focus = day.activities.single { it.source == ActivitySource.FOCUS_SUGGESTION }
            assertEquals(id("pomodoro"), focus.techniqueId)
            h.at(LocalTime.of(11, 30))
            h.refresh()
            h.complete(focus.id, h.resultFor(focus)).getOrThrow()
            assertNull(
                h.states.all().single {
                    it.techniqueId.value == "pomodoro"
                }.introCompletedAt
            )
            assertEquals(MasteryLevel.NONE, h.mastery("pomodoro"))
            assertEquals(1500L, h.db.focusSessionDao().totalSeconds().first())
        } finally {
            h.close()
        }
    }
}
