package com.wivernz.itera.feature.today

import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandHost
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceRejection

/**
 * Today's voice commands (milestone 012). StartFocus uses Today's existing focus entry only, and only while it
 * is actionable; it opens that entry's setup with the parsed length, where the normal Start stays with the user
 * and a length the setup does not offer is explained. The focus screen never replaces a running session.
 * ShowCurrentRecommendation points at the hero already on screen; it creates nothing.
 */
class TodayVoiceHost(
    private val state: () -> TodayUiState,
    private val openFocus: (TodayTarget.Focus, Int) -> Unit
) : VoiceCommandHost {
    override val voiceCommands: Set<VoiceCommandKind> =
        setOf(VoiceCommandKind.START_FOCUS, VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION)

    override fun planVoice(command: VoiceCommand): VoicePlan = when (command) {
        is VoiceCommand.StartFocus ->
            if (focusEntry() == null) {
                VoicePlan.Reject(VoiceRejection.NoFocusEntry)
            } else {
                VoicePlan.Run(VoiceAction.StartFocus(command.minutes))
            }
        VoiceCommand.ShowCurrentRecommendation -> VoicePlan.Run(VoiceAction.ShowRecommendation)
        else -> VoicePlan.Reject(VoiceRejection.NotHere)
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome = when (action) {
        is VoiceAction.StartFocus -> {
            val entry = focusEntry()
            if (entry == null) {
                VoiceOutcome.Rejected(VoiceRejection.NoFocusEntry)
            } else {
                openFocus(entry, action.minutes ?: 0)
                VoiceOutcome.Done(VoiceFeedback.Handover)
            }
        }
        VoiceAction.ShowRecommendation -> VoiceOutcome.Done(VoiceFeedback.Recommendation)
        else -> VoiceOutcome.Rejected(VoiceRejection.NotHere)
    }

    /** The hero's focus suggestion when it is enabled, else an actionable focus step. */
    private fun focusEntry(): TodayTarget.Focus? {
        val s = state()
        if (s.loading || s.error) return null
        (s.hero as? TodayHero.Focus)?.takeIf { it.enabled }?.let { hero ->
            (hero.target as? TodayTarget.Focus)?.let { return it }
        }
        return s.steps.firstNotNullOfOrNull { it.target as? TodayTarget.Focus }
    }
}
