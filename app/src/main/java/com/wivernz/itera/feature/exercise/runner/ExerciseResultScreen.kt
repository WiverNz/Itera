package com.wivernz.itera.feature.exercise.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.AnimatedCheck
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IntervalLadder
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.MasteryLadder
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel

@Composable
fun ExerciseResultRoute(vm: ExerciseResultViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }
    ExerciseResultScreen(
        state,
        onDifficulty = vm::setDifficulty,
        onNote = vm::setNote,
        onDone = {
            vm.flush()
            onDone()
        }
    )
}

private val Difficulty.label: Int get() = when (this) {
    Difficulty.EASY -> R.string.feel_easy
    Difficulty.OKAY -> R.string.feel_okay
    Difficulty.HARD -> R.string.feel_hard
}

/** Prototype `ExerciseResultScreen` (Exercise.kt): animated check, how it felt, a note, honest progress. */
@Composable
fun ExerciseResultScreen(
    state: ExerciseResultUiState,
    onDifficulty: (Difficulty) -> Unit,
    onNote: (String) -> Unit,
    onDone: () -> Unit
) {
    val c = Itera.colors
    if (state.missing) {
        ScreenColumn(
            modifier = Modifier.testTag("ExerciseResult"),
            bottom = { IteraButton(stringResource(R.string.action_done), onDone) }
        ) { ErrorState(stringResource(R.string.error_activity_missing)) }
        return
    }
    val sc = state.skill.colors(c.isDark)
    ScreenColumn(
        gap = 20.dp,
        modifier = Modifier.testTag("ExerciseResult"),
        bottom = {
            IteraButton(
                stringResource(R.string.action_done),
                onDone,
                modifier = Modifier.testTag("ResultDone")
            )
        }
    ) {
        AnimatedCheck(
            sc.container,
            sc.content,
            Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
        )
        Text(
            stringResource(R.string.result_title),
            style = Itera.type.title,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.result_sub),
            style = Itera.type.body,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        (state.variant as? ResultVariant.Focus)?.let { focus ->
            Text(
                pluralStringResource(
                    R.plurals.result_focus_minutes,
                    focus.minutes,
                    focus.minutes,
                    focus.plannedMinutes
                ),
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("FocusMinutes")
            )
            focus.breakMinutes?.let {
                Text(
                    pluralStringResource(R.plurals.focus_break_hint_n, it, it),
                    style = Itera.type.bodySmall,
                    color = c.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Text(stringResource(R.string.result_feel), style = Itera.type.label, color = c.ink)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                val on = d == state.difficulty
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(
                            2.dp,
                            if (on) c.ink else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .selectable(selected = on, role = Role.RadioButton) { onDifficulty(d) }
                        .testTag("Feel_${d.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(d.label),
                        style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink
                    )
                }
            }
        }
        CappedNoteField(
            state.note,
            onNote,
            stringResource(R.string.result_note_hint),
            modifier = Modifier.testTag("ResultNote")
        )
        when (val variant = state.variant) {
            is ResultVariant.Review -> IteraCard(padding = PaddingValues(18.dp), gap = 12.dp) {
                Text(
                    state.name,
                    style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink
                )
                IntervalLadder(variant.stageIndex, sc.content)
                Text(
                    variant.nextDays?.let {
                        pluralStringResource(R.plurals.review_next_in, it, it)
                    } ?: stringResource(R.string.review_retired),
                    style = Itera.type.caption,
                    color = c.ink2,
                    modifier = Modifier.testTag("ReviewNext")
                )
            }
            else -> if (state.level != MasteryLevel.NONE) {
                IteraCard(padding = PaddingValues(18.dp), gap = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.name,
                            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = c.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Pill(stringResource(state.level.title), sc.container, sc.content)
                    }
                    MasteryLadder(state.level, sc.content, animate = true)
                    levelHint(state.hint)?.let {
                        Text(it, style = Itera.type.caption, color = c.ink2)
                    }
                }
            }
        }
    }
}

/** The next-level hint (docs/engine/03 section 2.1); the thresholds come from the mastery rules. */
@Composable
fun levelHint(hint: LevelHint): String? = when (hint) {
    LevelHint.Unavailable -> null
    is LevelHint.ToPracticed -> pluralStringResource(
        R.plurals.level_hint_days,
        hint.moreDays,
        hint.moreDays,
        stringResource(R.string.level_practiced)
    )
    is LevelHint.ToApplied -> if (hint.moreUses > 0) {
        pluralStringResource(
            R.plurals.level_hint_uses,
            hint.moreUses,
            hint.moreUses,
            stringResource(R.string.level_applied)
        )
    } else {
        stringResource(R.string.level_hint_applied_span, stringResource(R.string.level_applied))
    }
    LevelHint.ToIntegrated ->
        stringResource(R.string.level_hint_integrated, stringResource(R.string.level_integrated))
    LevelHint.Integrated -> stringResource(R.string.level_hint_top)
}
