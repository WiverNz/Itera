package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.ReviewAttemptEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface ReviewAttemptDao {
    @Query(
        "SELECT * FROM review_attempt WHERE reviewItemId = :id ORDER BY createdAt, id"
    )
    fun observeForItem(id: Long): Flow<List<ReviewAttemptEntity>>

    @Insert suspend fun insert(row: ReviewAttemptEntity): Long
}
