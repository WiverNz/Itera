package com.wivernz.itera.feature.daycomplete

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill

@Composable
fun DayCompleteRoute(vm: DayCompleteViewModel, onGoodNight: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    DayCompleteScreen(state, onGoodNight)
}

/** End of the day: the ring closes, tomorrow's change is shown back, the next technique previewed. */
@Composable
fun DayCompleteScreen(state: DayCompleteUiState, onGoodNight: () -> Unit) {
    val c = Itera.colors
    val context = LocalContext.current
    val locale = currentLocale()
    ScreenColumn(
        gap = 18.dp,
        modifier = Modifier.testTag("DayComplete"),
        bottom = { IteraButton(stringResource(R.string.good_night), onGoodNight) }
    ) {
        if (state.loading) return@ScreenColumn
        DayRing(
            Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
            state.segments
        )
        Text(
            stringResource(R.string.day_complete_title, state.programDay),
            style = Itera.type.display,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() }
        )
        Text(
            summaryLine(state),
            style = Itera.type.body,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("DaySummary")
        )
        state.change?.let { change ->
            IteraCard(modifier = Modifier.testTag("ChangeCard")) {
                Eyebrow(
                    stringResource(R.string.day_change_label),
                    Skill.REFLECTION.colors(c.isDark).content
                )
                Text(
                    "“$change”",
                    style = Itera.type.headline.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Itera.type.userText.fontFamily
                    ),
                    color = c.ink
                )
                Text(
                    stringResource(R.string.day_change_hint),
                    style = Itera.type.bodySmall,
                    color = c.ink2
                )
            }
        }
        IteraCard(padding = PaddingValues(16.dp), modifier = Modifier.testTag("TomorrowCard")) {
            Row(
                Modifier.semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (val t = state.tomorrow) {
                    is TomorrowPreview.Technique ->
                        TechniqueToken(t.skill, techniqueIcon(t.techniqueId), 48.dp)
                    TomorrowPreview.Combination ->
                        TechniqueToken(Skill.PLANNING, techniqueIcon("pareto_principle"), 48.dp)
                    TomorrowPreview.Practice ->
                        TechniqueToken(
                            Skill.HABITS,
                            techniqueIcon("one_percent_improvement"),
                            48.dp
                        )
                }
                Column {
                    Text(
                        stringResource(R.string.day_tomorrow, state.programDay + 1),
                        style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink2
                    )
                    Text(
                        when (val t = state.tomorrow) {
                            is TomorrowPreview.Technique -> t.name
                            TomorrowPreview.Combination -> stringResource(
                                R.string.today_combination
                            )
                            TomorrowPreview.Practice -> stringResource(
                                R.string.day_tomorrow_practice
                            )
                        },
                        style = Itera.type.label,
                        color = c.ink
                    )
                    Text(
                        stringResource(
                            R.string.day_unlocks,
                            formatTime(state.morningTime, locale, context)
                        ),
                        style = Itera.type.bodySmall,
                        color = c.ink2
                    )
                }
            }
        }
    }
}

/** Honest about what happened: the prototype's full-day line only when it is literally true. */
@Composable
private fun summaryLine(state: DayCompleteUiState): String {
    if (state.fullDay) return stringResource(R.string.day_complete_sub)
    val parts = state.done.map {
        stringResource(
            when (it) {
                DayPartKind.EXERCISE -> R.string.day_part_exercise
                DayPartKind.REVIEW -> R.string.day_part_review
                DayPartKind.FOCUS -> R.string.day_part_focus
                DayPartKind.PRACTICE -> R.string.day_part_practice
                DayPartKind.REFLECTION -> R.string.day_part_reflection
            }
        )
    }.joinToString(", ")
    return stringResource(
        if (state.reflectionSkipped) R.string.day_summary_skipped else R.string.day_summary,
        parts
    )
}

/**
 * One arc per row of the day, drawn in sequence; the prototype's three arcs generalise to the real total (P-01).
 */
@Composable
private fun DayRing(modifier: Modifier, segments: List<RingSegment>) {
    val c = Itera.colors
    val reduced = LocalReduceMotion.current
    val count = segments.size.coerceAtLeast(1)
    val done = segments.count { it.done }
    val description = stringResource(R.string.today_count_a11y, done, segments.size)
    val sweeps = remember(segments) { segments.map { Animatable(if (reduced) 1f else 0f) } }
    LaunchedEffect(segments) {
        sweeps.forEachIndexed { i, a ->
            if (segments[i].done && !reduced) a.animateTo(1f, tween(ARC_MILLIS))
        }
    }
    Box(
        modifier.size(160.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            val slice = 360f / count
            segments.forEachIndexed { i, segment ->
                val sweep = sweeps[i].value
                if (segment.done && sweep > 0f) {
                    drawArc(
                        segment.skill.colors(c.isDark).content,
                        -90f + i * slice + GAP / 2,
                        (slice - GAP) * sweep,
                        false,
                        Offset(inset, inset),
                        arc,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        Text(
            stringResource(R.string.day_ring_count, done, segments.size),
            style = Itera.type.display,
            color = c.ink
        )
    }
}

private const val GAP = 14f
private const val ARC_MILLIS = 500
