package com.wivernz.itera.feature.exercise.premortem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatMonthYear
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.CappedNoteField
import com.wivernz.itera.feature.exercise.runner.LeaveExerciseDialog
import com.wivernz.itera.feature.exercise.runner.gated
import com.wivernz.itera.feature.voice.LocalVoiceToToday
import com.wivernz.itera.feature.voice.ProvideVoiceCommands
import com.wivernz.itera.feature.voice.VoiceCommandAction
import com.wivernz.itera.feature.voice.VoiceNoteField
import com.wivernz.itera.feature.voice.rememberExerciseVoice
import java.time.YearMonth

class PremortemActions(
    val project: (String) -> Unit,
    val newReason: (String) -> Unit,
    val addReason: () -> Unit,
    val cycle: (Int) -> Unit,
    val focus: (Int?) -> Unit,
    val move: (Int, Int) -> Unit,
    val remove: (Int) -> Unit,
    val action: (String) -> Unit,
    val addToToday: () -> Unit,
    val finishOnly: () -> Unit,
    val leave: () -> Unit
)

@Composable
fun PremortemRoute(
    vm: PremortemViewModel,
    showResult: (Long, String) -> Unit,
    onClose: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val voice = rememberExerciseVoice(vm, vm::leave)
    val toToday = LocalVoiceToToday.current
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is PremortemEffect.ShowResult -> showResult(effect.activityId, effect.techniqueId)
            PremortemEffect.Close -> voice.onClosed(onClose, toToday)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    ProvideVoiceCommands(voice.host) {
        PremortemScreen(
            state,
            PremortemActions(
                vm::setProject, vm::setNewReason, vm::addReason, vm::cycleLikelihood, vm::focus,
                vm::move, vm::remove, vm::setAction, vm::addToToday, vm::finishWithoutAdding,
                vm::leave
            )
        )
    }
}

private val Likelihood.label: Int get() = when (this) {
    Likelihood.POSSIBLE -> R.string.pm_possible
    Likelihood.LIKELY -> R.string.pm_likely
    Likelihood.CERTAIN -> R.string.pm_certain
}

/** Prototype `PremortemScreen` (Practice.kt) with the user's own project, reasons and action. */
@Composable
fun PremortemScreen(state: PremortemUiState, actions: PremortemActions) {
    val c = Itera.colors
    val sc = Skill.REFLECTION.colors(c.isDark)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val back = { if (state.hasDraft) confirming = true else actions.leave() }
    BackHandler(enabled = !state.loading) { back() }
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("Premortem")) {
            TopBar("", actions.leave)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        return
    }
    val reason = when {
        state.project.isBlank() -> stringResource(R.string.pm_gate_project)
        state.reasonsMissing > 0 ->
            pluralStringResource(
                R.plurals.pm_gate_reasons,
                state.reasonsMissing,
                state.reasonsMissing
            )
        else -> null
    }
    val locale = currentLocale()
    val month = if (state.loading) {
        ""
    } else {
        formatMonthYear(
            YearMonth.from(state.failureDate),
            locale
        )
    }
    ScreenColumn(
        gap = 18.dp,
        modifier = Modifier.testTag("Premortem"),
        bottom = {
            IteraButton(
                stringResource(R.string.pm_cta),
                actions.addToToday,
                enabled = reason == null && state.action.isNotBlank() && !state.busy,
                modifier = Modifier.testTag("PremortemAdd").gated(reason)
            )
            IteraButton(
                stringResource(R.string.pm_finish_only),
                actions.finishOnly,
                kind = ButtonKind.Ghost,
                height = 44.dp,
                enabled = reason == null && !state.busy,
                modifier = Modifier.testTag("PremortemFinish")
            )
        }
    ) {
        TopBar(state.name, back, trailing = {
            Pill(stringResource(Skill.REFLECTION.title), sc.container, sc.content)
        })
        VoiceCommandAction(Modifier.align(Alignment.End))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.pm_project), style = Itera.type.bodySmall, color = c.ink2)
            VoiceNoteField(
                state.project,
                actions.project,
                stringResource(R.string.pm_project_hint),
                maxChars = PremortemViewModel.PROJECT_MAX,
                minLines = 1,
                modifier = Modifier.testTag("PremortemProject")
            )
        }
        Text(
            stringResource(R.string.pm_title, month),
            style = Itera.type.title,
            color = c.ink,
            modifier = Modifier.testTag("PremortemTitle")
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.reasons.forEachIndexed { i, r ->
                ReasonRow(i, r, i == state.focused, state.reasons.size, actions)
            }
            if (state.canAdd) {
                CappedNoteField(
                    state.newReason,
                    actions.newReason,
                    stringResource(R.string.pm_add),
                    minLines = 1,
                    modifier = Modifier.testTag("PremortemNewReason"),
                    onDone = actions.addReason
                )
            }
        }
        Text(
            stringResource(R.string.pm_today_q),
            style = Itera.type.headline,
            color = c.ink,
            modifier = Modifier.padding(top = 6.dp)
        )
        CappedNoteField(
            state.action,
            actions.action,
            stringResource(R.string.pm_action_hint),
            bordered = true,
            modifier = Modifier.testTag("PremortemAction")
        )
        state.idea?.let {
            Text(
                stringResource(R.string.pm_idea_n, it),
                style = Itera.type.caption,
                color = c.ink2,
                modifier = Modifier.testTag("PremortemIdea")
            )
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
        LeaveExerciseDialog(onLeave = {
            confirming = false
            actions.leave()
        }, onStay = { confirming = false })
    }
}

@Composable
private fun ReasonRow(
    index: Int,
    reason: PremortemReason,
    focused: Boolean,
    count: Int,
    actions: PremortemActions
) {
    val c = Itera.colors
    val sc = Skill.REFLECTION.colors(c.isDark)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(2.dp, if (focused) c.ink else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { actions.focus(index) }
                .testTag("Reason_$index")
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${index + 1}",
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = c.ink2
            )
            Text(
                reason.text,
                style = Itera.type.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = Itera.type.userText.fontFamily
                ),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            val label = stringResource(reason.likelihood.label)
            val a11y = stringResource(R.string.pm_likelihood_a11y, label)
            val (bg, fg) = when (reason.likelihood) {
                Likelihood.POSSIBLE -> c.surface2 to c.ink2
                else -> sc.container to sc.content
            }
            Pill(
                label,
                bg,
                fg,
                Modifier.clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button) { actions.cycle(index) }
                    .semantics { contentDescription = a11y }
                    .testTag("Likelihood_$index")
            )
        }
        if (focused) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index > 0) {
                    SmallAction(stringResource(R.string.pm_move_up), "MoveUp_$index") {
                        actions.move(index, -1)
                    }
                }
                if (index < count - 1) {
                    SmallAction(stringResource(R.string.pm_move_down), "MoveDown_$index") {
                        actions.move(index, 1)
                    }
                }
                SmallAction(stringResource(R.string.pm_remove), "Remove_$index") {
                    actions.remove(index)
                }
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, tag: String, onClick: () -> Unit) {
    val c = Itera.colors
    Text(
        text,
        style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = c.ink,
        modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}
