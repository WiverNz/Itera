package com.itera.app.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.Mastery
import com.itera.app.model.Program
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.ui.components.ChoiceChip
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.MasteryLadder
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ProgressBar
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.SectionTitle
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ------------------------------------------------------------------ Train tab

/** The program path for the current week, reviews due, and the (secondary) library. */
@Composable
fun TrainScreen(
    vm: AppViewModel,
    onToday: () -> Unit,
    onReview: () -> Unit,
    onLibrary: () -> Unit,
    onTechnique: (Technique) -> Unit,
) {
    val c = Itera.colors
    val week = (vm.programDay - 1) / Program.WEEK_LENGTH + 1
    val weekName = stringResource(
        when (week) {
            1 -> R.string.week_foundations
            2 -> R.string.week_depth
            else -> R.string.week_combine
        },
    )
    val firstDay = (week - 1) * Program.WEEK_LENGTH + 1
    val unlocked = Technique.entries.count { vm.isUnlocked(it) }

    ScreenColumn(gap = 18.dp) {
        Text(stringResource(R.string.train_title), style = Itera.type.display, color = c.ink)
        Text(stringResource(R.string.train_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.train_week, week, weekName), if (vm.programDay <= 14) stringResource(R.string.train_day_of, vm.programDay, Program.COMBINATION_DAY) else stringResource(R.string.day_n, vm.programDay))
            if (vm.programDay <= 14) ProgressBar(vm.programDay.toFloat() / Program.COMBINATION_DAY, c.ink)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (day in firstDay until firstDay + Program.WEEK_LENGTH) {
                val t = Program.techniqueFor(day)
                val label = stringResource(R.string.day_n, day)
                val lockedLabel = stringResource(R.string.train_locked)
                when {
                    day == vm.programDay -> Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
                            .clickable(role = Role.Button, onClick = onToday).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(label, style = Itera.type.caption.copy(fontWeight = FontWeight.Bold), color = c.accent, modifier = Modifier.width(52.dp))
                        TechniqueToken(t ?: Technique.Pareto, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(t?.let { stringResource(it.title) } ?: stringResource(R.string.train_first_combo), style = Itera.type.label, color = c.ink)
                            Text(stringResource(R.string.nav_today) + " · " + stringResource((t?.skill ?: Skill.Planning).title), style = Itera.type.caption, color = c.ink2)
                        }
                        Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
                    }
                    else -> Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .then(if (t != null && day < vm.programDay) Modifier.clickable(role = Role.Button) { onTechnique(t) } else Modifier.semantics(mergeDescendants = true) { stateDescription = lockedLabel; disabled() })
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(label, style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, modifier = Modifier.width(52.dp))
                        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                            val color = (t?.skill ?: Skill.Planning).colors(c.isDark).content
                            if (day < vm.programDay) Icon(IteraIcons.Check, null, tint = color, modifier = Modifier.size(20.dp))
                            else Box(Modifier.size(8.dp).clip(CircleShape).background(if (t == null) c.ink else color))
                        }
                        Text(
                            t?.let { stringResource(it.title) } ?: stringResource(R.string.train_first_combo),
                            style = Itera.type.body, color = if (day < vm.programDay) c.ink2 else c.ink,
                        )
                    }
                }
            }
        }
        // spaced repetition: only when something is due
        if (vm.practiceCount(Technique.Feynman) > 0 || vm.programDay >= 7) {
            val sc = Skill.Learning.colors(c.isDark)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(sc.container)
                    .clickable(role = Role.Button, onClick = onReview).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TechniqueToken(Technique.Spaced, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.train_review_due), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = sc.content)
                    Text(stringResource(R.string.train_review_item), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                }
                Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
            }
        }
        LinkRow(IteraIcons.Eisenhower, stringResource(R.string.train_library), stringResource(R.string.train_library_sub, Technique.entries.size, unlocked), onLibrary)
    }
}

@Composable
fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, onClick: () -> Unit) {
    val c = Itera.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
        }
        Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
    }
}

// ------------------------------------------------------------------ library

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(vm: AppViewModel, onBack: () -> Unit, onTechnique: (Technique) -> Unit) {
    val c = Itera.colors
    var filter by remember { mutableStateOf<Skill?>(null) }
    val list = Technique.entries
        .filter { filter == null || it.skill == filter }
        .sortedWith(compareBy<Technique>({ !vm.isUnlocked(it) }, { Program.unlockDay(it) ?: 0 }))
    LazyColumn(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
        item { TopBar("", onBack, IteraIcons.Back); Spacer(Modifier.height(18.dp)) }
        item { Text(stringResource(R.string.library_title), style = Itera.type.display, color = c.ink); Spacer(Modifier.height(18.dp)) }
        item { Text(stringResource(R.string.library_sub), style = Itera.type.body, color = c.ink2); Spacer(Modifier.height(18.dp)) }
        stickyHeader { Row(Modifier.fillMaxWidth().background(c.bg).horizontalScroll(rememberScrollState()).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.filter_all), filter == null, onClick = { filter = null }, singleSelect = true)
            Skill.entries.forEach { s -> ChoiceChip(stringResource(s.title), filter == s, onClick = { filter = s }, singleSelect = true) }
        }
        }
        items(list, key = { it.name }) { t ->
                val locked = !vm.isUnlocked(t)
                val level = vm.masteryOf(t)
                val announcement = if (locked) stringResource(R.string.library_locked, stringResource(t.title), Program.unlockDay(t) ?: 1) else null
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(role = Role.Button) { onTechnique(t) }
                        .semantics(mergeDescendants = true) { if (announcement != null) contentDescription = announcement }.alpha(if (locked) 0.62f else 1f).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    TechniqueToken(t, 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(t.title), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                        Text(stringResource(t.short), style = Itera.type.caption, color = c.ink2)
                        if (LocalDensity.current.fontScale > 1.6f) LibraryState(t, locked, level)
                    }
                    if (LocalDensity.current.fontScale <= 1.6f) LibraryState(t, locked, level)
                }
                Divider()
        }
    }
}

