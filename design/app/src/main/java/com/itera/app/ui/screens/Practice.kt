package com.itera.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.MatrixTask
import com.itera.app.model.Quadrant
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.components.ChoiceChip
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.NoteField
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ------------------------------------------------------------------ combination

/** Eisenhower → 80/20 → Deep Work → Reflection, as one guided chain. */
@Composable
fun CombinationScreen(vm: AppViewModel, onClose: () -> Unit, onContinue: () -> Unit) {
    val c = Itera.colors
    var choice by remember { mutableIntStateOf(0) }
    val options = listOf(R.string.combo_opt1, R.string.combo_opt2, R.string.combo_opt3)
    ScreenColumn(gap = 20.dp, bottom = { IteraButton(stringResource(R.string.combo_cta), onContinue, icon = IteraIcons.Play) }) {
        TopBar(stringResource(R.string.combo_label, vm.programDay), onClose) {
            Pill(stringResource(R.string.minutes_short, 70), c.surface2, c.ink2)
        }
        Text(stringResource(R.string.combo_title), style = Itera.type.display, color = c.ink)
        Text(stringResource(R.string.combo_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ChainStep(Technique.Eisenhower, ChainState.Done, stringResource(R.string.combo_step1), stringResource(R.string.combo_step1_sub))
            ChainStep(Technique.Pareto, ChainState.Now, stringResource(R.string.combo_step2), stringResource(R.string.combo_step2_sub)) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface2).padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    options.forEachIndexed { i, res ->
                        val on = i == choice
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surface)
                                .border(2.dp, if (on) c.ink else Color.Transparent, RoundedCornerShape(14.dp))
                                .selectable(on, role = Role.RadioButton) { choice = i }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(18.dp).clip(CircleShape).border(if (on) 6.dp else 2.dp, if (on) c.ink else c.ink3, CircleShape))
                            Text(stringResource(res), style = Itera.type.bodySmall.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal), color = c.ink)
                        }
                    }
                }
            }
            ChainStep(Technique.DeepWork, ChainState.Next, stringResource(R.string.combo_step3, 50), stringResource(R.string.combo_step3_sub))
            ChainStep(Technique.DailyReflection, ChainState.Next, stringResource(R.string.combo_step4), stringResource(R.string.combo_step4_sub))
        }
    }
}

private enum class ChainState { Done, Now, Next }

