package com.wivernz.itera.core.notifications

import android.content.Context
import com.wivernz.itera.R
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.analytics.SuppressionReason
import com.wivernz.itera.core.navigation.ExerciseIntro
import com.wivernz.itera.core.navigation.Reflection
import com.wivernz.itera.core.navigation.Review
import com.wivernz.itera.core.navigation.Today
import com.wivernz.itera.data.database.dao.HabitStackDao
import com.wivernz.itera.data.database.dao.LearningTopicDao
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ReminderDelivery @Inject constructor(
    @ApplicationContext context: Context,
    private val plans: TrainingPlanRepository,
    private val preferences: PreferencesRepository,
    private val timers: FocusTimerRepository,
    private val habits: HabitStackDao,
    private val topics: LearningTopicDao,
    private val reviews: ReviewRepository,
    private val notifier: IteraNotifier,
    private val environment: ReminderEnvironment,
    private val analytics: Analytics,
    private val clock: Clock
) {
    private val mutex = Mutex()
    // Only dates and numeric target keys, never notification copy or user content.
    private val ledger = context.getSharedPreferences("reminder_delivery", Context.MODE_PRIVATE)

    /** Returns an evening retry instant, otherwise this occurrence is finished. */
    suspend fun deliver(type: NotificationType, target: Long): Instant? = mutex.withLock {
        val now = clock.instant()
        val local = now.atZone(ZoneId.systemDefault())
        val prefs = preferences.preferences.first()
        val day = plans.dayByDate(local.toLocalDate())
        val activities = day?.activities.orEmpty()
        val due = if (prefs.notifyReviews) reviews.observeDue(local.toLocalDate()).first().firstOrNull() else null
        val program = activities.firstOrNull { it.source == ActivitySource.PROGRAM || (it.source == ActivitySource.COMBINATION && it.exerciseType == ExerciseType.COMBINATION) }
        val review = activities.firstOrNull { it.reviewItemId == due?.id && it.source == ActivitySource.REVIEW }
        val activity = when (type) {
            NotificationType.MORNING, NotificationType.REVIEW -> if (prefs.notifyMorning) program else review
            NotificationType.EVENING -> activities.firstOrNull { it.source == ActivitySource.REFLECTION }
            NotificationType.FOCUS, NotificationType.SNOOZE -> activities.firstOrNull { it.id == target }
            NotificationType.HABIT -> null
        }
        val habit = if (type == NotificationType.HABIT) habits.observeActive().first().firstOrNull { it.id == target } else null
        val timer = timers.observe().first()
        val focusRunning = timer != null && (timer.pausedAt != null || timer.endsAt > now)
        val enabled = prefs.onboardingCompleted && environment.channelEnabled(IteraNotifier.channel(type)) && when (type) {
            NotificationType.MORNING -> prefs.notifyMorning || (prefs.notifyReviews && due != null)
            NotificationType.REVIEW -> prefs.notifyReviews
            NotificationType.FOCUS -> prefs.notifyFocus
            NotificationType.EVENING -> prefs.notifyEvening
            NotificationType.HABIT -> habit?.nudgeEnabled == true
            NotificationType.SNOOZE -> prefs.notifyMorning
        }
        val facts = ReminderFacts(
            permitted = environment.permitted(), enabled = enabled, foreground = environment.foreground,
            focusRunning = focusRunning, state = activity?.state, snoozedUntil = activity?.snoozedUntil,
            focusCompleted = activities.any { it.exerciseType == ExerciseType.FOCUS_TIMER && it.state == ActivityState.COMPLETED },
            habitArchived = habit == null,
            habitLogged = activities.any { it.exerciseType == ExerciseType.HABIT_STACK && it.state == ActivityState.COMPLETED }
        )
        val suppression = ReminderPolicy.suppression(type, facts, now)
        if (type == NotificationType.EVENING && enabled && environment.permitted() && focusRunning &&
            activity?.state !in setOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED)) {
            val retry = if (timer!!.pausedAt != null) now.plusSeconds(300) else timer.endsAt.plusSeconds(1)
            if (retry.atZone(local.zone).toLocalDate() == local.toLocalDate() && retry.atZone(local.zone).toLocalTime() < LocalTime.of(23, 0)) {
                analytics.track(Event.NotificationSuppressed(type, SuppressionReason.FOCUS_RUNNING))
                return@withLock retry
            }
            analytics.track(Event.NotificationSuppressed(type, SuppressionReason.TOO_LATE))
            return@withLock null
        }
        if (suppression != null) {
            analytics.track(Event.NotificationSuppressed(type, suppression))
            return@withLock null
        }
        if (type == NotificationType.EVENING && local.toLocalTime() >= LocalTime.of(23, 0)) {
            analytics.track(Event.NotificationSuppressed(type, SuppressionReason.TOO_LATE))
            return@withLock null
        }
        val occurrence = if (type == NotificationType.SNOOZE) "$target:${activity?.snoozedUntil}" else local.toLocalDate().toString()
        val ledgerKey = if (type == NotificationType.HABIT) "HABIT_$target" else type.name
        if (ledger.getString(ledgerKey, null) == occurrence) {
            analytics.track(Event.NotificationSuppressed(type, SuppressionReason.ALREADY_POSTED))
            return@withLock null
        }
        if (type == NotificationType.SNOOZE && activity?.state == ActivityState.SNOOZED) plans.makeAvailable(activity.id)
        val name = activity?.let { notifier.technique(it.techniqueId.value) }.orEmpty()
        val content = when (type) {
            NotificationType.MORNING, NotificationType.REVIEW -> {
                val topic = due?.topicId?.let { id -> topics.observeActive().first().firstOrNull { it.id == id }?.title }
                    ?: notifier.text(R.string.notification_review_topic)
                ReminderContent(type,
                    notifier.text(if (due != null) R.string.notification_morning_review_title else R.string.notification_morning_title),
                    if (due != null) notifier.text(R.string.notification_review_body, name, topic)
                    else notifier.text(R.string.notification_training_body, name, activity!!.estimatedMinutes),
                    if (!prefs.notifyMorning && review != null) Review(review.id) else ExerciseIntro(activity!!.id, activity.techniqueId.value))
            }
            NotificationType.FOCUS -> ReminderContent(type, notifier.text(R.string.notification_focus_title), notifier.text(R.string.notification_focus_body, activity!!.estimatedMinutes), Today)
            NotificationType.EVENING -> ReminderContent(type, notifier.text(R.string.notification_evening_title), notifier.text(R.string.notification_evening_body), Reflection(activity!!.id))
            NotificationType.HABIT -> ReminderContent(type, notifier.text(R.string.notification_habit_title, habit!!.anchor), habit.habit, Today)
            NotificationType.SNOOZE -> ReminderContent(type, notifier.text(R.string.notification_snooze_title), notifier.text(R.string.notification_training_body, name, activity!!.estimatedMinutes), ExerciseIntro(activity.id, activity.techniqueId.value))
        }
        if (notifier.post(content)) ledger.edit().putString(ledgerKey, occurrence).commit()
        null
    }
}
