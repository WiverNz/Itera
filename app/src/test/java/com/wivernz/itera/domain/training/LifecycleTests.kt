package com.wivernz.itera.domain.training

import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.FocusSessionRecord
import com.wivernz.itera.domain.repository.HabitStackRecord
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun harness(
    programDay: Int = 1,
    failFast: Boolean = true,
    records: (
        (PracticeRecordRepository) -> PracticeRecordRepository
    )? = null
) = EngineHarness(
    initialPreferences = UserPreferences(
        onboardingCompleted = true,
        currentProgramDay = programDay
    ),
    failFast = failFast,
    recordsOverride = records
)

private fun TrainingDay.program() = activities.first { isPrimary(it.source, it.exerciseType) }

private suspend fun EngineHarness.completeProgram(day: TrainingDay) =
    complete(day.program().id, resultFor(day.program()))

/** Fails the named effect so rollback can be observed. */
private class FailingRecords(
    private val real: PracticeRecordRepository,
    private val failFocus: Boolean
) : PracticeRecordRepository {
    override suspend fun insertFocusSession(record: FocusSessionRecord): Long =
        if (failFocus) error("disk full") else real.insertFocusSession(record)
    override suspend fun insertHabitStack(record: HabitStackRecord): Long =
        if (!failFocus) error("disk full") else real.insertHabitStack(record)
}

@RunWith(RobolectricTestRunner::class)
class CompleteActivityUseCaseTest {
    @Test fun persistsTheResultAndEffect1SetsIntroAndMet() = runTest {
        val h = harness()
        try {
            val day = h.ensureToday()
            h.clock.advance(java.time.Duration.ofMinutes(3))
            h.complete(
                day.program().id,
                h.resultFor(day.program()),
                com.wivernz.itera.domain.model.Difficulty.EASY,
                "quick"
            )
                .getOrThrow()
            val done = h.plans.activity(day.program().id)!!
            assertEquals(ActivityState.COMPLETED, done.state)
            assertEquals(com.wivernz.itera.domain.model.Difficulty.EASY, done.difficulty)
            assertEquals("quick", done.note)
            assertNotNull(done.completedAt)
            assertNotNull(done.result)
            assertEquals(0, done.durationSeconds)
            assertNotNull(
                h.states.all().single {
                    it.techniqueId.value == "two_minute_rule"
                }.introCompletedAt
            )
            assertEquals(MasteryLevel.MET, h.mastery("two_minute_rule"))
            assertEquals(TrainingDayStatus.IN_PROGRESS, h.plans.day(day.id)!!.status)
            // 7. analytics inside the transaction
            assertTrue("exercise_completed" in h.eventNames())
            assertTrue("cancel:${day.program().id}" in h.reminders.calls)
        } finally {
            h.close()
        }
    }

    @Test fun effect2SchedulesAReviewForTomorrow() = runTest {
        val h = harness(6)
        try {
            h.completeProgram(h.ensureToday()).getOrThrow()
            val item = h.reviews.active().single()
            assertEquals(h.today.plusDays(1), item.dueOn)
            assertEquals(0, item.stageIndex)
        } finally {
            h.close()
        }
    }

    @Test fun effect3WritesExactlyOneFocusSession() = runTest {
        val h = harness(2)
        try {
            h.completeProgram(h.ensureToday()).getOrThrow()
            assertEquals(1500L, h.db.focusSessionDao().totalSeconds().first())
            assertEquals(1, h.db.focusSessionDao().observeBetween(0, Long.MAX_VALUE).first().size)
        } finally {
            h.close()
        }
    }

    @Test fun effect4WritesTheStackAndRequestsItsNudge() = runTest {
        val h = harness(5)
        try {
            h.completeProgram(h.ensureToday()).getOrThrow()
            val stack = h.db.habitStackDao().observeActive().first().single()
            assertEquals("coffee", stack.anchor)
            assertTrue("habit:${stack.id}" in h.reminders.calls)
        } finally {
            h.close()
        }
    }

    @Test fun effect5AddsOneManualMitigation() = runTest {
        val h = harness(12)
        try {
            val day = h.ensureToday()
            val result = ActivityResult.Premortem(
                "launch",
                listOf(PremortemReason("scope", Likelihood.LIKELY)),
                "cut scope",
                true
            )
            h.complete(day.program().id, result).getOrThrow()
            val manual = h.plans.day(day.id)!!.activities.single {
                it.source ==
                    ActivitySource.MANUAL
            }
            assertTrue(manual.optional)
            assertEquals("premortem", manual.techniqueId.value)
            assertEquals(ActivityState.AVAILABLE, manual.state)
            val draft = h.db.planActivityDao().byId(manual.id)!!.draftPayload!!
            assertTrue(draft.contains("cut scope"))
        } finally {
            h.close()
        }
    }

