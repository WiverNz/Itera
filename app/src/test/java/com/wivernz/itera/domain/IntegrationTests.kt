@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.data.catalog.seedTechniques
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.SeedCallback
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.fileAssets
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.progress.SkillFacts
import com.wivernz.itera.domain.progress.SkillLevels
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanPersistenceTest {
    @get:Rule val folder = TemporaryFolder()

    private fun open(path: String): IteraDatabase = Room.databaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        IteraDatabase::class.java,
        path
    ).addCallback(SeedCallback(FakeClock()) { seedTechniques(fileAssets) }).build()

    @Test fun planSurvivesReopenAndConcurrentGenerationMakesOneRow() = runBlocking {
        val path = folder.newFile("itera.db").absolutePath
        val clock = FakeClock(HARNESS_START)
        val first = EngineHarness(clock = clock, db = open(path))
        val plans = (1..4).map {
            async(Dispatchers.IO) { first.generate(first.today) }
        }.awaitAll()
        assertEquals(1, plans.map { it.id }.toSet().size)
        val generated = plans.first()
        first.close()

        val reopened = EngineHarness(clock = clock, db = open(path))
        try {
            assertEquals(generated, reopened.plans.dayByDate(reopened.today))
            assertEquals(generated, reopened.generate(reopened.today))
            val rows = withContext(Dispatchers.IO) {
                reopened.db.openHelper.readableDatabase.query(
                    "SELECT COUNT(*) FROM training_day"
                ).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            }
            assertEquals(1, rows)
        } finally {
            reopened.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class MasterySourceExclusionTest {
    @Test fun genericFocusBeforeTheIntroGrantsNothingLaterFocusCounts() = runTest {
        val h = EngineHarness()
        try {
            // Day 1: the generic focus block is attached to Pomodoro
            h.trainFullDay()
            assertEquals(MasteryLevel.NONE, h.mastery("pomodoro"))
            assertTrue(h.progress.techniqueFacts().none { it.techniqueId.value == "pomodoro" })
            // Day 2: Pomodoro's own intro; Day 3: a Pomodoro focus suggestion now counts
            h.nextMorning()
            h.trainFullDay()
            h.nextMorning()
            val day3 = h.trainFullDay()
            assertTrue(
                day3.activities.any {
                    it.source == ActivitySource.FOCUS_SUGGESTION &&
                        it.techniqueId.value == "pomodoro"
                }
            )
            val facts = h.progress.techniqueFacts().single { it.techniqueId.value == "pomodoro" }
            assertEquals(2, facts.totalUses)
            assertEquals(2, facts.distinctDays)
            assertEquals(MasteryLevel.MET, h.mastery("pomodoro"))
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ProgressDerivationTest {
    /**
     * The Day-9 history: eight fully trained days, then Day 9's morning. The skill rows are the
     * D-09 bands over the trailing window, computed from the stored log (the obsolete artboard
     * rows no longer apply).
     */
    @Test fun day9FixtureProducesTheSkillRows() = runTest {
        val h = EngineHarness()
        try {
            repeat(8) {
                h.trainFullDay()
                h.nextMorning()
            }
            h.ensureToday()
            assertEquals(9, h.programDay())
            val summary = h.observeProgress().first()
            assertEquals(9, summary.windowDays)
            assertEquals(8, summary.trainedDays)
            val rows = summary.skills.associateBy { it.skill }
            // Daily reflection every evening: 8 practices on 8 days -> 16 -> Steady
            assertEquals(8, rows.getValue(Skill.REFLECTION).practiceCount)
            assertEquals(SkillLevel.STEADY, rows.getValue(Skill.REFLECTION).level)
            summary.skills.forEach {
                assertEquals(
                    SkillLevels.skillLevel(SkillFacts(it.practiceCount, it.daysPracticed)),
                    it.level
                )
            }
            val counted = h.progress.techniqueFacts().sumOf { it.totalUses }
            assertEquals(counted, summary.skills.sumOf { it.practiceCount })
            assertEquals(counted, summary.activityCount)
            assertTrue(summary.focusMinutesAllTime > 0)
            // completing one more exercise moves exactly one skill by one
            val program = h.plans.dayByDate(h.today)!!.activities.first()
            h.complete(program.id, h.resultFor(program)).getOrThrow()
            val after = h.observeProgress().first().skills.associateBy { it.skill }
            assertEquals(
                rows.getValue(Skill.PLANNING).practiceCount + 1,
                after.getValue(Skill.PLANNING).practiceCount
            )
            assertEquals(
                rows.getValue(Skill.FOCUS).practiceCount,
                after.getValue(Skill.FOCUS).practiceCount
            )
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ProgressPerformanceTest {
    @Test fun firstEmissionWithin300msFor1500Activities() = runTest {
        val h = EngineHarness()
        try {
            val start = LocalDate.of(2024, 3, 1)
            val techniques = h.catalog.catalog().map { it.id.value }
            repeat(750) { i ->
                val date = start.plusDays(i.toLong())
                val day = h.db.trainingDayDao().insert(
                    TrainingDayEntity(
                        programDay = i + 1,
                        date = date.toEpochDay(),
                        status = "COMPLETE",
                        generatorVersion = 1,
                        createdAt = 0
                    )
                )
                h.db.planActivityDao().insertAll(
                    (0..1).map { n ->
                        PlanActivityEntity(
                            trainingDayId = day, techniqueId = techniques[(i + n) % techniques.size],
                            exerciseType = "TEMPLATE", source = "PROGRAM", orderIndex = n, dayPart = "MORNING",
                            copyKey = "activity_program", copyArgs = "{}", estimatedMinutes = 5, optional = false,
                            state = "COMPLETED", completedAt = 1, practiceDate = -1
                        )
                    }
                )
            }
            h.prefs.update { it.copy(programStartedOn = start) }
            h.catalog.catalog()
            val began = System.nanoTime()
            val summary = h.observeProgress().first()
            val techniques2 = h.observeTechniques().first()
            val elapsedMs = (System.nanoTime() - began) / 1_000_000
            assertEquals(14, summary.windowDays)
            assertEquals(1500, summary.activityCount)
            assertEquals(14, techniques2.size)
            assertTrue("took $elapsedMs ms", elapsedMs < 300)
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class DailyLoopIntegrationTest {
    private val expectedPrograms = listOf(
        "two_minute_rule",
        "pomodoro",
        "eisenhower_matrix",
        "five_second_rule",
        "habit_stacking",
        "feynman_technique",
        "two_list_strategy",
        "deep_work",
        "pareto_principle",
        "spaced_repetition",
        "information_diet",
        "premortem",
        "one_percent_improvement"
    )

    @Test fun day1ThroughDay17FromASeededDatabase() = runTest {
        val h = EngineHarness()
        try {
            // Day 1 -> complete everything -> Day 2
            val day1 = h.trainFullDay()
            assertEquals(TrainingDayStatus.COMPLETE, day1.status)
            assertEquals(2, h.programDay())
            assertEquals(MasteryLevel.MET, h.mastery("two_minute_rule"))
            h.nextMorning()
            val day2 = h.ensureToday()
            assertEquals(
                listOf(
                    "pomodoro" to ActivitySource.PROGRAM,
                    "two_minute_rule" to ActivitySource.PRACTICE_PROMPT,
                    "daily_reflection" to ActivitySource.REFLECTION
                ),
                day2.activities.map { it.techniqueId.value to it.source }
            )
            assertEquals("start early", day2.carryOverIntent)

            // a missed day: open the app, do nothing; the same curriculum day comes back
            h.nextMorning()
            h.ensureToday()
            assertEquals(2, h.programDay())
            assertEquals(TrainingDayStatus.ABANDONED, h.plans.day(day2.id)!!.status)
            val retry = h.ensureToday()
            assertEquals("pomodoro", retry.activities.first().techniqueId.value)

            // a draft survives the rollover of an expired activity
            h.start(retry.activities.first().id).getOrThrow()
            h.saveDraft(
                retry.activities.first().id,
                ActivityResult.Focus("half", 1500, 600, 0, false)
            ).getOrThrow()
            h.nextMorning()
            h.ensureToday()
            val expired = h.db.planActivityDao().byId(retry.activities.first().id)!!
            assertEquals("EXPIRED", expired.state)
            assertNotNull(expired.draftPayload)

            var reviewSeen = false
            for (programDay in 2..17) {
                val day = h.trainFullDay()
                assertEquals(programDay, day.programDay)
                val primary = day.activities.first()
                when {
                    programDay <= 13 -> assertEquals(
                        expectedPrograms[programDay - 1],
                        primary.techniqueId.value
                    )
                    programDay == 14 -> {
                        assertEquals(ExerciseType.COMBINATION, primary.exerciseType)
                        assertTrue(
                            day.activities.single {
                                it.source == ActivitySource.REFLECTION
                            }.weeklyLookBack
                        )
                    }
                    programDay == 17 -> assertEquals(ExerciseType.COMBINATION, primary.exerciseType)
                    else -> assertEquals(ActivitySource.PROGRAM, primary.source)
                }
                if (day.activities.any { it.exerciseType == ExerciseType.REVIEW }) reviewSeen = true
                assertTrue(day.activities.all { it.state == ActivityState.COMPLETED })
                assertEquals(TrainingDayStatus.COMPLETE, day.status)
                h.nextMorning()
            }
            assertTrue(reviewSeen)
            assertEquals(18, h.programDay())
            assertEquals(MasteryLevel.INTEGRATED, h.mastery("eisenhower_matrix"))
            assertEquals(MasteryLevel.INTEGRATED, h.mastery("pareto_principle"))
            assertTrue(h.reviews.active().isNotEmpty())
            val names = h.eventNames()
            assertTrue(
                names.containsAll(
                    listOf(
                        "plan_generated",
                        "exercise_completed",
                        "day_completed",
                        "program_day_advanced",
                        "day_rolled_over"
                    )
                )
            )
        } finally {
            h.close()
        }
    }
}
