package com.itera.app.data

/** Prototype-only record. It never participates in curriculum, mastery or daily completion. */
data class PredictionState(
    val page: Int = 0,
    val prediction: String = "",
    val confidenceBefore: Int = -1,
    val action: String = "",
    val evidence: String = "",
    val outcome: String = "",
    val comparison: Int = -1,
    val confidenceAfter: Int = -1,
    val lesson: String = "",
    val noConclusion: Boolean = false,
    val preparedDate: String = "",
    val closed: Boolean = false,
) {
    val hasRecord: Boolean get() = prediction.isNotBlank() || action.isNotBlank()
    val frozen: Boolean get() = preparedDate.isNotEmpty()
    val finished: Boolean get() = page == 6 || closed
    val canAdvance: Boolean get() = when (page) {
        0 -> prediction.isNotBlank()
        1 -> action.isNotBlank() && evidence.isNotBlank()
        2 -> true
        3 -> outcome.isNotBlank()
        4 -> comparison in 0..3
        5 -> lesson.isNotBlank() || noConclusion
        else -> false
    }

    fun advance(date: String): PredictionState =
        if (!canAdvance || finished) this
        else copy(page = page + 1, preparedDate = if (page == 1) date else preparedDate)

    fun previous(): PredictionState =
        if (page == 1 || page in 4..5) copy(page = page - 1) else this
}

