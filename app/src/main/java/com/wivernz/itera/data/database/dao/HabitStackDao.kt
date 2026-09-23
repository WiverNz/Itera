package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.HabitStackEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface HabitStackDao {
    @Query(
        "SELECT * FROM habit_stack WHERE archived = 0 ORDER BY createdAt, id"
    )
    fun observeActive(): Flow<List<HabitStackEntity>>

    @Query("UPDATE habit_stack SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Insert suspend fun insert(row: HabitStackEntity): Long
}
