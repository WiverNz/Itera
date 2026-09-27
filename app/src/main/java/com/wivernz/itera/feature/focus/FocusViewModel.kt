package com.wivernz.itera.feature.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.domain.focus.FocusEnd
import com.wivernz.itera.domain.focus.FocusSessionController
import com.wivernz.itera.domain.focus.FocusTimer
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.feature.exercise.combination.ChainCarry
import com.wivernz.itera.feature.exercise.runner.capped
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandHost
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceRejection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The running timer as the screen draws it; recomputed from `endsAt` on every tick. */
data class FocusTimerUi(
    val taskLabel: String,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val paused: Boolean
) {
    val progress: Float get() =
        if (totalSeconds == 0) 1f else 1f - remainingSeconds.toFloat() / totalSeconds
}

data class FocusUiState(
    val loading: Boolean = true,
    // the pre-timer sheet
    val task: String = "",
    val minutes: Int = 25,
    val minuteOptions: List<Int> = listOf(15, 25, 50),
    // null while the sheet shows
    val timer: FocusTimerUi? = null,
    val confirmingEnd: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false,
    // a voice-requested length that is not one of [minuteOptions]: explained, never rounded
    val unsupportedMinutes: Int? = null
)

sealed interface FocusEffect {
    /** Recorded: the result step, or back into the combination chain. */
    data class Completed(
        val activityId: Long,
        val techniqueId: String,
        val chainStep: Boolean,
        val natural: Boolean
    ) : FocusEffect

