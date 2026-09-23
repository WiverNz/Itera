package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 5. */
sealed interface ActivityResult {

    data class Template(
        // keyed by ExerciseBlock.key
        val values: Map<String, BlockValue>
    ) : ActivityResult

    data class Focus(
        val taskLabel: String,
        val plannedSeconds: Int,
        val actualSeconds: Int,
        val extendedSeconds: Int,
        // false if ended early
        val completedNaturally: Boolean
    ) : ActivityResult

    data class Eisenhower(
        val items: List<EisenhowerItem>,
        // the "Do now" item carried into the next step
        val chosenItemId: String?
    ) : ActivityResult

    data class Feynman(
        val topicId: Long,
        val topicTitle: String,
        val explanation: String,
        val wordCount: Int,
        // chips + custom
        val hardestParts: List<String>,
        val reflectionNote: String?
    ) : ActivityResult

    data class Premortem(
        val projectName: String,
        val reasons: List<PremortemReason>,
        val mitigationAction: String?,
        val mitigationAddedToToday: Boolean
    ) : ActivityResult

    data class HabitStack(
        // "make coffee"
        val anchor: String,
        // "read one page"
        val habit: String,
        val nudgeEnabled: Boolean,
        val nudgeTime: LocalTime?
    ) : ActivityResult

    data class Reflection(
        val wentWell: String?,
        val wentWellChips: List<String>,
        val didNotGoWell: String?,
        val didNotGoWellChips: List<String>,
        val tomorrowChange: String?
    ) : ActivityResult

    data class Review(
        val reviewItemId: Long,
        val answer: String,
        val grade: RecallGrade,
        val previousAnswer: String
    ) : ActivityResult

    data class Combination(val stepResults: List<CombinationStepResult>) : ActivityResult
}

/** Domain model, docs/data/00-domain-model.md section 5. */
sealed interface BlockValue {
    data class Text(val text: String) : BlockValue
    data class Items(val items: List<ChecklistItem>) : BlockValue
    data class Choice(val options: List<String>, val chosenIndex: Int) : BlockValue
    data class Lists(val primary: List<String>, val secondary: List<String>) : BlockValue
    data class Chips(val selected: List<String>, val custom: String?) : BlockValue
}

/** Domain model, docs/data/00-domain-model.md section 5. */
data class ChecklistItem(
    val id: String,
    val label: String,
    val done: Boolean,
    val elapsedSeconds: Int?
)

/** Domain model, docs/data/00-domain-model.md section 5. */
data class EisenhowerItem(val id: String, val label: String, val quadrant: Quadrant)

/** Domain model, docs/data/00-domain-model.md section 5. */
enum class Quadrant { UNSORTED, DO_NOW, SCHEDULE, DELEGATE, DROP }

/** Domain model, docs/data/00-domain-model.md section 5. */
data class PremortemReason(val text: String, val likelihood: Likelihood)

/** Domain model, docs/data/00-domain-model.md section 5. */
enum class Likelihood { POSSIBLE, LIKELY, CERTAIN }

/** Domain model, docs/data/00-domain-model.md section 5. */
data class CombinationStepResult(
    val techniqueId: TechniqueId,
    val summary: String,
    val completedAt: Instant
)
