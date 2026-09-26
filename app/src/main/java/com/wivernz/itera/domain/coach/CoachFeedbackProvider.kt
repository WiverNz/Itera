package com.wivernz.itera.domain.coach
/** ADR-0016: interface only; no feedback provider ships in milestone 002. */
interface CoachFeedbackProvider {
    val isAvailable: Boolean
    suspend fun feedback(request: CoachFeedbackRequest): Result<CoachFeedback>
}

/** Minimal request contract, source-of-truth section 11. */
data class CoachFeedbackRequest(val topicTitle: String, val explanation: String)

/** Minimal response contract, source-of-truth section 11. */
data class CoachFeedback(
    val strengths: List<String>,
    val gaps: List<String>,
    val questions: List<String>
)

/** The only provider in the MVP (ADR-0016): never available, never produces text. */
class NoOpCoachFeedbackProvider @javax.inject.Inject constructor() : CoachFeedbackProvider {
    override val isAvailable: Boolean = false
    override suspend fun feedback(request: CoachFeedbackRequest): Result<CoachFeedback> =
        Result.failure(UnsupportedOperationException("No coach provider"))
}
