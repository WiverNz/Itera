package com.itera.app.ui.screens

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
                val entries = vm.log.filter { it.technique.skill == skill }
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
        LinkRow(IteraIcons.Progress, stringResource(R.string.progress_history), stringResource(R.string.progress_history_sub), onHistory)
        LinkRow(IteraIcons.Eisenhower, stringResource(R.string.train_library), stringResource(R.string.library_sub), onLibrary)
    }
}

// ------------------------------------------------------------------ history

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(vm: AppViewModel, onBack: () -> Unit) {
    val c = Itera.colors
    val locale = currentLocale()
    val month = YearMonth.now()
    val today = LocalDate.now()
    val firstDow = WeekFields.of(locale).firstDayOfWeek
    val offset = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val weekdays = (0 until 7).map { firstDow.plus(it.toLong()) }
    val dayFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)

    ScreenColumn(gap = 16.dp) {
        TopBar("", onBack, IteraIcons.Back)
        Text(stringResource(R.string.history_title), style = Itera.type.display, color = c.ink)
        Text(
            month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
            style = Itera.type.headline, color = c.ink,
        )
        // calendar grid (7 columns), dots coloured by skill
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row { weekdays.forEach { d -> Text(d.getDisplayName(TextStyle.NARROW, locale), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
            val cells = offset + month.lengthOfMonth()
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val dayNum = r * 7 + col - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(0.95f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNum)
                                val skills = vm.practicedOn(date)
                                val label = date.format(dayFormat)
                                Column(
                                    Modifier.fillMaxWidth().padding(1.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (skills.isNotEmpty()) c.surface else androidx.compose.ui.graphics.Color.Transparent)
                                        .then(if (date == today) Modifier.border(2.dp, c.ink, RoundedCornerShape(12.dp)) else Modifier)
                                        .padding(vertical = 6.dp)
                                        .semantics { contentDescription = label },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text("$dayNum", style = Itera.type.bodySmall.copy(fontWeight = if (skills.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal),
                                        color = if (date.isAfter(today)) c.ink3 else c.ink)
                                    Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        skills.take(3).forEach { s -> Box(Modifier.size(5.dp).clip(CircleShape).background(s.colors(c.isDark).content)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Skill.entries.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(s.colors(c.isDark).content))
                    Text(stringResource(s.title), style = Itera.type.caption, color = c.ink2)
                }
            }
        }
        // log, grouped by day
        if (vm.log.isEmpty()) {
            Text(stringResource(R.string.history_empty), style = Itera.type.body, color = c.ink2)
        }
        val byDay = vm.log.groupBy { it.date }.toSortedMap(compareByDescending { it })
        val oldest = byDay.keys.minOrNull()
        var d = today
        while (oldest != null && !d.isBefore(oldest)) {
            val entries = byDay[d]
            Text(d.format(dayFormat).replaceFirstChar { it.titlecase(locale) }, style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, modifier = Modifier.padding(top = 8.dp))
            if (entries == null) {
                Text(stringResource(R.string.history_rest), style = Itera.type.bodySmall, color = c.ink2)
            } else {
                entries.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TechniqueToken(e.technique, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(e.technique.title), style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                            if (e.note.isNotBlank()) Text(e.note, style = Itera.type.bodySmall, color = c.ink2)
                        }
                    }
                }
            }
            Divider()
            d = d.minusDays(1)
        }
    }
}
