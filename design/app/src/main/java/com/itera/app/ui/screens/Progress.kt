package com.itera.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.Skill
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

/**
 * Progress reflects real practice only: finished exercises and regularity.
 * No points, no streak pressure; missed days never reset anything.
 */
@Composable
fun ProgressScreen(vm: AppViewModel, onHistory: () -> Unit, onLibrary: () -> Unit) {
    val c = Itera.colors
    val today = LocalDate.now()
    val span = (ChronoUnit.DAYS.between(vm.startDate, today).toInt() + 1).coerceIn(1, 14)
    val days = (span - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val practicedDays = days.count { d -> vm.log.any { it.date == d } }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM", currentLocale())

    ScreenColumn(gap = 16.dp) {
        Text(stringResource(R.string.progress_title), style = Itera.type.display, color = c.ink)
        IteraCard(padding = PaddingValues(18.dp), gap = 12.dp) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.progress_days, practicedDays, span), style = Itera.type.headline, color = c.ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.progress_since, vm.startDate.format(dateFormat)), style = Itera.type.caption, color = c.ink2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                days.forEach { d ->
                    val on = vm.log.any { it.date == d }
                    Box(
                        Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (on) c.ink else c.surface2)
                            .then(if (d == today) Modifier.border(2.dp, c.accent, RoundedCornerShape(8.dp)) else Modifier),
                    )
                }
            }
            Text(stringResource(R.string.progress_missed_hint), style = Itera.type.caption, color = c.ink2)
        }
        Column {
            Skill.entries.forEach { skill ->
                val sc = skill.colors(c.isDark)
                val entries = vm.log.filter { it.technique.skill == skill && it.date in days }
                val count = entries.size
                val activeDays = entries.map { it.date }.distinct().size
                // Level = depth (practices) + regularity (distinct days), four honest steps.
                val score = count + activeDays
                val level = when {
                    score >= 20 -> 4
                    score >= 10 -> 3
                    score >= 4 -> 2
                    score >= 1 -> 1
                    else -> 0
                }
                val levelName = stringResource(
                    when (level) {
                        4 -> R.string.skill_level_strong
                        3 -> R.string.skill_level_steady
                        2 -> R.string.skill_level_building
                        else -> R.string.skill_level_starting
                    },
                )
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(skill.title), style = Itera.type.label, color = c.ink, modifier = Modifier.weight(1f))
                        Text(levelName, style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = sc.content)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(4) { i ->
                            val f by animateFloatAsState(if (i < level) 1f else 0f, tween(700, delayMillis = 80 * i), label = "skill$i")
                            Box(
                                Modifier.weight(1f).height(8.dp).clip(CircleShape).background(sc.container)
                                    .drawBehind { drawRect(sc.content, size = size.copy(width = size.width * f)) },
                            )
                        }
                    }
                    Text(pluralStringResource(R.plurals.practices, count, count), style = Itera.type.caption, color = c.ink2)
                }
            }
        }
        LinkRow(IteraIcons.Progress, stringResource(R.string.progress_history), pluralStringResource(R.plurals.history_activities, vm.log.size, vm.log.size), onHistory)
        LinkRow(IteraIcons.Eisenhower, stringResource(R.string.train_library), stringResource(R.string.library_sub), onLibrary)
    }
}

// ------------------------------------------------------------------ history

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onBack: () -> Unit) {
    val c = Itera.colors
    val locale = currentLocale()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val today = LocalDate.now()
    val first = maxOf(vm.startDate, month.atDay(1))
    val last = minOf(today, month.atEndOfMonth())
    val days = if (first > last) emptyList() else generateSequence(last) { it.minusDays(1).takeIf { d -> d >= first } }.toList()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val entry = selected?.let { vm.log.getOrNull(it) }
    if (entry != null) {
        BackHandler { selected = null }
        ScreenColumn {
            TopBar("", { selected = null }, IteraIcons.Back)
            Text(stringResource(entry.technique.title), style = Itera.type.display, color = c.ink)
            if (entry.note.isNotBlank()) Text(entry.note, style = Itera.type.userText, color = c.ink2)
        }
        return
    }
    val firstDow = WeekFields.of(locale).firstDayOfWeek
    val offset = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val dayFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val monthLabel = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
    LazyColumn(state = list, modifier = Modifier.fillMaxSize().background(c.bg).safeDrawingPadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "calendar") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TopBar("", onBack, IteraIcons.Back)
                Text(stringResource(R.string.history_title), style = Itera.type.display, color = c.ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(monthLabel, style = Itera.type.headline, color = c.ink, modifier = Modifier.weight(1f))
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() }, enabled = month > YearMonth.from(vm.startDate)) { Icon(IteraIcons.Back, stringResource(R.string.history_previous), tint = if (month > YearMonth.from(vm.startDate)) c.ink else c.ink3) }
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() }, enabled = month < YearMonth.from(today)) { Icon(IteraIcons.Chevron, stringResource(R.string.history_next), tint = if (month < YearMonth.from(today)) c.ink else c.ink3) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row { repeat(7) { i -> Text(firstDow.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
                    repeat((offset + month.lengthOfMonth() + 6) / 7) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { col ->
                                val num = row * 7 + col - offset + 1
                                Box(Modifier.weight(1f).aspectRatio(0.95f), contentAlignment = Alignment.Center) {
                                    if (num in 1..month.lengthOfMonth()) {
                                        val date = month.atDay(num)
                                        val skills = Skill.entries.filter { it in vm.practicedOn(date) }.take(3)
                                        Column(Modifier.fillMaxWidth().padding(1.dp).clip(RoundedCornerShape(12.dp))
                                            .background(if (skills.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else c.surface)
                                            .then(if (date == today) Modifier.border(2.dp, c.ink, RoundedCornerShape(12.dp)) else Modifier)
                                            .clickable(enabled = date in days) { scope.launch { list.scrollToItem(1 + days.indexOf(date)) } }
                                            .padding(vertical = 6.dp).semantics { contentDescription = date.format(dayFormat) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(num.toString(), style = Itera.type.bodySmall, color = if (date > today) c.ink3 else c.ink)
                                            Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { skills.forEach { skill -> Box(Modifier.size(5.dp).clip(CircleShape).background(skill.colors(c.isDark).content)) } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Skill.entries.forEach { skill -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(skill.colors(c.isDark).content))
                        Text(stringResource(skill.title), style = Itera.type.caption, color = c.ink2)
                    } }
                }
                if (vm.log.none { YearMonth.from(it.date) == month }) Text(stringResource(R.string.history_empty_month, monthLabel), style = Itera.type.bodySmall, color = c.ink2)
            }
        }
        days.forEach { date -> item(key = date.toString()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (date == today) stringResource(R.string.nav_today) else date.format(dayFormat), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, modifier = Modifier.padding(top = 8.dp))
                val entries = vm.log.withIndex().filter { it.value.date == date }
                if (entries.isEmpty()) Text(stringResource(R.string.history_rest), style = Itera.type.bodySmall, color = c.ink2)
                entries.forEach { (index, entry) ->
                    Row(Modifier.fillMaxWidth().clickable { selected = index }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TechniqueToken(entry.technique, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(entry.technique.title), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                            if (entry.note.isNotBlank()) Text(entry.note, style = Itera.type.userText, color = c.ink2)
                        }
                    }
                }
                Divider()
            }
        } }
    }
}
