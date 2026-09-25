package com.wivernz.itera.domain.repository
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.progress.CompletionFact
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
    override suspend fun updateActivityState(activityId: Long, state: ActivityState): Unit
    override suspend fun saveDraft(activityId: Long, draft: ActivityResult): Unit
    override suspend fun snoozeActivity(activityId: Long, until: Instant): Unit

    /** Sets SKIPPED and clears the draft. */
    override suspend fun skipActivity(activityId: Long): Unit
    suspend fun dayByDate(date: LocalDate): TrainingDay?
    suspend fun day(id: Long): TrainingDay?

    /** Days dated before [date], oldest first. */
    suspend fun daysBefore(date: LocalDate): List<TrainingDay>
    suspend fun latestCompletedDay(): TrainingDay?
    suspend fun activity(id: Long): PlanActivity?
    suspend fun generatorVersion(dayId: Long): Int?

    /** Inserts the day and its activities; if the date already has a row, returns that row's id. */
    suspend fun insertDay(plan: PlannedDay, createdAt: Instant): Long
    suspend fun deleteDay(id: Long): Unit
    suspend fun insertActivity(dayId: Long, activity: PlannedActivity): Long
    suspend fun updateDayStatus(dayId: Long, status: TrainingDayStatus, completedAt: Instant?): Unit
    suspend fun start(activityId: Long, at: Instant): Unit

    /** IN_PROGRESS -> AVAILABLE: keeps the draft, clears `startedAt`. */
    suspend fun abandon(activityId: Long): Unit

    /** SNOOZED/SCHEDULED -> AVAILABLE, clearing `snoozedUntil`. */
    suspend fun makeAvailable(activityId: Long): Unit
    suspend fun complete(
        activityId: Long,
        result: ActivityResult?,
        difficulty: Difficulty?,
        note: String?,
        at: Instant,
        durationSeconds: Int?
    ): Unit
    suspend fun expire(activityIds: List<Long>): Unit

    /** Program day of the training day holding [activityId]; null if it no longer exists. */
    suspend fun programDayOfActivity(activityId: Long): Int?

    /** The most recent "What will you change tomorrow?" answer. */
    suspend fun latestIntent(): String?
    suspend fun saveReflection(
        dayId: Long,
        date: LocalDate,
        result: ActivityResult.Reflection?,
        skipped: Boolean,
        at: Instant
    ): Unit
}

interface ReviewRepository : ReviewStorage {
    override fun observeDue(on: LocalDate): Flow<List<ReviewItem>>
    override fun observeUpcoming(): Flow<List<ReviewItem>>
    override suspend fun item(id: Long): ReviewItem?

    /** Every non-retired item. */
    suspend fun active(): List<ReviewItem>
    suspend fun activeFor(techniqueId: TechniqueId, topicId: Long?): ReviewItem?

    /** Inserts [item] (its id is ignored) and returns the new id. */
    suspend fun insert(item: ReviewItem, createdAt: Instant): Long

    /** Replaces the answer of an active item, leaving its stage unchanged. */
    suspend fun replaceAnswer(
        id: Long,
        prompt: String,
        answer: String,
        sourceActivityId: Long
    ): Unit
    suspend fun updateSchedule(item: ReviewItem): Unit
    suspend fun insertAttempt(attempt: ReviewAttemptRecord): Long
    suspend fun attemptCount(activityId: Long): Int

    /** The hidden answer, for the compare step only. */
    suspend fun revealAnswer(id: Long): String?
}

interface ProgressRepository : ProgressStorage {
    fun observeTechniqueFacts(): Flow<List<TechniqueFacts>>
    suspend fun techniqueFacts(): List<TechniqueFacts>

    /** Counted completions dated within [from]..[to]. */
    fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<CompletionFact>>
    fun observeActivityCount(): Flow<Int>

    /** Sum of `focus_session.actualSeconds` for sessions on or after [since] (all time when null). */
    fun observeFocusSeconds(since: LocalDate?): Flow<Long>
    override fun observeHistory(month: YearMonth): Flow<List<HistoryEntry>>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences): Unit

    /** Erase everything: removes every stored key, returning to first-run defaults. */
    suspend fun clear(): Unit
}

interface LearningTopicRepository {
    fun observeTopics(): Flow<List<LearningTopic>>
    suspend fun add(title: String): Long
    suspend fun rename(id: Long, title: String): Unit
    suspend fun archive(id: Long): Unit
    suspend fun nextTopicForReview(): LearningTopic?
}
