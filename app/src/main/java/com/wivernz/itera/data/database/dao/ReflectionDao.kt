package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.wivernz.itera.data.database.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ReflectionDao {
    @Upsert suspend fun upsert(row: ReflectionEntity): Long

    @Query("SELECT * FROM reflection_entry WHERE trainingDayId = :id")
    fun observeForDay(id: Long): Flow<ReflectionEntity?>

    @Query(
        "SELECT CASE WHEN skipped = 0 THEN tomorrowChange END FROM reflection_entry ORDER BY date DESC, id DESC LIMIT 1"
    )
    suspend fun latestIntent(): String?

    @Insert suspend fun insert(row: ReflectionEntity): Long

    @Query("SELECT * FROM reflection_entry WHERE trainingDayId = :id")
    suspend fun byDay(id: Long): ReflectionEntity?
}
