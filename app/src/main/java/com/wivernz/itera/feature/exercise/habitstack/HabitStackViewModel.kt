package com.wivernz.itera.feature.exercise.habitstack

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.feature.exercise.runner.ExerciseSession
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandHost
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceRejection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StackSlot { ANCHOR, HABIT }

/** A slot's value: a suggestion chip (by id, text resolved in the current language) or the user's own words. */
data class SlotValue(val chipId: String?, val text: String)

data class HabitStackUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    val anchor: SlotValue? = null,
    val habit: SlotValue? = null,
    // the slot whose custom field is open
    val editing: StackSlot? = null,
    val customAnchor: String = "",
    val customHabit: String = "",
    val nudgeEnabled: Boolean = false,
    val nudgeTime: LocalTime = LocalTime.of(8, 0),
    val nudgeTimeEdited: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val ready: Boolean get() = anchor != null && habit != null
    val hasDraft: Boolean get() = anchor != null || habit != null
}

object HabitStackRules {
    /** Anchor suggestions by id; the derived nudge time for each, null meaning the morning time. */
    val ANCHOR_TIMES: Map<String, LocalTime?> = linkedMapOf(
        "coffee" to LocalTime.of(7, 45),
        "teeth" to null,
        "laptop" to null,
        "lunch" to LocalTime.of(12, 30)
    )
    val HABITS: List<String> = listOf("page", "line", "water", "plan")

    /** Known anchors have a typical time; the laptop, teeth and any custom anchor use the morning time. */
    fun nudgeTimeFor(anchorChipId: String?, morning: LocalTime): LocalTime =
        anchorChipId?.let { ANCHOR_TIMES[it] } ?: morning
}

sealed interface HabitStackEffect {
    data class ShowResult(val activityId: Long, val techniqueId: String) : HabitStackEffect
    data object Close : HabitStackEffect
}

/**
 * "After I {anchor}, I will {habit}." Saving replaces the active stack (the old one is archived) and asks the
 * reminder scheduler to (re)schedule or cancel the nudge (completion effect 4).
 */
@HiltViewModel
class HabitStackViewModel @Inject constructor(
    saved: SavedStateHandle,
    deps: ExerciseSessionDeps,
    private val preferences: PreferencesRepository
) : ViewModel(),
    VoiceCommandHost {
    private val session =
        ExerciseSession(deps, checkNotNull(saved.get<Long>(ARG_ACTIVITY)), viewModelScope)
    private val mutable = MutableStateFlow(HabitStackUiState())
    val state: StateFlow<HabitStackUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<HabitStackEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var morning: LocalTime = LocalTime.of(8, 0)
    private var saving = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val loaded = session.load()
        if (loaded == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        morning = runCatching { preferences.preferences.first().morningTime }.getOrDefault(morning)
        val draft = loaded.draft as? ActivityResult.HabitStack
        mutable.update {
            it.copy(
                loading = false,
                techniqueId = loaded.technique.id.value,
                name = loaded.technique.name,
                // a draft keeps the words, not the chip: it restores as the user's own text
                anchor = draft?.anchor?.takeIf(String::isNotBlank)?.let { a -> SlotValue(null, a) },
                habit = draft?.habit?.takeIf(String::isNotBlank)?.let { h -> SlotValue(null, h) },
                nudgeEnabled = draft?.nudgeEnabled ?: false,
                nudgeTime = draft?.nudgeTime ?: morning,
                nudgeTimeEdited = draft?.nudgeTime != null
            )
        }
    }

    fun edit(slot: StackSlot) = mutable.update {
        it.copy(
            editing = if (it.editing ==
                slot
            ) {
                null
            } else {
                slot
            }
        )
    }

    /** A suggestion chip; [text] is its label in the current language. */
    fun pick(slot: StackSlot, chipId: String, text: String) = change { s ->
        val value = SlotValue(chipId, text)
        when (slot) {
            StackSlot.ANCHOR -> s.copy(anchor = value, editing = null).withDerivedTime()
            StackSlot.HABIT -> s.copy(habit = value, editing = null)
        }
    }

    fun setCustom(slot: StackSlot, text: String) = change { s ->
        val clean = text.take(CUSTOM_MAX)
        val value = clean.trim().takeIf(String::isNotEmpty)?.let { SlotValue(null, it) }
        when (slot) {
            StackSlot.ANCHOR -> s.copy(customAnchor = clean, anchor = value).withDerivedTime()
            StackSlot.HABIT -> s.copy(customHabit = clean, habit = value)
        }
    }

    fun setNudge(enabled: Boolean) = change { it.copy(nudgeEnabled = enabled) }

    fun setNudgeTime(time: LocalTime) = change { it.copy(nudgeTime = time, nudgeTimeEdited = true) }

    /** "Save my stack". */
    fun save() {
        val s = mutable.value
        if (saving || !s.ready) return
        saving = true
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            session.complete(result(s)).onSuccess {
                effectChannel.send(HabitStackEffect.ShowResult(session.activityId, s.techniqueId))
            }.onFailure {
                saving = false
                mutable.update { it.copy(busy = false, failed = true) }
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            session.leave(result(mutable.value))
            effectChannel.send(HabitStackEffect.Close)
        }
    }

    fun flushDraft() = session.flush()

    private fun HabitStackUiState.withDerivedTime() = if (nudgeTimeEdited) {
        this
    } else {
        copy(
            nudgeTime = HabitStackRules.nudgeTimeFor(anchor?.chipId, morning)
        )
    }

    private fun change(transform: (HabitStackUiState) -> HabitStackUiState) {
        if (mutable.value.loading) return
        mutable.update(transform)
        session.draft(result(mutable.value))
    }

    private fun result(s: HabitStackUiState) = ActivityResult.HabitStack(
        anchor = s.anchor?.text.orEmpty(),
        habit = s.habit?.text.orEmpty(),
        nudgeEnabled = s.nudgeEnabled,
        nudgeTime = s.nudgeTime.takeIf { s.nudgeEnabled || s.nudgeTimeEdited }
    )

    // ------------------------------------------------------------------ voice commands (milestone 012)

    override val voiceCommands: Set<VoiceCommandKind> =
        setOf(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE)

    override fun planVoice(command: VoiceCommand): VoicePlan = when (command) {
        VoiceCommand.CompleteCurrentExercise ->
            if (mutable.value.ready && !mutable.value.busy) {
                VoicePlan.Confirm(VoiceAction.CompleteExercise)
            } else {
                VoicePlan.Reject(VoiceRejection.ExerciseNotReady)
            }
        else -> VoicePlan.Reject(VoiceRejection.NotHere)
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome = when {
        action != VoiceAction.CompleteExercise -> VoiceOutcome.Rejected(VoiceRejection.NotHere)
        !mutable.value.ready || mutable.value.busy -> VoiceOutcome.Rejected(VoiceRejection.Stale)
        else -> {
            save()
            VoiceOutcome.Done(VoiceFeedback.Handover)
        }
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        const val CUSTOM_MAX = 120
    }
}
