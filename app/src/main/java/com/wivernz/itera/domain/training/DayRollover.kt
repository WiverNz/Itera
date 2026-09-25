package com.wivernz.itera.domain.training

import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus

/** The outcome of rolling a past day over; docs/engine/00-exercise-state-machine.md section 6. */
data class RolloverOutcome(
    val dayId: Long,
    val status: TrainingDayStatus,
    // non-terminal activities to move to EXPIRED; their drafts are kept
    val expiredActivityIds: List<Long>
)

object DayRollover {
    private val nonTerminal = setOf(
        ActivityState.SCHEDULED,
        ActivityState.AVAILABLE,
        ActivityState.IN_PROGRESS,
        ActivityState.SNOOZED
    )

    /**
     * A day is COMPLETE only when its primary activity (the PROGRAM activity, or the combination parent)
     * completed; otherwise it is ABANDONED however many optional items were done. A day already COMPLETE
     * stays COMPLETE.
     */
    fun rollOver(day: TrainingDay): RolloverOutcome {
        val primaryDone = day.activities.any {
            it.state == ActivityState.COMPLETED && isPrimary(it.source, it.exerciseType)
        }
        val status = if (day.status == TrainingDayStatus.COMPLETE || primaryDone) {
            TrainingDayStatus.COMPLETE
        } else {
            TrainingDayStatus.ABANDONED
        }
        return RolloverOutcome(
            day.id,
            status,
            day.activities.filter { it.state in nonTerminal }.map { it.id }
        )
    }

    fun needsRollover(day: TrainingDay): Boolean = day.status == TrainingDayStatus.PLANNED ||
        day.status == TrainingDayStatus.IN_PROGRESS ||
        day.activities.any { it.state in nonTerminal }
}

fun isPrimary(source: ActivitySource, exerciseType: ExerciseType): Boolean =
    source == ActivitySource.PROGRAM ||
        (source == ActivitySource.COMBINATION && exerciseType == ExerciseType.COMBINATION)
