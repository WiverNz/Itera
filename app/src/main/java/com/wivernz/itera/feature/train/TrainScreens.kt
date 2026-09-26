@file:Suppress("ktlint:standard:max-line-length", "ModifierParameter")

package com.wivernz.itera.feature.train

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LinkRow
import com.wivernz.itera.core.designsystem.component.MasteryDots
import com.wivernz.itera.core.designsystem.component.MasteryLadder
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ProgressBar
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.SectionTitle
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.screenInsets
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.feature.exercise.runner.levelHint
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Train.kt's current-week path, backed by the stored program day. */
@Composable
fun TrainScreen(state: TrainUiState, onEvent: (TrainUiEvent) -> Unit) {
    val c = Itera.colors
    val list = rememberLazyListState()
    var positioned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.nodes) {
        if (!positioned && !state.loading && state.nodes.isNotEmpty()) {
            // Keep one preceding row as context; bounded by the available scroll range.
            val index = state.nodes.indexOfFirst { it.state == TrainNodeState.TODAY }
            if (index > 1) list.scrollToItem(3 + index - 1)
            positioned = true
        }
    }
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize().background(c.bg).screenInsets().testTag("Train"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Text(stringResource(R.string.train_title), style = Itera.type.display, color = c.ink)
            Spacer(Modifier.height(18.dp))
        }
        item {
            Text(stringResource(R.string.train_sub), style = Itera.type.body, color = c.ink2)
            Spacer(Modifier.height(18.dp))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val name = when (state.week) {
                    1 -> R.string.week_foundations
                    2 -> R.string.week_depth
                    else -> R.string.week_combine
                }
                SectionTitle(
                    stringResource(R.string.train_week, state.week, stringResource(name)),
                    if (state.programDay <=
                        14
                    ) {
                        stringResource(R.string.train_day_of, state.programDay, 14)
                    } else {
                        stringResource(R.string.day_n, state.programDay)
                    }
                )
                if (state.programDay <= 14) ProgressBar(state.programDay / 14f, c.ink)
            }
            Spacer(Modifier.height(18.dp))
        }
        when {
            state.failed -> item {
                ErrorState(
                    stringResource(
                        R.string.error_generic
                    ),
                    action = stringResource(R.string.action_try_again),
                    onAction = {
                        onEvent(TrainUiEvent.Refresh)
                    }
                )
            }
            state.loading -> item { TrainSkeleton() }
            else -> items(state.nodes, key = { it.day }) { node ->
                TrainNodeRow(node) { onEvent(TrainUiEvent.OpenDay(node.day)) }
                Spacer(Modifier.height(2.dp))
            }
        }
        state.review?.let { review ->
            item {
                Spacer(Modifier.height(18.dp))
                val sc = Skill.LEARNING.colors(c.isDark)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(sc.container)
                        .clickable(role = Role.Button) {
                            onEvent(TrainUiEvent.Review)
                        }.testTag("TrainReview").padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TechniqueToken(Skill.LEARNING, techniqueIcon("spaced_repetition"), 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.train_review_due),
                            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = sc.content
                        )
                        Text(
                            if (review.topicId !=
                                null
                            ) {
                                review.prompt
                            } else {
                                stringResource(R.string.review_generic_topic)
                            },
                            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                            color = c.ink
                        )
                        if (state.moreReviews >
                            0
                        ) {
                            Text(
                                pluralStringResource(
                                    R.plurals.train_more_reviews,
                                    state.moreReviews,
                                    state.moreReviews
                                ),
                                style = Itera.type.caption,
                                color = c.ink2
                            )
                        }
                    }
                    Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            LinkRow(
                icon = IteraIcons.Eisenhower,
                title = stringResource(R.string.train_library),
                sub = stringResource(
                    R.string.train_library_sub,
                    state.catalogCount,
                    state.unlockedCount
                ),
                onClick = { onEvent(TrainUiEvent.Library) },
                modifier = Modifier.testTag("TrainLibrary")
            )
        }
    }
}

@Composable
private fun TrainSkeleton() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        show = true
    }
    if (show) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(7) {
                Box(
                    Modifier.fillMaxWidth().height(
                        44.dp
                    ).clip(RoundedCornerShape(16.dp)).background(Itera.colors.surface2)
                )
            }
        }
    }
}

