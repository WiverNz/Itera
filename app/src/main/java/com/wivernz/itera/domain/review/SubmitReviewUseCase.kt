package com.wivernz.itera.domain.review

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.repository.ReviewAttemptRecord
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.CompletionOutcome
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Grades a due review: writes the attempt (`stageBefore`/`stageAfter`), moves the item along the ladder and
 * completes the linked REVIEW activity, all in one transaction. A replayed submit - the linked activity is
 * already COMPLETED - writes nothing and returns the item as stored.
 */
class SubmitReviewUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val reviews: ReviewRepository,
    private val complete: CompleteActivityUseCase,
    private val tx: TransactionRunner,
    private val analytics: Analytics,
    private val clock: Clock
) {
    private class Graded(
        val before: ReviewItem,
        val after: ReviewItem,
        val outcome: CompletionOutcome?
    )

    suspend operator fun invoke(
        activityId: Long,
        answer: String,
        grade: RecallGrade,
        difficulty: Difficulty? = null,
        note: String? = null
    ): Result<ReviewItem> {
        val graded = try {
            tx.inTransaction { grade(activityId, answer, grade, difficulty, note) }
        } catch (e: DomainException) {
            return Result.failure(e)
        }
        graded.outcome?.let { outcome ->
            complete.afterCommit(outcome)
            AnalyticsTechnique.of(graded.before.techniqueId.value)?.let {
                analytics.track(
                    Event.ReviewGraded(
                        it,
                        graded.before.stageIndex,
                        stageAfter(graded.before, graded.after),
                        grade
                    )
                )
            }
        }
        return Result.success(graded.after)
    }

    /** Throws [DomainException], rolling the transaction back, when the activity cannot be completed. */
    private suspend fun grade(
        activityId: Long,
        answer: String,
        grade: RecallGrade,
        difficulty: Difficulty?,
        note: String?
    ): Graded {
        val activity = plans.activity(activityId)
            ?.takeIf { it.exerciseType == ExerciseType.REVIEW }
            ?: throw DomainException(DomainError.ActivityNotFound)
        val item = activity.reviewItemId?.let { reviews.item(it) }
            ?: throw DomainException(DomainError.ReviewNotFound)
        if (activity.state == ActivityState.COMPLETED) return Graded(item, item, null)
        val next = ReviewScheduler.next(item, grade, LocalDate.now(clock))
        reviews.insertAttempt(
            ReviewAttemptRecord(
                reviewItemId = item.id,
                activityId = activityId,
                answer = answer,
                grade = grade,
                stageBefore = item.stageIndex,
                stageAfter = stageAfter(item, next),
                createdAt = clock.instant()
            )
        )
        reviews.updateSchedule(next)
        val outcome = complete.completeInTransaction(
            activityId,
            ActivityResult.Review(item.id, answer, grade, item.sourceAnswer),
            difficulty,
            note
        ).getOrElse {
            throw it as? DomainException ?: DomainException(DomainError.ActivityNotFound)
        }
        return Graded(item, next, outcome)
    }

    /** The stage the item moved to; one past the ladder when SOLID retires it. */
    private fun stageAfter(before: ReviewItem, after: ReviewItem) =
        if (after.state == ReviewState.RETIRED) before.stageIndex + 1 else after.stageIndex
}
