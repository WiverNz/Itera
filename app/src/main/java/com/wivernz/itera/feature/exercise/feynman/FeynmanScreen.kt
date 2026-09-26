package com.wivernz.itera.feature.exercise.feynman

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.CappedNoteField
import com.wivernz.itera.feature.exercise.runner.LeaveExerciseDialog
import com.wivernz.itera.feature.exercise.runner.gated

/** "What part was hardest to explain?" - stored by id, rendered in the current language. */
internal val FEYNMAN_CHIPS: List<Pair<String, Int>> = listOf(
    "terms" to R.string.fey_chip_terms,
    "why" to R.string.fey_chip_why,
    "example" to R.string.fey_chip_example,
    "limits" to R.string.fey_chip_limits
)

class FeynmanActions(
    val openPicker: () -> Unit,
    val pickTopic: (Long) -> Unit,
    val newTopic: (String) -> Unit,
    val addTopic: () -> Unit,
    val explanation: (String) -> Unit,
    val toReflect: () -> Unit,
    val backToExplain: () -> Unit,
    val toggleHardest: (String) -> Unit,
    val note: (String) -> Unit,
    val finish: () -> Unit,
    val leave: () -> Unit
)

@Composable
fun FeynmanRoute(vm: FeynmanViewModel, showResult: (Long, String) -> Unit, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is FeynmanEffect.ShowResult -> showResult(effect.activityId, effect.techniqueId)
            FeynmanEffect.Close -> onClose()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    FeynmanScreen(
        state,
        FeynmanActions(
            vm::openPicker, vm::pickTopic, vm::setNewTopic, vm::addTopic, vm::setExplanation,
            vm::toReflect, vm::backToExplain, vm::toggleHardest, vm::setNote, vm::finish, vm::leave
        )
    )
}

