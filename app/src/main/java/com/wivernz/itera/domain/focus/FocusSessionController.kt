package com.wivernz.itera.domain.focus

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.AbandonActivityUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shows or removes the foreground-service notification; the Android side reads the stored state itself. */
interface FocusServiceLauncher {
    /** Running: foreground service with a chronometer. Paused: a plain notification. Null: nothing. */
    fun update(state: FocusTimerState?)
}

/** How a session left the timer. */
sealed interface FocusEnd {
    /** Under a minute: nothing written, the activity is open again. */
    data class TooShort(val activityId: Long) : FocusEnd

    data class Completed(
        val activityId: Long,
        val techniqueId: TechniqueId,
        val completedNaturally: Boolean
    ) : FocusEnd
}

/** What app start found (the restore table, docs/engine/05 section 2). */
sealed interface FocusRestoreResult {
    data object Nothing : FocusRestoreResult

    /** Re-attach the session screen, running or paused. */
    data class Attach(val state: FocusTimerState) : FocusRestoreResult

    /** The time ran out while the app was dead: already completed; show the result. */
    data class Finished(val end: FocusEnd.Completed) : FocusRestoreResult
}

/**
 * The single owner of focus-timer writes, used by the session screen, the notification actions and app start.
 * Every change is persisted to `focus_timer.pb` before the service is told about it. One instance per process
 * (scoped in `FocusModule`), so its lock serialises every writer.
 */
class FocusSessionController(
    private val timers: FocusTimerRepository,
    private val plans: TrainingPlanRepository,
    private val startActivity: StartActivityUseCase,
    private val abandon: AbandonActivityUseCase,
    private val complete: CompleteActivityUseCase,
    private val launcher: FocusServiceLauncher,
    private val analytics: Analytics,
    private val clock: Clock,
    private val logger: Logger
) {
    private val mutex = Mutex()

    val state: Flow<FocusTimerState?> = timers.observe()

    /** The running-session state Itera's reminder workers read to stay quiet (milestone 009). */
    val sessionRunning: Flow<Boolean> = timers.observe().map { it != null && it.pausedAt == null }

    suspend fun current(): FocusTimerState? = timers.observe().first()

    suspend fun start(
        activityId: Long,
        techniqueId: TechniqueId,
        taskLabel: String,
        minutes: Int
    ): Result<FocusTimerState> = mutex.withLock {
        timers.observe().first()?.let { return Result.success(it) }
        val activity = plans.activity(activityId)
            ?: return Result.failure(DomainException(DomainError.ActivityNotFound))
        if (activity.state == ActivityState.AVAILABLE || activity.state == ActivityState.SNOOZED) {
            startActivity(activityId).onFailure { return Result.failure(it) }
        } else if (activity.state != ActivityState.IN_PROGRESS) {
            return Result.failure(
                DomainException(DomainError.IllegalTransition(activity.state, "StartFocus"))
            )
        }
        val state = FocusTimer.start(activityId, techniqueId, taskLabel, minutes, clock.instant())
        timers.save(state)
        launcher.update(state)
        AnalyticsTechnique.of(techniqueId.value)?.let {
            analytics.track(Event.FocusStarted(it, minutes))
        }
        Result.success(state)
    }

    suspend fun pause() = change { state ->
        FocusTimer.pause(state, clock.instant()).also {
            analytics.track(Event.FocusPaused(FocusTimer.remainingSeconds(state, clock.instant())))
        }
    }

    suspend fun resume() = change { FocusTimer.resume(it, clock.instant()) }

    suspend fun extend() = change { state ->
        FocusTimer.extend(state).also {
            analytics.track(
                Event.FocusExtended(
                    FocusTimer.EXTEND_SECONDS,
                    it.extendedSeconds / FocusTimer.EXTEND_SECONDS
                )
            )
        }
    }

    /** "End" (after the confirmation) or the notification's End action. */
    suspend fun end(): FocusEnd? = mutex.withLock {
        val state = timers.observe().first() ?: return null
        finish(state)
    }

    /** Called by the 1 Hz render tick; completes a session whose time is up. */
    suspend fun finishIfElapsed(): FocusEnd? = mutex.withLock {
        val state = timers.observe().first() ?: return null
        if (FocusTimer.isFinished(state, clock.instant())) finish(state) else null
    }

    suspend fun restore(): FocusRestoreResult = mutex.withLock {
        when (val restored = timers.restore()) {
            FocusTimerRestore.None, FocusTimerRestore.Discarded -> FocusRestoreResult.Nothing
            is FocusTimerRestore.Running -> attach(restored.state)
            is FocusTimerRestore.Paused -> attach(restored.state)
            is FocusTimerRestore.AutoComplete -> when (val end = finish(restored.state)) {
                is FocusEnd.Completed -> FocusRestoreResult.Finished(end)
                else -> FocusRestoreResult.Nothing
            }
        }
    }

    private fun attach(state: FocusTimerState): FocusRestoreResult {
        launcher.update(state)
        analytics.track(
            Event.FocusRestored(
                FocusTimer.remainingSeconds(state, clock.instant()),
                afterProcessDeath = true
            )
        )
        return FocusRestoreResult.Attach(state)
    }

    private suspend fun change(transform: (FocusTimerState) -> FocusTimerState) = mutex.withLock {
        val state = timers.observe().first() ?: return@withLock
        val next = transform(state)
        if (next != state) {
            timers.save(next)
            launcher.update(next)
        }
    }

    private suspend fun finish(state: FocusTimerState): FocusEnd {
        val now = clock.instant()
        val outcome = FocusTimer.outcome(state, now)
        if (outcome is FocusOutcome.Record && outcome.completedNaturally &&
            now.isAfter(state.endsAt.plus(FocusTimer.CLOCK_JUMP))
        ) {
            logger.w(TAG, "Focus session completed after its end instant; seconds clamped")
        }
        val end = when (outcome) {
            FocusOutcome.TooShort -> {
                abandon(state.activityId)
                FocusEnd.TooShort(state.activityId)
            }
            is FocusOutcome.Record -> {
                val result = ActivityResult.Focus(
                    taskLabel = state.taskLabel,
                    plannedSeconds = state.plannedSeconds,
                    actualSeconds = outcome.actualSeconds,
                    extendedSeconds = state.extendedSeconds,
                    completedNaturally = outcome.completedNaturally
                )
                complete(state.activityId, result).onFailure {
                    logger.w(TAG, "Focus completion failed", it)
                }
                analytics.track(
                    Event.FocusCompleted(
                        state.plannedSeconds,
                        outcome.actualSeconds,
                        outcome.completedNaturally
                    )
                )
                FocusEnd.Completed(state.activityId, state.techniqueId, outcome.completedNaturally)
            }
        }
        timers.clear()
        launcher.update(null)
        return end
    }

    private companion object {
        const val TAG = "FocusSession"
    }
}
