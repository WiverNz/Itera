package com.itera.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.DayStep
import com.itera.app.model.Program
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.ui.components.VoiceCommandPill
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.components.ChoiceChip
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.NoteField
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.StepDot
import com.itera.app.ui.components.StepState
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors

/**
 * Evening reflection: three questions, one at a time. Quick-pick chips come from what
 * was logged today, so an answer can take a single tap. Answered questions collapse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReflectionScreen(vm: AppViewModel, onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val accent = Skill.Reflection.colors(c.isDark).content
    var step by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf("", "", "") }
    val picked = remember { mutableStateListOf<Int>() }

    val questions = listOf(R.string.reflection_q1, R.string.reflection_q2, R.string.reflection_q3)
    val chips = listOf(
        listOf(R.string.chip_cleared, R.string.chip_focus_done, R.string.chip_learned),
        listOf(R.string.chip_started_late, R.string.chip_email_first, R.string.chip_distracted, R.string.chip_no_time),
        listOf(R.string.chip_focus_before_email, R.string.chip_phone_away, R.string.chip_start_earlier),
    )
    val chipTexts = chips.map { row -> row.map { stringResource(it) } }

    ScreenColumn(
        gap = 22.dp,
        bottom = {
            IteraButton(
                stringResource(if (step < 2) R.string.action_next else R.string.action_done),
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        vm.tomorrowChange = answers[2]
                        vm.complete(DayStep.Reflection, Technique.DailyReflection)
                        onDone()
                    }
                },
            )
            IteraButton(stringResource(R.string.reflection_skip), onClose, kind = ButtonKind.Ghost, height = 44.dp)
        },
    ) {
        TopBar(stringResource(R.string.reflection_label), onClose) {
            Pill(stringResource(R.string.minutes_short, 2), c.surface2, c.ink2)
        }
        VoiceCommandPill(Modifier.align(Alignment.End))
        Text(stringResource(R.string.reflection_title), style = Itera.type.display, color = c.ink)
        if (vm.programDay % 7 == 0) WeeklyLookBack(vm, accent)

        questions.forEachIndexed { i, q ->
            val state = when {
                i < step -> StepState.Done
                i == step -> StepState.Now
                else -> StepState.Next
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StepDot(state, accent)
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { (fadeIn(tween(250)) + slideInVertically { it / 6 }) togetherWith fadeOut(tween(150)) },
                    label = "q$i",
                    modifier = Modifier.weight(1f),
                ) { s ->
                    when (s) {
                        StepState.Done -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(q), style = Itera.type.bodySmall, color = c.ink2)
                            Text(answers[i].ifBlank { "—" }, style = Itera.type.body, color = c.ink)
                        }
                        StepState.Now -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(stringResource(q), style = Itera.type.headline, color = c.ink)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                chipTexts[i].forEachIndexed { j, text ->
                                    val key = i * 10 + j
                                    ChoiceChip(text, key in picked, onClick = {
                                        if (key in picked) picked.remove(key) else picked.add(key)
                                        answers[i] = chipTexts[i].filterIndexed { k, _ -> (i * 10 + k) in picked }.joinToString(" · ")
                                    })
                                }
                            }
                            NoteField(answers[i], { answers[i] = it }, stringResource(R.string.reflection_one_line), dictation = true)
                        }
                        StepState.Next -> Text(stringResource(q), style = Itera.type.body, color = c.ink2, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}

/** Production addition: on Days 7, 14, 21 the reflection opens with a summary of the week. No extra tap. */
@Composable
private fun WeeklyLookBack(vm: AppViewModel, accent: androidx.compose.ui.graphics.Color) {
    val c = Itera.colors
    val since = java.time.LocalDate.now().minusDays(6)
    val week = vm.log.filter { !it.date.isBefore(since) }
    val days = week.map { it.date }.distinct().size
    val techniques = week.map { it.technique }.filter { it != Technique.DailyReflection }.distinct().size
    val focus = week.count { it.technique == Technique.Pomodoro } * 25 + week.count { it.technique == Technique.DeepWork } * 50
    IteraCard(gap = 8.dp) {
        Eyebrow(stringResource(R.string.reflection_week_title), accent)
        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.reflection_week_days, days, days), style = Itera.type.body, fontWeight = FontWeight.SemiBold, color = c.ink)
        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.reflection_week_techniques, techniques, techniques), style = Itera.type.bodySmall, color = c.ink2)
        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.reflection_week_focus, focus, focus), style = Itera.type.bodySmall, color = c.ink2)
        if (vm.carriedChange.isNotBlank()) {
            Text(stringResource(R.string.reflection_week_changes), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, modifier = Modifier.padding(top = 6.dp))
            Text("“${vm.carriedChange}”", style = Itera.type.bodySmall, color = c.ink)
        }
    }
}

/** End of the day: the ring closes, tomorrow's change is shown back, next technique previewed. */
@Composable
fun DayCompleteScreen(vm: AppViewModel, onGoodNight: () -> Unit) {
    val c = Itera.colors
    val next = Program.techniqueFor(vm.programDay + 1)
    ScreenColumn(gap = 18.dp, bottom = { IteraButton(stringResource(R.string.good_night), onGoodNight) }) {
        DayRing(Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp), done = vm.completed.size.coerceAtLeast(1))
        Text(stringResource(R.string.day_complete_title, vm.programDay), style = Itera.type.display, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.day_complete_sub), style = Itera.type.body, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (vm.tomorrowChange.isNotBlank()) {
            IteraCard {
                Eyebrow(stringResource(R.string.day_change_label), Skill.Reflection.colors(c.isDark).content)
                Text("“${vm.tomorrowChange}”", style = Itera.type.headline.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                Text(stringResource(R.string.day_change_hint), style = Itera.type.bodySmall, color = c.ink2)
            }
        }
        IteraCard(padding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TechniqueToken(next ?: Technique.Pareto, 48.dp)
                Column {
                    Text(stringResource(R.string.day_tomorrow, vm.programDay + 1), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
                    Text(if (next != null) stringResource(next.title) else stringResource(R.string.today_combination), style = Itera.type.label, color = c.ink)
                    Text(stringResource(R.string.day_unlocks, formatTime(vm.morningTime)), style = Itera.type.bodySmall, color = c.ink2)
                }
            }
        }
    }
}

/** Three arcs (exercise, focus, reflection) that draw themselves one after another. */
@Composable
private fun DayRing(modifier: Modifier, done: Int) {
    val c = Itera.colors
    val colors = listOf(Skill.Habits, Skill.Focus, Skill.Reflection).map { it.colors(c.isDark).content }
    val sweeps = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(done) {
        sweeps.forEachIndexed { i, a -> if (i < done) a.animateTo(1f, tween(500)) }
    }
    Box(modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            val gap = 14f
            sweeps.forEachIndexed { i, a ->
                if (a.value > 0f) {
                    drawArc(colors[i], -90f + i * 120f + gap / 2, (120f - gap) * a.value, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Text("$done/3", style = Itera.type.display, color = c.ink)
    }
}
