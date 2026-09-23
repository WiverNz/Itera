package com.wivernz.itera.domain.repository
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.ProgressSummary
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
interface TechniqueCatalogRepository {
    suspend fun catalog(): List<Technique>
    suspend fun technique(id: TechniqueId): Technique?
    suspend fun curriculum(): Curriculum
}

interface TrainingPlanRepository : TrainingPlanStorage {
    override fun observeToday(): Flow<TrainingDay?>
    override fun observeDay(id: Long): Flow<TrainingDay?>
    suspend fun ensurePlanFor(date: LocalDate): TrainingDay
    override suspend fun updateActivityState(activityId: Long, state: ActivityState): Unit
    override suspend fun saveDraft(activityId: Long, draft: ActivityResult): Unit
    suspend fun completeActivity(
        activityId: Long,
        result: ActivityResult,
        difficulty: Difficulty?,
        note: String?
    ): Unit
    override suspend fun snoozeActivity(activityId: Long, until: Instant): Unit
    override suspend fun skipActivity(activityId: Long): Unit
    suspend fun completeDay(dayId: Long): Unit
    suspend fun addManualPractice(techniqueId: TechniqueId): Long
}

interface ReviewRepository : ReviewStorage {
    override fun observeDue(on: LocalDate): Flow<List<ReviewItem>>
    override fun observeUpcoming(): Flow<List<ReviewItem>>
    override suspend fun item(id: Long): ReviewItem?
    suspend fun schedule(
        techniqueId: TechniqueId,
        topicId: Long?,
        prompt: String,
        answer: String,
        sourceActivityId: Long
    ): Long
    suspend fun submit(reviewItemId: Long, answer: String, grade: RecallGrade): Unit
}

interface ProgressRepository : ProgressStorage {
    fun observeSummary(): Flow<ProgressSummary>
    fun observeTechniqueProgress(): Flow<List<TechniqueProgress>>
    fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress>
    override fun observeHistory(month: YearMonth): Flow<List<HistoryEntry>>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences): Unit
}

interface LearningTopicRepository {
    fun observeTopics(): Flow<List<LearningTopic>>
    suspend fun add(title: String): Long
    suspend fun rename(id: Long, title: String): Unit
    suspend fun archive(id: Long): Unit
    suspend fun nextTopicForReview(): LearningTopic?
}