@Composable
private fun ChainStep(t: Technique, state: ChainState, title: String, sub: String, body: (@Composable () -> Unit)? = null) {
    val c = Itera.colors
    val sc = t.skill.colors(c.isDark)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        when (state) {
            ChainState.Done -> Box(Modifier.size(36.dp).clip(CircleShape).background(sc.content), contentAlignment = Alignment.Center) {
                Icon(IteraIcons.Check, null, tint = c.surface, modifier = Modifier.size(18.dp))
            }
            ChainState.Now -> TechniqueToken(t, 36.dp, 18.dp)
            ChainState.Next -> Box(Modifier.size(36.dp).border(2.dp, c.line, CircleShape), contentAlignment = Alignment.Center) {
                Icon(t.icon, null, tint = c.ink2, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow(stringResource(t.title), if (state == ChainState.Next) c.ink2 else sc.content)
            Text(title, style = if (state == ChainState.Now) Itera.type.label else Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
            if (body != null) Box(Modifier.padding(top = 10.dp)) { body() }
        }
    }
}

// ------------------------------------------------------------------ eisenhower

/** Tap a task, then tap a square. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EisenhowerScreen(onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Planning.colors(c.isDark)
    val tasks = remember {
        mutableStateListOf(
            MatrixTask(1, R.string.eis_task_1, Quadrant.DoNow),
            MatrixTask(2, R.string.eis_task_2, Quadrant.Schedule),
            MatrixTask(3, R.string.eis_task_3, Quadrant.Delegate),
            MatrixTask(4, R.string.eis_task_4, Quadrant.Drop),
            MatrixTask(5, R.string.eis_task_5, null),
            MatrixTask(6, R.string.eis_task_6, null),
            MatrixTask(7, R.string.eis_task_7, null),
        )
    }
    var selected by remember { mutableStateOf<Int?>(5) }
    val inbox = tasks.filter { it.quadrant == null }

    fun place(q: Quadrant) {
        val id = selected ?: return
        val idx = tasks.indexOfFirst { it.id == id }
        if (idx >= 0) tasks[idx] = tasks[idx].copy(quadrant = q)
        selected = tasks.firstOrNull { it.quadrant == null }?.id
    }

    ScreenColumn(gap = 16.dp, bottom = { IteraButton(stringResource(R.string.eis_cta), onDone) }) {
        TopBar(stringResource(Technique.Eisenhower.title), onClose) { Pill(stringResource(Skill.Planning.title), sc.container, sc.content) }
        Text(stringResource(R.string.eis_title), style = Itera.type.title, color = c.ink)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.eis_to_sort), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
            Text(stringResource(if (selected != null) R.string.eis_tap_square else R.string.eis_tap_task), style = Itera.type.bodySmall, color = c.ink2)
        }
        FlowRow(Modifier.animateContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (inbox.isEmpty()) {
                Text(stringResource(R.string.eis_all_sorted), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = Skill.Habits.colors(c.isDark).content)
            }
            inbox.forEach { t ->
                val on = t.id == selected
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) c.ink else c.surface)
                        .border(2.dp, if (on) c.ink else c.line, RoundedCornerShape(14.dp))
                        .selectable(on, role = Role.RadioButton) { selected = t.id }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(t.label), style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = if (on) c.onInk else c.ink) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.padding(start = 30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AxisLabel(stringResource(R.string.eis_urgent), Modifier.weight(1f))
                AxisLabel(stringResource(R.string.eis_not_urgent), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideLabel(stringResource(R.string.eis_important))
                QuadrantBox(stringResource(R.string.eis_q1), stringResource(R.string.eis_q1_sub), c.accentSoft, c.accent, tasks.filter { it.quadrant == Quadrant.DoNow }, Modifier.weight(1f)) { place(Quadrant.DoNow) }
                QuadrantBox(stringResource(R.string.eis_q2), stringResource(R.string.eis_q2_sub), sc.container, sc.content, tasks.filter { it.quadrant == Quadrant.Schedule }, Modifier.weight(1f)) { place(Quadrant.Schedule) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideLabel(stringResource(R.string.eis_not_important))
                QuadrantBox(stringResource(R.string.eis_q3), stringResource(R.string.eis_q3_sub), c.surface2, c.ink, tasks.filter { it.quadrant == Quadrant.Delegate }, Modifier.weight(1f)) { place(Quadrant.Delegate) }
                QuadrantBox(stringResource(R.string.eis_q4), stringResource(R.string.eis_q4_sub), Color.Transparent, c.ink2, tasks.filter { it.quadrant == Quadrant.Drop }, Modifier.weight(1f), dashed = true) { place(Quadrant.Drop) }
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = Itera.colors.ink2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable
private fun SideLabel(text: String) {
    Box(Modifier.width(22.dp).height(168.dp), contentAlignment = Alignment.Center) {
        Text(
            text, maxLines = 1, softWrap = false,
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = Itera.colors.ink2,
            modifier = Modifier.rotate(-90f).requiredWidth(168.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun QuadrantBox(
    title: String, sub: String, bg: Color, fg: Color, items: List<MatrixTask>,
    modifier: Modifier, dashed: Boolean = false, onClick: () -> Unit,
) {
    val c = Itera.colors
    val line = c.ink3
    Column(
        modifier
            .heightIn(min = 168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .then(
                if (dashed) Modifier.drawBehind {
                    drawRoundRect(line, cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                } else Modifier,
            )
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = Itera.type.headline.copy(fontSize = Itera.type.label.fontSize), color = fg)
        Text(sub, style = Itera.type.caption, color = c.ink2)
        items.forEach {
            Text(
                stringResource(it.label),
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = c.ink,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ feynman

@Composable
fun FeynmanScreen(onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Learning.colors(c.isDark)
    var text by remember { mutableStateOf("") }
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    ScreenColumn(gap = 16.dp, bottom = { IteraButton(stringResource(R.string.fey_done), onDone, enabled = words > 0) }) {
        TopBar(stringResource(Technique.Feynman.title), onClose) { Pill(stringResource(Skill.Learning.title), sc.container, sc.content) }
        IteraCard(color = sc.container, gap = 10.dp) {
            Eyebrow(stringResource(R.string.fey_topic), sc.content)
            Text(stringResource(R.string.fey_topic_value), style = Itera.type.headline, color = c.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fey_from_list), style = Itera.type.bodySmall, color = c.ink2, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.fey_change),
                    style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.ink,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.surface).clickable(role = Role.Button) { }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Text(stringResource(R.string.fey_instruction), style = Itera.type.body, color = c.ink2)
        Text(stringResource(R.string.fey_your_expl), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
        NoteField(text, { text = it }, stringResource(R.string.fey_hint), minLines = 9, textStyle = Itera.type.bodyLarge, bordered = true)
        Row {
            Text(pluralStringResource(R.plurals.words, words, words), style = Itera.type.caption, color = c.ink2, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.fey_no_notes), style = Itera.type.caption, color = c.ink2)
        }
    }
}

/** Self-review plus the reserved slot for AI feedback (not wired yet). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeynmanFeedbackScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Learning.colors(c.isDark)
    val picked = remember { mutableStateListOf(0) }
    var note by remember { mutableStateOf("") }
    val chips = listOf(R.string.fey_chip_crash, R.string.fey_chip_which, R.string.fey_chip_tradeoffs)
    ScreenColumn(gap = 18.dp, bottom = { IteraButton(stringResource(R.string.action_finish), onDone) }) {
        TopBar(stringResource(Technique.Feynman.title), onBack, IteraIcons.Back)
        Text(stringResource(R.string.fey_back_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.fey_hardest), style = Itera.type.label, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEachIndexed { i, res ->
                ChoiceChip(stringResource(res), i in picked, onClick = { if (i in picked) picked.remove(i) else picked.add(i) })
            }
        }
        NoteField(note, { note = it }, stringResource(R.string.fey_note_hint))

        // Placeholder for the future AI coach: layout is final, content is sample.
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(sc.content, cornerRadius = CornerRadius(24.dp.toPx()),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
                }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(IteraIcons.Spark, null, tint = sc.content, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.coach_feedback), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
                Pill(stringResource(R.string.coach_later), sc.container, sc.content)
            }
            FeedbackRow(IteraIcons.Check, stringResource(R.string.coach_clear), stringResource(R.string.coach_clear_text), sc.content)
            FeedbackRow(IteraIcons.Eye, stringResource(R.string.coach_gap), stringResource(R.string.coach_gap_text), sc.content)
            FeedbackRow(IteraIcons.Feynman, stringResource(R.string.coach_beginner), stringResource(R.string.coach_beginner_text), sc.content)
            IteraButton(stringResource(R.string.coach_improve), onClick = {}, kind = ButtonKind.Secondary, height = 44.dp, enabled = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(IteraIcons.Spaced, null, tint = sc.content, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.fey_review_note), style = Itera.type.bodySmall, color = c.ink2)
        }
    }
}

@Composable
private fun FeedbackRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, text: String, tint: Color) {
    val c = Itera.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
            Text(text, style = Itera.type.body, color = c.ink)
        }
    }
}

// ------------------------------------------------------------------ spaced repetition

@Composable
fun ReviewScreen(onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Learning.colors(c.isDark)
    var text by remember { mutableStateOf("") }
    ScreenColumn(gap = 16.dp, bottom = { IteraButton(stringResource(R.string.review_compare), onDone, enabled = text.isNotBlank()) }) {
        TopBar(stringResource(R.string.review_label), onClose) { Pill(stringResource(Skill.Learning.title), sc.container, sc.content) }
        TechniqueToken(Technique.Spaced, 48.dp)
        Text(pluralStringResource(R.plurals.review_days_ago, 4, 4), style = Itera.type.body, color = c.ink2)
        Text(stringResource(R.string.review_topic), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.review_again), style = Itera.type.bodyLarge, color = c.ink2)
        NoteField(text, { text = it }, stringResource(R.string.review_field), minLines = 7, textStyle = Itera.type.bodyLarge, bordered = true)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface2).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(IteraIcons.Lock, null, tint = c.ink2, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.review_hidden), style = Itera.type.bodySmall, color = c.ink2)
        }
        IntervalLadder(sc.content)
        Text(pluralStringResource(R.plurals.review_next, 9, 9), style = Itera.type.caption, color = c.ink2)
    }
}

@Composable
fun IntervalLadder(accent: Color, stageIndex: Int = 1) {
    val c = Itera.colors
    val steps = listOf(
        stringResource(R.string.interval_days, 1), stringResource(R.string.interval_days, 4), stringResource(R.string.interval_days, 9),
        stringResource(R.string.interval_weeks, 3), stringResource(R.string.interval_months, 2),
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(48.dp)) {
                val mod = Modifier.size(22.dp).clip(CircleShape)
                when {
                    i < stageIndex -> Box(mod.background(accent), contentAlignment = Alignment.Center) { Icon(IteraIcons.Check, null, tint = c.surface, modifier = Modifier.size(13.dp)) }
                    i == stageIndex -> Box(mod.background(c.surface).border(3.dp, accent, CircleShape))
                    else -> Box(mod.border(2.dp, c.line, CircleShape))
                }
                Text(label, style = Itera.type.caption.copy(fontWeight = if (i == stageIndex) FontWeight.Bold else FontWeight.Normal), color = if (i <= stageIndex) c.ink else c.ink2, maxLines = 1)
            }
            if (i < steps.lastIndex) Box(Modifier.weight(1f).padding(top = 10.dp).height(2.dp).background(if (i < stageIndex) accent else c.line))
        }
    }
}

// ------------------------------------------------------------------ premortem

@Composable
fun PremortemScreen(onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Reflection.colors(c.isDark)
    val inSixMonths = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ofPattern("LLLL yyyy", currentLocale()))
    var action by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    val reasons = listOf(
        Triple(R.string.pm_reason_1, true, true),
        Triple(R.string.pm_reason_2, true, false),
        Triple(R.string.pm_reason_3, false, false),
    )
    ScreenColumn(gap = 18.dp, bottom = { IteraButton(stringResource(R.string.pm_cta), onDone) }) {
        TopBar(stringResource(Technique.Premortem.title), onClose) { Pill(stringResource(Skill.Reflection.title), sc.container, sc.content) }
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.pm_project) + " ")
                withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) { append(stringResource(R.string.pm_project_value)) }
            },
            style = Itera.type.bodySmall, color = c.ink2,
        )
        Text(stringResource(R.string.pm_title, inSixMonths), style = Itera.type.title, color = c.ink)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reasons.forEachIndexed { i, (res, likely, focused) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(2.dp, if (focused) c.ink else Color.Transparent, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${i + 1}", style = Itera.type.bodySmall.copy(fontWeight = FontWeight.Bold), color = c.ink2)
                    Text(stringResource(res), style = Itera.type.bodySmall.copy(fontWeight = FontWeight.Medium), color = c.ink, modifier = Modifier.weight(1f))
                    if (likely) Pill(stringResource(R.string.pm_likely), sc.container, sc.content)
                    else Pill(stringResource(R.string.pm_possible), c.surface2, c.ink2)
                }
            }
            NoteField(extra, { extra = it }, stringResource(R.string.pm_add), minLines = 1)
        }
        Text(stringResource(R.string.pm_today_q), style = Itera.type.headline, color = c.ink, modifier = Modifier.padding(top = 6.dp))
        NoteField(action, { action = it }, stringResource(R.string.pm_action_hint), bordered = true)
        Text(stringResource(R.string.pm_idea), style = Itera.type.caption, color = c.ink2)
    }
}

// ------------------------------------------------------------------ habit stacking

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitStackScreen(onClose: () -> Unit, onDone: () -> Unit) {
    val c = Itera.colors
    val sc = Skill.Habits.colors(c.isDark)
    val anchors = listOf(R.string.hs_anchor_1, R.string.hs_anchor_2, R.string.hs_anchor_3, R.string.hs_anchor_4)
    val habits = listOf(R.string.hs_habit_1, R.string.hs_habit_2, R.string.hs_habit_3, R.string.hs_habit_4)
    var anchor by remember { mutableIntStateOf(0) }
    var habit by remember { mutableIntStateOf(0) }
    var nudge by remember { mutableStateOf(true) }

    val a = stringResource(anchors[anchor])
    val h = stringResource(habits[habit])
    val sentence = stringResource(R.string.hs_sentence, a, h)
    val slot = SpanStyle(color = sc.content, background = sc.container, textDecoration = TextDecoration.Underline)

    ScreenColumn(gap = 20.dp, bottom = {
        Text(stringResource(R.string.hs_log_hint), style = Itera.type.caption, color = c.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
        IteraButton(stringResource(R.string.hs_save), onDone)
    }) {
        TopBar(stringResource(Technique.HabitStack.title), onClose) { Pill(stringResource(Skill.Habits.title), sc.container, sc.content) }
        Text(
            buildAnnotatedString {
                append(sentence)
                listOf(a, h).forEach { part ->
                    val start = sentence.indexOf(part)
                    if (start >= 0) addStyle(slot, start, start + part.length)
                }
            },
            style = Itera.type.title.copy(lineHeight = Itera.type.title.fontSize * 1.4f),
            color = c.ink,
        )
        Text(stringResource(R.string.hs_sub), style = Itera.type.body, color = c.ink2)
        Text(stringResource(R.string.hs_existing), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            anchors.forEachIndexed { i, res -> ChoiceChip(stringResource(res), i == anchor, onClick = { anchor = i }) }
        }
        Text(stringResource(R.string.hs_new), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            habits.forEachIndexed { i, res -> ChoiceChip(stringResource(res), i == habit, onClick = { habit = i }) }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(IteraIcons.Bell, null, tint = c.ink2, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.hs_nudge), style = Itera.type.body, color = c.ink, modifier = Modifier.weight(1f))
            Switch(
                checked = nudge, onCheckedChange = { nudge = it },
                colors = SwitchDefaults.colors(checkedTrackColor = sc.content, checkedThumbColor = c.surface, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.line),
            )
        }
    }
}