@Composable
private fun TrainNodeRow(node: TrainNode, onClick: () -> Unit) {
    val c = Itera.colors
    val today = node.state == TrainNodeState.TODAY
    val locked = stringResource(R.string.train_locked)
    val t = node.technique
    val skill = t?.skill ?: Skill.PLANNING
    val title =
        t?.name
            ?: stringResource(
                if (node.firstCombination) R.string.train_first_combo else R.string.day_tomorrow_practice
            )
    val modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("TrainDay${node.day}")
        .clip(
            RoundedCornerShape(20.dp)
        ).then(if (today) Modifier.background(c.surface) else Modifier)
        .then(
            if (node.state !=
                TrainNodeState.FUTURE
            ) {
                Modifier.clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier.semantics(mergeDescendants = true) {
                    stateDescription = locked
                    disabled()
                }
            }
        )
        .padding(horizontal = 14.dp, vertical = if (today) 12.dp else 0.dp)
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.day_n, node.day),
            style = Itera.type.caption.copy(
                fontWeight = if (today) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (today) c.accent else c.ink2,
            modifier = Modifier.width(52.dp)
        )
        if (today) {
            TechniqueToken(skill, techniqueIcon(t?.id?.value ?: "pareto_principle"), 40.dp)
        } else {
            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                if (node.state ==
                    TrainNodeState.PAST
                ) {
                    Icon(
                        IteraIcons.Check,
                        null,
                        tint = skill.colors(c.isDark).content,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        Modifier.size(
                            8.dp
                        ).clip(CircleShape).background(skill.colors(c.isDark).content)
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (today) Itera.type.label else Itera.type.body,
                color = if (node.state ==
                    TrainNodeState.PAST
                ) {
                    c.ink2
                } else {
                    c.ink
                }
            )
            if (today) {
                Text(
                    stringResource(R.string.train_today_skill, stringResource(skill.title)),
                    style = Itera.type.caption,
                    color = c.ink2
                )
            }
        }
        if (today) Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(state: LibraryUiState, onEvent: (LibraryUiEvent) -> Unit, onBack: () -> Unit) {
    val c = Itera.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).screenInsets().testTag("Library"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
    ) {
        item {
            TopBar("", onBack, IteraIcons.Back)
            Spacer(Modifier.height(18.dp))
        }
        item {
            Text(stringResource(R.string.library_title), style = Itera.type.display, color = c.ink)
            Spacer(Modifier.height(18.dp))
        }
        item {
            Text(stringResource(R.string.library_sub), style = Itera.type.body, color = c.ink2)
            Spacer(Modifier.height(18.dp))
        }
        stickyHeader {
            Row(
                Modifier.fillMaxWidth().background(
                    c.bg
                ).horizontalScroll(
                    rememberScrollState()
                ).testTag("LibraryFilters").padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoiceChip(
                    stringResource(
                        R.string.filter_all
                    ),
                    state.filter == null,
                    singleSelect = true,
                    onClick = {
                        onEvent(LibraryUiEvent.Filter(null))
                    }
                )
                Skill.entries.forEach { skill ->
                    ChoiceChip(
                        stringResource(skill.title),
                        state.filter == skill,
                        singleSelect = true,
                        onClick = { onEvent(LibraryUiEvent.Filter(skill)) }
                    )
                }
            }
        }
        if (state.failed) {
            item {
                ErrorState(
                    stringResource(
                        R.string.error_generic
                    ),
                    action = stringResource(R.string.action_try_again),
                    onAction = {
                        onEvent(LibraryUiEvent.Retry)
                    }
                )
            }
        }
        items(state.rows, key = { it.technique.id.value }) { row ->
            LibraryTechniqueRow(row) { onEvent(LibraryUiEvent.Open(row.technique.id)) }
            Divider()
        }
    }
}

@Composable
private fun LibraryTechniqueRow(row: LibraryRow, onClick: () -> Unit) {
    val c = Itera.colors
    val t = row.technique
    val announcement = if (!row.unlocked) {
        stringResource(
            R.string.library_locked,
            t.name,
            t.introDay ?: 1
        )
    } else {
        null
    }
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = 72.dp
        ).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (announcement !=
                    null
                ) {
                    contentDescription = announcement
                }
            }
            .testTag(
                "Technique-${t.id.value}"
            ).alpha(if (row.unlocked) 1f else 0.62f).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TechniqueToken(t.skill, techniqueIcon(t.id.value), 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                t.name,
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink
            )
            Text(t.shortDescription, style = Itera.type.caption, color = c.ink2)
            if (LocalDensity.current.fontScale > 1.6f) LibraryRowState(row)
        }
        if (LocalDensity.current.fontScale <= 1.6f) LibraryRowState(row)
    }
}

