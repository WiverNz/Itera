@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.core.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.core.notifications.work.DailyPlanWorker
import com.wivernz.itera.core.notifications.work.EventLogTrimWorker
import com.wivernz.itera.core.notifications.work.ExportCleanupWorker
import com.wivernz.itera.core.notifications.work.ReminderWorker
import com.wivernz.itera.data.database.dao.HabitStackDao
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ReminderWork(
    val name: String,
    val type: NotificationType,
    val at: Instant,
    val target: Long = 0,
    val successor: Boolean = false
)

/** Facade keeps desired work independently testable without starting a WorkManager database. */
interface ReminderWorkQueue {
    suspend fun replace(work: ReminderWork)
    suspend fun cancel(name: String)
    suspend fun retain(names: Set<String>)
    suspend fun maintenance()
    suspend fun cancelAll()
}

@Singleton
class AndroidReminderWorkQueue @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock
) : ReminderWorkQueue {
    private val manager get() = WorkManager.getInstance(context)
    override suspend fun replace(work: ReminderWork) {
        if (!work.successor && withContext(Dispatchers.IO) {
                manager.getWorkInfosForUniqueWork(work.name).get().any {
                    it.state ==
                        androidx.work.WorkInfo.State.RUNNING
                }
            }
        ) {
            return
        }
        val request = OneTimeWorkRequest.Builder(
            when (work.type) {
                NotificationType.MORNING, NotificationType.REVIEW -> com.wivernz.itera.core.notifications.work.MorningReminderWorker::class.java
                NotificationType.FOCUS -> com.wivernz.itera.core.notifications.work.FocusSuggestionWorker::class.java
                NotificationType.EVENING -> com.wivernz.itera.core.notifications.work.EveningReminderWorker::class.java
                NotificationType.SNOOZE -> com.wivernz.itera.core.notifications.work.SnoozeReminderWorker::class.java
                NotificationType.HABIT -> com.wivernz.itera.core.notifications.work.HabitNudgeWorker::class.java
            }
        )
            .setInputData(
                Data.Builder().putString(
                    "type",
                    work.type.name
                ).putLong("target", work.target).putString("name", work.name).build()
            )
            .setInitialDelay(ReminderPolicy.delay(clock.instant(), work.at))
            .addTag(REMINDERS).addTag(work.name).build()
        manager.enqueueUniqueWork(work.name, ExistingWorkPolicy.REPLACE, request)
    }
    override suspend fun cancel(name: String) {
        manager.cancelUniqueWork(name)
    }
    override suspend fun retain(names: Set<String>) = withContext(Dispatchers.IO) {
        manager.getWorkInfosByTag(REMINDERS).get().filter {
            !it.state.isFinished &&
                it.state != androidx.work.WorkInfo.State.RUNNING
        }.forEach { work ->
            if (work.tags.none { it in names }) manager.cancelWorkById(work.id)
        }
    }
    override suspend fun maintenance() {
        val now = clock.instant().atZone(ZoneId.systemDefault())
        manager.enqueueUniquePeriodicWork(
            "daily_plan",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequest.Builder(DailyPlanWorker::class.java, 1, TimeUnit.DAYS)
                .setInitialDelay(
                    ReminderPolicy.delay(
                        now.toInstant(),
                        ReminderPolicy.next(now, LocalTime.of(3, 0)).toInstant()
                    )
                )
                .addTag(MAINTENANCE).build()
        )
        manager.enqueueUniquePeriodicWork(
            "event_log_trim",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequest.Builder(EventLogTrimWorker::class.java, 7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiresDeviceIdle(
                        true
                    ).setRequiresBatteryNotLow(true).build()
                )
                .addTag(MAINTENANCE).build()
        )
        manager.enqueueUniquePeriodicWork(
            "export_cleanup",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequest.Builder(
                ExportCleanupWorker::class.java,
                1,
                TimeUnit.DAYS
            ).addTag(MAINTENANCE).build()
        )
    }
    override suspend fun cancelAll() = withContext(Dispatchers.IO) {
        manager.cancelAllWorkByTag(REMINDERS).result.get()
        manager.cancelAllWorkByTag(MAINTENANCE).result.get()
        context.getSharedPreferences(
            "reminder_delivery",
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        Unit
    }
    companion object {
        const val REMINDERS = "itera_reminders"
        const val MAINTENANCE = "itera_maintenance"
    }
}

@Singleton
class WorkReminderScheduler @Inject constructor(
    private val queue: ReminderWorkQueue,
    private val preferences: PreferencesRepository,
    private val plans: TrainingPlanRepository,
    private val habits: HabitStackDao,
    private val environment: ReminderEnvironment,
    private val notifier: IteraNotifier,
    private val analytics: Analytics,
    private val clock: Clock
) : ReminderScheduler {
    override suspend fun rescheduleAll() {
        val desired = desired()
        queue.retain(desired.map { it.name }.toSet())
        desired.forEach { enqueue(it) }
        if (preferences.preferences.first().onboardingCompleted) queue.maintenance()
        if (!environment.permitted()) notifier.cancelAll()
    }

    suspend fun desired(): List<ReminderWork> {
        val prefs = preferences.preferences.first()
        if (!prefs.onboardingCompleted || !environment.permitted()) return emptyList()
        val now = clock.instant().atZone(ZoneId.systemDefault())
        val day = plans.dayByDate(now.toLocalDate())
        fun next(time: LocalTime) = ReminderPolicy.next(now, time).toInstant()
        return buildList {
            if (prefs.notifyMorning ||
                prefs.notifyReviews
            ) {
                add(
                    ReminderWork(
                        "reminder_morning",
                        NotificationType.MORNING,
                        next(prefs.morningTime)
                    )
                )
            }
            if (prefs.notifyEvening) {
                add(
                    ReminderWork(
                        "reminder_evening",
                        NotificationType.EVENING,
                        next(prefs.eveningTime)
                    )
                )
            }
            day?.activities?.forEach { activity ->
                if (prefs.notifyFocus && activity.source == ActivitySource.FOCUS_SUGGESTION &&
                    activity.state in setOf(ActivityState.AVAILABLE, ActivityState.SCHEDULED)
                ) {
                    activity.scheduledAt?.let { time ->
                        val at = day.date.atTime(time).atZone(now.zone).toInstant()
                        if (at.isAfter(
                                now.toInstant()
                            )
                        ) {
                            add(
                                ReminderWork(
                                    "reminder_focus",
                                    NotificationType.FOCUS,
                                    at,
                                    activity.id
                                )
                            )
                        }
                    }
                }
                if (prefs.notifyMorning &&
                    activity.state == ActivityState.SNOOZED
                ) {
                    activity.snoozedUntil?.let {
                        add(
                            ReminderWork(
                                "snooze_${activity.id}",
                                NotificationType.SNOOZE,
                                it,
                                activity.id
                            )
                        )
                    }
                }
            }
            habits.observeActive().first().filter { it.nudgeEnabled }.forEach { habit ->
                habit.nudgeTimeMinutes?.let {
                    add(
                        ReminderWork(
                            "habit_nudge_${habit.id}",
                            NotificationType.HABIT,
                            next(LocalTime.ofSecondOfDay(it * 60L)),
                            habit.id
                        )
                    )
                }
            }
        }
    }

    suspend fun enqueue(work: ReminderWork) {
        queue.replace(work)
        analytics.track(
            Event.NotificationScheduled(
                work.type,
                ReminderPolicy.delay(
                    clock.instant(),
                    work.at
                ).toMinutes().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            )
        )
    }

    /** Reschedule only this chain; unrelated settings and snoozes must not be disturbed. */
    suspend fun next(type: NotificationType, target: Long, name: String) {
        val work = desired().firstOrNull { it.name == name } ?: return
        if (type == NotificationType.SNOOZE || type == NotificationType.FOCUS) return
        enqueue(work.copy(target = target, successor = true))
    }

    override suspend fun cancelAll() {
        queue.cancelAll()
        notifier.cancelAll()
    }
    override suspend fun onPlanGenerated(dayId: Long) = rescheduleAll()
    override suspend fun scheduleSnooze(activityId: Long, at: Instant) {
        if (environment.permitted() &&
            preferences.preferences.first().notifyMorning
        ) {
            enqueue(ReminderWork("snooze_$activityId", NotificationType.SNOOZE, at, activityId))
        }
    }
    override suspend fun cancelForActivity(activityId: Long) {
        queue.cancel("snooze_$activityId")
        val activity = plans.activity(activityId) ?: return
        val type = when (activity.source) {
            ActivitySource.REFLECTION -> NotificationType.EVENING
            ActivitySource.FOCUS_SUGGESTION -> NotificationType.FOCUS
            else -> NotificationType.MORNING
        }
        notifier.cancel(type)
        notifier.cancel(NotificationType.SNOOZE)
        if (type == NotificationType.FOCUS) queue.cancel("reminder_focus")
        // Daily chains remain alive and will recheck the completed activity before posting.
    }
    override suspend fun scheduleHabitNudge(habitStackId: Long) = rescheduleAll()
}
