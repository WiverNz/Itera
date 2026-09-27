package com.wivernz.itera.feature.exercise.eisenhower

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.CappedNoteField
import com.wivernz.itera.feature.exercise.runner.LeaveExerciseDialog
import com.wivernz.itera.feature.exercise.runner.gated
import com.wivernz.itera.feature.exercise.template.RadioGroup
import com.wivernz.itera.feature.voice.LocalVoiceToToday
import com.wivernz.itera.feature.voice.ProvideVoiceCommands
import com.wivernz.itera.feature.voice.VoiceCommandAction
import com.wivernz.itera.feature.voice.rememberExerciseVoice

@Composable
fun EisenhowerRoute(
    vm: EisenhowerViewModel,
    showResult: (Long, String) -> Unit,
    onClose: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val voice = rememberExerciseVoice(vm, vm::leave)
    val toToday = LocalVoiceToToday.current
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is EisenhowerEffect.ShowResult -> showResult(effect.activityId, effect.techniqueId)
            EisenhowerEffect.Close -> voice.onClosed(onClose, toToday)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    ProvideVoiceCommands(voice.host) {
        EisenhowerScreen(
            state,
            onEntry = vm::setEntry,
            onSort = vm::startSorting,
            actions = EisenhowerActions(vm::select, vm::place, vm::choose),
            onFinish = vm::finish,
            onLeave = vm::leave
        )
    }
}

class EisenhowerActions(
    val select: (String) -> Unit,
    val place: (Quadrant) -> Unit,
    val choose: (String) -> Unit
)

/** Prototype `EisenhowerScreen` (Practice.kt), preceded by the entry step for the user's own tasks. */
@Composable
fun EisenhowerScreen(
    state: EisenhowerUiState,
    onEntry: (String) -> Unit,
    onSort: () -> Unit,
    actions: EisenhowerActions,
    onFinish: () -> Unit,
    onLeave: () -> Unit
) {
    val c = Itera.colors
    val sc = Skill.PLANNING.colors(c.isDark)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val back = { if (state.hasDraft) confirming = true else onLeave() }
    BackHandler(enabled = !state.loading) { back() }
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("Eisenhower")) {
            TopBar("", onLeave)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        return
    }
    val reason = if (state.entering) {
        state.entryMissing.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.eis_gate_tasks, it, it)
        }
    } else {
        eisenhowerGateReason(state.board.gate)
    }
    ScreenColumn(
        gap = 16.dp,
        modifier = Modifier.testTag("Eisenhower"),
        bottom = {
            IteraButton(
                stringResource(
                    when {
                        state.entering -> R.string.eis_entry_cta
                        state.combination -> R.string.eis_cta
                        else -> R.string.two_finish
                    }
                ),
                if (state.entering) onSort else onFinish,
                enabled = reason == null && !state.busy && !state.loading,
                modifier = Modifier.testTag("EisenhowerPrimary").gated(reason)
            )
        }
    ) {
        TopBar(state.name, back, trailing = {
            Pill(stringResource(Skill.PLANNING.title), sc.container, sc.content)
        })
        VoiceCommandAction(Modifier.align(Alignment.End))
        if (state.entering) {
            Text(stringResource(R.string.eis_entry_title), style = Itera.type.title, color = c.ink)
            Text(stringResource(R.string.eis_entry_sub), style = Itera.type.body, color = c.ink2)
            CappedNoteField(
                state.entryText,
                onEntry,
                stringResource(R.string.eis_entry_hint),
                minLines = 7,
                bordered = true,
                modifier = Modifier.testTag("EisenhowerEntry")
            )
        } else {
            Text(stringResource(R.string.eis_title), style = Itera.type.title, color = c.ink)
            EisenhowerBody(state.board, actions)
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
            onLeave()
        }, onStay = { confirming = false })
    }
}

@Composable
fun eisenhowerGateReason(gate: EisenhowerGate): String? = when (gate) {
    EisenhowerGate.READY -> null
    EisenhowerGate.SORT_ALL -> stringResource(R.string.eis_gate_sort)
    EisenhowerGate.CHOOSE ->
        stringResource(R.string.eis_gate_choose, stringResource(R.string.eis_q1))
}

