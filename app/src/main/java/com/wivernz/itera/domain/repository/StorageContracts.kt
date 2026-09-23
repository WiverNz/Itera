package com.wivernz.itera.domain.repository
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
/** Storage portion of TrainingPlanRepository; coordinated writes land in milestone 004. */
interface TrainingPlanStorage {
    fun observeToday(): Flow<TrainingDay?>
    fun observeDay(id: Long): Flow<TrainingDay?>
    suspend fun updateActivityState(activityId: Long, state: ActivityState)
    suspend fun saveDraft(activityId: Long, draft: ActivityResult)
    suspend fun snoozeActivity(activityId: Long, until: Instant)
    suspend fun skipActivity(activityId: Long)
}

/** Storage portion of ReviewRepository, without scheduling policy. */
interface ReviewStorage {
    fun observeDue(on: LocalDate): Flow<List<ReviewItem>>
    fun observeUpcoming(): Flow<List<ReviewItem>>
    suspend fun item(id: Long): ReviewItem?
}

/** Unclassified practice facts; mastery thresholds are milestone 004. */
data class PracticeFacts(
    val distinctDays: Int,
    val uses: Int,
    val firstUse: LocalDate?,
    val usedInCombination: Boolean
)
interface ProgressStorage {
    fun observeHistory(month: YearMonth): Flow<List<HistoryEntry>>
    fun observePracticeFacts(id: TechniqueId, since: LocalDate): Flow<PracticeFacts>
}
