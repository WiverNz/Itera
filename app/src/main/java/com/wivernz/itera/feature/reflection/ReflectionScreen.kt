package com.wivernz.itera.feature.reflection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.StepDot
import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill

/** Chip ids per question; ids are stored, the text is rendered in the current language. */
internal val REFLECTION_CHIPS: List<List<Pair<String, Int>>> = listOf(
    listOf(
        "cleared" to R.string.chip_cleared,
        "focus_done" to R.string.chip_focus_done,
        "learned" to R.string.chip_learned
    ),
    listOf(
        "started_late" to R.string.chip_started_late,
        "email_first" to R.string.chip_email_first,
        "distracted" to R.string.chip_distracted,
        "no_time" to R.string.chip_no_time
    ),
    listOf(
        "focus_before_email" to R.string.chip_focus_before_email,
        "phone_away" to R.string.chip_phone_away,
        "start_earlier" to R.string.chip_start_earlier
    )
)

@Composable
fun ReflectionRoute(
    vm: ReflectionViewModel,
    onClose: () -> Unit,
    onFinished: (ReflectionEffect.Finished) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is ReflectionEffect.Finished -> onFinished(effect)
        }
    }
    val prefill = state.prefill?.let { renderPrefill(it) }
    LaunchedEffect(prefill) { prefill?.let(vm::applyPrefill) }
    ReflectionScreen(
        state = state,
        onAnswer = vm::setAnswer,
        onChip = vm::toggleChip,
        onNext = vm::next,
        onSkip = vm::skipTonight,
        onClose = {
            vm.onClose()
            onClose()
        }
    )
}

/** The pre-filled answer to question 1, one clause per part. */
@Composable
fun renderPrefill(parts: List<PrefillPart>): String = parts.map { part ->
    when (part) {
        is PrefillPart.Practised ->
            stringResource(R.string.reflection_prefill_practised, part.names.joinToString(", "))
        is PrefillPart.Tasks ->
            pluralStringResource(R.plurals.reflection_prefill_tasks, part.count, part.count)
        is PrefillPart.Focus ->
            pluralStringResource(R.plurals.reflection_prefill_focus, part.minutes, part.minutes)
    }
}.joinToString(ReflectionPrefill.SEPARATOR)

/**
 * Evening reflection on the night surface: three questions, one at a time; answered ones collapse.
 * Port of the prototype's `ReflectionScreen` (Reflection.kt).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReflectionScreen(
    state: ReflectionUiState,
    onAnswer: (Int, String) -> Unit,
    onChip: (Int, String, (Set<String>) -> String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    val c = Itera.colors
    val accent = Skill.REFLECTION.colors(c.isDark).content
    val reduced = LocalReduceMotion.current
    val questions = listOf(R.string.reflection_q1, R.string.reflection_q2, R.string.reflection_q3)
    val chipTexts = REFLECTION_CHIPS.map { row ->
        row.map { (id, res) -> id to stringResource(res) }
    }
    val answered = stringResource(R.string.reflection_answered)
    ScreenColumn(
        gap = 22.dp,
        modifier = Modifier.testTag("Reflection"),
        bottom = {
            IteraButton(
                stringResource(
                    if (state.step <
                        ReflectionViewModel.LAST_STEP
                    ) {
                        R.string.action_next
                    } else {
                        R.string.action_done
                    }
                ),
                onClick = onNext,
                enabled = !state.loading && !state.saving
            )
            IteraButton(
                stringResource(R.string.reflection_skip),
                onSkip,
                kind = ButtonKind.Ghost,
                height = 44.dp,
                enabled = !state.saving
            )
        }
    ) {
        TopBar(
            stringResource(R.string.reflection_label),
            onClose,
            trailing = { Pill(stringResource(R.string.minutes_short, 2), c.surface2, c.ink2) }
        )
        Text(stringResource(R.string.reflection_title), style = Itera.type.display, color = c.ink)
        state.lookBack?.let { LookBack(it, accent) }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
        questions.forEachIndexed { i, q ->
            val step = when {
                i < state.step -> StepState.Done
                i == state.step -> StepState.Now
                else -> StepState.Next
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StepDot(step, accent, modifier = Modifier.clearAndSetSemantics { })
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (reduced) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(tween(250)) + slideInVertically { it / 6 }) togetherWith
                                fadeOut(tween(150))
                        }
                    },
                    label = "q$i",
                    modifier = Modifier.weight(1f)
                ) { s ->
                    when (s) {
                        StepState.Done -> Column(
                            Modifier.semantics(mergeDescendants = true) {
                                stateDescription =
                                    answered
                            },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                stringResource(q),
                                style = Itera.type.bodySmall,
                                color = c.ink2,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                state.answers[i].ifBlank { "—" },
                                style = Itera.type.body.copy(
                                    fontFamily = Itera.type.userText.fontFamily
                                ),
                                color = c.ink
                            )
                        }
                        StepState.Now -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                stringResource(q),
                                style = Itera.type.headline,
                                color = c.ink,
                                modifier = Modifier.semantics { heading() }
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chipTexts[i].forEach { (id, text) ->
                                    ChoiceChip(text, id in state.chips[i], onClick = {
                                        onChip(i, id) { selected ->
                                            chipTexts[i].filter { it.first in selected }
                                                .joinToString(ReflectionPrefill.SEPARATOR) {
                                                    it.second
                                                }
                                        }
                                    })
                                }
                            }
                            NoteField(
                                state.answers[i],
                                { onAnswer(i, it) },
                                stringResource(R.string.reflection_one_line),
                                modifier = Modifier.testTag("ReflectionField")
                            )
                        }
                        StepState.Next -> Text(
                            stringResource(q),
                            style = Itera.type.body,
                            color = c.ink2,
                            modifier = Modifier.padding(top = 3.dp).semantics { heading() }
                        )
                    }
                }
            }
        }
    }
}

/** Production addition: the weekly look-back header on days 7, 14, 21 (docs/ux/02-screen-specs-exercise.md 5). */
@Composable
private fun LookBack(lookBack: WeeklyLookBack, accent: androidx.compose.ui.graphics.Color) {
    val c = Itera.colors
    IteraCard(gap = 8.dp, modifier = Modifier.testTag("WeeklyLookBack")) {
        Eyebrow(stringResource(R.string.reflection_week_title), accent)
        Text(
            pluralStringResource(
                R.plurals.reflection_week_days,
                lookBack.daysTrained,
                lookBack.daysTrained
            ),
            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink
        )
        Text(
            pluralStringResource(
                R.plurals.reflection_week_techniques,
                lookBack.techniquesPractised,
                lookBack.techniquesPractised
            ),
            style = Itera.type.bodySmall,
            color = c.ink2
        )
        Text(
            pluralStringResource(
                R.plurals.reflection_week_focus,
                lookBack.focusMinutes,
                lookBack.focusMinutes
            ),
            style = Itera.type.bodySmall,
            color = c.ink2
        )
        if (lookBack.changes.isNotEmpty()) {
            Text(
                stringResource(R.string.reflection_week_changes),
                style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink2,
                modifier = Modifier.padding(top = 6.dp)
            )
            lookBack.changes.forEach {
                Text(
                    "“$it”",
                    style = Itera.type.bodySmall.copy(fontFamily = Itera.type.userText.fontFamily),
                    color = c.ink
                )
            }
        }
    }
}
