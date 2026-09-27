package com.wivernz.itera.core.notifications

import com.wivernz.itera.analytics.NotificationType
import com.wivernz.itera.analytics.SuppressionReason
import com.wivernz.itera.domain.model.ActivityState
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

data class ReminderFacts(
    val permitted: Boolean = true,
    val enabled: Boolean = true,
    val foreground: Boolean = false,
    val focusRunning: Boolean = false,
    val state: ActivityState? = ActivityState.AVAILABLE,
    val snoozedUntil: Instant? = null,
    val focusCompleted: Boolean = false,
    val habitArchived: Boolean = false,
    val habitLogged: Boolean = false
)

object ReminderPolicy {
    fun suppression(type: NotificationType, facts: ReminderFacts, now: Instant): SuppressionReason? = with(facts) {
        when {
            !permitted -> SuppressionReason.PERMISSION_DENIED
            !enabled -> SuppressionReason.DISABLED
            foreground -> SuppressionReason.FOREGROUND
            focusRunning -> SuppressionReason.FOCUS_RUNNING
            type == NotificationType.HABIT -> if (habitArchived || habitLogged) SuppressionReason.ALREADY_COMPLETE else null
            state == null || state in setOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED) -> SuppressionReason.ALREADY_COMPLETE
            state == ActivityState.IN_PROGRESS -> SuppressionReason.IN_PROGRESS
            snoozedUntil?.isAfter(now) == true -> SuppressionReason.SNOOZED
            type == NotificationType.FOCUS && (state != ActivityState.AVAILABLE || focusCompleted) -> SuppressionReason.ALREADY_COMPLETE
            else -> null
        }
    }

    /** Zoned arithmetic preserves local clock times across both DST transitions. */
    fun next(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }
    fun delay(now: Instant, at: Instant): Duration = Duration.between(now, at).coerceAtLeast(Duration.ZERO)
}
