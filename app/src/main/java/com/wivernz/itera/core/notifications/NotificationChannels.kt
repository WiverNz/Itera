package com.wivernz.itera.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
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
        val localized = ContextCompat.getContextForLanguage(context)
        val focus = NotificationChannel(
            FOCUS_SESSION,
            localized.getString(R.string.focus_label),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = localized.getString(R.string.focus_channel_desc)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannels(
            listOf(
                focus,
                NotificationChannel(
                    TRAINING,
                    localized.getString(R.string.notification_channel_training),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    REFLECTION,
                    localized.getString(R.string.notification_channel_reflection),
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    HABITS,
                    localized.getString(R.string.notification_channel_habits),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        )
    }
}
