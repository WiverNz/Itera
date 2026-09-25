package com.wivernz.itera.domain.review

import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.TechniqueId
import java.time.LocalDate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    private val today = LocalDate.of(2026, 4, 10)

    private fun item(
        id: Long = 1,
        stage: Int = 0,
        due: LocalDate = today,
        state: ReviewState = ReviewState.SCHEDULED
    ) = ReviewItem(
        id,
        TechniqueId(
            "feynman_technique"
        ),
        1, "topic", 1, "answer", stage, due, null, state
    )

    @Test fun intervalsAreFixed() {
        assertArrayEquals(intArrayOf(1, 4, 9, 21, 60), ReviewScheduler.INTERVALS_DAYS)
        assertEquals(today.plusDays(1), ReviewScheduler.firstDueOn(today))
    }

    @Test fun fifteenCaseTable() {
        val intervals = listOf(1L, 4, 9, 21, 60)
        for (stage in 0..4) {
            val forgot = ReviewScheduler.next(item(stage = stage), RecallGrade.FORGOT, today)
            assertEquals(0, forgot.stageIndex)
            assertEquals(today.plusDays(1), forgot.dueOn)
            assertEquals(ReviewState.SCHEDULED, forgot.state)

            val partial = ReviewScheduler.next(item(stage = stage), RecallGrade.PARTIAL, today)
            assertEquals(stage, partial.stageIndex)
            assertEquals(today.plusDays(intervals[stage]), partial.dueOn)
            assertEquals(ReviewState.SCHEDULED, partial.state)

            val solid = ReviewScheduler.next(item(stage = stage), RecallGrade.SOLID, today)
            if (stage < 4) {
                assertEquals(stage + 1, solid.stageIndex)
                assertEquals(today.plusDays(intervals[stage + 1]), solid.dueOn)
                assertEquals(ReviewState.SCHEDULED, solid.state)
            } else {
                assertEquals(ReviewState.RETIRED, solid.state)
                assertEquals(4, solid.stageIndex)
            }
            assertEquals(today, solid.lastReviewedOn)
        }
    }

    @Test fun retiredItemsAreNeverDue() {
        val retired = ReviewScheduler.next(item(stage = 4), RecallGrade.SOLID, today)
        assertTrue(
            ReviewScheduler.dueOn(listOf(retired.copy(dueOn = today.minusDays(5))), today).isEmpty()
        )
    }

    @Test fun dueSelectionAndOrdering() {
        val items = listOf(
            item(1, stage = 0, due = today.plusDays(7)),
            item(2, stage = 0, due = today),
            item(3, stage = 1, due = today.minusDays(1)),
            item(4, stage = 3, due = today),
            item(5, stage = 3, due = today)
        )
        assertEquals(listOf(3L, 4L, 5L, 2L), ReviewScheduler.dueOn(items, today).map { it.id })
    }

    @Test fun ninetyDaysOverdueIsDueOnce() {
        val due = ReviewScheduler.dueOn(listOf(item(due = today.minusDays(90))), today)
        assertEquals(1, due.size)
    }

    @Test fun previewShowsTheSolidOutcome() {
        assertEquals(4, ReviewScheduler.previewNextInterval(0))
        assertEquals(60, ReviewScheduler.previewNextInterval(3))
        assertNull(ReviewScheduler.previewNextInterval(4))
    }
}