/** Prototype `FeynmanScreen` and `FeynmanFeedbackScreen` (Practice.kt), as two steps of one exercise. */
@Composable
fun FeynmanScreen(state: FeynmanUiState, actions: FeynmanActions) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state.hasDraft) confirming = true else actions.leave() }
    BackHandler(enabled = !state.loading) {
        if (state.step == 1) actions.backToExplain() else leave()
    }
    when {
        state.missing -> ScreenColumn(modifier = Modifier.testTag("Feynman")) {
            TopBar("", actions.leave)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        state.step == 0 -> Explain(state, actions, leave)
        else -> Reflect(state, actions)
    }
    if (confirming) {
        LeaveExerciseDialog(onLeave = {
            confirming = false
            actions.leave()
        }, onStay = { confirming = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Explain(state: FeynmanUiState, actions: FeynmanActions, onClose: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.LEARNING.colors(c.isDark)
    val reason = when {
        state.topic == null -> stringResource(R.string.fey_gate_topic)
        state.wordsMissing > 0 ->
            pluralStringResource(R.plurals.fey_gate_words, state.wordsMissing, state.wordsMissing)
        else -> null
    }
    // Announced politely every 25 words, never per keystroke.
    val announced = state.words / WORD_STEP * WORD_STEP
    val announcement = pluralStringResource(R.plurals.words, announced, announced)
    ScreenColumn(
        gap = 16.dp,
        modifier = Modifier.testTag("Feynman"),
        bottom = {
            IteraButton(
                stringResource(R.string.fey_done),
                actions.toReflect,
                enabled = reason == null && !state.loading,
                modifier = Modifier.testTag("FeynmanDone").gated(reason)
            )
        }
    ) {
        TopBar(state.name, onClose, trailing = {
            Pill(stringResource(Skill.LEARNING.title), sc.container, sc.content)
        })
        IteraCard(color = sc.container, gap = 10.dp) {
            Eyebrow(stringResource(R.string.fey_topic), sc.content)
            state.topic?.let {
                Text(
                    it.title,
                    style = Itera.type.headline.copy(fontFamily = Itera.type.userText.fontFamily),
                    color = c.ink,
                    modifier = Modifier.testTag("FeynmanTopic")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.fey_from_list),
                        style = Itera.type.bodySmall,
                        color = c.ink2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.fey_change),
                        style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.surface)
                            .clickable(role = Role.Button, onClick = actions.openPicker)
                            .testTag("FeynmanChangeTopic")
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (state.creatingTopic) {
                if (state.topics.isNotEmpty()) {
                    Text(
                        stringResource(R.string.fey_topic_pick),
                        style = Itera.type.label,
                        color = c.ink
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.topics.forEach { topic ->
                            ChoiceChip(topic.title, topic.id == state.topicId, onClick = {
                                actions.pickTopic(topic.id)
                            })
                        }
                    }
                    Text(
                        stringResource(R.string.fey_topic_add),
                        style = Itera.type.label,
                        color = c.ink
                    )
                } else {
                    Text(
                        stringResource(R.string.fey_topic_new),
                        style = Itera.type.headline,
                        color = c.ink
                    )
                }
                NoteField(
                    state.newTopic,
                    actions.newTopic,
                    stringResource(R.string.fey_topic_new_hint),
                    minLines = 1,
                    modifier = Modifier.testTag("FeynmanNewTopic"),
                    onDone = actions.addTopic
                )
                IteraButton(
                    stringResource(R.string.fey_topic_use),
                    actions.addTopic,
                    kind = ButtonKind.Secondary,
                    height = 44.dp,
                    enabled = state.newTopic.isNotBlank(),
                    modifier = Modifier.testTag("FeynmanAddTopic")
                )
            }
        }
        Text(stringResource(R.string.fey_instruction), style = Itera.type.body, color = c.ink2)
        Text(
            stringResource(R.string.fey_your_expl),
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink2
        )
        CappedNoteField(
            state.explanation,
            actions.explanation,
            stringResource(R.string.fey_hint),
            minLines = 9,
            textStyle = Itera.type.bodyLarge,
            bordered = true,
            modifier = Modifier.testTag("FeynmanExplanation")
        )
        Row {
            Text(
                pluralStringResource(R.plurals.words, state.words, state.words),
                style = Itera.type.caption,
                color = c.ink2,
                modifier = Modifier.weight(1f).testTag("FeynmanWords")
            )
            Text(stringResource(R.string.fey_no_notes), style = Itera.type.caption, color = c.ink2)
        }
        Box(
            Modifier.size(1.dp).testTag("FeynmanWordAnnouncement").semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = if (announced > 0) announcement else ""
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Reflect(state: FeynmanUiState, actions: FeynmanActions) {
    val c = Itera.colors
    val sc = Skill.LEARNING.colors(c.isDark)
    ScreenColumn(
        gap = 18.dp,
        modifier = Modifier.testTag("FeynmanReflect"),
        bottom = {
            IteraButton(
                stringResource(R.string.action_finish),
                actions.finish,
                enabled = !state.busy,
                modifier = Modifier.testTag("FeynmanFinish")
            )
        }
    ) {
        TopBar(state.name, actions.backToExplain, IteraIcons.Back)
        Text(stringResource(R.string.fey_back_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.fey_hardest), style = Itera.type.label, color = c.ink)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FEYNMAN_CHIPS.forEach { (id, res) ->
                ChoiceChip(stringResource(res), id in state.hardest, onClick = {
                    actions.toggleHardest(id)
                })
            }
        }
        CappedNoteField(
            state.note,
            actions.note,
            stringResource(R.string.fey_note_hint),
            modifier = Modifier.testTag("FeynmanNote")
        )
        CoachBox(sc.content, sc.container)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(IteraIcons.Spaced, null, tint = sc.content, modifier = Modifier.size(18.dp))
            Text(
                pluralStringResource(
                    R.plurals.fey_review_in,
                    state.reviewInDays,
                    state.reviewInDays
                ),
                style = Itera.type.bodySmall,
                color = c.ink2,
                modifier = Modifier.testTag("FeynmanReviewNote")
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
}

/**
 * The AI coach slot (Q-01, D-10, ADR-0016): dashed box, `Spark`, "Coach feedback", "Coming later" and a disabled
 * action, with one honest line in place of the prototype's sample rows. No coaching text is ever invented.
 */
@Composable
private fun CoachBox(content: Color, container: Color) {
    val c = Itera.colors
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    content,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(
                        2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }
            .testTag("CoachBox")
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(IteraIcons.Spark, null, tint = content, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.coach_feedback),
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            Pill(stringResource(R.string.coming_later), container, content)
        }
        Text(stringResource(R.string.fey_coach_line), style = Itera.type.body, color = c.ink2)
        IteraButton(
            stringResource(R.string.coach_improve),
            onClick = {},
            kind = ButtonKind.Secondary,
            height = 44.dp,
            enabled = false,
            modifier = Modifier.testTag("CoachButton")
        )
    }
}

/** One row of real coach output. Unreachable until a provider exists (`CoachFeedbackProvider.isAvailable`). */
@Suppress("unused")
@Composable
internal fun FeedbackRow(icon: ImageVector, label: String, text: String, tint: Color) {
    val c = Itera.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink2
            )
            Text(text, style = Itera.type.body, color = c.ink)
        }
    }
}

private const val WORD_STEP = 25
