package com.wivernz.itera.domain.training

import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayRolloverTest {
    private fun activity(
        id: Long,
        source: ActivitySource,
        state: ActivityState,
        optional: Boolean = false,
        type: ExerciseType = ExerciseType.TEMPLATE
    ) = PlanActivity(
        id, 1,
        TechniqueId(
            "two_minute_rule"
        ),
        type, source, id.toInt(), DayPart.MORNING, "", "", "", 5,
        state, optional, null, null, null, null, null, null, null, null, null
    )

    private fun day(
        vararg activities: PlanActivity,
        status: TrainingDayStatus = TrainingDayStatus.IN_PROGRESS
    ) = TrainingDay(1, 3, LocalDate.of(2026, 3, 28), status, null, activities.toList(), null)

    @Test fun nothingDoneIsAbandonedAndEverythingExpires() {
        val outcome = DayRollover.rollOver(
            day(
                activity(1, ActivitySource.PROGRAM, ActivityState.AVAILABLE),
                activity(2, ActivitySource.FOCUS_SUGGESTION, ActivityState.SCHEDULED, true),
                activity(3, ActivitySource.REVIEW, ActivityState.SNOOZED),
                activity(4, ActivitySource.REFLECTION, ActivityState.IN_PROGRESS)
            )
        )
        assertEquals(TrainingDayStatus.ABANDONED, outcome.status)
        assertEquals(listOf(1L, 2L, 3L, 4L), outcome.expiredActivityIds)
    }

    @Test fun fullyDoneIsCompleteAndNothingExpires() {
        val done = day(
            activity(1, ActivitySource.PROGRAM, ActivityState.COMPLETED),
            activity(2, ActivitySource.FOCUS_SUGGESTION, ActivityState.SKIPPED, true),
            activity(3, ActivitySource.REFLECTION, ActivityState.COMPLETED),
            status = TrainingDayStatus.COMPLETE
        )
        val outcome = DayRollover.rollOver(done)
        assertEquals(TrainingDayStatus.COMPLETE, outcome.status)
        assertTrue(outcome.expiredActivityIds.isEmpty())
        assertFalse(DayRollover.needsRollover(done))
    }

    @Test fun programDoneOptionalOpenIsComplete() {
        val outcome = DayRollover.rollOver(
            day(
                activity(1, ActivitySource.PROGRAM, ActivityState.COMPLETED),
                activity(2, ActivitySource.PRACTICE_PROMPT, ActivityState.AVAILABLE, true),
                activity(3, ActivitySource.REFLECTION, ActivityState.SCHEDULED)
            )
        )
        assertEquals(TrainingDayStatus.COMPLETE, outcome.status)
        assertEquals(listOf(2L, 3L), outcome.expiredActivityIds)
    }

    @Test fun optionalDoneProgramNotIsAbandoned() {
        val outcome = DayRollover.rollOver(
            day(
                activity(1, ActivitySource.PROGRAM, ActivityState.IN_PROGRESS),
                activity(2, ActivitySource.FOCUS_SUGGESTION, ActivityState.COMPLETED, true),
                activity(3, ActivitySource.REFLECTION, ActivityState.COMPLETED)
            )
        )
        assertEquals(TrainingDayStatus.ABANDONED, outcome.status)
        assertEquals(listOf(1L), outcome.expiredActivityIds)
    }

    @Test fun combinationParentCountsAsThePrimary() {
        val outcome = DayRollover.rollOver(
            day(
                activity(
                    1,
                    ActivitySource.COMBINATION,
                    ActivityState.COMPLETED,
                    type = ExerciseType.COMBINATION
                )
            )
        )
        assertEquals(TrainingDayStatus.COMPLETE, outcome.status)
        assertTrue(DayRollover.needsRollover(day(status = TrainingDayStatus.PLANNED)))
    }

    /** Rollover only moves states; drafts stay on the rows (verified end to end in DailyLoopIntegrationTest). */
    @Test fun expiryNeverTouchesTerminalStates() {
        val terminal = listOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED)
        val outcome = DayRollover.rollOver(
            day(
                *terminal.mapIndexed { i, s ->
                    activity(i + 1L, ActivitySource.MANUAL, s, true)
                }.toTypedArray()
            )
        )
        assertTrue(outcome.expiredActivityIds.isEmpty())
    }
}
