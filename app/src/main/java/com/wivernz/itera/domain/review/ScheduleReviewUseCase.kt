package com.wivernz.itera.domain.review

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Completion effect 2 (docs/engine/02-spaced-repetition.md section 2). A review-eligible completion with a
 * recallable answer creates an item at stage 0 due the next day. A topic-bound item is unique per
 * `(techniqueId, topicId)`: redoing it replaces the answer and keeps the stage.
 */
class ScheduleReviewUseCase @Inject constructor(
    private val reviews: ReviewRepository,
    private val catalog: TechniqueCatalogRepository,
    private val tx: TransactionRunner,
    private val clock: Clock
) {
    /** Returns the created or updated item id, or null when nothing is reviewable. */
    suspend operator fun invoke(
        sourceActivityId: Long,
        techniqueId: TechniqueId,
        result: ActivityResult,
        completedOn: LocalDate
    ): Long? {
        if (catalog.technique(techniqueId)?.reviewEligible != true) return null
        val recall = recallable(result) ?: return null
        return tx.inTransaction {
            val existing = recall.topicId?.let { reviews.activeFor(techniqueId, it) }
            if (existing != null) {
                reviews.replaceAnswer(existing.id, recall.prompt, recall.answer, sourceActivityId)
                existing.id
            } else {
                reviews.insert(
                    ReviewItem(
                        id = 0,
                        techniqueId = techniqueId,
                        topicId = recall.topicId,
                        prompt = recall.prompt,
                        sourceActivityId = sourceActivityId,
                        sourceAnswer = recall.answer,
                        stageIndex = 0,
                        dueOn = ReviewScheduler.firstDueOn(completedOn),
                        lastReviewedOn = null,
                        state = ReviewState.SCHEDULED
                    ),
                    clock.instant()
                )
            }
        }
    }

    private class Recall(val topicId: Long?, val prompt: String, val answer: String)

    /**
     * Feynman: the explanation, bound to its topic; the prompt holds the topic title. Spaced repetition: the
     * `item` text the user chose to remember. The prompt sentence itself is rendered from `review_prompt_*`.
     */
    private fun recallable(result: ActivityResult): Recall? = when (result) {
        is ActivityResult.Feynman ->
            Recall(result.topicId, result.topicTitle, result.explanation)
                .takeIf { result.explanation.isNotBlank() }
        is ActivityResult.Template ->
            (result.values[SPACED_ITEM_KEY] as? BlockValue.Text)?.text
                ?.takeIf { it.isNotBlank() }
                ?.let { Recall(null, "", it) }
        else -> null
    }

    private companion object {
        const val SPACED_ITEM_KEY = "item"
    }
}
