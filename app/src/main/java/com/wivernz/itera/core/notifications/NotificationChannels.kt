package com.wivernz.itera.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.wivernz.itera.R

/**
 * The notification channel registry. Milestone 006 creates `focus_session` for the timer's foreground service;
 * milestone 009 adds the reminder channels here.
 */
object NotificationChannels {
    const val TRAINING = "training"
    const val REFLECTION = "reflection"
    const val HABITS = "habits"
    const val FOCUS_SESSION = "focus_session"

    fun register(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val focus = NotificationChannel(
            FOCUS_SESSION,
            context.getString(R.string.focus_label),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.focus_channel_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannels(listOf(
            focus,
            NotificationChannel(TRAINING, context.getString(R.string.notification_channel_training), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(REFLECTION, context.getString(R.string.notification_channel_reflection), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(HABITS, context.getString(R.string.notification_channel_habits), NotificationManager.IMPORTANCE_LOW)
        ))
    }
}
