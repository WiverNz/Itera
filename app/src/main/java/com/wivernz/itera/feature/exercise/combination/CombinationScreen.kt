package com.wivernz.itera.feature.exercise.combination

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerActions
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerBody
import com.wivernz.itera.feature.exercise.eisenhower.eisenhowerGateReason
import com.wivernz.itera.feature.exercise.runner.CappedNoteField
import com.wivernz.itera.feature.exercise.runner.ConfirmDialog
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.exercise.runner.gated
import com.wivernz.itera.feature.exercise.template.PickOneBody
import com.wivernz.itera.feature.voice.LocalVoiceToToday
import com.wivernz.itera.feature.voice.ProvideVoiceCommands
import com.wivernz.itera.feature.voice.VoiceCommandAction
import com.wivernz.itera.feature.voice.rememberExerciseVoice

class CombinationActions(
    val entry: (String) -> Unit,
    val eisenhower: EisenhowerActions,
    val option: (Int, String) -> Unit,
    val chooseOption: (Int) -> Unit,
    val primary: () -> Unit,
    val leave: () -> Unit
)

@Composable
fun CombinationRoute(
    vm: CombinationViewModel,
    openFocus: (Long, Int, String) -> Unit,
    openBody: (Long, String, ExerciseBody) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val voice = rememberExerciseVoice(vm, vm::leave)
    val toToday = LocalVoiceToToday.current
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is CombinationEffect.OpenFocus -> openFocus(
                effect.activityId,
                effect.minutes,
                effect.techniqueId
            )
            is CombinationEffect.OpenBody -> openBody(
                effect.activityId,
                effect.techniqueId,
                effect.body
            )
            CombinationEffect.Close -> voice.onClosed(onClose, toToday)
            CombinationEffect.Done -> onDone()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    ProvideVoiceCommands(voice.host) {
        CombinationScreen(
            state,
            CombinationActions(
                vm::setEntry,
                EisenhowerActions(vm::select, { q: Quadrant -> vm.place(q) }, vm::choose),
                vm::setOption,
                vm::chooseOption,
                vm::onPrimary,
                vm::leave
            )
        )
    }
}

/** Prototype `CombinationScreen` and `ChainStep` (Practice.kt): a rail of steps, each body inline. */
@Composable
fun CombinationScreen(state: CombinationUiState, actions: CombinationActions) {
    val c = Itera.colors
    var confirming by rememberSaveable { mutableStateOf(false) }
    // Closing mid-chain confirms only once a step is done; finished steps stay done either way.
    val close = { if (state.anyDone && !state.chainDone) confirming = true else actions.leave() }
    BackHandler(enabled = !state.loading) { close() }
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("Combination")) {
            TopBar("", actions.leave)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        return
    }
    val current = state.current
    val next = state.next
    val reason = when {
        state.loading || state.canContinue -> null
        current?.kind == StepKind.Eisenhower && !state.entering -> eisenhowerGateReason(
            state.board.gate
        )
        current?.kind == StepKind.Eisenhower -> null
        else -> stringResource(R.string.gate_pick)
    }
    val label = when {
        current == null -> stringResource(R.string.action_done)
        state.entering -> stringResource(R.string.eis_entry_cta)
        current.kind is StepKind.Focus || current.kind is StepKind.Screen ->
            stringResource(R.string.combo_start_step, current.name)
        next != null && next.kind != StepKind.Reflection ->
            stringResource(R.string.combo_continue_to, next.name)
        else -> stringResource(R.string.action_finish)
    }
    ScreenColumn(
        gap = 20.dp,
        modifier = Modifier.testTag("Combination"),
        bottom = {
            IteraButton(
                label,
                actions.primary,
                icon = if (current?.kind is StepKind.Focus) IteraIcons.Play else null,
                enabled = !state.loading && !state.busy && state.canContinue,
                modifier = Modifier.testTag("ChainPrimary").gated(reason)
            )
        }
    ) {
        TopBar(stringResource(R.string.combo_label, state.programDay), close, trailing = {
            Pill(stringResource(R.string.minutes_short, state.totalMinutes), c.surface2, c.ink2)
        })
        VoiceCommandAction(Modifier.align(Alignment.End))
        Text(
            stringResource(
                if (state.curriculum) R.string.combo_title else R.string.activity_combination_title
            ),
            style = Itera.type.display,
            color = c.ink
        )
        Text(
            stringResource(
                if (state.curriculum) R.string.combo_sub else R.string.combo_generated_sub
            ),
            style = Itera.type.body,
            color = c.ink2
        )
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            state.steps.forEach { step ->
                ChainStep(step, state, actions)
            }
        }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.combo_leave_title),
            body = stringResource(R.string.combo_leave_body),
            confirm = stringResource(R.string.exercise_leave),
            dismiss = stringResource(R.string.exercise_keep_going),
            onConfirm = {
                confirming = false
                actions.leave()
            },
            onDismiss = { confirming = false }
        )
    }
}

