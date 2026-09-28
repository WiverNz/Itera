package com.wivernz.itera.data.repository

import androidx.room.withTransaction
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.ReflectionEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.domain.repository.PlannedDay
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.resultTypeOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class RoomTrainingPlanRepository @Inject constructor(
    private val db: IteraDatabase,
    private val copy: CopyResolver,
    private val codec: ResultPayloadCodec,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : TrainingPlanRepository {
    private val days = db.trainingDayDao()
    private val activities = db.planActivityDao()
    private val reflections = db.reflectionDao()

    override fun observeToday() = days.observeByDate(
        LocalDate.now(clock)
            .toEpochDay()
    )
        .combine(AppLanguage.locale) { row, _ -> row?.toDomain(copy, codec) }
    override fun observeDay(id: Long) = days.observeWithActivities(id)
        .combine(AppLanguage.locale) { row, _ -> row?.toDomain(copy, codec) }
    override suspend fun updateActivityState(activityId: Long, state: ActivityState) =
        withContext(io) { activities.updateState(activityId, state.name) }
    override suspend fun saveDraft(activityId: Long, draft: ActivityResult) = withContext(io) {
        val row = requireNotNull(activities.byId(activityId))
        require(resultTypeOf(draft).name == row.exerciseType)
        activities.updateDraft(activityId, codec.encode(draft))
    }
    override suspend fun draft(activityId: Long): ActivityResult? = withContext(io) {
        val row = activities.byId(activityId) ?: return@withContext null
        codec.decode(activities.draft(activityId), activityId, row.exerciseType)
    }
    override suspend fun snoozeActivity(activityId: Long, until: Instant) =
        withContext(io) { activities.snooze(activityId, until.toEpochMilli()) }
    override suspend fun skipActivity(activityId: Long) =
        withContext(io) { activities.skip(activityId) }

    override suspend fun dayByDate(date: LocalDate) =
        withContext(io) { days.byDate(date.toEpochDay())?.toDomain(copy, codec) }
    override suspend fun day(id: Long) = withContext(io) { days.byId(id)?.toDomain(copy, codec) }
    override suspend fun daysBefore(date: LocalDate) =
        withContext(io) { days.before(date.toEpochDay()).map { it.toDomain(copy, codec) } }
    override suspend fun latestCompletedDay() =
        withContext(io) { days.latestCompleted()?.let { days.byId(it.id)?.toDomain(copy, codec) } }
    override suspend fun activity(id: Long) =
        withContext(io) { activities.byId(id)?.toDomain(copy, codec) }
    override suspend fun generatorVersion(dayId: Long) =
        withContext(io) { days.generatorVersion(dayId) }

    override suspend fun insertDay(plan: PlannedDay, createdAt: Instant): Long =
        db.withTransaction {
            days.findByDate(plan.date.toEpochDay())?.id ?: run {
                val dayId = days.insert(
                    TrainingDayEntity(
                        programDay = plan.programDay,
                        date = plan.date.toEpochDay(),
                        status = TrainingDayStatus.PLANNED.name,
                        carryOverIntent = plan.carryOverIntent,
                        generatorVersion = plan.generatorVersion,
                        createdAt = createdAt.toEpochMilli()
                    )
                )
                activities.insertAll(plan.activities.map { it.toEntity(dayId) })
                dayId
            }
        }
    override suspend fun deleteDay(id: Long) = withContext(io) { days.delete(id) }
    override suspend fun insertActivity(dayId: Long, activity: PlannedActivity): Long =
        withContext(io) { activities.insertAll(listOf(activity.toEntity(dayId))).single() }
    override suspend fun updateDayStatus(
        dayId: Long,
        status: TrainingDayStatus,
        completedAt: Instant?
    ) = withContext(io) { days.updateStatus(dayId, status.name, completedAt?.toEpochMilli()) }
    override suspend fun start(activityId: Long, at: Instant) =
        withContext(io) { activities.start(activityId, at.toEpochMilli()) }
    override suspend fun abandon(activityId: Long) =
        withContext(io) { activities.abandon(activityId) }
    override suspend fun makeAvailable(activityId: Long) =
        withContext(io) { activities.makeAvailable(activityId) }
    override suspend fun complete(
        activityId: Long,
        result: ActivityResult?,
        difficulty: Difficulty?,
        note: String?,
        at: Instant,
        durationSeconds: Int?
    ) = withContext(io) {
        activities.complete(
            activityId,
            result?.let(codec::encode),
            difficulty?.name,
            note,
            at.toEpochMilli(),
            durationSeconds
        )
    }
    override suspend fun updateFeedback(activityId: Long, difficulty: Difficulty?, note: String?) =
        withContext(io) { activities.updateFeedback(activityId, difficulty?.name, note) }
    override suspend fun expire(activityIds: List<Long>) = withContext(io) {
        if (activityIds.isNotEmpty()) activities.expire(activityIds)
    }
    override suspend fun programDayOfActivity(activityId: Long) =
        withContext(io) { days.programDayOfActivity(activityId) }
    override suspend fun latestIntent() = withContext(io) { reflections.latestIntent() }
    override suspend fun saveReflection(
        dayId: Long,
        date: LocalDate,
        result: ActivityResult.Reflection?,
        skipped: Boolean,
        at: Instant
    ) = withContext(io) {
        val existing = reflections.byDay(dayId)
        val chips = ListSerializer(String.serializer())
        reflections.upsert(
            ReflectionEntity(
                id = existing?.id ?: 0,
                trainingDayId = dayId,
                date = date.toEpochDay(),
                wentWell = result?.wentWell,
                wentWellChips = Json.encodeToString(chips, result?.wentWellChips.orEmpty()),
                didNotGoWell = result?.didNotGoWell,
                didNotGoWellChips = Json.encodeToString(chips, result?.didNotGoWellChips.orEmpty()),
                tomorrowChange = result?.tomorrowChange,
                skipped = skipped,
                createdAt = existing?.createdAt ?: at.toEpochMilli()
            )
        )
        Unit
    }

    private fun PlannedActivity.toEntity(dayId: Long) = PlanActivityEntity(
        trainingDayId = dayId,
        techniqueId = techniqueId.value,
        exerciseType = exerciseType.name,
        source = source.name,
        orderIndex = orderIndex,
        dayPart = dayPart.name,
        copyKey = copyKey,
        copyArgs = encodeCopyArgs(copyArgs),
        estimatedMinutes = estimatedMinutes,
        optional = optional,
        state = state.name,
        scheduledAtMinutes = scheduledAt?.let { it.hour * 60 + it.minute },
        draftPayload = draft?.let(codec::encode),
        reviewItemId = reviewItemId,
        // derived from the parent day by PlanActivityDao.insertAll
        practiceDate = -1
    )
}

/** Deterministic: keys keep the generator's insertion order. */
internal fun encodeCopyArgs(args: Map<String, Any>): String = JsonObject(
    args.mapValues { (_, value) ->
        when (value) {
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> error("Unsupported copy argument")
        }
    }
).toString()
