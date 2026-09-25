package com.wivernz.itera.domain.repository

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.TechniqueId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

/** One database transaction. Nested calls join the outer transaction. */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

/** Stored unlock facts for one technique; docs/data/01-room-schema.md `technique_state`. */
data class TechniqueStateRecord(
    val techniqueId: TechniqueId,
    val unlockedAt: Instant?,
    val unlockedOnProgramDay: Int?,
    val introCompletedAt: Instant?
) {
    val unlocked: Boolean get() = unlockedAt != null
}

interface TechniqueStateRepository {
    fun observeAll(): Flow<List<TechniqueStateRecord>>
    suspend fun all(): List<TechniqueStateRecord>

    /** Inserts a locked row for every id that has none. */
    suspend fun ensureRows(ids: List<TechniqueId>)

    /** Sets `unlockedAt` only if still locked. True when a row changed. */
    suspend fun unlock(id: TechniqueId, at: Instant, programDay: Int): Boolean

    /** Sets `introCompletedAt` only once. True when a row changed. */
    suspend fun markIntroCompleted(id: TechniqueId, at: Instant): Boolean

    /** Program reset: clears every unlock and intro fact, then unlocks [seeded] on program day 1. */
    suspend fun resetProgramFacts(seeded: Set<TechniqueId>, at: Instant)
}

/** A plan activity before it is persisted. `copyArgs` values are Int, Boolean or String. */
data class PlannedActivity(
    val techniqueId: TechniqueId,
    val exerciseType: ExerciseType,
    val source: ActivitySource,
    val dayPart: DayPart,
    val orderIndex: Int,
    val copyKey: String,
    val copyArgs: Map<String, Any>,
    val estimatedMinutes: Int,
    val optional: Boolean,
    val state: ActivityState,
    val scheduledAt: LocalTime?,
    val reviewItemId: Long? = null,
    val draft: ActivityResult? = null
)

data class PlannedDay(
    val programDay: Int,
    val date: LocalDate,
    val carryOverIntent: String?,
    val generatorVersion: Int,
    val activities: List<PlannedActivity>
)

data class FocusSessionRecord(
    val activityId: Long,
    val techniqueId: TechniqueId,
    val taskLabel: String,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val extendedSeconds: Int,
    val completedNaturally: Boolean,
    val startedAt: Instant,
    val endedAt: Instant
)

data class HabitStackRecord(
    val activityId: Long?,
    val anchor: String,
    val habit: String,
    val nudgeEnabled: Boolean,
    val nudgeTime: LocalTime?,
    val createdAt: Instant
)

data class ReviewAttemptRecord(
    val reviewItemId: Long,
    val activityId: Long,
    val answer: String,
    val grade: RecallGrade,
    val stageBefore: Int,
    val stageAfter: Int,
    val createdAt: Instant
)

/** Fact rows written by completion effects 3 and 4. */
interface PracticeRecordRepository {
    suspend fun insertFocusSession(record: FocusSessionRecord): Long
    suspend fun insertHabitStack(record: HabitStackRecord): Long
}

/** Aggregated counted completions for one technique (mastery input). */
data class TechniqueFacts(
    val techniqueId: TechniqueId,
    val distinctDays: Int,
    val totalUses: Int,
    val firstUse: LocalDate?,
    val lastUse: LocalDate?,
    val usedInCombination: Boolean
)

/** Program reset and erase, ADR-0014. Table membership lives with the schema (`ResetTiers`). */
interface DataResetRepository {
    /** Deletes the tier-1 tables. */
    suspend fun deleteProgramData()

    /** Deletes every application table's rows. */
    suspend fun deleteAllData()
}
