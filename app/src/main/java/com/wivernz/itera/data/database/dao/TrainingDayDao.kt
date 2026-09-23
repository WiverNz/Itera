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
}
