package com.wivernz.itera.feature.voice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceCommandParser
import com.wivernz.itera.domain.voice.VoiceInterpretation
import com.wivernz.itera.domain.voice.VoiceItem
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.domain.voice.VoiceParse
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A validated command bound to this screen's current state, ready for its existing action. */
sealed interface VoiceAction {
    data class AddItem(val text: String) : VoiceAction
    data class CompleteItem(val item: VoiceItem) : VoiceAction
    data class StartFocus(val minutes: Int?) : VoiceAction
    data object PauseFocus : VoiceAction
    data object ResumeFocus : VoiceAction
    data object EndFocus : VoiceAction
    data object CompleteExercise : VoiceAction
    data object ShowRecommendation : VoiceAction
}

/** Why nothing happened. Every rejection leaves business state untouched. */
sealed interface VoiceRejection {
    data object NotHere : VoiceRejection
    data object NoList : VoiceRejection
    data object ListFull : VoiceRejection
    data class NoMatch(val query: String) : VoiceRejection
    data object FocusNotRunning : VoiceRejection
    data object FocusAlreadyPaused : VoiceRejection
    data object FocusNotPaused : VoiceRejection
    data object FocusActive : VoiceRejection
    data class FocusDuration(val minutes: Int, val options: List<Int>) : VoiceRejection
    data object NoFocusEntry : VoiceRejection
    data object ExerciseNotReady : VoiceRejection
    data object Stale : VoiceRejection
    data object Failed : VoiceRejection
}

sealed interface VoiceFeedback {
    data class Added(val text: String) : VoiceFeedback
    data class Completed(val label: String) : VoiceFeedback
    data object FocusStarted : VoiceFeedback
    data class FocusSetup(val minutes: Int) : VoiceFeedback
    data object FocusPaused : VoiceFeedback
    data object FocusResumed : VoiceFeedback
    data object Recommendation : VoiceFeedback

    /** The screen took over (a dialog, a result, another screen): the sheet just closes. */
    data object Handover : VoiceFeedback
}

/** How a screen answers a recognised command, against its state right now. */
sealed interface VoicePlan {
    data class Run(val action: VoiceAction) : VoicePlan

    /** Destructive or leaving: a visible confirmation first. [note] names what is not kept. */
    data class Confirm(val action: VoiceAction, val note: VoiceConfirmNote? = null) : VoicePlan

    /** Duplicate or partial matches: pick one, then Confirm. */
    data class Choose(val candidates: List<VoiceItem>) : VoicePlan
    data class Reject(val reason: VoiceRejection) : VoicePlan
}

enum class VoiceConfirmNote { MITIGATION_NOT_ADDED }

sealed interface VoiceOutcome {
    data class Done(val feedback: VoiceFeedback) : VoiceOutcome
    data class Rejected(val reason: VoiceRejection) : VoiceOutcome
}

/**
 * A screen that accepts voice commands. It validates against its current state and executes through the same
 * view-model actions its buttons use; execution re-checks that state, so a stale plan never mutates.
 */
interface VoiceCommandHost {
    val voiceCommands: Set<VoiceCommandKind>

    fun planVoice(command: VoiceCommand): VoicePlan

    suspend fun executeVoice(action: VoiceAction): VoiceOutcome
}

/**
 * Adds ShowCurrentRecommendation to a host: [show] opens Today's hero. Leaving an exercise asks first ([confirm]),
 * and its draft is kept by the screen's own leave path.
 */
fun VoiceCommandHost.withRecommendation(confirm: Boolean, show: () -> Unit): VoiceCommandHost {
    val base = this
    return object : VoiceCommandHost {
        override val voiceCommands =
            base.voiceCommands + VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION

        override fun planVoice(command: VoiceCommand): VoicePlan =
            if (command is VoiceCommand.ShowCurrentRecommendation) {
                if (confirm) {
                    VoicePlan.Confirm(VoiceAction.ShowRecommendation)
                } else {
                    VoicePlan.Run(VoiceAction.ShowRecommendation)
                }
            } else {
                base.planVoice(command)
            }

        override suspend fun executeVoice(action: VoiceAction): VoiceOutcome =
            if (action == VoiceAction.ShowRecommendation) {
                show()
                VoiceOutcome.Done(VoiceFeedback.Handover)
            } else {
                base.executeVoice(action)
            }
    }
}

