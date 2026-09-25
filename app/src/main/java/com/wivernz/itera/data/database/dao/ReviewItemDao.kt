package com.wivernz.itera.data.database.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wivernz.itera.data.database.entity.ReviewItemEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import kotlinx.coroutines.flow.Flow
@Dao
interface ReviewItemDao {
    @Query(
        "SELECT * FROM review_item WHERE dueOn <= :epochDay AND state != 'RETIRED' ORDER BY dueOn, id"
    )
    fun observeDue(epochDay: Long): Flow<List<ReviewItemEntity>>

    @Query(
        "SELECT * FROM review_item WHERE state != 'RETIRED' ORDER BY dueOn, id"
    )
    fun observeUpcoming(): Flow<List<ReviewItemEntity>>

    @Query("SELECT * FROM review_item WHERE id = :id")
    suspend fun byId(id: Long): ReviewItemEntity?

    @Query(
        "UPDATE review_item SET stageIndex = :stage, dueOn = :dueOn, lastReviewedOn = :reviewedOn, state = :state WHERE id = :id"
    )
    suspend fun updateStage(id: Long, stage: Int, dueOn: Long, reviewedOn: Long?, state: String)

    @Insert suspend fun insert(row: ReviewItemEntity): Long

    @Query("SELECT * FROM review_item WHERE state != 'RETIRED' ORDER BY dueOn, id")
    suspend fun active(): List<ReviewItemEntity>

    @Query(
        "SELECT * FROM review_item WHERE state != 'RETIRED' AND techniqueId = :techniqueId AND topicId IS :topicId ORDER BY id LIMIT 1"
    )
    suspend fun activeFor(techniqueId: String, topicId: Long?): ReviewItemEntity?

    @Query(
        "UPDATE review_item SET prompt = :prompt, sourceAnswer = :answer, sourceActivityId = :activityId WHERE id = :id"
    )
    suspend fun replaceAnswer(id: Long, prompt: String, answer: String, activityId: Long)

    @Query("SELECT sourceAnswer FROM review_item WHERE id = :id")
    suspend fun answer(id: Long): String?
}
