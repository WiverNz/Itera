package com.wivernz.itera.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.analytics.SuppressionReason
import com.wivernz.itera.core.navigation.ExerciseIntro
import com.wivernz.itera.core.navigation.ExerciseResult
import com.wivernz.itera.core.navigation.Reflection
import com.wivernz.itera.core.navigation.Review
import com.wivernz.itera.core.navigation.Today
import com.wivernz.itera.core.navigation.Train
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.repository.HabitStackRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

class NotificationSuppressionTest {
    private val now = Instant.parse("2026-09-27T12:00:00Z")
    private fun check(type: NotificationType, facts: ReminderFacts, reason: SuppressionReason?) =
        assertEquals(reason, ReminderPolicy.suppression(type, facts, now))

    @Test fun permissionAndTogglesApplyToEveryType() {
        NotificationType.entries.forEach {
            check(it, ReminderFacts(permitted = false), SuppressionReason.PERMISSION_DENIED)
            check(it, ReminderFacts(enabled = false), SuppressionReason.DISABLED)
        }
    }

    @Test fun foregroundAndFocusSuppressEveryType() {
        NotificationType.entries.forEach {
            check(it, ReminderFacts(foreground = true), SuppressionReason.FOREGROUND)
            check(it, ReminderFacts(focusRunning = true), SuppressionReason.FOCUS_RUNNING)
        }
    }

    @Test fun completedSkippedExpiredAndMissingTargetsNeverPost() {
        listOf(
            ActivityState.COMPLETED,
            ActivityState.SKIPPED,
            ActivityState.EXPIRED,
            null
        ).forEach {
            check(
                NotificationType.MORNING,
                ReminderFacts(state = it),
                SuppressionReason.ALREADY_COMPLETE
            )
            check(
                NotificationType.EVENING,
                ReminderFacts(state = it),
                SuppressionReason.ALREADY_COMPLETE
            )
        }
    }

    @Test fun morningInProgressAndFutureSnoozeAreSuppressed() {
        check(
            NotificationType.MORNING,
            ReminderFacts(state = ActivityState.IN_PROGRESS),
            SuppressionReason.IN_PROGRESS
        )
        check(
            NotificationType.MORNING,
            ReminderFacts(snoozedUntil = now.plusSeconds(1)),
            SuppressionReason.SNOOZED
        )
        check(NotificationType.MORNING, ReminderFacts(snoozedUntil = now), null)
    }

    @Test fun focusRequiresAvailabilityAndNoEarlierSession() {
        check(
            NotificationType.FOCUS,
            ReminderFacts(state = ActivityState.SCHEDULED),
            SuppressionReason.ALREADY_COMPLETE
        )
        check(
            NotificationType.FOCUS,
            ReminderFacts(focusCompleted = true),
            SuppressionReason.ALREADY_COMPLETE
        )
        check(NotificationType.FOCUS, ReminderFacts(), null)
    }

    @Test fun habitMustBeActiveAndUnlogged() {
        check(
            NotificationType.HABIT,
            ReminderFacts(habitArchived = true),
            SuppressionReason.ALREADY_COMPLETE
        )
        check(
            NotificationType.HABIT,
            ReminderFacts(habitLogged = true),
            SuppressionReason.ALREADY_COMPLETE
        )
        check(NotificationType.HABIT, ReminderFacts(state = null), null)
    }
}

class RecordingQueue : ReminderWorkQueue {
    val work = linkedMapOf<String, ReminderWork>()
    var maintained = false
    override suspend fun replace(work: ReminderWork) {
        this.work[work.name] = work
    }
    override suspend fun cancel(name: String) {
        work.remove(name)
    }
    override suspend fun retain(names: Set<String>) {
        work.keys.retainAll(names)
    }
    override suspend fun maintenance() {
        maintained = true
    }
    override suspend fun cancelAll() {
        work.clear()
        maintained = false
    }
}

open class ReminderTestBase {
    lateinit var h: EngineHarness
    lateinit var context: Context
    lateinit var environment: ReminderEnvironment
    lateinit var notifier: IteraNotifier
    lateinit var queue: RecordingQueue
    lateinit var scheduler: WorkReminderScheduler

