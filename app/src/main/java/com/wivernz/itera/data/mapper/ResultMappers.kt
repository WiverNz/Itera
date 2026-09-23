package com.wivernz.itera.data.mapper
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CombinationStepResult
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.TechniqueId
import java.time.Instant
import java.time.LocalTime
fun ActivityResult.toDto(): ActivityResultDto = when (this) {
    is ActivityResult.Template ->
        ActivityResultDto.Template(
            values.mapValues {
                it.value.toDto()
            }
        )
    is ActivityResult.Focus -> ActivityResultDto.Focus(
        taskLabel,
        plannedSeconds,
        actualSeconds,
        extendedSeconds,
        completedNaturally
    )
    is ActivityResult.Eisenhower ->
        ActivityResultDto.Eisenhower(
            items.map {
                it.toDto()
            },
            chosenItemId
        )
    is ActivityResult.Feynman -> ActivityResultDto.Feynman(
        topicId,
        topicTitle,
        explanation,
        wordCount,
        hardestParts,
        reflectionNote
    )
    is ActivityResult.Premortem -> ActivityResultDto.Premortem(
        projectName,
        reasons.map { it.toDto() },
        mitigationAction,
        mitigationAddedToToday
    )
    is ActivityResult.HabitStack -> ActivityResultDto.HabitStack(
        anchor,
        habit,
        nudgeEnabled,
        nudgeTime?.let { it.hour * 60 + it.minute }
    )
    is ActivityResult.Reflection -> ActivityResultDto.Reflection(
        wentWell,
        wentWellChips,
        didNotGoWell,
        didNotGoWellChips,
        tomorrowChange
    )
    is ActivityResult.Review ->
        ActivityResultDto.Review(
            reviewItemId,
            answer,
            grade.name,
            previousAnswer
        )
    is ActivityResult.Combination ->
        ActivityResultDto.Combination(
            stepResults.map {
                it.toDto()
            }
        )
}
fun ActivityResultDto.toDomain(): ActivityResult = when (this) {
    is ActivityResultDto.Template ->
        ActivityResult.Template(
            values.mapValues {
                it.value.toDomain()
            }
        )
    is ActivityResultDto.Focus -> ActivityResult.Focus(
        taskLabel,
        plannedSeconds,
        actualSeconds,
        extendedSeconds,
        completedNaturally
    )
    is ActivityResultDto.Eisenhower ->
        ActivityResult.Eisenhower(
            items.map {
                it.toDomain()
            },
            chosenItemId
        )
    is ActivityResultDto.Feynman -> ActivityResult.Feynman(
        topicId,
        topicTitle,
        explanation,
        wordCount,
        hardestParts,
        reflectionNote
    )
    is ActivityResultDto.Premortem -> ActivityResult.Premortem(
        projectName,
        reasons.map { it.toDomain() },
        mitigationAction,
        mitigationAddedToToday
    )
    is ActivityResultDto.HabitStack -> ActivityResult.HabitStack(
        anchor,
        habit,
        nudgeEnabled,
        nudgeTime?.let { LocalTime.ofSecondOfDay(it * 60L) }
    )
    is ActivityResultDto.Reflection -> ActivityResult.Reflection(
        wentWell,
        wentWellChips,
        didNotGoWell,
        didNotGoWellChips,
        tomorrowChange
    )
    is ActivityResultDto.Review ->
        ActivityResult.Review(
            reviewItemId,
            answer,
            RecallGrade.valueOf(grade),
            previousAnswer
        )
    is ActivityResultDto.Combination ->
        ActivityResult.Combination(
            stepResults.map {
                it.toDomain()
            }
        )
}
fun BlockValue.toDto(): BlockValueDto = when (this) {
    is BlockValue.Text -> BlockValueDto.Text(text)
    is BlockValue.Items -> BlockValueDto.Items(items.map { it.toDto() })
    is BlockValue.Choice -> BlockValueDto.Choice(options, chosenIndex)
    is BlockValue.Lists -> BlockValueDto.Lists(primary, secondary)
    is BlockValue.Chips -> BlockValueDto.Chips(selected, custom)
}
fun BlockValueDto.toDomain(): BlockValue = when (this) {
    is BlockValueDto.Text -> BlockValue.Text(text)
    is BlockValueDto.Items -> BlockValue.Items(items.map { it.toDomain() })
    is BlockValueDto.Choice -> BlockValue.Choice(options, chosenIndex)
    is BlockValueDto.Lists -> BlockValue.Lists(primary, secondary)
    is BlockValueDto.Chips -> BlockValue.Chips(selected, custom)
}
fun ChecklistItem.toDto(): ChecklistItemDto = ChecklistItemDto(
    id,
    label,
    done,
    elapsedSeconds
)
fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    id,
    label,
    done,
    elapsedSeconds
)
fun EisenhowerItem.toDto(): EisenhowerItemDto = EisenhowerItemDto(
    id,
    label,
    quadrant.name
)
fun EisenhowerItemDto.toDomain(): EisenhowerItem = EisenhowerItem(
    id,
    label,
    Quadrant.valueOf(quadrant)
)
fun PremortemReason.toDto(): PremortemReasonDto = PremortemReasonDto(
    text,
    likelihood.name
)
fun PremortemReasonDto.toDomain(): PremortemReason = PremortemReason(
    text,
    Likelihood.valueOf(likelihood)
)
fun CombinationStepResult.toDto(): CombinationStepResultDto = CombinationStepResultDto(
    techniqueId.value,
    summary,
    completedAt.toEpochMilli()
)
fun CombinationStepResultDto.toDomain(): CombinationStepResult = CombinationStepResult(
    TechniqueId(techniqueId),
    summary,
    Instant.ofEpochMilli(completedAt)
)
