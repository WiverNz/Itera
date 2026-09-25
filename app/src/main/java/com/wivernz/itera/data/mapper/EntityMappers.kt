package com.wivernz.itera.data.mapper
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.entity.FocusSessionEntity
import com.wivernz.itera.data.database.entity.HabitStackEntity
import com.wivernz.itera.data.database.entity.LearningTopicEntity
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.ReflectionEntity
import com.wivernz.itera.data.database.entity.ReviewAttemptEntity
import com.wivernz.itera.data.database.entity.ReviewItemEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
fun TrainingDayWithActivities.toDomain(copy: CopyResolver, codec: ResultPayloadCodec): TrainingDay =
    TrainingDay(
        day.id,
        day.programDay,
        LocalDate.ofEpochDay(day.date),
        TrainingDayStatus.valueOf(day.status),
        day.carryOverIntent,
        activities.sortedWith(
            compareBy(
                { it.orderIndex },
                { it.id }
            )
        ).map {
            it.toDomain(
                copy,
                codec
            )
        },
        day.completedAt?.let(Instant::ofEpochMilli)
    )
fun TrainingDay.toEntity(original: TrainingDayEntity): TrainingDayEntity = original.copy(
    id = id,
    programDay = programDay,
    date = date.toEpochDay(),
    status = status.name,
    carryOverIntent = carryOverIntent,
    completedAt = completedAt?.toEpochMilli()
)
fun PlanActivityEntity.toDomain(copy: CopyResolver, codec: ResultPayloadCodec): PlanActivity {
    val text = copy.resolve(copyKey, copyArgs, techniqueId)
    return PlanActivity(
        id,
        trainingDayId,
        TechniqueId(techniqueId),
        ExerciseType.valueOf(exerciseType),
        ActivitySource.valueOf(source),
        orderIndex,
        DayPart.valueOf(dayPart),
        text.title,
        text.subtitle,
        text.instruction,
        estimatedMinutes,
        ActivityState.valueOf(state),
        optional,
        scheduledAtMinutes?.let { LocalTime.ofSecondOfDay(it * 60L) },
        snoozedUntil?.let(Instant::ofEpochMilli),
        startedAt?.let(Instant::ofEpochMilli),
        completedAt?.let(Instant::ofEpochMilli),
        durationSeconds,
        difficulty?.let(Difficulty::valueOf),
        note,
        codec.decode(
            resultPayload,
            id,
            exerciseType
        ),
        reviewItemId,
        weeklyLookBack = copyArgs.contains("\"weeklyLookBack\":true")
    )
}
fun PlanActivity.toEntity(
    original: PlanActivityEntity,
    codec: ResultPayloadCodec
): PlanActivityEntity = original.copy(
    id = id,
    trainingDayId = trainingDayId,
    techniqueId = techniqueId.value,
    exerciseType = exerciseType.name,
    source = source.name,
    orderIndex = orderIndex,
    dayPart = dayPart.name,
    estimatedMinutes = estimatedMinutes,
    state = state.name,
    optional = optional,
    scheduledAtMinutes = scheduledAt?.let { it.hour * 60 + it.minute },
    snoozedUntil = snoozedUntil?.toEpochMilli(),
    startedAt = startedAt?.toEpochMilli(),
    completedAt = completedAt?.toEpochMilli(),
    durationSeconds = durationSeconds,
    difficulty = difficulty?.name,
    note = note,
    resultPayload = result?.let(codec::encode),
    reviewItemId = reviewItemId
)
fun ReviewItemEntity.toDomain(): ReviewItem = ReviewItem(
    id,
    TechniqueId(techniqueId),
    topicId,
    prompt,
    sourceActivityId,
    sourceAnswer,
    stageIndex,
    LocalDate.ofEpochDay(dueOn),
    lastReviewedOn?.let(LocalDate::ofEpochDay),
    ReviewState.valueOf(state)
)
fun ReviewItem.toEntity(createdAt: Instant): ReviewItemEntity = ReviewItemEntity(
    id,
    techniqueId.value,
    topicId,
    prompt,
    sourceActivityId,
    sourceAnswer,
    stageIndex,
    dueOn.toEpochDay(),
    lastReviewedOn?.toEpochDay(),
    state.name,
    createdAt.toEpochMilli()
)
fun LearningTopicEntity.toDomain(): LearningTopic = LearningTopic(
    id,
    title,
    Instant.ofEpochMilli(createdAt),
    archived
)
fun LearningTopic.toEntity(): LearningTopicEntity = LearningTopicEntity(
    id,
    title,
    archived,
    createdAt.toEpochMilli()
)
fun FocusSessionEntity.toDomain(): ActivityResult.Focus = ActivityResult.Focus(
    taskLabel,
    plannedSeconds,
    actualSeconds,
    extendedSeconds,
    completedNaturally
)
fun ActivityResult.Focus.toEntity(original: FocusSessionEntity): FocusSessionEntity = original.copy(
    taskLabel = taskLabel,
    plannedSeconds = plannedSeconds,
    actualSeconds = actualSeconds,
    extendedSeconds = extendedSeconds,
    completedNaturally = completedNaturally
)
fun ReflectionEntity.toDomain(): ActivityResult.Reflection = ActivityResult.Reflection(
    wentWell,
    kotlinx.serialization.json.Json.decodeFromString<List<String>>(wentWellChips),
    didNotGoWell,
    kotlinx.serialization.json.Json.decodeFromString<List<String>>(didNotGoWellChips),
    tomorrowChange
)
fun ActivityResult.Reflection.toEntity(original: ReflectionEntity): ReflectionEntity =
    original.copy(
        wentWell = wentWell,
        wentWellChips = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(
                kotlinx.serialization.serializer<String>()
            ),

            wentWellChips
        ),
        didNotGoWell = didNotGoWell,
        didNotGoWellChips = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(
                kotlinx.serialization.serializer<String>()
            ),

            didNotGoWellChips
        ),
        tomorrowChange = tomorrowChange
    )
fun HabitStackEntity.toDomain(): ActivityResult.HabitStack = ActivityResult.HabitStack(
    anchor,
    habit,
    nudgeEnabled,
    nudgeTimeMinutes?.let { LocalTime.ofSecondOfDay(it * 60L) }
)
fun ActivityResult.HabitStack.toEntity(original: HabitStackEntity): HabitStackEntity =
    original.copy(
        anchor = anchor,
        habit = habit,
        nudgeEnabled = nudgeEnabled,
        nudgeTimeMinutes = nudgeTime?.let { it.hour * 60 + it.minute }
    )
fun ReviewAttemptEntity.toDomain(previousAnswer: String): ActivityResult.Review =
    ActivityResult.Review(
        reviewItemId,
        answer,
        RecallGrade.valueOf(grade),
        previousAnswer
    )
fun ActivityResult.Review.toEntity(original: ReviewAttemptEntity): ReviewAttemptEntity =
    original.copy(
        reviewItemId =
        reviewItemId,
        answer =
        answer,
        grade =
        grade.name
    )