@Composable
private fun LibraryRowState(row: LibraryRow) {
    val c = Itera.colors
    if (row.unlocked) {
        MasteryDots(row.level, row.technique.skill.colors(c.isDark).content)
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(IteraIcons.Lock, null, tint = c.ink2, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.day_n, row.technique.introDay ?: 1),
                style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink2
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechniqueDetailScreen(
    state: TechniqueDetailUiState,
    onEvent: (TechniqueDetailUiEvent) -> Unit,
    onBack: () -> Unit
) {
    val c = Itera.colors
    val t = state.technique
    ScreenColumn(gap = 22.dp, modifier = Modifier.testTag("TechniqueDetail")) {
        TopBar("", onBack, IteraIcons.Back)
        if (t == null) {
            if (state.failed) {
                ErrorState(
                    stringResource(
                        R.string.error_generic
                    ),
                    action = stringResource(R.string.action_try_again),
                    onAction = {
                        onEvent(TechniqueDetailUiEvent.Retry)
                    }
                )
            }
            return@ScreenColumn
        }
        val sc = t.skill.colors(c.isDark)
        val locked = state.progress?.unlocked != true
        val level = if (locked) MasteryLevel.NONE else state.progress?.level ?: MasteryLevel.NONE
        TechniqueToken(t.skill, techniqueIcon(t.id.value), 64.dp)
        Eyebrow(stringResource(t.skill.title), sc.content)
        Text(t.name, style = Itera.type.hero, color = c.ink)
        Text(t.shortDescription, style = Itera.type.bodyLarge, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.detail_why), style = Itera.type.label, color = c.ink)
            Text(t.explanation, style = Itera.type.body, color = c.ink)
        }
        IteraCard(padding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.detail_level),
                    style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink,
                    modifier = Modifier.weight(1f)
                )
                Pill(stringResource(level.title), sc.container, sc.content)
            }
            MasteryLadder(level, sc.content)
            if (locked) {
                Text(
                    stringResource(R.string.detail_locked, t.introDay ?: 1),
                    style = Itera.type.caption,
                    color = c.ink2
                )
            } else {
                Text(
                    stringResource(
                        R.string.detail_facts,
                        state.progress?.totalUses ?: 0,
                        state.progress?.distinctPracticeDays ?: 0
                    ),
                    style = Itera.type.caption,
                    color = c.ink2
                )
                levelHint(state.progress?.nextLevelHint ?: LevelHint.Unavailable)?.let {
                    Text(it, style = Itera.type.caption, color = c.ink2)
                }
            }
        }
        IteraButton(
            if (locked) {
                stringResource(R.string.detail_locked, t.introDay ?: 1)
            } else if (t.exerciseType ==
                ExerciseType.FOCUS_TIMER
            ) {
                stringResource(
                    R.string.detail_practice_minutes,
                    t.defaults.focusMinutes ?: t.estimatedMinutes
                )
            } else {
                stringResource(R.string.detail_practice)
            },
            {
                onEvent(TechniqueDetailUiEvent.Practice)
            },
            icon = if (locked) IteraIcons.Lock else IteraIcons.Play,
            enabled = !locked && !state.busy,
            modifier = Modifier.testTag("PracticeNow")
        )
        if (state.practiceFailed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
        if (!locked) {
            Column(Modifier.testTag("TechniqueHistory")) {
                Text(
                    stringResource(R.string.detail_history),
                    style = Itera.type.label,
                    color = c.ink,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (state.historyFailed) {
                    ErrorState(
                        stringResource(
                            R.string.error_generic
                        ),
                        action = stringResource(R.string.action_try_again),
                        onAction = {
                            onEvent(TechniqueDetailUiEvent.Retry)
                        }
                    )
                } else if (state.history.isEmpty()) {
                    Text(
                        stringResource(R.string.detail_no_history),
                        style = Itera.type.bodySmall,
                        color = c.ink2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                val format = DateTimeFormatter.ofLocalizedDate(
                    FormatStyle.MEDIUM
                ).withLocale(currentLocale())
                state.history.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            e.date.format(format),
                            style = Itera.type.bodySmall,
                            color = c.ink2,
                            modifier = Modifier.width(110.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                summaryText(e.summary),
                                style = Itera.type.bodySmall.copy(
                                    fontFamily = Itera.type.userText.fontFamily
                                ),
                                color = c.ink
                            )
                            e.note?.takeIf {
                                it.isNotBlank()
                            }?.let {
                                Text(
                                    stringResource(R.string.detail_note, it),
                                    style = Itera.type.userText,
                                    color = c.ink2
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.detail_related), style = Itera.type.label, color = c.ink)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.related.forEach { related ->
                    Row(
                        Modifier.clip(
                            CircleShape
                        ).background(c.surface).clickable(role = Role.Button) {
                            onEvent(TechniqueDetailUiEvent.Related(related.id))
                        }
                            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TechniqueToken(related.skill, techniqueIcon(related.id.value), 40.dp, 20.dp)
                        Text(
                            related.name,
                            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = c.ink
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun summaryText(summary: PracticeSummary): String = when (summary) {
    is PracticeSummary.Review -> stringResource(
        when (summary.grade) {
            com.wivernz.itera.domain.model.RecallGrade.FORGOT -> R.string.review_forgot
            com.wivernz.itera.domain.model.RecallGrade.PARTIAL -> R.string.review_partial
            com.wivernz.itera.domain.model.RecallGrade.SOLID -> R.string.review_solid
        }
    )
    is PracticeSummary.Text -> summary.value
    is PracticeSummary.Topic -> stringResource(R.string.detail_explained, summary.title)
    is PracticeSummary.Habit -> stringResource(
        R.string.detail_habit_summary,
        summary.anchor,
        summary.habit
    )
    is PracticeSummary.Count -> pluralStringResource(
        when (summary.kind) {
            PracticeKind.MINUTES -> R.plurals.detail_minutes
            PracticeKind.TASKS -> R.plurals.detail_tasks
            PracticeKind.REASONS -> R.plurals.detail_reasons
            PracticeKind.STEPS -> R.plurals.detail_steps
            PracticeKind.ITEMS -> R.plurals.detail_items
        },
        summary.count,
        summary.count
    )
}
