package com.wivernz.itera.domain.review

import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import java.time.LocalDate

/** The fixed five-stage ladder, docs/engine/02-spaced-repetition.md (ADR-0012). Pure; never reads a clock. */
object ReviewScheduler {
    val INTERVALS_DAYS: IntArray = intArrayOf(1, 4, 9, 21, 60)

    fun next(item: ReviewItem, grade: RecallGrade, reviewedOn: LocalDate): ReviewItem {
        val nextStage = when (grade) {
            RecallGrade.FORGOT -> 0
            RecallGrade.PARTIAL -> item.stageIndex
            RecallGrade.SOLID -> item.stageIndex + 1
        }
        return if (nextStage > INTERVALS_DAYS.lastIndex) {
            item.copy(state = ReviewState.RETIRED, lastReviewedOn = reviewedOn)
        } else {
            item.copy(
                stageIndex = nextStage,
                dueOn = reviewedOn.plusDays(INTERVALS_DAYS[nextStage].toLong()),
                lastReviewedOn = reviewedOn,
                state = ReviewState.SCHEDULED
            )
        }
    }

    /** Due items, overdue first: `(dueOn asc, stageIndex desc, id asc)`. An overdue item is due once. */
    fun dueOn(items: List<ReviewItem>, date: LocalDate): List<ReviewItem> =
        items.filter { it.state != ReviewState.RETIRED && !it.dueOn.isAfter(date) }
            .sortedWith(compareBy<ReviewItem>({ it.dueOn }, { -it.stageIndex }, { it.id }))

    /** "If this goes well, the next review is in {n} days" - the SOLID outcome; null at the last stage. */
    fun previewNextInterval(stageIndex: Int): Int? = INTERVALS_DAYS.getOrNull(stageIndex + 1)

    /** Stage 0, due the day after the completion. */
    fun firstDueOn(completedOn: LocalDate): LocalDate =
        completedOn.plusDays(INTERVALS_DAYS[0].toLong())
}