@Composable
private fun LibraryState(t: Technique, locked: Boolean, level: Mastery?) {
    val c = Itera.colors
    if (locked) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(IteraIcons.Lock, null, tint = c.ink2, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.day_n, Program.unlockDay(t) ?: 1), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
    } else MasteryDots(level, t.skill.colors(c.isDark).content)
}

@Composable
fun MasteryDots(level: Mastery?, color: androidx.compose.ui.graphics.Color) {
    val c = Itera.colors
    val reached = level?.ordinal?.plus(1) ?: 0
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < reached) color else c.line)) }
        }
        Text(level?.let { stringResource(it.title) } ?: stringResource(R.string.not_started), style = Itera.type.caption.copy(fontSize = Itera.type.caption.fontSize * 0.85f), color = c.ink2)
    }
}

// ------------------------------------------------------------------ technique detail

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechniqueDetailScreen(
    vm: AppViewModel,
    technique: Technique,
    onBack: () -> Unit,
    onPractice: () -> Unit,
    onTechnique: (Technique) -> Unit,
) {
    val c = Itera.colors
    val sc = technique.skill.colors(c.isDark)
    val level = vm.masteryOf(technique)
    val locked = !vm.isUnlocked(technique)
    val history = vm.log.filter { it.technique == technique }.take(5)
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale())

    ScreenColumn(gap = 22.dp) {
        TopBar("", onBack, IteraIcons.Back)
        TechniqueToken(technique, 64.dp)
        Eyebrow(stringResource(technique.skill.title), sc.content)
        Text(stringResource(technique.title), style = Itera.type.hero, color = c.ink)
        Text(stringResource(technique.short), style = Itera.type.bodyLarge, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.detail_why), style = Itera.type.label, color = c.ink)
            Text(stringResource(technique.why), style = Itera.type.body, color = c.ink)
        }
        IteraCard(padding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.detail_level), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
                Pill(level?.let { stringResource(it.title) } ?: stringResource(R.string.not_started), sc.container, sc.content)
            }
            MasteryLadder(if (locked) null else level, sc.content)
            if (!locked) Text(stringResource(R.string.detail_facts, vm.log.count { it.technique == technique }, vm.log.filter { it.technique == technique }.map { it.date }.distinct().size), style = Itera.type.caption, color = c.ink2)
            Text(
                if (locked) stringResource(R.string.detail_locked, Program.unlockDay(technique) ?: 1) else when (level) {
                    Mastery.Met -> pluralStringResource(R.plurals.level_hint_days, (3 - vm.log.filter { it.technique == technique }.map { it.date }.distinct().size).coerceAtLeast(0), (3 - vm.log.filter { it.technique == technique }.map { it.date }.distinct().size).coerceAtLeast(0), stringResource(Mastery.Practiced.title))
                    Mastery.Practiced -> stringResource(R.string.level_hint_applied_span, stringResource(Mastery.Applied.title))
                    Mastery.Applied -> stringResource(R.string.level_hint_integrated, stringResource(Mastery.Integrated.title))
                    Mastery.Integrated -> stringResource(R.string.level_hint_top)
                    null -> ""
                },
                style = Itera.type.caption, color = c.ink2,
            )
        }
        IteraButton(
            if (locked) stringResource(R.string.detail_locked, Program.unlockDay(technique) ?: 1)
            else if (technique == Technique.Pomodoro || technique == Technique.DeepWork) stringResource(R.string.detail_practice_minutes, if (technique == Technique.Pomodoro) 25 else 50)
            else stringResource(R.string.detail_practice),
            onPractice,
            icon = if (locked) IteraIcons.Lock else IteraIcons.Play,
            enabled = !locked,
        )
        if (!locked) Column {
            Text(stringResource(R.string.detail_history), style = Itera.type.label, color = c.ink, modifier = Modifier.padding(bottom = 4.dp))
            if (history.isEmpty()) {
                Text(stringResource(R.string.detail_no_history), style = Itera.type.bodySmall, color = c.ink2, modifier = Modifier.padding(vertical = 8.dp))
            }
            history.forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(e.date.format(dateFormat), style = Itera.type.bodySmall, color = c.ink2, modifier = Modifier.width(110.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(e.technique.title), style = Itera.type.bodySmall, color = c.ink)
                        if (e.note.isNotBlank()) Text(stringResource(R.string.detail_note, e.note), style = Itera.type.bodySmall, color = c.ink2)
                    }
                }
                Divider()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.detail_related), style = Itera.type.label, color = c.ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                technique.related.forEach { r ->
                    Row(
                        Modifier.clip(CircleShape).background(c.surface).clickable(role = Role.Button) { onTechnique(r) }
                            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TechniqueToken(r, 40.dp, 20.dp)
                        Text(stringResource(r.title), style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                    }
                }
            }
        }
    }
}