    /** Under a minute: nothing written, back to Today. */
    data object TooShort : FocusEffect
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val controller: FocusSessionController,
    private val plans: TrainingPlanRepository,
    private val analytics: Analytics,
    private val clock: Clock,
    private val logger: Logger
) : ViewModel(),
    VoiceCommandHost {
    private val activityId: Long = checkNotNull(saved.get<Long>(ARG_ACTIVITY))
    private val techniqueId: String = saved.get<String>(ARG_TECHNIQUE) ?: POMODORO
    private val seedMinutes: Int = saved.get<Int>(ARG_MINUTES) ?: DEFAULT_MINUTES
    private val requested: Int = saved.get<Int>(ARG_REQUESTED) ?: 0
    private val mutable = MutableStateFlow(
        optionsFor(seedMinutes).let { options ->
            FocusUiState(
                minutes = requested.takeIf { it in options } ?: seedMinutes,
                minuteOptions = options,
                unsupportedMinutes = requested.takeIf { it > 0 && it !in options }
            )
        }
    )
    val state: StateFlow<FocusUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<FocusEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var lastSeen: Instant = clock.instant()
    private var chainStep = false

    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.FOCUS))
        viewModelScope.launch { load() }
        viewModelScope.launch { controller.state.collect(::render) }
        viewModelScope.launch { tick() }
    }

    private suspend fun load() {
        val activity = plans.activity(activityId)
        chainStep = activity?.source == ActivitySource.COMBINATION
        val carried = activity?.takeIf { chainStep }
            ?.let { plans.day(it.trainingDayId) }
            ?.let { ChainCarry.carriedInto(activity, it) }
        val running = controller.current()
        if (running == null && activity?.state == ActivityState.COMPLETED) {
            // Ended from the notification while away: show what was recorded.
            effectChannel.send(FocusEffect.Completed(activityId, techniqueId, chainStep, false))
        }
        mutable.update { s ->
            s.copy(loading = false, task = s.task.ifEmpty { carried.orEmpty() })
        }
    }

    private fun render(timer: FocusTimerState?) {
        val now = clock.instant()
        mutable.update {
            it.copy(
                timer = timer?.let { t ->
                    FocusTimerUi(
                        t.taskLabel,
                        FocusTimer.remainingSeconds(t, now),
                        FocusTimer.totalSeconds(t),
                        t.pausedAt != null
                    )
                }
            )
        }
    }

    /** The 1 Hz render tick. It only redraws; a starved tick is corrected by the next one. */
    private suspend fun tick() {
        while (viewModelScope.isActive) {
            delay(1.seconds)
            val now = clock.instant()
            if (FocusTimer.jumpedBackwards(lastSeen, now)) {
                logger.w(
                    TAG,
                    "Clock moved backwards during a focus session; keeping the end instant"
                )
            }
            lastSeen = now
            val timer = controller.current() ?: continue
            if (FocusTimer.isFinished(timer, now)) {
                controller.finishIfElapsed()?.let { emit(it) }
            } else {
                render(timer)
            }
        }
    }

    fun setTask(text: String) = mutable.update { it.copy(task = capped(text)) }

    fun setMinutes(minutes: Int) = mutable.update {
        it.copy(minutes = minutes, unsupportedMinutes = null)
    }

    fun start() {
        val s = mutable.value
        if (s.busy || s.timer != null || s.loading) return
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            val result = controller.start(
                activityId,
                TechniqueId(techniqueId),
                s.task.trim(),
                s.minutes
            )
            mutable.update { it.copy(busy = false, failed = result.isFailure) }
        }
    }

    fun togglePause() {
        val timer = mutable.value.timer ?: return
        viewModelScope.launch { if (timer.paused) controller.resume() else controller.pause() }
    }

    fun extend() {
        if (mutable.value.timer == null) return
        viewModelScope.launch { controller.extend() }
    }

    fun requestEnd() = mutable.update { it.copy(confirmingEnd = it.timer != null) }

    fun dismissEnd() = mutable.update { it.copy(confirmingEnd = false) }

    fun confirmEnd() {
        mutable.update { it.copy(confirmingEnd = false) }
        viewModelScope.launch { controller.end()?.let { emit(it) } }
    }

    private suspend fun emit(end: FocusEnd) {
        effectChannel.send(
            when (end) {
                is FocusEnd.TooShort -> FocusEffect.TooShort
                is FocusEnd.Completed -> FocusEffect.Completed(
                    end.activityId,
                    end.techniqueId.value,
                    chainStep,
                    end.completedNaturally
                )
            }
        )
    }

    // ------------------------------------------------------------------ voice commands (milestone 012)

    override val voiceCommands: Set<VoiceCommandKind>
        get() = if (mutable.value.timer == null) {
            setOf(VoiceCommandKind.START_FOCUS)
        } else {
            setOf(
                VoiceCommandKind.PAUSE_FOCUS,
                VoiceCommandKind.RESUME_FOCUS,
                VoiceCommandKind.END_FOCUS
            )
        }

    override fun planVoice(command: VoiceCommand): VoicePlan {
        val s = mutable.value
        val timer = s.timer
        return when (command) {
            is VoiceCommand.StartFocus -> when {
                timer != null -> VoicePlan.Reject(VoiceRejection.FocusActive)
                s.loading || s.busy -> VoicePlan.Reject(VoiceRejection.Stale)
                command.minutes != null && command.minutes !in s.minuteOptions ->
                    VoicePlan.Reject(VoiceRejection.FocusDuration(command.minutes, s.minuteOptions))
                else -> VoicePlan.Run(VoiceAction.StartFocus(command.minutes))
            }
            VoiceCommand.PauseFocus -> when {
                timer == null -> VoicePlan.Reject(VoiceRejection.FocusNotRunning)
                timer.paused -> VoicePlan.Reject(VoiceRejection.FocusAlreadyPaused)
                else -> VoicePlan.Run(VoiceAction.PauseFocus)
            }
            VoiceCommand.ResumeFocus -> when {
                timer == null -> VoicePlan.Reject(VoiceRejection.FocusNotRunning)
                !timer.paused -> VoicePlan.Reject(VoiceRejection.FocusNotPaused)
                else -> VoicePlan.Run(VoiceAction.ResumeFocus)
            }
            // the existing end-session confirmation always follows
            VoiceCommand.EndFocus ->
                if (timer == null) {
                    VoicePlan.Reject(VoiceRejection.FocusNotRunning)
                } else {
                    VoicePlan.Run(VoiceAction.EndFocus)
                }
            else -> VoicePlan.Reject(VoiceRejection.NotHere)
        }
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome {
        val s = mutable.value
        val timer = controller.current()
        return when (action) {
            is VoiceAction.StartFocus -> {
                val minutes = action.minutes
                when {
                    timer != null || s.timer != null -> VoiceOutcome.Rejected(
                        VoiceRejection.FocusActive
                    )
                    minutes != null && minutes !in s.minuteOptions ->
                        VoiceOutcome.Rejected(
                            VoiceRejection.FocusDuration(minutes, s.minuteOptions)
                        )
                    else -> {
                        minutes?.let(::setMinutes)
                        if (mutable.value.task.isBlank()) {
                            // setup incomplete: the normal Start stays with the user
                            VoiceOutcome.Done(VoiceFeedback.FocusSetup(mutable.value.minutes))
                        } else {
                            start()
                            VoiceOutcome.Done(VoiceFeedback.FocusStarted)
                        }
                    }
                }
            }
            VoiceAction.PauseFocus ->
                if (timer == null || timer.pausedAt != null) {
                    VoiceOutcome.Rejected(VoiceRejection.Stale)
                } else {
                    controller.pause()
                    VoiceOutcome.Done(VoiceFeedback.FocusPaused)
                }
            VoiceAction.ResumeFocus ->
                if (timer == null || timer.pausedAt == null) {
                    VoiceOutcome.Rejected(VoiceRejection.Stale)
                } else {
                    controller.resume()
                    VoiceOutcome.Done(VoiceFeedback.FocusResumed)
                }
            VoiceAction.EndFocus ->
                if (timer == null) {
                    VoiceOutcome.Rejected(VoiceRejection.Stale)
                } else {
                    requestEnd()
                    VoiceOutcome.Done(VoiceFeedback.Handover)
                }
            else -> VoiceOutcome.Rejected(VoiceRejection.NotHere)
        }
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        const val ARG_MINUTES = "minutes"
        const val ARG_TECHNIQUE = "technique"
        const val ARG_REQUESTED = "requested"

        /** The setup's minute choices: 15/25/50 plus the seeded suggestion. */
        fun optionsFor(seed: Int): List<Int> = (OPTIONS + seed).distinct().sorted()
        private const val TAG = "Focus"
        private const val POMODORO = "pomodoro"
        private const val DEFAULT_MINUTES = 25
        private val OPTIONS = listOf(15, 25, 50)
    }
}
