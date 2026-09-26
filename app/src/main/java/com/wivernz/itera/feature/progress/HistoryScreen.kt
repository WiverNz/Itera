@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.progress

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.firstDayOfWeek
import com.wivernz.itera.core.common.time.formatDateFull
import com.wivernz.itera.core.common.time.formatMonthYear
import com.wivernz.itera.core.common.time.weekdayNarrow
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.description
import com.wivernz.itera.core.designsystem.component.screenInsets
import com.wivernz.itera.core.designsystem.component.summaryText
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.practiceSummary
import kotlinx.coroutines.launch

@Composable
fun HistoryRoute(vm: HistoryViewModel, back: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    HistoryScreen(state, vm::move, vm::refresh, back)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    move: (Long) -> Unit,
    retry: () -> Unit,
    back: () -> Unit
) {
    val c = Itera.colors
    val locale = currentLocale()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    val entry = state.days.flatMap { it.entries }.firstOrNull { it.activity.id == selected }
    if (entry != null) {
        BackHandler { selected = null }
        HistoryResultScreen(entry.activity) { selected = null }
        return
    }
    val dayIndices = remember(state.days) {
        var index = 1
        state.days.associate { day ->
            (day.date to index).also {
                index +=
                    2 + maxOf(1, day.entries.size)
            }
        }
    }
    LaunchedEffect(state.selectedDate, state.loading) {
        state.selectedDate?.let { dayIndices[it] }?.let { list.scrollToItem(it) }
    }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(c.bg).screenInsets(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "calendar") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TopBar("", back, IteraIcons.Back)
                Text(
                    stringResource(R.string.history_title),
                    style = Itera.type.display,
                    color = c.ink
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatMonthYear(state.month, locale),
                        style = Itera.type.headline,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    MonthButton(false, state.previousEnabled) { move(-1) }
                    MonthButton(true, state.nextEnabled) { move(1) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        repeat(7) { index ->
                            Text(
                                weekdayNarrow(firstDayOfWeek(locale).plus(index.toLong()), locale),
                                style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = c.ink2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    calendarCells(state.month, locale).chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { date ->
                                val day = state.days.firstOrNull { it.date == date }
                                val description = if (date ==
                                    null
                                ) {
                                    ""
                                } else {
                                    stringResource(
                                        R.string.history_cell,
                                        formatDateFull(date, locale),
                                        day?.entries?.size ?: 0,
                                        day?.skills.orEmpty().map {
                                            stringResource(it.title)
                                        }.joinToString(", ")
                                    )
                                }
                                Box(
                                    Modifier.weight(1f).aspectRatio(0.95f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date !=
                                        null
                                    ) {
                                        Column(
                                            Modifier.fillMaxWidth().padding(
                                                1.dp
                                            ).clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (day?.skills?.isNotEmpty() ==
                                                        true
                                                    ) {
                                                        c.surface
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                )
                                                .then(
                                                    if (date ==
                                                        state.today
                                                    ) {
                                                        Modifier.border(
                                                            2.dp,
                                                            c.ink,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable(
                                                    enabled = day != null,
                                                    role = Role.Button
                                                ) {
                                                    dayIndices[date]?.let {
                                                        scope.launch { list.scrollToItem(it) }
                                                    }
                                                }
                                                .padding(
                                                    vertical = 6.dp
                                                ).semantics(mergeDescendants = true) {
                                                    contentDescription =
                                                        description
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                date.dayOfMonth.toString(),
                                                style = Itera.type.bodySmall,
                                                color = if (date >
                                                    state.today
                                                ) {
                                                    c.ink3
                                                } else {
                                                    c.ink
                                                }
                                            )
                                            Row(
                                                Modifier.height(5.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                day?.skills?.forEach { skill ->
                                                    Box(
                                                        Modifier.size(
                                                            5.dp
                                                        ).clip(
                                                            CircleShape
                                                        ).background(skill.colors(c.isDark).content)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Skill.entries.forEach { skill ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier.size(
                                    8.dp
                                ).clip(CircleShape).background(skill.colors(c.isDark).content)
                            )
                            Text(
                                stringResource(skill.title),
                                style = Itera.type.caption,
                                color = c.ink2
                            )
                        }
                    }
                }
                if (state.loading) Skeleton(true, Modifier.fillMaxWidth().height(60.dp))
                if (state.failed) {
                    ErrorState(
                        stringResource(R.string.settings_failed),
                        stringResource(R.string.action_retry),
                        retry
                    )
                }
                if (!state.loading &&
                    state.days.all { it.entries.isEmpty() }
                ) {
                    Text(
                        stringResource(
                            R.string.history_empty_month,
                            formatMonthYear(state.month, locale)
                        ),
                        style = Itera.type.bodySmall,
                        color = c.ink2
                    )
                }
            }
        }
        state.days.forEach { day ->
            item(key = "day${day.date}") {
                Text(
                    if (day.date ==
                        state.today
                    ) {
                        stringResource(R.string.nav_today)
                    } else {
                        formatDateFull(day.date, locale)
                    },
                    style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink2,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (day.entries.isEmpty()) {
                item(key = "rest${day.date}") {
                    Text(
                        stringResource(R.string.history_rest),
                        style = Itera.type.bodySmall,
                        color = c.ink2
                    )
                }
            }
            items(day.entries, key = { it.activity.id }) { row ->
                val activity = row.activity
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) {
                        selected = activity.id
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    state.techniques[activity.techniqueId]?.let {
                        TechniqueToken(it.skill, techniqueIcon(it.id.value), 40.dp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            activity.title,
                            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = c.ink
                        )
                        Text(
                            if (activity.state ==
                                ActivityState.SKIPPED
                            ) {
                                stringResource(R.string.history_skipped)
                            } else {
                                summaryText(practiceSummary(activity.result, activity.title))
                            },
                            style = Itera.type.userText,
                            color = c.ink2
                        )
                        activity.note?.takeIf {
                            it.isNotBlank()
                        }?.let { Text(it, style = Itera.type.userText, color = c.ink2) }
                    }
                }
            }
            item(key = "divider${day.date}") { Divider() }
        }
    }
}

@Composable
private fun MonthButton(next: Boolean, enabled: Boolean, click: () -> Unit) {
    val label = stringResource(if (next) R.string.history_next else R.string.history_previous)
    val disabled = stringResource(R.string.history_boundary)
    IconButton(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.semantics {
            contentDescription =
                label
            if (!enabled) stateDescription = disabled
        }
    ) {
        Icon(
            if (next) IteraIcons.Chevron else IteraIcons.Back,
            null,
            tint = if (enabled) Itera.colors.ink else Itera.colors.ink3
        )
    }
}

@Composable
fun HistoryResultScreen(activity: PlanActivity, back: () -> Unit) {
    ScreenColumn {
        TopBar("", back, IteraIcons.Back)
        Text(activity.title, style = Itera.type.display, color = Itera.colors.ink)
        if (activity.state ==
            ActivityState.SKIPPED
        ) {
            Text(
                stringResource(R.string.history_skipped),
                style = Itera.type.body,
                color = Itera.colors.ink2
            )
        } else {
            Text(
                summaryText(practiceSummary(activity.result, activity.title)),
                style = Itera.type.userText,
                color = Itera.colors.ink
            )
        }
        resultText(activity.result).forEach {
            Text(it, style = Itera.type.userText, color = Itera.colors.ink)
        }
        activity.note?.let { Text(it, style = Itera.type.userText, color = Itera.colors.ink2) }
    }
}

/** User-authored fields only; labels and enum values are rendered separately in the current locale. */
fun resultText(result: ActivityResult?): List<String> = when (result) {
    is ActivityResult.Focus -> listOf(result.taskLabel)
    is ActivityResult.Reflection -> listOfNotNull(
        result.wentWell,
        result.didNotGoWell,
        result.tomorrowChange
    )
    is ActivityResult.Feynman -> listOfNotNull(
        result.topicTitle,
        result.explanation,
        result.reflectionNote
    )
    is ActivityResult.Premortem -> listOfNotNull(result.projectName) +
        result.reasons.map { it.text } +
        listOfNotNull(result.mitigationAction)
    is ActivityResult.Eisenhower -> result.items.map { it.label }
    is ActivityResult.Review -> listOf(result.answer, result.previousAnswer)
    is ActivityResult.HabitStack -> listOf(result.anchor, result.habit)
    is ActivityResult.Combination -> result.stepResults.map { it.summary }
    is ActivityResult.Template -> result.values.values.flatMap { value ->
        when (value) {
            is BlockValue.Text -> listOf(value.text)
            is BlockValue.Items -> value.items.map { it.label }
            is BlockValue.Choice -> value.options
            is BlockValue.Lists -> value.primary + value.secondary
            is BlockValue.Chips -> listOfNotNull(value.custom)
        }
    }
    null -> emptyList()
}