    @Test fun effect6CompletesTheDayAndAdvancesOnce() = runTest {
        val h = harness()
        try {
            val done = h.trainFullDay()
            assertEquals(TrainingDayStatus.COMPLETE, done.status)
            assertNotNull(done.completedAt)
            assertEquals(2, h.programDay())
            // completing an already-completed activity: success, no second advance
            assertTrue(h.complete(done.program().id, h.resultFor(done.program())).isSuccess)
            assertFalse(h.advance(done.id))
            assertEquals(2, h.programDay())
            assertEquals(1, h.eventNames().count { it == "day_completed" })
        } finally {
            h.close()
        }
    }

    @Test fun mismatchedResultFailsFastInDebugAndIsADomainErrorInRelease() = runTest {
        val debug = harness()
        try {
            val day = debug.ensureToday()
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking {
                    debug.complete(day.program().id, ActivityResult.Focus("t", 1, 1, 0, true))
                }
            }
        } finally {
            debug.close()
        }
        val release = harness(failFast = false)
        try {
            val day = release.ensureToday()
            val result = release.complete(
                day.program().id,
                ActivityResult.Focus("t", 1, 1, 0, true)
            )
            assertTrue(
                (result.exceptionOrNull() as DomainException).error is DomainError.IllegalTransition
            )
            assertTrue(release.complete(-5, ActivityResult.Focus("t", 1, 1, 0, true)).isFailure)
        } finally {
            release.close()
        }
    }

    @Test fun anyEffectFailureRollsBackEverything() = runTest {
        val h = harness(5, records = { FailingRecords(it, failFocus = false) })
        try {
            val day = h.ensureToday()
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking { h.completeProgram(day) }
            }
            assertEquals(ActivityState.AVAILABLE, h.plans.activity(day.program().id)!!.state)
            assertNull(
                h.states.all().single {
                    it.techniqueId.value == "habit_stacking"
                }.introCompletedAt
            )
            assertFalse("exercise_completed" in h.eventNames())
        } finally {
            h.close()
        }
    }

    @Test fun combinationParentSettlesItsSteps() = runTest {
        val h = harness(14)
        try {
            val day = h.ensureToday()
            val steps = day.activities.filter { it.isCombinationStep }
            assertEquals(3, steps.size)
            // the runner completes Deep Work itself; Eisenhower gets its result from the chain; Pareto is left out
            val deep = steps.single { it.exerciseType == ExerciseType.FOCUS_TIMER }
            h.complete(deep.id, h.resultFor(deep)).getOrThrow()
            val chain = ActivityResult.Combination(
                listOf(
                    com.wivernz.itera.domain.model.CombinationStepResult(
                        steps[0].techniqueId,
                        "Q4",
                        h.clock.instant()
                    )
                )
            )
            h.complete(day.program().id, chain).getOrThrow()
            val after = h.plans.day(day.id)!!.activities.associateBy { it.id }
            assertEquals(ActivityState.COMPLETED, after.getValue(steps[0].id).state)
            assertEquals(ActivityState.SKIPPED, after.getValue(steps[1].id).state)
            assertEquals(ActivityState.COMPLETED, after.getValue(deep.id).state)
            assertEquals(MasteryLevel.INTEGRATED, h.mastery("eisenhower_matrix"))
            assertEquals(MasteryLevel.INTEGRATED, h.mastery("deep_work"))
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class AdvanceProgramDayUseCaseTest {
    @Test fun advancesOnceAndSecondCallIsANoOp() = runTest {
        val h = harness()
        try {
            val day = h.trainFullDay()
            assertEquals(2, h.programDay())
            assertFalse(h.advance(day.id))
            assertFalse(h.advance.reconcile())
            assertEquals(2, h.programDay())
            assertEquals(1, h.eventNames().count { it == "program_day_advanced" })
        } finally {
            h.close()
        }
    }

    @Test fun abandonedDayDoesNotAdvance() = runTest {
        val h = harness()
        try {
            val day = h.ensureToday()
            h.nextMorning()
            val next = h.ensureToday()
            assertEquals(TrainingDayStatus.ABANDONED, h.plans.day(day.id)!!.status)
            assertFalse(h.advance(day.id))
            assertEquals(1, h.programDay())
            assertEquals("two_minute_rule", next.program().techniqueId.value)
        } finally {
            h.close()
        }
    }

    @Test fun recoversAnAdvanceLostAfterTheCommit() = runTest {
        val h = harness()
        try {
            val day = h.ensureToday()
            h.plans.updateDayStatus(day.id, TrainingDayStatus.COMPLETE, h.clock.instant())
            assertTrue(h.advance.reconcile())
            assertEquals(2, h.programDay())
            assertFalse(h.advance.reconcile())
        } finally {
            h.close()
        }
    }

    @Test fun programDoneButReviewOpenCompletesAtRollover() = runTest {
        val h = harness(9)
        try {
            h.scheduleReview(
                999,
                com.wivernz.itera.domain.id("feynman_technique"),
                ActivityResult.Feynman(h.topics.add("t"), "t", "x", 1, emptyList(), null),
                h.today.minusDays(1)
            )
            val day = h.ensureToday()
            h.completeProgram(day).getOrThrow()
            assertEquals(9, h.programDay())
            h.nextMorning()
            h.ensureToday()
            assertEquals(TrainingDayStatus.COMPLETE, h.plans.day(day.id)!!.status)
            assertEquals(10, h.programDay())
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class TransactionAtomicityTest {
    @Test fun failureInEffect3WritesNothing() = runTest {
        val h = harness(2, records = { FailingRecords(it, failFocus = true) })
        try {
            val day = h.ensureToday()
            val events = h.eventNames().size
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking { h.completeProgram(day) }
            }
            val row = h.db.planActivityDao().byId(day.program().id)!!
            assertEquals("AVAILABLE", row.state)
            assertNull(row.resultPayload)
            assertNull(row.completedAt)
            assertNull(
                h.states.all().single {
                    it.techniqueId.value == "pomodoro"
                }.introCompletedAt
            )
            assertEquals(TrainingDayStatus.PLANNED, h.plans.day(day.id)!!.status)
            assertEquals(events, h.eventNames().size)
            assertEquals(2, h.programDay())
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ActivityLifecycleUseCaseTest {
    @Test fun startSnoozeSkipAbandonDraftAndManualPractice() = runTest {
        val h = harness(3)
        try {
            val day = h.ensureToday()
            val program = day.program()
            val focus = day.activities.single { it.source == ActivitySource.FOCUS_SUGGESTION }
            val reflection = day.activities.single { it.source == ActivitySource.REFLECTION }
            // required activities cannot be skipped; scheduled ones cannot start yet
            assertTrue(h.skip(program.id).isFailure)
            assertTrue(h.start(focus.id).isFailure)
            // snooze until noon, then reach it
            val noon = h.today.atTime(12, 0).atZone(h.clock.zone).toInstant()
            assertTrue(h.snooze(program.id, noon).isSuccess)
            assertTrue("snooze:${program.id}" in h.reminders.calls)
            assertTrue(h.refresh().isEmpty())
            h.at(LocalTime.of(12, 1))
            assertEquals(setOf(program.id, focus.id), h.refresh().toSet())
            // start, draft, abandon keeps the draft
            h.start(program.id).getOrThrow()
            val draft = ActivityResult.Eisenhower(emptyList(), null)
            h.saveDraft(program.id, draft).getOrThrow()
            h.abandon(program.id).getOrThrow()
            val abandoned = h.db.planActivityDao().byId(program.id)!!
            assertEquals("AVAILABLE", abandoned.state)
            assertNull(abandoned.startedAt)
            assertNotNull(abandoned.draftPayload)
            // optional focus can be skipped
            h.skip(focus.id).getOrThrow()
            assertEquals(ActivityState.SKIPPED, h.plans.activity(focus.id)!!.state)
            // manual practice of an unlocked technique; a locked one is refused
            val manual = h.addManual(com.wivernz.itera.domain.id("pomodoro")).getOrThrow()
            assertEquals(ActivitySource.MANUAL, h.plans.activity(manual)!!.source)
            assertTrue(h.addManual(com.wivernz.itera.domain.id("deep_work")).isFailure)
            // skipping the reflection records it and can finish the day
            h.complete(program.id, h.resultFor(program)).getOrThrow()
            h.at(LocalTime.of(20, 30))
            h.refresh()
            h.skip(reflection.id).getOrThrow()
            assertEquals(TrainingDayStatus.COMPLETE, h.plans.day(day.id)!!.status)
            assertEquals(4, h.programDay())
            assertTrue(h.db.reflectionDao().byDay(day.id)!!.skipped)
        } finally {
            h.close()
        }
    }

    @Test fun draftTypeIsChecked() = runTest {
        val h = harness(failFast = false)
        try {
            val program = h.ensureToday().program()
            assertTrue(h.saveDraft(program.id, ActivityResult.Focus("t", 1, 1, 0, true)).isFailure)
            assertTrue(
                h.saveDraft(
                    program.id,
                    ActivityResult.Template(mapOf("tasks" to BlockValue.Text("x")))
                ).isSuccess
            )
        } finally {
            h.close()
        }
    }
}
