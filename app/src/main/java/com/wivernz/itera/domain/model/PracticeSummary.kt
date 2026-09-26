package com.wivernz.itera.domain.model

sealed interface PracticeSummary {
    data class Review(val grade: com.wivernz.itera.domain.model.RecallGrade) : PracticeSummary
    data class Text(val value: String) : PracticeSummary
    data class Count(val kind: PracticeKind, val count: Int) : PracticeSummary
    data class Topic(val title: String) : PracticeSummary
    data class Habit(val anchor: String, val habit: String) : PracticeSummary
}
enum class PracticeKind { MINUTES, TASKS, REASONS, STEPS, ITEMS }
fun practiceSummary(result: ActivityResult?, fallback: String): PracticeSummary = when (result) {
    is ActivityResult.Focus -> PracticeSummary.Count(
        PracticeKind.MINUTES,
        result.actualSeconds / 60
    )
    is ActivityResult.Eisenhower -> PracticeSummary.Count(
        PracticeKind.TASKS,
        result.items.count {
            it.quadrant !=
                Quadrant.UNSORTED
        }
    )
    is ActivityResult.Feynman -> PracticeSummary.Topic(result.topicTitle)
    is ActivityResult.Premortem -> PracticeSummary.Count(PracticeKind.REASONS, result.reasons.size)
    is ActivityResult.HabitStack -> PracticeSummary.Habit(result.anchor, result.habit)
    is ActivityResult.Combination -> PracticeSummary.Count(
        PracticeKind.STEPS,
        result.stepResults.size
    )
    is ActivityResult.Template -> {
        val items = result.values.values.filterIsInstance<BlockValue.Items>()
        if (items.isNotEmpty()) {
            PracticeSummary.Count(
                PracticeKind.ITEMS,
                items.sumOf { v ->
                    v.items.count { it.done }
                }
            )
        } else {
            PracticeSummary.Text(
                result.values.values.filterIsInstance<BlockValue.Text>().firstOrNull {
                    it.text.isNotBlank()
                }?.text
                    ?: fallback
            )
        }
    }
    is ActivityResult.Reflection -> PracticeSummary.Text(
        result.tomorrowChange?.takeIf {
            it.isNotBlank()
        } ?: fallback
    )
    is ActivityResult.Review -> PracticeSummary.Review(result.grade)
    null -> PracticeSummary.Text(fallback)
}
