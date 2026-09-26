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
        manager.createNotificationChannel(focus)
    }
}
