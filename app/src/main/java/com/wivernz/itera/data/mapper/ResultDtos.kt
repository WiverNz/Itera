package com.wivernz.itera.data.mapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
sealed interface ActivityResultDto {

    @Serializable
    @SerialName("template")
    data class Template(val values: Map<String, BlockValueDto>) : ActivityResultDto

    @Serializable
    @SerialName("focus")
    data class Focus(
        val taskLabel: String,
        val plannedSeconds: Int,
        val actualSeconds: Int,
        val extendedSeconds: Int,
        val completedNaturally: Boolean
    ) : ActivityResultDto

    @Serializable
    @SerialName("eisenhower")
    data class Eisenhower(val items: List<EisenhowerItemDto>, val chosenItemId: String?) :
        ActivityResultDto

    @Serializable
    @SerialName("feynman")
    data class Feynman(
        val topicId: Long,
        val topicTitle: String,
        val explanation: String,
        val wordCount: Int,
        val hardestParts: List<String>,
        val reflectionNote: String?
    ) : ActivityResultDto

    @Serializable
    @SerialName("premortem")
    data class Premortem(
        val projectName: String,
        val reasons: List<PremortemReasonDto>,
        val mitigationAction: String?,
        val mitigationAddedToToday: Boolean
    ) : ActivityResultDto

    @Serializable
    @SerialName("habit_stack")
    data class HabitStack(
        val anchor: String,
        val habit: String,
        val nudgeEnabled: Boolean,
        val nudgeTime: Int?
    ) : ActivityResultDto

    @Serializable
    @SerialName("reflection")
    data class Reflection(
        val wentWell: String?,
        val wentWellChips: List<String>,
        val didNotGoWell: String?,
        val didNotGoWellChips: List<String>,
        val tomorrowChange: String?
    ) : ActivityResultDto

    @Serializable
    @SerialName("review")
    data class Review(
        val reviewItemId: Long,
        val answer: String,
        val grade: String,
        val previousAnswer: String
    ) : ActivityResultDto

    @Serializable
    @SerialName("combination")
    data class Combination(val stepResults: List<CombinationStepResultDto>) : ActivityResultDto
}

@Serializable
sealed interface BlockValueDto {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : BlockValueDto

    @Serializable
    @SerialName("items")
    data class Items(val items: List<ChecklistItemDto>) : BlockValueDto

    @Serializable
    @SerialName("choice")
    data class Choice(val options: List<String>, val chosenIndex: Int) : BlockValueDto

    @Serializable
    @SerialName("lists")
    data class Lists(val primary: List<String>, val secondary: List<String>) : BlockValueDto

    @Serializable
    @SerialName("chips")
    data class Chips(val selected: List<String>, val custom: String?) : BlockValueDto
}

@Serializable
@SerialName("checklist_item")
data class ChecklistItemDto(
    val id: String,
    val label: String,
    val done: Boolean,
    val elapsedSeconds: Int?
)

@Serializable
@SerialName("eisenhower_item")
data class EisenhowerItemDto(val id: String, val label: String, val quadrant: String)

@Serializable
@SerialName("premortem_reason")
data class PremortemReasonDto(val text: String, val likelihood: String)

@Serializable
@SerialName("combination_step_result")
data class CombinationStepResultDto(
    val techniqueId: String,
    val summary: String,
    val completedAt: Long
)
