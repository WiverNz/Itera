package com.wivernz.itera.domain.focus

import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import java.time.Duration
import java.time.Instant

/**
 * Pure timer arithmetic, docs/engine/05-timer-lifecycle.md sections 1, 5 and 7. There is never a decrementing
 * counter: remaining time is always `endsAt - now`, so a starved render tick corrects itself on the next one.
 */
object FocusTimer {
    const val EXTEND_SECONDS = 300

    /** A session shorter than this writes nothing and returns to Today (a mis-tap). */
    const val MIN_RECORDED_SECONDS = 60

    /** A backward clock jump larger than this is logged; the timer keeps its original `endsAt`. */
    val CLOCK_JUMP: Duration = Duration.ofSeconds(60)

    fun start(
        activityId: Long,
        techniqueId: TechniqueId,
        taskLabel: String,
        minutes: Int,
        now: Instant
    ): FocusTimerState = FocusTimerState(
        activityId = activityId,
        techniqueId = techniqueId,
        taskLabel = taskLabel,
        plannedSeconds = minutes * SECONDS_PER_MINUTE,
        extendedSeconds = 0,
        startedAt = now,
        endsAt = now.plusSeconds(minutes * SECONDS_PER_MINUTE.toLong()),
        pausedAt = null,
        accumulatedPauseMs = 0
    )

    fun pause(state: FocusTimerState, now: Instant): FocusTimerState =
        if (state.pausedAt != null) state else state.copy(pausedAt = now)

    /** Shifts `endsAt` forward by exactly the pause. */
    fun resume(state: FocusTimerState, now: Instant): FocusTimerState {
        val pausedAt = state.pausedAt ?: return state
        val pause = Duration.between(pausedAt, now).coerceAtLeast(Duration.ZERO)
        return state.copy(
            endsAt = state.endsAt.plus(pause),
            pausedAt = null,
            accumulatedPauseMs = state.accumulatedPauseMs + pause.toMillis()
        )
    }

    /** `+5 min`: additive and repeatable, added to both the end and the total so the ring does not jump. */
    fun extend(state: FocusTimerState): FocusTimerState = state.copy(
        endsAt = state.endsAt.plusSeconds(EXTEND_SECONDS.toLong()),
        extendedSeconds = state.extendedSeconds + EXTEND_SECONDS
    )

    fun totalSeconds(state: FocusTimerState): Int = state.plannedSeconds + state.extendedSeconds

    /** Whole seconds left, rounded up so the readout reaches 00:00 only when the time is really up. */
    fun remainingSeconds(state: FocusTimerState, now: Instant): Int {
        val millis = state.remaining(now).toMillis()
        return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    fun isFinished(state: FocusTimerState, now: Instant): Boolean =
        state.pausedAt == null && state.remaining(now).isZero

    /**
     * Seconds actually focused: elapsed minus pauses, clamped to the planned-plus-extended total. A forward
     * clock jump past `endsAt` therefore yields exactly the total.
     */
    fun actualSeconds(state: FocusTimerState, now: Instant): Int {
        val end = minOf(state.pausedAt ?: now, state.endsAt)
        val focused = Duration.between(state.startedAt, end).toMillis() - state.accumulatedPauseMs
        return (focused / MILLIS_PER_SECOND).toInt().coerceIn(0, totalSeconds(state))
    }

    /** How a session ends, given whether the time ran out. */
    fun outcome(state: FocusTimerState, now: Instant): FocusOutcome {
        if (isFinished(state, now)) {
            return FocusOutcome.Record(totalSeconds(state), completedNaturally = true)
        }
        val actual = actualSeconds(state, now)
        return if (actual < MIN_RECORDED_SECONDS) {
            FocusOutcome.TooShort
        } else {
            FocusOutcome.Record(actual, completedNaturally = false)
        }
    }

    /** True when the wall clock moved backwards by more than [CLOCK_JUMP] since [lastSeen]. */
    fun jumpedBackwards(lastSeen: Instant, now: Instant): Boolean =
        Duration.between(now, lastSeen) > CLOCK_JUMP

    private const val SECONDS_PER_MINUTE = 60
    private const val MILLIS_PER_SECOND = 1000L
}

sealed interface FocusOutcome {
    /** Under a minute: nothing is written and the activity stays open. */
    data object TooShort : FocusOutcome
    data class Record(val actualSeconds: Int, val completedNaturally: Boolean) : FocusOutcome
}
