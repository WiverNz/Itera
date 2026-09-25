package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.relation.CompletionRow
import com.wivernz.itera.data.database.relation.TechniqueFactsRow
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

    @Query("SELECT draftPayload FROM plan_activity WHERE id = :id")
    abstract suspend fun draft(id: Long): String?

    @Query(
        "UPDATE plan_activity SET draftPayload = :payload WHERE id = :id"
    )
    abstract suspend fun updateDraft(id: Long, payload: String)

    @Query(
        "UPDATE plan_activity SET state = 'COMPLETED', snoozedUntil = NULL, resultPayload = :payload, draftPayload = NULL, difficulty = :difficulty, note = :note, completedAt = :at, durationSeconds = :duration WHERE id = :id"
    )
    abstract suspend fun complete(
        id: Long,
        payload: String?,
        difficulty: String?,
        note: String?,
        at: Long,
        duration: Int?
    )

    @Query(
        "UPDATE plan_activity SET state = 'SNOOZED', snoozedUntil = :until WHERE id = :id"
    )
    abstract suspend fun snooze(id: Long, until: Long)

    @Query(
        "UPDATE plan_activity SET state = 'IN_PROGRESS', startedAt = :at, snoozedUntil = NULL WHERE id = :id"
    )
    abstract suspend fun start(id: Long, at: Long)

    @Query("UPDATE plan_activity SET state = 'AVAILABLE', startedAt = NULL WHERE id = :id")
    abstract suspend fun abandon(id: Long)

    @Query("UPDATE plan_activity SET state = 'AVAILABLE', snoozedUntil = NULL WHERE id = :id")
    abstract suspend fun makeAvailable(id: Long)

    @Query(
        "UPDATE plan_activity SET state = 'SKIPPED', draftPayload = NULL, snoozedUntil = NULL WHERE id = :id"
    )
    abstract suspend fun skip(id: Long)

    @Query("UPDATE plan_activity SET state = 'EXPIRED' WHERE id IN (:ids)")
    abstract suspend fun expire(ids: List<Long>)

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

    /*
     * Counted completions (docs/engine/03 section 2): COMPLETED rows, excluding the combination parent (its steps
     * carry the credit) and FOCUS_SUGGESTION completions made before the technique's own intro (the Day-1
     * generic focus block is attached to Pomodoro but grants it nothing).
     */
    @Query(
        "SELECT COUNT(DISTINCT a.practiceDate) FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) AND a.techniqueId = :techniqueId"
    )
    abstract fun countDistinctPracticeDays(techniqueId: String): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) AND a.techniqueId = :techniqueId AND a.practiceDate >= :epochDay"
    )
    abstract fun usesSince(techniqueId: String, epochDay: Long): Flow<Int>

    @Query(
        "SELECT MIN(a.practiceDate) FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) AND a.techniqueId = :techniqueId"
    )
    abstract fun firstUse(techniqueId: String): Flow<Long?>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) AND a.techniqueId = :techniqueId AND a.source = 'COMBINATION')"
    )
    abstract fun usedInCombination(techniqueId: String): Flow<Boolean>

    @Query(
        "SELECT a.techniqueId AS techniqueId, COUNT(DISTINCT a.practiceDate) AS distinctDays, COUNT(*) AS totalUses, MIN(a.practiceDate) AS firstUse, MAX(a.practiceDate) AS lastUse, MAX(a.source = 'COMBINATION') AS usedInCombination FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) GROUP BY a.techniqueId"
    )
    abstract fun observeTechniqueFacts(): Flow<List<TechniqueFactsRow>>

    @Query(
        "SELECT a.techniqueId AS techniqueId, COUNT(DISTINCT a.practiceDate) AS distinctDays, COUNT(*) AS totalUses, MIN(a.practiceDate) AS firstUse, MAX(a.practiceDate) AS lastUse, MAX(a.source = 'COMBINATION') AS usedInCombination FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) GROUP BY a.techniqueId"
    )
    abstract suspend fun techniqueFacts(): List<TechniqueFactsRow>

    @Query(
        "SELECT a.practiceDate AS practiceDate, a.techniqueId AS techniqueId FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt)) AND a.practiceDate BETWEEN :start AND :end ORDER BY a.practiceDate, a.id"
    )
    abstract fun observeCompletions(start: Long, end: Long): Flow<List<CompletionRow>>

    @Query(
        "SELECT COUNT(*) FROM plan_activity a LEFT JOIN technique_state s ON s.techniqueId = a.techniqueId WHERE a.state = 'COMPLETED' AND a.exerciseType != 'COMBINATION' AND NOT (a.source = 'FOCUS_SUGGESTION' AND (s.introCompletedAt IS NULL OR a.completedAt IS NULL OR a.completedAt < s.introCompletedAt))"
    )
    abstract fun observeCompletedCount(): Flow<Int>

    @Query(
        "SELECT * FROM plan_activity WHERE practiceDate BETWEEN :start AND :end AND state IN ('COMPLETED', 'SKIPPED') ORDER BY practiceDate DESC, orderIndex, id"
    )
    abstract fun observeHistory(start: Long, end: Long): Flow<List<PlanActivityEntity>>
}
