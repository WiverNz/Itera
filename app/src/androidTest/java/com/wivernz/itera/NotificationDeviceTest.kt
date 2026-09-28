package com.wivernz.itera

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.core.navigation.Today
import com.wivernz.itera.core.notifications.IteraNotifier
import com.wivernz.itera.core.notifications.NotificationChannels
import com.wivernz.itera.core.notifications.ReminderContent
import com.wivernz.itera.core.notifications.ReminderEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the real platform channel/posting boundary, in addition to the host delivery-policy tests. */
@RunWith(AndroidJUnit4::class)
class NotificationDeviceTest {
    @Test fun channelsPostReplaceAndSuppressForegroundReminders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val environment = ReminderEnvironment(context)
        val posted = mutableListOf<Event>()
        val notifier = IteraNotifier(
            context,
            environment,
            object : Analytics {
                override fun track(event: Event) {
                    posted += event
                }
                override suspend fun append(event: Event) {
                    posted += event
                }
            }
        )
        NotificationChannels.register(context)
        val expected = mapOf(
            NotificationChannels.TRAINING to NotificationManager.IMPORTANCE_DEFAULT,
            NotificationChannels.REFLECTION to NotificationManager.IMPORTANCE_DEFAULT,
            NotificationChannels.HABITS to NotificationManager.IMPORTANCE_LOW,
            NotificationChannels.FOCUS_SESSION to NotificationManager.IMPORTANCE_LOW
        )
        expected.forEach { (id, importance) ->
            assertEquals(importance, manager.getNotificationChannel(id).importance)
        }
        val reminderIds = NotificationType.entries.map(IteraNotifier::id).toSet()
        fun active() = manager.activeNotifications.filter { it.id in reminderIds }
        notifier.cancelAll()
        try {
            NotificationType.entries.forEach { type ->
                val content = ReminderContent(type, "Reminder QA", "Synthetic reminder", Today)
                assertTrue(notifier.post(content))
                val deadline = System.nanoTime() + 5_000_000_000L
                while ((active().size != 1 || active().single().id != IteraNotifier.id(type)) &&
                    System.nanoTime() < deadline
                ) {
                    Thread.sleep(50)
                }
                val notification = active().single().notification
                assertEquals(IteraNotifier.channel(type), notification.channelId)
                assertEquals(content.title, notification.extras.getString(Notification.EXTRA_TITLE))
                assertEquals(content.body, notification.extras.getString(Notification.EXTRA_TEXT))
                assertNotNull(notification.contentIntent)
                if (Build.VERSION.SDK_INT >= 31) assertTrue(notification.contentIntent.isImmutable)
            }
            assertEquals(NotificationType.entries.size, posted.size)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                environment.onActivityStarted(Activity())
            }
            assertFalse(notifier.post(ReminderContent(NotificationType.MORNING, "QA", "QA", Today)))
            assertEquals(NotificationType.entries.size, posted.size)
        } finally {
            notifier.cancelAll()
        }
    }
}
