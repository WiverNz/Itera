package com.itera.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.DayStep
import com.itera.app.model.ExerciseKind
import com.itera.app.model.Feeling
import com.itera.app.model.Technique
import com.itera.app.ui.components.VoiceCommandPill
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.MasteryLadder
import com.itera.app.ui.components.NoteField
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors

/** Short "why", then one concrete action to do in real life. */
@Composable
fun ExerciseIntroScreen(vm: AppViewModel, technique: Technique, onClose: () -> Unit, onStart: () -> Unit) {
    val c = Itera.colors
    val sc = technique.skill.colors(c.isDark)
    val generic = technique.kind == ExerciseKind.Generic
    ScreenColumn(
        gap = 22.dp,
        bottom = {
            IteraButton(
                stringResource(if (generic) R.string.exercise_did_it else R.string.exercise_start),
                onStart,
                icon = if (generic) IteraIcons.Check else IteraIcons.Play,
            )
            IteraButton(stringResource(R.string.exercise_later), onClose, kind = ButtonKind.Ghost, height = 48.dp)
        },
    ) {
        TopBar(stringResource(R.string.exercise_label), onClose)
        TechniqueToken(technique, 64.dp)
        Eyebrow(stringResource(R.string.exercise_meta, stringResource(technique.skill.title), vm.programDay), sc.content)
        Text(stringResource(technique.title), style = Itera.type.hero, color = c.ink)
        Text(stringResource(technique.why), style = Itera.type.bodyLarge, color = c.ink2)
        IteraCard(radius = 28.dp, padding = PaddingValues(22.dp)) {
            Eyebrow(stringResource(R.string.exercise_yours))
            Text(stringResource(technique.task), style = Itera.type.headline.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        }
    }
}

/** 2-minute rule: find two quick tasks, do them, tick them off. */
@Composable
fun TwoMinuteScreen(onClose: () -> Unit, onFinish: () -> Unit) {
    val c = Itera.colors
    val sc = Technique.TwoMinute.skill.colors(c.isDark)
    val samples = listOf(stringResource(R.string.two_sample_1), stringResource(R.string.two_sample_2))
    val tasks = remember { mutableStateListOf(samples[0] to true, samples[1] to false) }
    var extra by remember { mutableStateOf("") }
    val done = tasks.count { it.second }
    ScreenColumn(gap = 22.dp, bottom = { IteraButton(stringResource(R.string.two_finish), onFinish, enabled = done > 0) }) {
        TopBar(stringResource(Technique.TwoMinute.title), onClose, IteraIcons.Back) {
            Pill("$done / ${tasks.size}", sc.container, sc.content)
        }
        VoiceCommandPill(Modifier.align(Alignment.End))
        Text(stringResource(R.string.two_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.two_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tasks.forEachIndexed { i, (label, checked) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .toggleable(value = checked, role = Role.Checkbox) { tasks[i] = label to it }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CheckCircle(checked, sc.content)
                    Text(
                        label,
                        style = Itera.type.body.copy(fontWeight = FontWeight.Medium, textDecoration = if (checked) TextDecoration.LineThrough else null),
                        color = if (checked) c.ink2 else c.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            NoteField(extra, { extra = it }, stringResource(R.string.two_add_hint), minLines = 1, dictation = true, onAdd = {
                tasks += extra.trim() to false
                extra = ""
            })
        }
    }
}

@Composable
fun CheckCircle(checked: Boolean, color: androidx.compose.ui.graphics.Color) {
    val c = Itera.colors
    Box(
        Modifier.size(30.dp).clip(CircleShape)
            .background(if (checked) color else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, if (checked) color else c.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(IteraIcons.Check, null, tint = c.surface, modifier = Modifier.size(16.dp))
    }
}

/** Completion: animated check, how it felt, an optional note, and honest progress. */
@Composable
fun ExerciseResultScreen(vm: AppViewModel, technique: Technique, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = technique.skill.colors(c.isDark)
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    var note by remember { mutableStateOf("") }
    ScreenColumn(
        gap = 20.dp,
        bottom = {
            IteraButton(stringResource(R.string.action_done), onClick = {
                vm.lastFeeling = feeling
                vm.lastNote = note
                if (technique == vm.todaysTechnique || (vm.isCombinationDay && technique == Technique.Pareto)) {
                    vm.complete(DayStep.Exercise, technique)
                } else {
                    vm.record(technique, note)
                }
                onDone()
            })
        },
    ) {
        AnimatedCheck(sc.container, sc.content, Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        Text(stringResource(R.string.result_title), style = Itera.type.title, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.result_sub), style = Itera.type.body, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.result_feel), style = Itera.type.label, color = c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Feeling.entries.forEach { f ->
                val on = f == feeling
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(2.dp, if (on) c.ink else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp))
                        .selectable(selected = on, role = Role.RadioButton) { feeling = f },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(f.title), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink) }
            }
        }
        NoteField(note, { note = it }, stringResource(R.string.result_note_hint), dictation = true)
        IteraCard(padding = PaddingValues(18.dp), gap = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(technique.title), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
                val level = vm.masteryOf(technique) ?: com.itera.app.model.Mastery.Met
                Pill(stringResource(level.title), sc.container, sc.content)
            }
            MasteryLadder(vm.masteryOf(technique) ?: com.itera.app.model.Mastery.Met, sc.content)
            Text(stringResource(R.string.result_level_hint), style = Itera.type.caption, color = c.ink2)
        }
    }
}

/** Circle pops in with a spring, then the check mark draws itself. */
@Composable
fun AnimatedCheck(container: androidx.compose.ui.graphics.Color, stroke: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val reduced = com.itera.app.ui.theme.LocalReduceMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0.5f) }
    val draw = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, com.itera.app.ui.theme.IteraMotion.pop(reduced, true))
        draw.animateTo(1f, com.itera.app.ui.theme.IteraMotion.check(reduced))
    }
    Box(modifier.size(96.dp).scale(pop.value).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(48.dp)) {
            val s = size.width / 24f
            val path = Path().apply {
                moveTo(5f * s, 12.5f * s); lineTo(9.5f * s, 17f * s); lineTo(19f * s, 7.5f * s)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * draw.value, partial, true)
            drawPath(partial, stroke, style = Stroke(width = 2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
