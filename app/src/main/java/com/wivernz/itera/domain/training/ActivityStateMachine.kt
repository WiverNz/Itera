package com.wivernz.itera.domain.training

import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Events of docs/engine/00-exercise-state-machine.md section 3. Side effects named on each event are the
 * caller's responsibility; the machine only decides the target state.
 */
sealed interface ActivityEvent {
    /** Day part or snooze time reached. Caller clears `snoozedUntil` when leaving SNOOZED. */
    data object Reach : ActivityEvent

    /** User taps Start. Caller sets `startedAt`, moves the day to IN_PROGRESS, clears snooze and its reminder. */
    data object Start : ActivityEvent

    /** "Not now". Caller sets `snoozedUntil` and schedules a one-shot reminder. */
    data class Snooze(val until: Instant) : ActivityEvent

    /** Optional activities and the reflection only. Caller clears the draft. */
    data object Skip : ActivityEvent

    /** Result saved. Caller persists it, clears the draft and fires the post-completion effects (section 5). */
    data class Complete(val result: ActivityResult) : ActivityEvent

    /** Back out without a result. Caller keeps the draft and clears `startedAt`. */
    data object Abandon : ActivityEvent

    /** The day ended. Caller cancels reminders and keeps drafts. */
    data object DayRollover : ActivityEvent
}

/** The facts a guard needs. Pure data; the machine performs no I/O. */
data class TransitionFacts(
    val exerciseType: ExerciseType,
    val source: ActivitySource,
    val optional: Boolean,
    val becomesAvailableAt: Instant,
    val snoozedUntil: Instant?,
    // exclusive: the start of the next local day
    val endOfDay: Instant
)

object ActivityStateMachine {
    fun transition(
        current: ActivityState,
        event: ActivityEvent,
        now: Instant,
        facts: TransitionFacts
    ): Result<ActivityState> {
        val target: ActivityState? = when (current) {
            ActivityState.SCHEDULED -> when (event) {
                ActivityEvent.Reach ->
                    ActivityState.AVAILABLE.takeIf { now >= facts.becomesAvailableAt }
                ActivityEvent.DayRollover -> ActivityState.EXPIRED
                else -> null
            }
            ActivityState.AVAILABLE -> when (event) {
                ActivityEvent.Start -> ActivityState.IN_PROGRESS
                is ActivityEvent.Snooze ->
                    ActivityState.SNOOZED.takeIf {
                        event.until > now && event.until < facts.endOfDay
                    }
                ActivityEvent.Skip -> ActivityState.SKIPPED.takeIf { canSkip(facts) }
                ActivityEvent.DayRollover -> ActivityState.EXPIRED
                else -> null
            }
            ActivityState.SNOOZED -> when (event) {
                ActivityEvent.Reach ->
                    ActivityState.AVAILABLE.takeIf {
                        facts.snoozedUntil == null || now >= facts.snoozedUntil
                    }
                ActivityEvent.Start -> ActivityState.IN_PROGRESS
                ActivityEvent.DayRollover -> ActivityState.EXPIRED
                else -> null
            }
            ActivityState.IN_PROGRESS -> when (event) {
                is ActivityEvent.Complete ->
                    ActivityState.COMPLETED.takeIf {
                        resultTypeOf(event.result) == facts.exerciseType
                    }
                ActivityEvent.Abandon -> ActivityState.AVAILABLE
                ActivityEvent.Skip -> ActivityState.SKIPPED.takeIf { canSkip(facts) }
                ActivityEvent.DayRollover -> ActivityState.EXPIRED
                else -> null
            }
            ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED -> null
        }
        return target?.let { Result.success(it) }
            ?: Result.failure(
                DomainException(DomainError.IllegalTransition(current, eventName(event)))
            )
    }

    private fun canSkip(facts: TransitionFacts) =
        facts.optional || facts.source == ActivitySource.REFLECTION

    private fun eventName(event: ActivityEvent): String = when (event) {
        ActivityEvent.Reach -> "Reach"
        ActivityEvent.Start -> "Start"
        is ActivityEvent.Snooze -> "Snooze"
        ActivityEvent.Skip -> "Skip"
        is ActivityEvent.Complete -> "Complete"
        ActivityEvent.Abandon -> "Abandon"
        ActivityEvent.DayRollover -> "DayRollover"
    }
}

/** Section 4. MORNING from 00:00; DAYTIME `max(morning + 90 min, 11:00)`; EVENING an hour before the evening time. */
fun becomesAvailableAt(dayPart: DayPart, preferences: UserPreferences): LocalTime = availableFrom(
    dayPart,
    when (dayPart) {
        DayPart.MORNING -> null
        DayPart.DAYTIME -> daytimeTime(preferences.morningTime)
        DayPart.EVENING -> preferences.eveningTime
    }
)

/**
 * Availability of a stored activity from its own `scheduledAt`, so a plan stays stable for its day: DAYTIME
 * items open at their scheduled time, the EVENING reflection an hour before it.
 */
fun availableFrom(dayPart: DayPart, scheduledAt: LocalTime?): LocalTime = when (dayPart) {
    DayPart.MORNING -> LocalTime.MIDNIGHT
    DayPart.DAYTIME -> scheduledAt ?: LocalTime.of(11, 0)
    DayPart.EVENING -> {
        val evening = scheduledAt ?: LocalTime.of(21, 0)
        if (evening.toSecondOfDay() < 3600) LocalTime.MIDNIGHT else evening.minusMinutes(60)
    }
}

fun availableAtInstant(
    dayPart: DayPart,
    scheduledAt: LocalTime?,
    date: LocalDate,
    zone: ZoneId
): Instant = date.atTime(availableFrom(dayPart, scheduledAt)).atZone(zone).toInstant()

/** `max(morningTime + 90 min, 11:00)`, saturating at the end of the day. */
fun daytimeTime(morningTime: LocalTime): LocalTime {
    val minutes = (morningTime.hour * 60 + morningTime.minute + 90).coerceAtMost(23 * 60 + 59)
    return maxOf(LocalTime.of(minutes / 60, minutes % 60), LATEST_DAYTIME_FLOOR)
}

fun endOfDay(date: LocalDate, zone: ZoneId): Instant =
    date.plusDays(1).atStartOfDay(zone).toInstant()

fun resultTypeOf(result: ActivityResult): ExerciseType = when (result) {
    is ActivityResult.Template -> ExerciseType.TEMPLATE
    is ActivityResult.Focus -> ExerciseType.FOCUS_TIMER
    is ActivityResult.Eisenhower -> ExerciseType.EISENHOWER
    is ActivityResult.Feynman -> ExerciseType.FEYNMAN
    is ActivityResult.Premortem -> ExerciseType.PREMORTEM
    is ActivityResult.HabitStack -> ExerciseType.HABIT_STACK
    is ActivityResult.Reflection -> ExerciseType.REFLECTION
    is ActivityResult.Review -> ExerciseType.REVIEW
    is ActivityResult.Combination -> ExerciseType.COMBINATION
}

private val LATEST_DAYTIME_FLOOR: LocalTime = LocalTime.of(11, 0)
