package com.wivernz.itera.feature.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind

/** A screen with no commands of its own; ShowCurrentRecommendation is added by [withRecommendation]. */
object NoVoiceCommands : VoiceCommandHost {
    override val voiceCommands: Set<VoiceCommandKind> = emptySet()

    override fun planVoice(command: VoiceCommand): VoicePlan =
        VoicePlan.Reject(VoiceRejection.NotHere)

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome =
        VoiceOutcome.Rejected(VoiceRejection.NotHere)
}

/**
 * Voice for an exercise screen: its own commands plus ShowCurrentRecommendation, which (after confirming) takes
 * the screen's normal leave path, so the draft is kept and the activity is released, and then opens Today.
 */
class ExerciseVoice(base: VoiceCommandHost, leave: () -> Unit) {
    private var leavingForToday = false

    val host: VoiceCommandHost = base.withRecommendation(confirm = true) {
        leavingForToday = true
        leave()
    }

    /** The screen's Close effect: back as usual, or on to Today after ShowCurrentRecommendation. */
    fun onClosed(close: () -> Unit, toToday: () -> Unit) {
        if (leavingForToday) {
            leavingForToday = false
            toToday()
        } else {
            close()
        }
    }
}

@Composable
fun rememberExerciseVoice(base: VoiceCommandHost, leave: () -> Unit): ExerciseVoice =
    remember(base) { ExerciseVoice(base, leave) }
