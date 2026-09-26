package com.wivernz.itera.feature.exercise.habitstack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TimePickerSheet
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.progress.Mastery
import com.wivernz.itera.feature.exercise.runner.LeaveExerciseDialog
import com.wivernz.itera.feature.exercise.runner.gated
import java.time.LocalTime

private val ANCHOR_LABELS = mapOf(
    "coffee" to R.string.hs_anchor_1,
    "teeth" to R.string.hs_anchor_2,
    "laptop" to R.string.hs_anchor_3,
    "lunch" to R.string.hs_anchor_4
)
private val HABIT_LABELS = mapOf(
    "page" to R.string.hs_habit_1,
    "line" to R.string.hs_habit_2,
    "water" to R.string.hs_habit_3,
    "plan" to R.string.hs_habit_4
)

class HabitStackActions(
    val edit: (StackSlot) -> Unit,
    val pick: (StackSlot, String, String) -> Unit,
    val custom: (StackSlot, String) -> Unit,
    val nudge: (Boolean) -> Unit,
    val nudgeTime: (LocalTime) -> Unit,
    val save: () -> Unit,
    val leave: () -> Unit
)

@Composable
fun HabitStackRoute(
    vm: HabitStackViewModel,
    showResult: (Long, String) -> Unit,
    onClose: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is HabitStackEffect.ShowResult -> showResult(effect.activityId, effect.techniqueId)
            HabitStackEffect.Close -> onClose()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    HabitStackScreen(
        state,
        HabitStackActions(
            vm::edit,
            vm::pick,
            vm::setCustom,
            vm::setNudge,
            vm::setNudgeTime,
            vm::save,
            vm::leave
        )
    )
}