@Composable
private fun ChainStep(step: ChainStepUi, state: CombinationUiState, actions: CombinationActions) {
    val c = Itera.colors
    val sc = step.skill.colors(c.isDark)
    val icon = techniqueIcon(step.techniqueId)
    val stateLabel = stringResource(
        when (step.state) {
            ChainState.DONE -> StepState.Done.label
            ChainState.NOW -> StepState.Now.label
            ChainState.NEXT -> StepState.Next.label
        }
    )
    Row(
        Modifier.testTag("Step_${step.techniqueId}_${step.state.name}")
            .semantics { stateDescription = stateLabel },
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (step.state) {
            ChainState.DONE -> Box(
                Modifier.size(36.dp).clip(CircleShape).background(sc.content),
                contentAlignment = Alignment.Center
            ) { Icon(IteraIcons.Check, null, tint = c.surface, modifier = Modifier.size(18.dp)) }
            ChainState.NOW -> TechniqueToken(step.skill, icon, 36.dp, 18.dp)
            ChainState.NEXT -> Box(
                Modifier.size(36.dp).border(2.dp, c.line, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = c.ink2, modifier = Modifier.size(18.dp)) }
        }
        Column(
            Modifier.weight(1f).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Eyebrow(step.name, if (step.state == ChainState.NEXT) c.ink2 else sc.content)
            val (title, sub) = when (step.state) {
                ChainState.DONE -> summaryTitle(step) to summarySub(step)
                else -> step.prompt to step.hint
            }
            Text(
                title,
                style = if (step.state == ChainState.NOW) {
                    Itera.type.label
                } else {
                    Itera.type.body.copy(fontWeight = FontWeight.SemiBold)
                },
                color = c.ink
            )
            if (sub.isNotBlank()) Text(sub, style = Itera.type.bodySmall, color = c.ink2)
            if (step.state == ChainState.NOW) {
                state.subject?.let {
                    Text(
                        stringResource(R.string.combo_subject, it),
                        style = Itera.type.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Itera.type.userText.fontFamily
                        ),
                        color = c.ink,
                        modifier = Modifier.padding(top = 4.dp).testTag("ChainSubject")
                    )
                }
                InlineBody(step, state, actions)
            }
        }
    }
}

@Composable
private fun InlineBody(step: ChainStepUi, state: CombinationUiState, actions: CombinationActions) {
    when (val kind = step.kind) {
        StepKind.Eisenhower -> Box(Modifier.padding(top = 10.dp)) {
            if (state.entering) {
                CappedNoteField(
                    state.entryText,
                    actions.entry,
                    stringResource(R.string.eis_entry_hint),
                    minLines = 5,
                    bordered = true,
                    modifier = Modifier.testTag("ChainEntry")
                )
            } else {
                EisenhowerBody(state.board, actions.eisenhower)
            }
        }
        is StepKind.PickOne -> Box(Modifier.padding(top = 10.dp)) {
            PickOneBody(kind.block, state.choice, actions.option, actions.chooseOption)
        }
        else -> Unit
    }
}

@Composable
private fun summaryTitle(step: ChainStepUi): String = when (val summary = step.summary) {
    is StepSummary.Sorted -> summary.chosen?.let { stringResource(R.string.combo_done_eis, it) }
        ?: stringResource(R.string.combo_done_eis_none)
    is StepSummary.Picked -> stringResource(R.string.combo_done_pick, summary.choice)
    is StepSummary.Focused ->
        pluralStringResource(R.plurals.combo_done_focus, summary.minutes, summary.minutes)
    else -> step.prompt
}

@Composable
private fun summarySub(step: ChainStepUi): String = when (step.summary) {
    is StepSummary.Sorted -> stringResource(R.string.combo_step1_sub)
    else -> step.hint
}
