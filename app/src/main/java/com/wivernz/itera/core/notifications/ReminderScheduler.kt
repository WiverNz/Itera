package com.wivernz.itera.core.notifications

import java.time.Instant
import javax.inject.Inject

/**
 * The reminder seam (docs/engine/06-workmanager-strategy.md section 4). Milestone 004 binds [NoOpReminderScheduler];
 * milestone 009 replaces the binding with the WorkManager implementation without changing callers.
 */
interface ReminderScheduler {
    /** Recomputes every reminder from preferences and today's plan. Idempotent. */
    suspend fun rescheduleAll()

    /** Cancels all unique work, before a reset deletes the rows it targets. */
    suspend fun cancelAll()

    /** A day's plan was generated: schedule its focus and evening one-shots. */
    suspend fun onPlanGenerated(dayId: Long)
    suspend fun scheduleSnooze(activityId: Long, at: Instant)

    /** An activity completed, was skipped or expired: cancel its pending reminders. */
    suspend fun cancelForActivity(activityId: Long)

    /**
     * A habit stack was saved, replacing (archiving) the previous one: schedule its nudge when enabled, cancel
     * it when disabled, and cancel the nudge of any archived stack.
     */
    suspend fun scheduleHabitNudge(habitStackId: Long)
}

class NoOpReminderScheduler @Inject constructor() : ReminderScheduler {
    override suspend fun rescheduleAll() = Unit
    override suspend fun cancelAll() = Unit
    override suspend fun onPlanGenerated(dayId: Long) = Unit
    override suspend fun scheduleSnooze(activityId: Long, at: Instant) = Unit
    override suspend fun cancelForActivity(activityId: Long) = Unit
    override suspend fun scheduleHabitNudge(habitStackId: Long) = Unit
}
