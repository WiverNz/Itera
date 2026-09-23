package com.wivernz.itera.data.repository
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.dao.PlanActivityDao
import com.wivernz.itera.data.database.dao.TrainingDayDao
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.repository.TrainingPlanStorage
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
/** Storage implementation; the full TrainingPlanRepository is composed by milestone 004. */
class RoomTrainingPlanRepository @Inject constructor(
    private val days: TrainingDayDao,
    private val activities: PlanActivityDao,
    private val copy: CopyResolver,
    private val codec: ResultPayloadCodec,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : TrainingPlanStorage {
    override fun observeToday() = days.observeByDate(
        LocalDate.now(clock)
            .toEpochDay()
    )
        .map {
            it?.toDomain(copy, codec)
        }
    override fun observeDay(id: Long) =
        days.observeWithActivities(id).map { it?.toDomain(copy, codec) }
    override suspend fun updateActivityState(activityId: Long, state: ActivityState) =
        withContext(io) { activities.updateState(activityId, state.name) }
    override suspend fun saveDraft(activityId: Long, draft: ActivityResult) = withContext(io) {
        val row = requireNotNull(activities.byId(activityId))
        require(resultType(draft).name == row.exerciseType)
        activities.updateDraft(activityId, codec.encode(draft))
    }
    override suspend fun snoozeActivity(activityId: Long, until: Instant) =
        withContext(io) { activities.snooze(activityId, until.toEpochMilli()) }
    override suspend fun skipActivity(activityId: Long) =
        updateActivityState(activityId, ActivityState.SKIPPED)
}
internal fun resultType(result: ActivityResult): ExerciseType = when (result) {
    is ActivityResult.Template -> ExerciseType.TEMPLATE
    is ActivityResult.Focus -> ExerciseType.FOCUS_TIMER
    is ActivityResult.Eisenhower -> ExerciseType.EISENHOWER
    is ActivityResult.Feynman -> ExerciseType.FEYNMAN
    is ActivityResult.Premortem -> ExerciseType.PREMORTEM
    is ActivityResult.HabitStack -> ExerciseType.HABIT_STACK
    is ActivityResult.Reflection -> ExerciseType.REFLECTION
    is ActivityResult.Review -> ExerciseType.REVIEW
    is ActivityResult.Combination -> ExerciseType.COMBINATION
}
