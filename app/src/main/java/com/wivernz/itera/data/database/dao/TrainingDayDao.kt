package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface TrainingDayDao {
    @Transaction
    @Query("SELECT * FROM training_day WHERE date = :date")
    fun observeByDate(date: Long): Flow<TrainingDayWithActivities?>

    @Transaction
    @Query("SELECT * FROM training_day WHERE id = :id")
    fun observeWithActivities(id: Long): Flow<TrainingDayWithActivities?>

    @Query("SELECT * FROM training_day WHERE date = :date")
    suspend fun findByDate(date: Long): TrainingDayEntity?

    @Query(
        "SELECT d.date, d.programDay, COUNT(a.id) AS activityCount FROM training_day d " +
            "LEFT JOIN plan_activity a ON a.practiceDate = d.date " +
            "AND a.state IN ('COMPLETED', 'SKIPPED') " +
            "WHERE d.date BETWEEN :from AND :to GROUP BY d.id ORDER BY d.date"
    )
    suspend fun journalHeaders(from: Long, to: Long): List<JournalDayHeader>

    @Query(
        "UPDATE training_day SET status = :status, completedAt = :completedAt WHERE id = :id"
    )
    suspend fun updateStatus(id: Long, status: String, completedAt: Long?)

    @Query(
        "SELECT * FROM training_day WHERE status = 'COMPLETE' ORDER BY date DESC LIMIT 1"
    )
    suspend fun latestCompleted(): TrainingDayEntity?

    @Query("DELETE FROM training_day WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert suspend fun insert(row: TrainingDayEntity): Long

    @Transaction
    @Query("SELECT * FROM training_day WHERE id = :id")
    suspend fun byId(id: Long): TrainingDayWithActivities?

    @Transaction
    @Query("SELECT * FROM training_day WHERE date = :date")
    suspend fun byDate(date: Long): TrainingDayWithActivities?

    @Transaction
    @Query("SELECT * FROM training_day WHERE date < :date ORDER BY date")
    suspend fun before(date: Long): List<TrainingDayWithActivities>

    @Query("SELECT generatorVersion FROM training_day WHERE id = :id")
    suspend fun generatorVersion(id: Long): Int?

    @Query(
        "SELECT d.programDay FROM training_day d JOIN plan_activity a ON a.trainingDayId = d.id WHERE a.id = :activityId"
    )
    suspend fun programDayOfActivity(activityId: Long): Int?
}

data class JournalDayHeader(val date: Long, val programDay: Int, val activityCount: Int)