/** The inbox, the 2x2 and, once everything is sorted, the Do-now choice. Also the combination's step 1 body. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EisenhowerBody(board: EisenhowerBoard, actions: EisenhowerActions) {
    val c = Itera.colors
    val sc = Skill.PLANNING.colors(c.isDark)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.eis_to_sort),
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(
                    if (board.selectedId != null) R.string.eis_tap_square else R.string.eis_tap_task
                ),
                style = Itera.type.bodySmall,
                color = c.ink2,
                modifier = Modifier.testTag("EisenhowerHint")
            )
        }
        FlowRow(
            Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (board.inbox.isEmpty()) {
                Text(
                    stringResource(R.string.eis_all_sorted),
                    style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = Skill.HABITS.colors(c.isDark).content,
                    modifier = Modifier.testTag("EisenhowerAllSorted")
                )
            }
            board.inbox.forEach { t ->
                val on = t.id == board.selectedId
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) c.ink else c.surface)
                        .border(2.dp, if (on) c.ink else c.line, RoundedCornerShape(14.dp))
                        .selectable(on, role = Role.RadioButton) { actions.select(t.id) }
                        .testTag("Inbox_${t.id}")
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.label,
                        style = Itera.type.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Itera.type.userText.fontFamily
                        ),
                        color = if (on) c.onInk else c.ink
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.padding(start = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AxisLabel(stringResource(R.string.eis_urgent), Modifier.weight(1f))
                AxisLabel(stringResource(R.string.eis_not_urgent), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideLabel(stringResource(R.string.eis_important))
                Quadrant(
                    Quadrant.DO_NOW,
                    board,
                    c.accentSoft,
                    c.accent,
                    actions,
                    Modifier.weight(1f)
                )
                Quadrant(
                    Quadrant.SCHEDULE,
                    board,
                    sc.container,
                    sc.content,
                    actions,
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideLabel(stringResource(R.string.eis_not_important))
                Quadrant(Quadrant.DELEGATE, board, c.surface2, c.ink, actions, Modifier.weight(1f))
                Quadrant(
                    Quadrant.DROP,
                    board,
                    Color.Transparent,
                    c.ink2,
                    actions,
                    Modifier.weight(1f),
                    dashed = true
                )
            }
        }
        if (board.allSorted && board.doNow.isNotEmpty()) {
            Text(
                stringResource(R.string.eis_choose_hint, stringResource(R.string.eis_q1)),
                style = Itera.type.label,
                color = c.ink
            )
            val doNow = board.doNow
            RadioGroup(doNow.map { it.label }, doNow.indexOfFirst { it.id == board.chosenId }) {
                actions.choose(doNow[it].id)
            }
        }
    }
}

private val Quadrant.title: Int get() = when (this) {
    Quadrant.DO_NOW -> R.string.eis_q1
    Quadrant.SCHEDULE -> R.string.eis_q2
    Quadrant.DELEGATE -> R.string.eis_q3
    else -> R.string.eis_q4
}

private val Quadrant.sub: Int get() = when (this) {
    Quadrant.DO_NOW -> R.string.eis_q1_sub
    Quadrant.SCHEDULE -> R.string.eis_q2_sub
    Quadrant.DELEGATE -> R.string.eis_q3_sub
    else -> R.string.eis_q4_sub
}

@Composable
private fun AxisLabel(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
        color = Itera.colors.ink2,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SideLabel(text: String) {
    Box(Modifier.width(22.dp).height(168.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            maxLines = 1,
            softWrap = false,
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = Itera.colors.ink2,
            modifier = Modifier.rotate(-90f).requiredWidth(168.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Quadrant(
    quadrant: Quadrant,
    board: EisenhowerBoard,
    bg: Color,
    fg: Color,
    actions: EisenhowerActions,
    modifier: Modifier,
    dashed: Boolean = false
) {
    val c = Itera.colors
    val line = c.ink3
    val title = stringResource(quadrant.title)
    val sub = stringResource(quadrant.sub)
    val items: List<EisenhowerItem> = board.placedIn(quadrant)
    val selected = board.selected
    val description =
        pluralStringResource(R.plurals.eis_quadrant_a11y, items.size, title, sub, items.size)
    val placeLabel = selected?.let { stringResource(R.string.eis_place, it.label) }
    val pickFirst = stringResource(R.string.eis_pick_first)
    Column(
        modifier
            .heightIn(min = 168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            line,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(
                                2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = selected != null) { actions.place(quadrant) }
            .semantics {
                role = Role.Button
                contentDescription = description
                if (selected == null) {
                    disabled()
                    stateDescription = pickFirst
                } else {
                    onClick(placeLabel) {
                        actions.place(quadrant)
                        true
                    }
                }
            }
            .testTag("Quadrant_${quadrant.name}")
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = Itera.type.headline.copy(fontSize = Itera.type.label.fontSize),
            color = fg
        )
        Text(sub, style = Itera.type.caption, color = c.ink2)
        items.forEach {
            val on = it.id == board.selectedId
            Text(
                it.label,
                style = Itera.type.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = Itera.type.userText.fontFamily
                ),
                color = if (on) c.onInk else c.ink,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.ink else c.surface)
                    .selectable(on, role = Role.RadioButton) { actions.select(it.id) }
                    .testTag("Placed_${it.id}")
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}
