package com.wivernz.itera.data.database.dao

import androidx.room.Dao
import androidx.room.Query

/** Whole-table deletes for the two reset tiers (ADR-0014). */
@Dao
interface ResetDao {
    @Query("DELETE FROM review_attempt")
    suspend fun deleteReviewAttempts()

    @Query("DELETE FROM focus_session")
    suspend fun deleteFocusSessions()

    @Query("DELETE FROM reflection_entry")
    suspend fun deleteReflections()

    @Query("DELETE FROM plan_activity")
    suspend fun deletePlanActivities()

    @Query("DELETE FROM training_day")
    suspend fun deleteTrainingDays()

    @Query("DELETE FROM review_item")
    suspend fun deleteReviewItems()

    @Query("DELETE FROM habit_stack")
    suspend fun deleteHabitStacks()

    @Query("DELETE FROM learning_topic")
    suspend fun deleteLearningTopics()

    @Query("DELETE FROM event_log")
    suspend fun deleteEventLog()
}
