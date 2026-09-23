package com.wivernz.itera.domain.model
import java.time.Duration
import java.time.Instant
/** docs/engine/05-timer-lifecycle.md section 1. */
data class FocusTimerState(
    val activityId: Long,
    val techniqueId: TechniqueId,
    val taskLabel: String,
    val plannedSeconds: Int,
    val extendedSeconds: Int,
    val startedAt: Instant,
    val endsAt: Instant,
    val pausedAt: Instant?,
    val accumulatedPauseMs: Long
) {
    init {
        require(plannedSeconds > 0)
        require(extendedSeconds >= 0)
        require(accumulatedPauseMs >= 0)
    }
    fun remaining(now: Instant): Duration =
        Duration.between(pausedAt ?: now, endsAt).coerceAtLeast(Duration.ZERO)
}

/** Restore decisions only; completion effects belong to the timer feature. */
sealed interface FocusTimerRestore {
    data object None : FocusTimerRestore
    data class Running(val state: FocusTimerState) : FocusTimerRestore
    data class Paused(val state: FocusTimerState) : FocusTimerRestore
    data class AutoComplete(val state: FocusTimerState) : FocusTimerRestore
    data object Discarded : FocusTimerRestore
}
