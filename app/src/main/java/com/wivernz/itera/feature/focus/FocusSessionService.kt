package com.wivernz.itera.feature.focus

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wivernz.itera.MainActivity
import com.wivernz.itera.R
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.navigation.FocusSession
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.core.notifications.NotificationChannels
import com.wivernz.itera.domain.focus.FocusServiceLauncher
import com.wivernz.itera.domain.focus.FocusSessionController
import com.wivernz.itera.domain.model.FocusTimerState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The running timer's `specialUse` foreground service (ADR-0009). It holds nothing but the notification: the
 * countdown is drawn by the system chronometer against `endsAt`, so nothing is posted every second.
 */
@AndroidEntryPoint
class FocusSessionService : Service() {
    @Inject lateinit var logger: Logger

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val snapshot = intent?.let(FocusNotificationSnapshot::from)
        if (snapshot == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(
                this,
                FocusNotifications.RUNNING_ID,
                FocusNotifications.running(this, snapshot),
                type
            )
        } catch (e: IllegalStateException) {
            // Android 12+ may refuse a background start; the timer still runs from its stored end instant.
            logger.w(TAG, "Focus foreground service not started", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private companion object {
        const val TAG = "FocusSessionService"
    }
}

/** The notification's Pause/Resume and End actions. */
@AndroidEntryPoint
class FocusActionReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: FocusSessionController

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    FocusNotifications.ACTION_PAUSE -> controller.pause()
                    FocusNotifications.ACTION_RESUME -> controller.resume()
                    // No dialog can be shown from a notification; the sub-minute rule still applies.
                    FocusNotifications.ACTION_END -> controller.end()
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** What the notification shows; passed in the start intent so `startForeground` needs no disk read. */
data class FocusNotificationSnapshot(
    val activityId: Long,
    val techniqueId: String,
    val taskLabel: String,
    val plannedMinutes: Int,
    val endsAtMillis: Long
) {
    fun toIntent(intent: Intent): Intent = intent
        .putExtra(EXTRA_ACTIVITY, activityId)
        .putExtra(EXTRA_TECHNIQUE, techniqueId)
        .putExtra(EXTRA_TASK, taskLabel)
        .putExtra(EXTRA_MINUTES, plannedMinutes)
        .putExtra(EXTRA_ENDS_AT, endsAtMillis)

    companion object {
        private const val EXTRA_ACTIVITY = "focus.activity"
        private const val EXTRA_TECHNIQUE = "focus.technique"
        private const val EXTRA_TASK = "focus.task"
        private const val EXTRA_MINUTES = "focus.minutes"
        private const val EXTRA_ENDS_AT = "focus.endsAt"
        private const val SECONDS_PER_MINUTE = 60

        fun of(state: FocusTimerState) = FocusNotificationSnapshot(
            state.activityId,
            state.techniqueId.value,
            state.taskLabel,
            state.plannedSeconds / SECONDS_PER_MINUTE,
            state.endsAt.toEpochMilli()
        )

        fun from(intent: Intent): FocusNotificationSnapshot? {
            val id = intent.getLongExtra(EXTRA_ACTIVITY, -1)
            if (id < 0) return null
            return FocusNotificationSnapshot(
                id,
                intent.getStringExtra(EXTRA_TECHNIQUE).orEmpty(),
                intent.getStringExtra(EXTRA_TASK).orEmpty(),
                intent.getIntExtra(EXTRA_MINUTES, 0),
                intent.getLongExtra(EXTRA_ENDS_AT, 0)
            )
        }
    }
}

internal object FocusNotifications {
    const val RUNNING_ID = 6001
    const val PAUSED_ID = 6002
    const val ACTION_PAUSE = "com.wivernz.itera.focus.PAUSE"
    const val ACTION_RESUME = "com.wivernz.itera.focus.RESUME"
    const val ACTION_END = "com.wivernz.itera.focus.END"

    fun running(context: Context, s: FocusNotificationSnapshot): Notification = base(context, s)
        .setWhen(s.endsAtMillis)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .addAction(0, context.getString(R.string.focus_pause), action(context, ACTION_PAUSE))
        .addAction(0, context.getString(R.string.focus_end), action(context, ACTION_END))
        .build()

    fun paused(context: Context, s: FocusNotificationSnapshot): Notification = base(context, s)
        .setSubText(context.getString(R.string.focus_notification_paused))
        .setShowWhen(false)
        .addAction(0, context.getString(R.string.focus_resume), action(context, ACTION_RESUME))
        .addAction(0, context.getString(R.string.focus_end), action(context, ACTION_END))
        .build()

    private fun base(context: Context, s: FocusNotificationSnapshot) =
        NotificationCompat.Builder(context, NotificationChannels.FOCUS_SESSION)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(context.getString(R.string.focus_label))
            .setContentText(s.taskLabel)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open(context, s))

    /** Tapping the notification returns to the session. */
    private fun open(context: Context, s: FocusNotificationSnapshot): PendingIntent {
        val route = FocusSession(s.activityId, s.plannedMinutes, s.techniqueId)
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(RouteCodec.EXTRA, RouteCodec.encode(route))
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun action(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, FocusActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/** Starts, updates and stops the service. Paused sessions show a plain notification with Resume and End. */
class AndroidFocusServiceLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger
) : FocusServiceLauncher {
    override fun update(state: FocusTimerState?) {
        val manager = NotificationManagerCompat.from(context)
        val service = Intent(context, FocusSessionService::class.java)
        when {
            state == null -> {
                context.stopService(service)
                manager.cancel(FocusNotifications.PAUSED_ID)
            }
            state.pausedAt != null -> {
                context.stopService(service)
                if (canNotify()) {
                    try {
                        manager.notify(
                            FocusNotifications.PAUSED_ID,
                            FocusNotifications.paused(context, FocusNotificationSnapshot.of(state))
                        )
                    } catch (e: SecurityException) {
                        logger.w(TAG, "Paused notification not posted", e)
                    }
                }
            }
            else -> {
                manager.cancel(FocusNotifications.PAUSED_ID)
                try {
                    ContextCompat.startForegroundService(
                        context,
                        FocusNotificationSnapshot.of(state).toIntent(service)
                    )
                } catch (e: IllegalStateException) {
                    logger.w(TAG, "Focus service not started", e)
                }
            }
        }
    }

    private fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "FocusServiceLauncher"
    }
}
