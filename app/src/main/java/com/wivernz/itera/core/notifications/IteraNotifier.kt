package com.wivernz.itera.core.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wivernz.itera.MainActivity
import com.wivernz.itera.R
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.core.navigation.AppRoute
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.data.catalog.CatalogKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderContent(val type: NotificationType, val title: String, val body: String, val route: AppRoute)

@Singleton
class IteraNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val environment: ReminderEnvironment,
    private val analytics: Analytics
) {
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    // Read AppCompat's persisted locale even when a worker cold-starts the process on API 26-32.
    fun text(id: Int, vararg args: Any): String = ContextCompat.getContextForLanguage(context).getString(id, *args)

    @SuppressLint("DiscouragedApi")
    fun technique(id: String): String {
        val localized = ContextCompat.getContextForLanguage(context)
        val resource = localized.resources.getIdentifier(CatalogKeys.name(id), "string", context.packageName)
        return if (resource != 0) localized.getString(resource) else text(R.string.app_name)
    }

    fun build(content: ReminderContent): Notification {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(RouteCodec.EXTRA, RouteCodec.encode(content.route))
            .putExtra(TYPE_EXTRA, content.type.name)
        val tap = PendingIntent.getActivity(context, id(content.type), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channel(content.type))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(shortBody(content.body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
    }

    @Synchronized
    fun post(content: ReminderContent): Boolean {
        if (!environment.permitted() || environment.foreground || !environment.channelEnabled(channel(content.type))) return false
        return try {
            NotificationType.entries.filter { it != content.type }.forEach { manager.cancel(id(it)) }
            manager.notify(id(content.type), build(content))
            analytics.track(Event.NotificationPosted(content.type))
            true
        } catch (_: SecurityException) { false }
    }

    fun cancel(type: NotificationType) = manager.cancel(id(type))
    fun cancelAll() = NotificationType.entries.forEach(::cancel)

    companion object {
        const val TYPE_EXTRA = "itera.notification.type"
        fun id(type: NotificationType): Int = 200 + type.ordinal
        fun channel(type: NotificationType): String = when (type) {
            NotificationType.EVENING -> NotificationChannels.REFLECTION
            NotificationType.HABIT -> NotificationChannels.HABITS
            else -> NotificationChannels.TRAINING
        }
        fun shortBody(value: String): String {
            val line = value.replace(Regex("\\s+"), " ").trim()
            return if (line.codePointCount(0, line.length) < 60) line else line.substring(0, line.offsetByCodePoints(0, 58)) + "…"
        }
    }
}
