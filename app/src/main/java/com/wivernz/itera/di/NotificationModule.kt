package com.wivernz.itera.di

import com.wivernz.itera.core.notifications.AndroidReminderWorkQueue
import com.wivernz.itera.core.notifications.ReminderWorkQueue
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds abstract fun queue(impl: AndroidReminderWorkQueue): ReminderWorkQueue
}