/** Prototype `HabitStackScreen` (Practice.kt): the sentence, two chip groups and the nudge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitStackScreen(state: HabitStackUiState, actions: HabitStackActions) {
    val c = Itera.colors
    val sc = Skill.HABITS.colors(c.isDark)
    var confirming by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val back = { if (state.hasDraft) confirming = true else actions.leave() }
    BackHandler(enabled = !state.loading) { back() }
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("HabitStack")) {
            TopBar("", actions.leave)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        return
    }
    val reason = if (state.ready) null else stringResource(R.string.hs_gate)
    val days = Mastery.PRACTICED_DAYS
    ScreenColumn(
        gap = 20.dp,
        modifier = Modifier.testTag("HabitStack"),
        bottom = {
            Text(
                pluralStringResource(
                    R.plurals.hs_log_hint_n,
                    days,
                    days,
                    stringResource(R.string.level_practiced)
                ),
                style = Itera.type.caption,
                color = c.ink2,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            IteraButton(
                stringResource(R.string.hs_save),
                actions.save,
                enabled = reason == null && !state.busy,
                modifier = Modifier.testTag("HabitSave").gated(reason)
            )
        }
    ) {
        TopBar(state.name, back, trailing = {
            Pill(stringResource(Skill.HABITS.title), sc.container, sc.content)
        })
        Sentence(state, actions)
        Text(stringResource(R.string.hs_sub), style = Itera.type.body, color = c.ink2)
        SlotGroup(
            StackSlot.ANCHOR,
            stringResource(R.string.hs_existing),
            ANCHOR_LABELS,
            state.anchor,
            state.editing == StackSlot.ANCHOR,
            state.customAnchor,
            stringResource(R.string.hs_custom_anchor),
            actions
        )
        SlotGroup(
            StackSlot.HABIT,
            stringResource(R.string.hs_new),
            HABIT_LABELS,
            state.habit,
            state.editing == StackSlot.HABIT,
            state.customHabit,
            stringResource(R.string.hs_custom_habit),
            actions
        )
        NudgeRow(state, actions) { pickingTime = true }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
    if (pickingTime) {
        TimePickerSheet(
            title = stringResource(R.string.hs_nudge_time_title),
            initialTime = state.nudgeTime,
            onConfirm = {
                pickingTime = false
                actions.nudgeTime(it)
            },
            onDismiss = { pickingTime = false }
        )
    }
    if (confirming) {
        LeaveExerciseDialog(onLeave = {
            confirming = false
            actions.leave()
        }, onStay = { confirming = false })
    }
}

/** "After I ___, I will ___." - it reads correctly with zero, one or two slots filled. Slots are tappable. */
@Composable
private fun Sentence(state: HabitStackUiState, actions: HabitStackActions) {
    val c = Itera.colors
    val sc = Skill.HABITS.colors(c.isDark)
    val blank = stringResource(R.string.hs_blank)
    val a = state.anchor?.text ?: blank
    val h = state.habit?.text ?: blank
    val sentence = stringResource(R.string.hs_sentence, a, h)
    val slot = TextLinkStyles(
        SpanStyle(
            color = sc.content,
            background = sc.container,
            textDecoration = TextDecoration.Underline
        )
    )
    val anchorA11y = stringResource(R.string.hs_slot_a11y, a)
    val habitA11y = stringResource(R.string.hs_slot_a11y, h)
    val text = buildAnnotatedString {
        append(sentence)
        val anchorStart = sentence.indexOf(a)
        val habitStart = sentence.lastIndexOf(h)
        if (anchorStart >= 0) {
            addLink(
                LinkAnnotation.Clickable(anchorA11y, slot) { actions.edit(StackSlot.ANCHOR) },
                anchorStart,
                anchorStart + a.length
            )
        }
        if (habitStart >= 0 && habitStart >= anchorStart + a.length) {
            addLink(
                LinkAnnotation.Clickable(habitA11y, slot) { actions.edit(StackSlot.HABIT) },
                habitStart,
                habitStart + h.length
            )
        }
    }
    Text(
        text,
        style = Itera.type.title.copy(lineHeight = Itera.type.title.fontSize * 1.4f),
        color = c.ink,
        modifier = Modifier.testTag("HabitSentence")
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotGroup(
    slot: StackSlot,
    title: String,
    labels: Map<String, Int>,
    value: SlotValue?,
    editing: Boolean,
    custom: String,
    customHint: String,
    actions: HabitStackActions
) {
    val c = Itera.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("Group_${slot.name}")
    ) {
        Text(title, style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { (id, res) ->
                val text = stringResource(res)
                ChoiceChip(
                    text,
                    value?.chipId == id,
                    onClick = { actions.pick(slot, id, text) },
                    modifier = Modifier.testTag("Chip_$id")
                )
            }
            ChoiceChip(
                stringResource(R.string.template_custom_hint),
                editing || (value != null && value.chipId == null),
                onClick = { actions.edit(slot) },
                modifier = Modifier.testTag("Custom_${slot.name}")
            )
        }
        if (editing || (value != null && value.chipId == null)) {
            NoteField(
                custom.ifEmpty { value?.takeIf { it.chipId == null }?.text.orEmpty() },
                { actions.custom(slot, it) },
                customHint,
                minLines = 1,
                modifier = Modifier.testTag("CustomField_${slot.name}")
            )
        }
    }
}

@Composable
private fun NudgeRow(state: HabitStackUiState, actions: HabitStackActions, onPickTime: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.HABITS.colors(c.isDark)
    val context = LocalContext.current
    val label = state.anchor?.let { stringResource(R.string.hs_nudge_after, it.text) }
        ?: stringResource(R.string.hs_nudge)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(IteraIcons.Bell, null, tint = c.ink2, modifier = Modifier.size(20.dp))
            Text(label, style = Itera.type.body, color = c.ink, modifier = Modifier.weight(1f))
            Switch(
                checked = state.nudgeEnabled,
                onCheckedChange = actions.nudge,
                modifier = Modifier.testTag("NudgeSwitch"),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = sc.content,
                    checkedThumbColor = c.surface,
                    uncheckedTrackColor = c.surface2,
                    uncheckedBorderColor = c.line
                )
            )
        }
        if (state.nudgeEnabled) {
            Text(
                stringResource(
                    R.string.hs_nudge_time,
                    formatTime(state.nudgeTime, currentLocale(), context)
                ),
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink,
                modifier = Modifier.padding(start = 34.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onPickTime)
                    .testTag("NudgeTime")
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            )
        }
    }
}
