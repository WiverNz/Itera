package com.wivernz.itera.feature.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.CircleIconButton
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LanguagePill
import com.wivernz.itera.core.designsystem.component.LanguageSheet
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.RadioDot
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TimePickerSheet
import com.wivernz.itera.core.designsystem.component.TimeRow
import com.wivernz.itera.core.designsystem.component.description
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TimeBudget
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ welcome

@Composable
fun WelcomeScreen(
    demoAvailable: Boolean,
    busy: Boolean,
    onStart: () -> Unit,
    onDemo: () -> Unit,
    onLanguage: (String) -> Unit = AppLanguage::set
) {
    val c = Itera.colors
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    ScreenColumn(
        modifier = Modifier.testTag("Welcome"),
        bottom = {
            IteraButton(stringResource(R.string.welcome_get_started), onStart, enabled = !busy)
            if (demoAvailable) {
                IteraButton(
                    stringResource(R.string.welcome_demo),
                    onDemo,
                    kind = ButtonKind.Ghost,
                    enabled = !busy
                )
            }
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.ink),
                contentAlignment = Alignment.Center
            ) {
                Icon(IteraIcons.OnePercent, null, tint = c.onInk, modifier = Modifier.size(18.dp))
            }
            Text(
                stringResource(R.string.app_name),
                style = Itera.type.headline,
                color = c.ink,
                modifier = Modifier.padding(start = 10.dp).weight(1f)
            )
            LanguagePill({ showLanguage = true })
        }
        RisingPath()
        Text(
            stringResource(R.string.welcome_title),
            style = Itera.type.hero,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.app_description), style = Itera.type.bodyLarge, color = c.ink2)
    }
    if (showLanguage) {
        LanguageSheet(
            selected = AppLanguage.current(),
            onSelect = onLanguage,
            onDismiss = { showLanguage = false }
        )
    }
}

/** Five technique tokens climbing a dotted path; each pops in with a spring. */
@Composable
private fun RisingPath() {
    val c = Itera.colors
    val reduced = LocalReduceMotion.current
    val nodes = listOf(
        Triple(Skill.HABITS, "two_minute_rule", Offset(0.05f, 0.73f)),
        Triple(Skill.FOCUS, "pomodoro", Offset(0.26f, 0.56f)),
        Triple(Skill.LEARNING, "feynman_technique", Offset(0.47f, 0.39f)),
        Triple(Skill.HABITS, "habit_stacking", Offset(0.68f, 0.26f)),
        Triple(Skill.REFLECTION, "daily_reflection", Offset(0.86f, 0.06f))
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .clearAndSetSemantics { }
            .drawBehind {
                val p = Path().apply {
                    moveTo(size.width * 0.13f, size.height * 0.83f)
                    cubicTo(
                        size.width * 0.3f,
                        size.height * 0.75f,
                        size.width * 0.4f,
                        size.height * 0.55f,
                        size.width * 0.55f,
                        size.height * 0.49f
                    )
                    cubicTo(
                        size.width * 0.7f,
                        size.height * 0.42f,
                        size.width * 0.8f,
                        size.height * 0.3f,
                        size.width * 0.95f,
                        size.height * 0.18f
                    )
                }
                drawPath(
                    p,
                    c.ink3,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 14f))
                    )
                )
            }
    ) {
        val w = maxWidth
        val h = maxHeight
        nodes.forEachIndexed { i, (skill, id, pos) ->
            var shown by remember { mutableStateOf(reduced) }
            LaunchedEffect(Unit) {
                delay(POP_STAGGER * i)
                shown = true
            }
            val scale by animateFloatAsState(
                if (shown) 1f else 0.4f,
                if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "pop$i"
            )
            Box(
                Modifier
                    .offset(x = w * pos.x, y = h * pos.y)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = if (shown) 1f else 0f
                    }
            ) {
                TechniqueToken(skill, techniqueIcon(id), if (i == 4) 64.dp else 56.dp, 20.dp)
            }
        }
    }
}

// ------------------------------------------------------------------ onboarding steps

@Composable
private fun OnboardingHeader(step: Int, onBack: () -> Unit) {
    val c = Itera.colors
    val reduced = LocalReduceMotion.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircleIconButton(IteraIcons.Back, stringResource(R.string.action_back), onBack)
        Row(
            Modifier.weight(1f).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(STEPS) { i ->
                val f by animateFloatAsState(
                    if (i < step) 1f else 0f,
                    if (reduced) snap() else tween(),
                    label = "seg$i"
                )
                Box(
                    Modifier.weight(1f).height(4.dp).clip(CircleShape).background(c.line)
                        .testTag("OnboardingSegment")
                        .drawBehind { drawRect(c.ink, size = size.copy(width = size.width * f)) }
                )
            }
        }
        Text(
            stringResource(R.string.onb_step, step, STEPS),
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink2
        )
    }
}

@Composable
fun GoalsScreen(
    focusAreas: List<Skill>,
    onToggle: (Skill) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val c = Itera.colors
    ScreenColumn(
        modifier = Modifier.testTag("Goals"),
        bottom = {
            IteraButton(
                stringResource(R.string.action_continue),
                onNext,
                enabled = focusAreas.isNotEmpty()
            )
        }
    ) {
        OnboardingHeader(1, onBack)
        Text(
            stringResource(R.string.goals_title),
            style = Itera.type.title,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.goals_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Skill.entries.forEach { skill ->
                val on = skill in focusAreas
                val sc = skill.colors(c.isDark)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .border(
                            2.dp,
                            if (on) c.ink else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .toggleable(value = on, role = Role.Checkbox) { onToggle(skill) }
                        .testTag("Goal" + skill.name)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(
                            40.dp
                        ).clip(RoundedCornerShape(13.dp)).background(sc.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(sc.content))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(skill.title), style = Itera.type.label, color = c.ink)
                        Text(
                            stringResource(skill.description),
                            style = Itera.type.bodySmall,
                            color = c.ink2
                        )
                    }
                    if (on) RadioDot(true)
                }
            }
        }
    }
}

