package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
abstract class PlanActivityDao {
    @Query(
        "SELECT * FROM plan_activity WHERE trainingDayId = :dayId ORDER BY orderIndex, id"
    )
    abstract fun observeForDay(dayId: Long): Flow<List<PlanActivityEntity>>

    @Query("SELECT * FROM plan_activity WHERE id = :id")
    abstract suspend fun byId(id: Long): PlanActivityEntity?

    @Query("UPDATE plan_activity SET state = :state WHERE id = :id")
    abstract suspend fun updateState(id: Long, state: String)

    @Query(
        "UPDATE plan_activity SET draftPayload = :payload WHERE id = :id"
    )
    abstract suspend fun updateDraft(id: Long, payload: String)

    @Query(
        "UPDATE plan_activity SET state = 'COMPLETED', resultPayload = :payload, draftPayload = NULL, difficulty = :difficulty, note = :note, completedAt = :at, durationSeconds = :duration WHERE id = :id"
    )
    abstract suspend fun complete(
        id: Long,
        payload: String,
        difficulty: String?,
        note: String?,
        at: Long,
        duration: Int?
    )

    @Query(
        "UPDATE plan_activity SET state = 'SNOOZED', snoozedUntil = :until WHERE id = :id"
    )
    abstract suspend fun snooze(id: Long, until: Long)

    @Insert protected abstract suspend fun insertRows(rows: List<PlanActivityEntity>): List<Long>

    @Query("SELECT date FROM training_day WHERE id = :id")
    protected abstract suspend fun parentDate(id: Long): Long?

/** All insert paths derive practiceDate from the parent, including manual practice. */
    @Transaction
    open suspend fun insertAll(rows: List<PlanActivityEntity>): List<Long> = insertRows(
        rows.map { row ->
            row.copy(
                practiceDate =
                requireNotNull(parentDate(row.trainingDayId))
            )
        }
    )

    @Query(
        "SELECT COUNT(DISTINCT practiceDate) FROM plan_activity WHERE techniqueId = :techniqueId AND state = 'COMPLETED' AND copyKey != 'activity_focus_generic'"
    )
    abstract fun countDistinctPracticeDays(techniqueId: String): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM plan_activity WHERE techniqueId = :techniqueId AND practiceDate >= :epochDay AND state = 'COMPLETED' AND copyKey != 'activity_focus_generic'"
    )
    abstract fun usesSince(techniqueId: String, epochDay: Long): Flow<Int>

    @Query(
        "SELECT MIN(practiceDate) FROM plan_activity WHERE techniqueId = :techniqueId AND state = 'COMPLETED' AND copyKey != 'activity_focus_generic'"
    )
    abstract fun firstUse(techniqueId: String): Flow<Long?>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM plan_activity WHERE techniqueId = :techniqueId AND source = 'COMBINATION' AND state = 'COMPLETED')"
    )
    abstract fun usedInCombination(techniqueId: String): Flow<Boolean>

    @Query(
        "SELECT * FROM plan_activity WHERE practiceDate BETWEEN :start AND :end AND state IN ('COMPLETED', 'SKIPPED') ORDER BY practiceDate DESC, orderIndex, id"
    )
    abstract fun observeHistory(start: Long, end: Long): Flow<List<PlanActivityEntity>>
}
