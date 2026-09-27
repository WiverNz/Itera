package com.wivernz.itera.di
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
/** Debug-only device verification of the production Hilt graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageTestEntryPoint {
    fun ensureToday(): com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
    fun reminders(): com.wivernz.itera.core.notifications.WorkReminderScheduler
    fun notifier(): com.wivernz.itera.core.notifications.IteraNotifier
    fun delivery(): com.wivernz.itera.core.notifications.ReminderDelivery
    fun clock(): Clock
    fun logger(): Logger

    @IoDispatcher fun io(): CoroutineDispatcher
    fun database(): IteraDatabase
    fun preferences(): PreferencesRepository
    fun focus(): FocusTimerRepository
}
