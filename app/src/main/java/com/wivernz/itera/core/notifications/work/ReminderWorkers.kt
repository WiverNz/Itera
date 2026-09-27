package com.wivernz.itera.core.notifications.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.core.notifications.ReminderDelivery
import com.wivernz.itera.core.notifications.ReminderWork
import com.wivernz.itera.core.notifications.WorkReminderScheduler
import com.wivernz.itera.data.database.dao.EventLogDao
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import com.wivernz.itera.domain.training.RefreshAvailabilityUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** One delivery engine for all reminder types; the stable work name identifies each chain. */
open class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
    private val preferences: PreferencesRepository,
    private val ensure: EnsureTodayPlanUseCase,
    private val refresh: RefreshAvailabilityUseCase,
    private val delivery: ReminderDelivery,
    private val scheduler: WorkReminderScheduler
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val type =
            NotificationType.entries.firstOrNull { it.name == inputData.getString("type") }
                ?: return Result.failure()
        val name = inputData.getString("name") ?: return Result.failure()
        val target = inputData.getLong("target", 0)
        return try {
            if (!preferences.preferences.first().onboardingCompleted) return Result.success()
            ensure()
            if (type != NotificationType.SNOOZE) refresh()
            val deferred = delivery.deliver(type, target)
            // Enqueue is last: REPLACE may cancel this running WorkSpec, but cannot cancel the new one.
            if (deferred !=
                null
            ) {
                scheduler.enqueue(ReminderWork(name, type, deferred, target, successor = true))
            } else {
                scheduler.next(type, target, name)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

@HiltWorker
class DailyPlanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val preferences: PreferencesRepository,
    private val ensure: EnsureTodayPlanUseCase
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        if (preferences.preferences.first().onboardingCompleted) ensure()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Result.retry()
    }
}

@HiltWorker
class EventLogTrimWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val events: EventLogDao
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        events.trimTo(2000)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Result.retry()
    }
}

@HiltWorker
class ExportCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val clock: Clock
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val cutoff = clock.instant().minusSeconds(86400).toEpochMilli()
        File(applicationContext.cacheDir, "export").listFiles()?.filter {
            it.isFile &&
                it.lastModified() < cutoff
        }?.forEach { it.delete() }
        return Result.success()
    }
}

@HiltWorker
class MorningReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    preferences: PreferencesRepository,
    ensure: EnsureTodayPlanUseCase,
    refresh: RefreshAvailabilityUseCase,
    delivery: ReminderDelivery,
    scheduler: WorkReminderScheduler
) : ReminderWorker(context, parameters, preferences, ensure, refresh, delivery, scheduler)

@HiltWorker
class FocusSuggestionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    preferences: PreferencesRepository,
    ensure: EnsureTodayPlanUseCase,
    refresh: RefreshAvailabilityUseCase,
    delivery: ReminderDelivery,
    scheduler: WorkReminderScheduler
) : ReminderWorker(context, parameters, preferences, ensure, refresh, delivery, scheduler)

@HiltWorker
class EveningReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    preferences: PreferencesRepository,
    ensure: EnsureTodayPlanUseCase,
    refresh: RefreshAvailabilityUseCase,
    delivery: ReminderDelivery,
    scheduler: WorkReminderScheduler
) : ReminderWorker(context, parameters, preferences, ensure, refresh, delivery, scheduler)

@HiltWorker
class SnoozeReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    preferences: PreferencesRepository,
    ensure: EnsureTodayPlanUseCase,
    refresh: RefreshAvailabilityUseCase,
    delivery: ReminderDelivery,
    scheduler: WorkReminderScheduler
) : ReminderWorker(context, parameters, preferences, ensure, refresh, delivery, scheduler)

@HiltWorker
class HabitNudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    preferences: PreferencesRepository,
    ensure: EnsureTodayPlanUseCase,
    refresh: RefreshAvailabilityUseCase,
    delivery: ReminderDelivery,
    scheduler: WorkReminderScheduler
) : ReminderWorker(context, parameters, preferences, ensure, refresh, delivery, scheduler)
