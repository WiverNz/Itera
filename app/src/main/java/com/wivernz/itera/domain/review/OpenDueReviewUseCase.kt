package com.wivernz.itera.domain.review

import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Train can explicitly open the next due review after the plan's automatic review cap is exhausted. */
class OpenDueReviewUseCase @Inject constructor(
    private val ensure: EnsureTodayPlanUseCase,
    private val plans: TrainingPlanRepository,
    private val reviews: ReviewRepository,
    private val tx: TransactionRunner,
    private val clock: Clock
) {
    suspend operator fun invoke(): PlanActivity? {
        val today = ensure()
        return tx.inTransaction {
            val due = ReviewScheduler.dueOn(reviews.active(), LocalDate.now(clock)).firstOrNull()
                ?: return@inTransaction null
            val day = plans.day(today.id) ?: return@inTransaction null
            day.activities.firstOrNull {
                it.reviewItemId == due.id &&
                    it.state !in
                    setOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED)
            }
                ?: plans.activity(
                    plans.insertActivity(
                        day.id,
                        PlannedActivity(
                            techniqueId = due.techniqueId,
                            exerciseType = ExerciseType.REVIEW,
                            source = ActivitySource.REVIEW,
                            dayPart = DayPart.DAYTIME,
                            orderIndex = (day.activities.maxOfOrNull { it.orderIndex } ?: -1) + 1,
                            copyKey = "activity_review",
                            copyArgs = mapOf("technique" to due.techniqueId.value, "minutes" to 3),
                            estimatedMinutes = 3,
                            optional = true,
                            state = ActivityState.AVAILABLE,
                            scheduledAt = null,
                            reviewItemId = due.id
                        )
                    )
                )
        }
    }
}
