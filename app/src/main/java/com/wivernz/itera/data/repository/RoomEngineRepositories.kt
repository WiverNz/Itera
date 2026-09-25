package com.wivernz.itera.data.repository

import androidx.room.withTransaction
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.ResetTiers
import com.wivernz.itera.data.database.dao.FocusSessionDao
import com.wivernz.itera.data.database.dao.HabitStackDao
import com.wivernz.itera.data.database.dao.ResetDao
import com.wivernz.itera.data.database.dao.TechniqueStateDao
import com.wivernz.itera.data.database.entity.FocusSessionEntity
import com.wivernz.itera.data.database.entity.HabitStackEntity
import com.wivernz.itera.data.database.entity.TechniqueStateEntity
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.DataResetRepository
import com.wivernz.itera.domain.repository.FocusSessionRecord
import com.wivernz.itera.domain.repository.HabitStackRecord
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import com.wivernz.itera.domain.repository.TechniqueStateRecord
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomTransactionRunner @Inject constructor(private val db: IteraDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        db.withTransaction { block() }
}

class RoomTechniqueStateRepository @Inject constructor(
    private val dao: TechniqueStateDao,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : TechniqueStateRepository {
    override fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    override suspend fun all() = withContext(io) { dao.all().map { it.toDomain() } }
    override suspend fun ensureRows(ids: List<TechniqueId>) = withContext(io) {
        dao.insertMissing(ids.map { TechniqueStateEntity(techniqueId = it.value) })
    }
    override suspend fun unlock(id: TechniqueId, at: Instant, programDay: Int) =
        withContext(io) { dao.unlock(id.value, at.toEpochMilli(), programDay) > 0 }
    override suspend fun markIntroCompleted(id: TechniqueId, at: Instant) =
        withContext(io) { dao.markIntroComplete(id.value, at.toEpochMilli()) > 0 }
    override suspend fun resetProgramFacts(seeded: Set<TechniqueId>, at: Instant) =
        withContext(io) {
            dao.clearAll()
            seeded.forEach { dao.unlock(it.value, at.toEpochMilli(), 1) }
        }

    private fun TechniqueStateEntity.toDomain() = TechniqueStateRecord(
        TechniqueId(techniqueId),
        unlockedAt?.let(Instant::ofEpochMilli),
        unlockedOnProgramDay,
        introCompletedAt?.let(Instant::ofEpochMilli)
    )
}

class RoomPracticeRecordRepository @Inject constructor(
    private val focus: FocusSessionDao,
    private val habits: HabitStackDao,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : PracticeRecordRepository {
    override suspend fun insertFocusSession(record: FocusSessionRecord) = withContext(io) {
        focus.insert(
            FocusSessionEntity(
                activityId = record.activityId,
                techniqueId = record.techniqueId.value,
                taskLabel = record.taskLabel,
                plannedSeconds = record.plannedSeconds,
                actualSeconds = record.actualSeconds,
                extendedSeconds = record.extendedSeconds,
                completedNaturally = record.completedNaturally,
                startedAt = record.startedAt.toEpochMilli(),
                endedAt = record.endedAt.toEpochMilli()
            )
        )
    }
    override suspend fun insertHabitStack(record: HabitStackRecord) = withContext(io) {
        habits.insert(
            HabitStackEntity(
                activityId = record.activityId,
                anchor = record.anchor,
                habit = record.habit,
                nudgeEnabled = record.nudgeEnabled,
                nudgeTimeMinutes = record.nudgeTime?.let { it.hour * 60 + it.minute },
                createdAt = record.createdAt.toEpochMilli(),
                archived = false
            )
        )
    }
}

/**
 * Executes the ADR-0014 tiers. [deletes] names every deletable table with its delete, in foreign-key-safe order;
 * `ResetCoverageTest` checks it against the schema and [ResetTiers].
 */
class RoomDataResetRepository @Inject constructor(private val dao: ResetDao) : DataResetRepository {
    val deletes: List<Pair<String, suspend () -> Unit>> = listOf(
        "review_attempt" to dao::deleteReviewAttempts,
        "focus_session" to dao::deleteFocusSessions,
        "reflection_entry" to dao::deleteReflections,
        "habit_stack" to dao::deleteHabitStacks,
        "plan_activity" to dao::deletePlanActivities,
        "training_day" to dao::deleteTrainingDays,
        "review_item" to dao::deleteReviewItems,
        "learning_topic" to dao::deleteLearningTopics,
        "event_log" to dao::deleteEventLog
    )

    override suspend fun deleteProgramData() = run(ResetTiers.program)

    override suspend fun deleteAllData() = run(ResetTiers.program + ResetTiers.eraseOnly)

    private suspend fun run(tables: Set<String>) {
        deletes.filter { it.first in tables }.forEach { it.second() }
    }
}