    @Before fun setup() {
        h = EngineHarness()
        context = ApplicationProvider.getApplicationContext()
        NotificationChannels.register(context)
        environment = ReminderEnvironment(context)
        notifier = IteraNotifier(context, environment, h.analytics)
        queue = RecordingQueue()
        scheduler =
            WorkReminderScheduler(
                queue,
                h.prefs,
                h.plans,
                h.db.habitStackDao(),
                environment,
                notifier,
                h.analytics,
                h.clock
            )
        shadowOf(
            context.getSystemService(NotificationManager::class.java)
        ).setNotificationsEnabled(true)
    }

    @After fun close() {
        h.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest : ReminderTestBase() {
    @Test fun idempotentSetAndIndependentToggles() = runBlocking {
        h.ensureToday()
        scheduler.rescheduleAll()
        val first = queue.work.toMap()
        scheduler.rescheduleAll()
        assertEquals(first, queue.work)
        assertTrue(first.keys.containsAll(listOf("reminder_morning", "reminder_evening")))
        h.prefs.update {
            it.copy(
                notifyMorning = false,
                notifyFocus = false,
                notifyReviews = false,
                notifyEvening = false
            )
        }
        scheduler.rescheduleAll()
        assertTrue(queue.work.isEmpty())
        assertTrue(queue.maintained)
        h.prefs.update { it.copy(notifyReviews = true) }
        scheduler.rescheduleAll()
        assertEquals(setOf("reminder_morning"), queue.work.keys)
        h.prefs.update { it.copy(notifyReviews = false, notifyEvening = true) }
        scheduler.rescheduleAll()
        assertEquals(setOf("reminder_evening"), queue.work.keys)
    }

    @Test fun deniedPermissionRemovesEveryReminderButKeepsMaintenance() = runBlocking {
        scheduler.rescheduleAll()
        shadowOf(
            context.getSystemService(NotificationManager::class.java)
        ).setNotificationsEnabled(false)
        scheduler.rescheduleAll()
        assertTrue(queue.work.isEmpty())
        assertTrue(queue.maintained)
    }

    @Test fun wallClockChainHandlesBothDstChanges() {
        val spring = ZonedDateTime.of(2026, 3, 28, 8, 30, 0, 0, ZoneId.of("Europe/Berlin"))
        assertEquals(
            23,
            Duration.between(spring, ReminderPolicy.next(spring, LocalTime.of(8, 30))).toHours()
        )
        val fall = ZonedDateTime.of(2026, 10, 24, 8, 30, 0, 0, spring.zone)
        assertEquals(
            25,
            Duration.between(fall, ReminderPolicy.next(fall, LocalTime.of(8, 30))).toHours()
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class RescheduleTest : ReminderTestBase() {
    @Test fun changedTimesReplaceAndCompletionCancelsSnooze() = runBlocking {
        val day = h.ensureToday()
        scheduler.rescheduleAll()
        val before = queue.work.getValue("reminder_morning").at
        h.prefs.update { it.copy(morningTime = it.morningTime.plusHours(1)) }
        scheduler.rescheduleAll()
        assertEquals(before.plusSeconds(3600), queue.work.getValue("reminder_morning").at)
        val activity = day.activities.first { it.source == ActivitySource.PROGRAM }
        scheduler.scheduleSnooze(activity.id, h.clock.instant().plusSeconds(600))
        assertTrue(queue.work.containsKey("snooze_${activity.id}"))
        scheduler.cancelForActivity(activity.id)
        assertFalse(queue.work.containsKey("snooze_${activity.id}"))
    }
}

@RunWith(RobolectricTestRunner::class)
class HabitNudgeTest : ReminderTestBase() {
    @Test fun replacingAndDisablingStackCancelsOldUniqueWork() = runBlocking {
        val first = h.records.insertHabitStack(
            HabitStackRecord(null, "coffee", "read", true, LocalTime.NOON, h.clock.instant())
        )
        scheduler.scheduleHabitNudge(first)
        assertTrue(queue.work.containsKey("habit_nudge_$first"))
        val second = h.records.insertHabitStack(
            HabitStackRecord(null, "lunch", "walk", false, LocalTime.NOON, h.clock.instant())
        )
        scheduler.scheduleHabitNudge(second)
        assertFalse(queue.work.keys.any { it.startsWith("habit_nudge_") })
    }
}

@RunWith(RobolectricTestRunner::class)
class NotificationContentTest : ReminderTestBase() {
    @Test fun channelImportanceAndOneVisibleReminder() {
        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals(4, manager.notificationChannels.size)
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            manager.getNotificationChannel("training").importance
        )
        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            manager.getNotificationChannel("habits").importance
        )
        NotificationType.entries.forEach { type ->
            val content = ReminderContent(type, "Synthetic title", "Synthetic body", Today)
            val built = notifier.build(content)
            assertEquals(IteraNotifier.channel(type), built.channelId)
            assertEquals("Synthetic body", built.extras.getString(Notification.EXTRA_TEXT))
            assertNotNull(built.contentIntent)
            assertTrue(notifier.post(content))
            assertEquals(1, manager.activeNotifications.size)
        }
    }

    @Test fun bodyIsSingleLineUnderSixtyCodePointsIncludingEmoji() {
        val text = IteraNotifier.shortBody("🌱".repeat(80) + "\nprivate")
        assertEquals(59, text.codePointCount(0, text.length))
        assertTrue(text.endsWith("…"))
        assertFalse(text.contains('\n'))
    }
}

@RunWith(RobolectricTestRunner::class)
class DeepLinkTest : ReminderTestBase() {
    @Test fun completedTargetsAreReadOnlyAndMissingTargetsFallBack() = runBlocking {
        val resolver = NotificationDestination(h.plans)
        val day = h.ensureToday()
        val activity = day.activities.first { it.source == ActivitySource.PROGRAM }
        val intro = ExerciseIntro(activity.id, activity.techniqueId.value)
        assertEquals(intro, resolver.resolve(intro))
        h.plans.updateActivityState(activity.id, ActivityState.COMPLETED)
        assertEquals(
            ExerciseResult(activity.id, activity.techniqueId.value, true),
            resolver.resolve(intro)
        )
        assertEquals(Today, resolver.resolve(Reflection(Long.MAX_VALUE)))
        assertEquals(Train, resolver.resolve(Review(Long.MAX_VALUE)))
    }
}

@RunWith(RobolectricTestRunner::class)
class ReminderDeliveryTest : ReminderTestBase() {
    private fun delivery() = ReminderDelivery(
        context, h.plans, h.prefs, h.focusTimer,
        h.db.habitStackDao(), h.db.learningTopicDao(), h.reviews, notifier, environment,
        h.analytics, h.clock
    )

    @Test fun completedProgramAndDuplicateMorningDoNotPost() = runBlocking {
        val day = h.ensureToday()
        val engine = delivery()
        engine.deliver(NotificationType.MORNING, 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, manager.activeNotifications.size)
        engine.deliver(NotificationType.MORNING, 0)
        assertEquals(1, h.eventNames().count { it == "notification_posted" })
        notifier.cancelAll()
        h.plans.updateActivityState(
            day.activities.first {
                it.source == ActivitySource.PROGRAM
            }.id,
            ActivityState.COMPLETED
        )
        engine.deliver(NotificationType.MORNING, 0)
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test fun expiredSnoozeBecomesAvailableWithoutPermissionAndRetryDoesNotPost() = runBlocking {
        val activity = h.ensureToday().activities.first { it.source == ActivitySource.PROGRAM }
        h.snooze(activity.id, h.clock.instant().plusSeconds(3600)).getOrThrow()
        h.clock.advance(Duration.ofHours(3))
        shadowOf(
            context.getSystemService(NotificationManager::class.java)
        ).setNotificationsEnabled(false)
        delivery().deliver(NotificationType.SNOOZE, activity.id)
        assertEquals(ActivityState.AVAILABLE, h.plans.activity(activity.id)?.state)
        shadowOf(
            context.getSystemService(NotificationManager::class.java)
        ).setNotificationsEnabled(true)
        delivery().deliver(NotificationType.SNOOZE, activity.id)
        assertTrue(
            context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty()
        )
    }
}
