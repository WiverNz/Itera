package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.FocusSessionEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface FocusSessionDao {
    @Query(
        "SELECT * FROM focus_session WHERE startedAt BETWEEN :start AND :end ORDER BY startedAt"
    )
    fun observeBetween(start: Long, end: Long): Flow<List<FocusSessionEntity>>

    @Query(
        "SELECT COALESCE(SUM(f.actualSeconds), 0) FROM focus_session f JOIN plan_activity a ON a.id = f.activityId WHERE a.practiceDate >= :epochDay"
    )
    fun totalSecondsSince(epochDay: Long): Flow<Long>

    @Insert suspend fun insert(row: FocusSessionEntity): Long

    @Query("SELECT COALESCE(SUM(actualSeconds), 0) FROM focus_session")
    fun totalSeconds(): Flow<Long>
}
