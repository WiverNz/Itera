package com.wivernz.itera.feature.exercise.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IntervalLadder
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.CappedNoteField
import com.wivernz.itera.feature.exercise.runner.ConfirmDialog
import com.wivernz.itera.feature.exercise.runner.LeaveExerciseDialog

@Composable
fun ReviewRoute(vm: ReviewViewModel, showResult: (Long, String) -> Unit, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is ReviewEffect.ShowResult -> showResult(effect.activityId, effect.techniqueId)
            ReviewEffect.Close -> onClose()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    ReviewScreen(state, vm::setAnswer, vm::reveal, vm::grade, vm::leave)
}

private val RecallGrade.label: Int get() = when (this) {
    RecallGrade.FORGOT -> R.string.review_forgot
    RecallGrade.PARTIAL -> R.string.review_partial
    RecallGrade.SOLID -> R.string.review_solid
}

/** Prototype `ReviewScreen` (Practice.kt), then the side-by-side comparison and the three grades. */
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onAnswer: (String) -> Unit,
    onReveal: () -> Unit,
    onGrade: (RecallGrade) -> Unit,
    onLeave: () -> Unit
) {
    val c = Itera.colors
    val sc = Skill.LEARNING.colors(c.isDark)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val close = {
        if (state.revealed || state.answer.isNotBlank()) confirming = true else onLeave()
    }
    BackHandler(enabled = !state.loading) { close() }
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("Review")) {
            TopBar(stringResource(R.string.review_label), onLeave)
            ErrorState(stringResource(R.string.error_review_missing))
        }
        return
    }
    ScreenColumn(
        gap = 16.dp,
        modifier = Modifier.testTag("Review"),
        bottom = {
            if (!state.revealed) {
                IteraButton(
                    stringResource(R.string.review_compare),
                    onReveal,
                    enabled = !state.loading,
                    modifier = Modifier.testTag("ReviewCompare")
                )
            } else {
                Text(
                    stringResource(R.string.review_grade_title),
                    style = Itera.type.label,
                    color = c.ink
                )
                RecallGrade.entries.reversed().forEach { grade ->
                    IteraButton(
                        stringResource(grade.label),
                        { onGrade(grade) },
                        kind = if (grade ==
                            RecallGrade.SOLID
                        ) {
                            ButtonKind.Primary
                        } else {
                            ButtonKind.Secondary
                        },
                        enabled = !state.busy,
                        height = 48.dp,
                        modifier = Modifier.testTag("Grade_${grade.name}")
                    )
                }
            }
        }
    ) {
        TopBar(stringResource(R.string.review_label), close, trailing = {
            Pill(stringResource(Skill.LEARNING.title), sc.container, sc.content)
        })
        TechniqueToken(Skill.LEARNING, IteraIcons.Spaced, 48.dp)
        Text(
            pluralStringResource(R.plurals.review_days_ago, state.daysAgo, state.daysAgo),
            style = Itera.type.body,
            color = c.ink2
        )
        Text(
            state.topic.ifBlank { stringResource(R.string.review_generic_topic) },
            style = Itera.type.title.copy(
                fontFamily = if (state.topic.isBlank()) null else Itera.type.userText.fontFamily
            ),
            color = c.ink
        )
        Text(stringResource(R.string.review_again), style = Itera.type.bodyLarge, color = c.ink2)
        if (!state.revealed) {
            CappedNoteField(
                state.answer,
                onAnswer,
                stringResource(R.string.review_field),
                minLines = 7,
                textStyle = Itera.type.bodyLarge,
                bordered = true,
                modifier = Modifier.testTag("ReviewAnswer")
            )
            val hidden = stringResource(R.string.review_hidden_a11y)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface2)
                    .testTag("ReviewHidden")
                    .clearAndSetSemantics { contentDescription = hidden }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(IteraIcons.Lock, null, tint = c.ink2, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.review_hidden),
                    style = Itera.type.bodySmall,
                    color = c.ink2
                )
            }
        } else {
            Comparison(state.answer, state.previousAnswer.orEmpty())
        }
        IntervalLadder(state.stageIndex, sc.content, Modifier.testTag("ReviewLadder"))
        Text(
            state.nextDays?.let { pluralStringResource(R.plurals.review_next, it, it) }
                ?: stringResource(R.string.review_retire_preview),
            style = Itera.type.caption,
            color = c.ink2
        )
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
    if (confirming) {
        if (state.revealed) {
            ConfirmDialog(
                title = stringResource(R.string.review_discard_title),
                body = stringResource(R.string.review_discard_body),
                confirm = stringResource(R.string.review_discard),
                dismiss = stringResource(R.string.review_keep),
                onConfirm = {
                    confirming = false
                    onLeave()
                },
                onDismiss = { confirming = false }
            )
        } else {
            LeaveExerciseDialog(onLeave = {
                confirming = false
                onLeave()
            }, onStay = { confirming = false })
        }
    }
}

/** Today's answer and the first one, side by side; stacked at large font scales. */
@Composable
private fun Comparison(today: String, first: String) {
    val wide = LocalDensity.current.fontScale <= SIDE_BY_SIDE_MAX_SCALE
    val cards = @Composable { modifier: Modifier ->
        AnswerCard(
            stringResource(R.string.review_today_answer),
            today,
            modifier.testTag("TodayAnswer")
        )
        AnswerCard(
            stringResource(R.string.review_first_answer),
            first,
            modifier.testTag("FirstAnswer")
        )
    }
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { cards(Modifier.weight(1f)) }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { cards(Modifier) }
    }
}

@Composable
private fun AnswerCard(label: String, text: String, modifier: Modifier) {
    val c = Itera.colors
    IteraCard(
        gap = 8.dp,
        padding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        modifier = modifier
    ) {
        Text(
            label,
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink2
        )
        Text(
            text.ifBlank { stringResource(R.string.review_empty_answer) },
            style = Itera.type.body.copy(fontFamily = Itera.type.userText.fontFamily),
            color = if (text.isBlank()) c.ink2 else c.ink
        )
    }
}

private const val SIDE_BY_SIDE_MAX_SCALE = 1.3f
