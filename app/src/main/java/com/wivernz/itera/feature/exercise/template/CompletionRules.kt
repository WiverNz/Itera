package com.wivernz.itera.feature.exercise.template

import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.CompletionRule
import com.wivernz.itera.domain.model.ExerciseBlock

/** Whether the primary button may complete the exercise, and if not, why (read out as its state). */
sealed interface CompletionGate {
    data object Ready : CompletionGate

    /** A checklist needs [missing] more ticked items. */
    data class TickMore(val missing: Int) : CompletionGate

    /** A required block is still empty. */
    data object FillBlocks : CompletionGate

    /** A pick-one block has entries but no choice. */
    data object PickOne : CompletionGate
}

/** The three rules of docs/data/00-domain-model.md section 4, evaluated over the current block values. */
object CompletionRules {
    fun evaluate(
        rule: CompletionRule,
        blocks: List<ExerciseBlock>,
        values: Map<String, BlockValue>
    ): CompletionGate = when (rule) {
        CompletionRule.Always -> CompletionGate.Ready
        is CompletionRule.RequireChecked -> {
            val ticked = (values[rule.key] as? BlockValue.Items)?.items
                ?.count { it.done && it.label.isNotBlank() } ?: 0
            if (ticked >= rule.count) {
                CompletionGate.Ready
            } else {
                CompletionGate.TickMore(rule.count - ticked)
            }
        }
        is CompletionRule.RequireBlocks -> {
            val missing = rule.keys.filterNot { isFilled(values[it]) }
            when {
                missing.isEmpty() -> CompletionGate.Ready
                missing.all { key ->
                    blocks.firstOrNull { it.key == key } is ExerciseBlock.PickOne &&
                        (values[key] as? BlockValue.Choice)?.options?.any(String::isNotBlank) ==
                        true
                } -> CompletionGate.PickOne
                else -> CompletionGate.FillBlocks
            }
        }
    }

    /** "Non-empty" per block value type. */
    fun isFilled(value: BlockValue?): Boolean = when (value) {
        null -> false
        is BlockValue.Text -> value.text.isNotBlank()
        is BlockValue.Items -> value.items.any { it.label.isNotBlank() }
        is BlockValue.Choice ->
            value.options.getOrNull(value.chosenIndex)?.isNotBlank() == true
        is BlockValue.Lists ->
            value.primary.any(String::isNotBlank) && value.secondary.any(String::isNotBlank)
        is BlockValue.Chips -> value.selected.isNotEmpty() || !value.custom.isNullOrBlank()
    }

    /** Every free-text field caps at this length; a counter appears past [COUNTER_FROM]. */
    const val MAX_CHARS = 4000
    const val COUNTER_FROM = 3500

    /** The 2-minute rule's stopwatch turns `accent` past this. */
    const val STOPWATCH_THRESHOLD_SECONDS = 120
}
