package com.wivernz.itera.core.notifications

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Counts started activities, including configuration changes; never retains an Activity. */
@Singleton
class ReminderEnvironment @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Application.ActivityLifecycleCallbacks {
    @Volatile private var started = 0
    val foreground: Boolean get() = started > 0
    fun permitted(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun channelEnabled(channel: String): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
    override fun onActivityStarted(activity: Activity) {
        started++
    }
    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
