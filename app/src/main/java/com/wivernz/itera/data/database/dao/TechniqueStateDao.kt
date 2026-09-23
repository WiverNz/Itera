package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.TechniqueStateEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface TechniqueStateDao {
    @Query("SELECT * FROM technique_state ORDER BY techniqueId")
    fun observeAll(): Flow<List<TechniqueStateEntity>>

    @Query(
        "UPDATE technique_state SET unlockedAt = :at, unlockedOnProgramDay = :day WHERE techniqueId = :id AND unlockedAt IS NULL"
    )
    suspend fun unlock(id: String, at: Long, day: Int)

    @Query(
        "UPDATE technique_state SET introCompletedAt = :at WHERE techniqueId = :id AND introCompletedAt IS NULL"
    )
    suspend fun markIntroComplete(id: String, at: Long)

    @Query("SELECT * FROM technique_state WHERE techniqueId = :id")
    suspend fun byId(id: String): TechniqueStateEntity?

    @Insert suspend fun insert(row: TechniqueStateEntity): Long
}
