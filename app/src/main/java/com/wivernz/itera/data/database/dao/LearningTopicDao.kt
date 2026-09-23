package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.LearningTopicEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface LearningTopicDao {
    @Query(
        "SELECT * FROM learning_topic WHERE archived = 0 ORDER BY createdAt, id"
    )
    fun observeActive(): Flow<List<LearningTopicEntity>>

    @Query("UPDATE learning_topic SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE learning_topic SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query(
        "SELECT t.* FROM learning_topic t LEFT JOIN review_item r ON r.topicId = t.id WHERE t.archived = 0 GROUP BY t.id ORDER BY MAX(r.lastReviewedOn), t.createdAt, t.id LIMIT 1"
    )
    suspend fun leastRecentlyReviewed(): LearningTopicEntity?

    @Insert suspend fun insert(row: LearningTopicEntity): Long
}
