package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 6. */
data class ReviewItem(
    val id: Long,
    val techniqueId: TechniqueId,
    // Feynman reviews are bound to a learning topic
    val topicId: Long?,
    // "4 days ago you explained Redis persistence."
    val prompt: String,
    // the activity that created this item
    val sourceActivityId: Long,
    // hidden until the review is submitted
    val sourceAnswer: String,
    // 0..4 -> intervals 1, 4, 9, 21, 60
    val stageIndex: Int,
    val dueOn: LocalDate,
    val lastReviewedOn: LocalDate?,
    val state: ReviewState
) {
    init {
        require(stageIndex in 0..4)
    }
}

/** Domain model, docs/data/00-domain-model.md section 6. */
enum class ReviewState { SCHEDULED, DUE, COMPLETED_STAGE, RETIRED }

/** Domain model, docs/data/00-domain-model.md section 6. */
data class LearningTopic(
    val id: Long,
    // "Explain what Redis persistence is"
    val title: String,
    val createdAt: Instant,
    val archived: Boolean
)
