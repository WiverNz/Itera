package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.EventLogEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface EventLogDao {
    @Query(
        "DELETE FROM event_log WHERE id NOT IN (SELECT id FROM event_log ORDER BY timestamp DESC, id DESC LIMIT :limit)"
    )
    suspend fun trimTo(limit: Int)

    @Query(
        "SELECT * FROM event_log WHERE timestamp >= :timestamp ORDER BY timestamp, id"
    )
    suspend fun exportSince(timestamp: Long): List<EventLogEntity>

    @Insert suspend fun insert(row: EventLogEntity): Long
}
