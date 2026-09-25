package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal val TERMINAL_STATES =
    setOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED)

internal fun PlanActivity.transitionFacts(day: TrainingDay, zone: ZoneId) = TransitionFacts(
    exerciseType = exerciseType,
    source = source,
    optional = optional,
    becomesAvailableAt = availableAtInstant(dayPart, scheduledAt, day.date, zone),
    snoozedUntil = snoozedUntil,
    endOfDay = endOfDay(day.date, zone)
)

internal fun failure(error: DomainError): Result<Nothing> = Result.failure(DomainException(error))

internal fun secondsBetween(from: Instant?, to: Instant): Int =
    from?.let { Duration.between(it, to).seconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt() } ?: 0

/**
 * Completion effect 6, first half: when every non-optional activity is COMPLETED or SKIPPED, marks the day
 * COMPLETE and stamps `completedAt`. A no-op on a day that is already COMPLETE. Returns true when this call
 * completed the day; the caller then runs `AdvanceProgramDayUseCase` after the transaction commits.
 */
internal suspend fun completeDayIfDone(
    plans: TrainingPlanRepository,
    analytics: Analytics,
    dayId: Long,
    now: Instant
): Boolean {
    val day = plans.day(dayId) ?: return false
    if (day.status == TrainingDayStatus.COMPLETE) return false
    val required = day.activities.filter { !it.optional }
    val done = required.all {
        it.state == ActivityState.COMPLETED ||
            it.state == ActivityState.SKIPPED
    }
    if (required.isEmpty() || !done) return false
    plans.updateDayStatus(dayId, TrainingDayStatus.COMPLETE, now)
    analytics.append(
        Event.DayCompleted(
            programDay = day.programDay,
            completedCount = day.activities.count { it.state == ActivityState.COMPLETED },
            skippedCount = day.activities.count { it.state == ActivityState.SKIPPED },
            durationMinutes = day.activities.sumOf { it.durationSeconds ?: 0 } / 60
        )
    )
    return true
}