@Composable
fun RhythmScreen(
    morningTime: LocalTime,
    eveningTime: LocalTime,
    timeBudget: TimeBudget,
    showRationale: Boolean,
    onMorning: (LocalTime) -> Unit,
    onEvening: (LocalTime) -> Unit,
    onBudget: (TimeBudget) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val c = Itera.colors
    val context = LocalContext.current
    val locale = currentLocale()
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenColumn(
        modifier = Modifier.testTag("Rhythm"),
        bottom = { IteraButton(stringResource(R.string.action_continue), onNext) }
    ) {
        OnboardingHeader(2, onBack)
        Text(
            stringResource(R.string.rhythm_title),
            style = Itera.type.title,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        IteraCard(padding = PaddingValues(16.dp), gap = 8.dp) {
            TimeRow(
                IteraIcons.Today,
                stringResource(R.string.rhythm_morning),
                stringResource(R.string.rhythm_morning_sub),
                formatTime(morningTime, locale, context),
                { picking = PICK_MORNING }
            )
            Divider()
            TimeRow(
                IteraIcons.Reflection,
                stringResource(R.string.rhythm_evening),
                stringResource(R.string.rhythm_evening_sub),
                formatTime(eveningTime, locale, context),
                { picking = PICK_EVENING }
            )
        }
        if (showRationale) NotificationRationaleCard()
        Text(stringResource(R.string.rhythm_time_q), style = Itera.type.label, color = c.ink)
        Segmented(
            options = listOf(
                TimeBudget.SHORT to stringResource(
                    R.string.minutes_short,
                    TimeBudget.SHORT.minutes
                ),
                TimeBudget.STANDARD to
                    stringResource(R.string.minutes_short, TimeBudget.STANDARD.minutes),
                TimeBudget.LONG to stringResource(R.string.minutes_plus, TimeBudget.LONG.minutes)
            ),
            selected = timeBudget,
            onSelect = onBudget
        )
        Text(
            stringResource(R.string.rhythm_time_hint),
            style = Itera.type.bodySmall,
            color = c.ink2
        )
    }
    when (picking) {
        PICK_MORNING -> TimePickerSheet(
            stringResource(R.string.rhythm_morning),
            morningTime,
            onConfirm = {
                onMorning(it)
                picking = null
            },
            onDismiss = { picking = null }
        )
        PICK_EVENING -> TimePickerSheet(
            stringResource(R.string.rhythm_evening),
            eveningTime,
            onConfirm = {
                onEvening(it)
                picking = null
            },
            onDismiss = { picking = null }
        )
    }
}

/** Production addition (docs/ux/02-screen-specs-onboarding.md section 3): shown only while not granted. */
@Composable
private fun NotificationRationaleCard() {
    val c = Itera.colors
    IteraCard(
        color = c.surface2,
        padding = PaddingValues(16.dp),
        gap = 6.dp,
        modifier = Modifier.testTag("NotificationRationale")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                IteraIcons.Bell,
                null,
                tint = c.ink,
                modifier = Modifier.size(18.dp).padding(top = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.rhythm_notif_title),
                    style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.ink
                )
                Text(
                    stringResource(R.string.rhythm_notif_body),
                    style = Itera.type.bodySmall,
                    color = c.ink2
                )
            }
        }
    }
}

@Composable
fun FirstWeekScreen(
    rows: List<FirstWeekRow>,
    busy: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    val c = Itera.colors
    ScreenColumn(
        gap = 18.dp,
        modifier = Modifier.testTag("FirstWeek"),
        bottom = { IteraButton(stringResource(R.string.week_start), onStart, enabled = !busy) }
    ) {
        OnboardingHeader(3, onBack)
        Text(
            stringResource(R.string.week_title),
            style = Itera.type.title,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(stringResource(R.string.week_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEach { row ->
                val sc = row.skill.colors(c.isDark)
                if (row.day == 1) {
                    Row(
                        Modifier.fillMaxWidth().clip(
                            RoundedCornerShape(20.dp)
                        ).background(c.surface)
                            .testTag("WeekRow")
                            .semantics(mergeDescendants = true) { }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.day_n, row.day),
                            style = Itera.type.caption.copy(fontWeight = FontWeight.Bold),
                            color = c.accent,
                            modifier = Modifier.width(52.dp)
                        )
                        TechniqueToken(row.skill, techniqueIcon(row.techniqueId), 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(row.name, style = Itera.type.label, color = c.ink)
                            Text(
                                stringResource(row.skill.title),
                                style = Itera.type.caption,
                                color = c.ink2
                            )
                        }
                        Pill(stringResource(R.string.nav_today), c.accentSoft, c.accent)
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("WeekRow")
                            .semantics(mergeDescendants = true) { }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.day_n, row.day),
                            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = c.ink2,
                            modifier = Modifier.width(52.dp)
                        )
                        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(sc.content))
                        }
                        Text(
                            if (row.withReview) {
                                stringResource(
                                    R.string.week_row_with_review,
                                    row.name,
                                    stringResource(R.string.week_review_suffix)
                                )
                            } else {
                                row.name
                            },
                            style = Itera.type.body,
                            color = c.ink
                        )
                    }
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(IteraIcons.Reflection, null, tint = c.ink2, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.week_every_evening),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
}

private const val STEPS = 3
private val POP_STAGGER = 120.milliseconds
private const val PICK_MORNING = "morning"
private const val PICK_EVENING = "evening"
