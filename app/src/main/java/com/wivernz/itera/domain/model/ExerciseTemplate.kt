package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 4. */
data class ExerciseTemplate(val blocks: List<ExerciseBlock>, val completionRule: CompletionRule)

/** Domain model, docs/data/00-domain-model.md section 4. */
sealed interface ExerciseBlock {
    val key: String

    /** Static guidance paragraph. */
    data class Instruction(override val key: String, val text: String, val emphasis: Boolean) :
        ExerciseBlock

    /** Free text. Autosaved as a draft. */
    data class TextInput(
        override val key: String,
        val label: String,
        val placeholder: String,
        val minLines: Int,
        val maxLines: Int
    ) : ExerciseBlock

    /** Tick-off list the user fills in themselves. `withStopwatch` powers the 2-minute rule. */
    data class Checklist(
        override val key: String,
        val label: String,
        val minItems: Int,
        val maxItems: Int,
        val withStopwatch: Boolean,
        val addItemLabel: String
    ) : ExerciseBlock

    /** User enters several items, then picks exactly one. Powers 80/20. */
    data class PickOne(
        override val key: String,
        val label: String,
        val itemCount: Int,
        val suggestions: List<String>
    ) : ExerciseBlock

    /** Two named lists. Powers the Two-list strategy. */
    data class TwoLists(
        override val key: String,
        // "Top 5"
        val primaryLabel: String,
        // "Avoid at all costs"
        val secondaryLabel: String,
        val primaryCount: Int,
        val secondaryCount: Int
    ) : ExerciseBlock

    /** Multi-select suggestion chips plus an optional free-text escape hatch. */
    data class ChipSelect(
        override val key: String,
        val label: String,
        val options: List<String>,
        val allowCustom: Boolean,
        val maxSelections: Int
    ) : ExerciseBlock
}

/** Domain model, docs/data/00-domain-model.md section 4. */
sealed interface CompletionRule {
    /** Always completable - a single "Done" tap. Used by practice prompts. */
    data object Always : CompletionRule

    /** Every listed block key must be non-empty. */
    data class RequireBlocks(val keys: List<String>) : CompletionRule

    /** A Checklist block must have at least N ticked items. */
    data class RequireChecked(val key: String, val count: Int) : CompletionRule
}