sealed interface VoiceCommandPhase {
    /** Mode, language and what can be said here. */
    data object Ready : VoiceCommandPhase

    /** Parsed, but not a usable command; [heard] is the top alternative, shown back once. */
    data class NotUnderstood(val parse: VoiceParse, val heard: String) : VoiceCommandPhase

    /** The alternatives disagree: the user selects the one they meant. */
    data class Competing(val commands: List<VoiceCommand>, val selected: Int? = null) :
        VoiceCommandPhase

    data class Choosing(val candidates: List<VoiceItem>, val selected: Int? = null) :
        VoiceCommandPhase

    data class Confirming(val action: VoiceAction, val note: VoiceConfirmNote?) : VoiceCommandPhase

    data object Working : VoiceCommandPhase

    data class Done(val feedback: VoiceFeedback) : VoiceCommandPhase

    data class Rejected(val reason: VoiceRejection) : VoiceCommandPhase
}

/**
 * The command sheet's state: interpret one final utterance, plan it on the [host], and run at most one action per
 * utterance. Nothing here is persisted; closing the sheet discards it.
 */
class VoiceCommandFlow(private val host: VoiceCommandHost, private val scope: CoroutineScope) {
    var phase: VoiceCommandPhase by mutableStateOf(VoiceCommandPhase.Ready)
        private set

    private var busy = false

    val commands: Set<VoiceCommandKind> get() = host.voiceCommands

    fun onFinal(alternatives: List<String>, language: VoiceLanguage) {
        if (busy) return
        when (val interpretation = VoiceCommandParser.interpret(alternatives, language)) {
            is VoiceInterpretation.Competing ->
                phase =
                    VoiceCommandPhase.Competing(interpretation.commands)
            is VoiceInterpretation.Single -> when (val parse = interpretation.parse) {
                is VoiceParse.Recognised -> plan(parse.command)
                else -> phase = VoiceCommandPhase.NotUnderstood(parse, alternatives.first())
            }
        }
    }

    fun select(index: Int) {
        phase = when (val p = phase) {
            is VoiceCommandPhase.Competing -> p.copy(
                selected = index.takeIf {
                    it in
                        p.commands.indices
                }
            )
            is VoiceCommandPhase.Choosing -> p.copy(
                selected = index.takeIf {
                    it in
                        p.candidates.indices
                }
            )
            else -> p
        }
    }

    /** The explicit Confirm of a selection or a confirmation dialog. */
    fun confirm() {
        when (val p = phase) {
            is VoiceCommandPhase.Competing -> p.selected?.let { plan(p.commands[it]) }
            is VoiceCommandPhase.Choosing ->
                p.selected?.let { execute(VoiceAction.CompleteItem(p.candidates[it])) }
            is VoiceCommandPhase.Confirming -> execute(p.action)
            else -> Unit
        }
    }

    /** Cancel, Back or dismiss on a pending choice or confirmation: nothing runs. */
    fun reset() {
        if (!busy) phase = VoiceCommandPhase.Ready
    }

    private fun plan(command: VoiceCommand) {
        if (command.kind !in host.voiceCommands) {
            phase = VoiceCommandPhase.Rejected(VoiceRejection.NotHere)
            return
        }
        when (val plan = host.planVoice(command)) {
            is VoicePlan.Run -> execute(plan.action)
            is VoicePlan.Confirm -> phase = VoiceCommandPhase.Confirming(plan.action, plan.note)
            is VoicePlan.Choose -> phase = VoiceCommandPhase.Choosing(plan.candidates)
            is VoicePlan.Reject -> phase = VoiceCommandPhase.Rejected(plan.reason)
        }
    }

    private fun execute(action: VoiceAction) {
        if (busy) return
        busy = true
        phase = VoiceCommandPhase.Working
        scope.launch {
            val outcome = try {
                host.executeVoice(action)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                VoiceOutcome.Rejected(VoiceRejection.Failed)
            }
            busy = false
            phase = when (outcome) {
                is VoiceOutcome.Done -> VoiceCommandPhase.Done(outcome.feedback)
                is VoiceOutcome.Rejected -> VoiceCommandPhase.Rejected(outcome.reason)
            }
        }
    }
}
