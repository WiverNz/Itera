package com.wivernz.itera.feature.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatShortDate
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LinkRow
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.SkillLevel

@Composable
fun ProgressRoute(vm: ProgressViewModel, history: () -> Unit, library: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    ProgressScreen(state, history, library, vm::refresh)
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    history: () -> Unit,
    library: () -> Unit,
    retry: () -> Unit
) {
    val c = Itera.colors
    val locale = currentLocale()
    ScreenColumn(gap = 16.dp) {
        Text(stringResource(R.string.progress_title), style = Itera.type.display, color = c.ink)
        IteraCard(padding = PaddingValues(18.dp), gap = 12.dp) {
            val summary = state.summary
            if (summary != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        pluralStringResource(
                            R.plurals.progress_window_count,
                            summary.windowDays,
                            summary.trainedDays,
                            summary.windowDays
                        ),
                        style = Itera.type.headline,
                        color = c.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(
                            R.string.progress_since,
                            formatShortDate(state.startedOn ?: summary.windowStart, locale)
                        ),
                        style = Itera.type.caption,
                        color = c.ink2
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.dayDots.forEachIndexed { index, day ->
                        Box(
                            Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (day.trained) c.ink else c.surface2)
                                .then(
                                    if (index ==
                                        summary.dayDots.lastIndex
                                    ) {
                                        Modifier.border(2.dp, c.accent, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            } else {
                Skeleton(state.loading, Modifier.fillMaxWidth().height(76.dp))
            }
            Text(
                stringResource(R.string.progress_missed_hint),
                style = Itera.type.caption,
                color = c.ink2
            )
        }
        if (state.failed) {
            ErrorState(
                stringResource(R.string.settings_failed),
                stringResource(R.string.action_retry),
                retry
            )
        }
        Column {
            state.summary?.skills?.forEach { row ->
                val sc = row.skill.colors(c.isDark)
                val level = when (row.level) {
                    SkillLevel.STARTING -> R.string.skill_level_starting
                    SkillLevel.BUILDING -> R.string.skill_level_building
                    SkillLevel.STEADY -> R.string.skill_level_steady
                    SkillLevel.STRONG -> R.string.skill_level_strong
                }
                Column(
                    Modifier.padding(vertical = 10.dp).semantics(mergeDescendants = true) {
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            stringResource(row.skill.title),
                            style = Itera.type.label,
                            color = c.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(level),
                            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = sc.content
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(4) { index ->
                            val fill by animateFloatAsState(
                                if (row.practiceCount > 0 &&
                                    index <= row.level.ordinal
                                ) {
                                    1f
                                } else {
                                    0f
                                },
                                if (LocalReduceMotion.current) {
                                    snap()
                                } else {
                                    tween(
                                        700,
                                        delayMillis =
                                        index * 80
                                    )
                                },
                                label = "skill"
                            )
                            Box(
                                Modifier.weight(
                                    1f
                                ).height(8.dp).clip(CircleShape).background(sc.container)
                                    .drawBehind {
                                        drawRect(
                                            sc.content,
                                            size = size.copy(
                                                width =
                                                size.width * fill
                                            )
                                        )
                                    }
                            )
                        }
                    }
                    Text(
                        pluralStringResource(
                            R.plurals.practices,
                            row.practiceCount,
                            row.practiceCount
                        ),
                        style = Itera.type.caption,
                        color = c.ink2
                    )
                }
            }
        }
        LinkRow(
            IteraIcons.Progress,
            stringResource(R.string.progress_history),
            pluralStringResource(
                R.plurals.history_activities,
                state.summary?.activityCount ?: 0,
                state.summary?.activityCount ?: 0
            ),
            history
        )
        LinkRow(
            IteraIcons.Eisenhower,
            stringResource(R.string.train_library),
            stringResource(R.string.library_sub),
            library
        )
    }
}
