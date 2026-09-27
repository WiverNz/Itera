package com.wivernz.itera.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.StepRow
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.voice.VoiceCommandAction

/**
 * Answers one question: what should I do right now? One hero card with a single action, then the day's steps.
 * Port of the prototype's `TodayScreen` (Today.kt).
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onTarget: (TodayTarget) -> Unit,
    onRetry: () -> Unit,
    onUndoPractice: (Long) -> Unit = {},
    onPracticeMessageShown: () -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    val logged = state.loggedPractice
    val loggedText = logged?.let { stringResource(R.string.today_practice_logged, it.name) }
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(logged) {
        if (logged != null && loggedText != null) {
            val result = snackbar.showSnackbar(
                loggedText,
                undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoPractice(logged.activityId)
            } else {
                onPracticeMessageShown()
            }
        }
    }
    Box(Modifier.fillMaxSize().testTag("Today")) {
        ScreenColumn(gap = 22.dp) {
            Header(state)
            state.carryOver?.let { CarryOver(it) }
            when {
                state.error -> ErrorState(
                    stringResource(R.string.today_error),
                    stringResource(R.string.action_try_again),
                    onRetry,
                    Modifier.testTag("TodayError")
                )
                state.hero != null -> Hero(state.hero, onTarget)
                else -> Skeleton(true, Modifier.fillMaxWidth().height(HERO_SKELETON.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Counter(state)
                if (state.steps.isEmpty() && state.loading) {
                    repeat(3) { Skeleton(true, Modifier.fillMaxWidth().height(56.dp)) }
                }
                state.steps.forEach { step -> Step(step, onTarget) }
            }
            VoiceCommandAction()
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun Header(state: TodayUiState) {
    val c = Itera.colors
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.greeting_morning
            Greeting.AFTERNOON -> R.string.greeting_afternoon
            Greeting.EVENING -> R.string.greeting_evening
        }
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            greeting,
            style = Itera.type.display,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        Pill(stringResource(R.string.day_n, state.programDay), c.surface2, c.ink)
    }
}

/** Last night's plan, carried into the morning. Not tappable. */
@Composable
private fun CarryOver(text: String) {
    val c = Itera.colors
    val label = stringResource(R.string.today_plan_label)
    Row(
        Modifier.testTag("CarryOver").semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            com.wivernz.itera.core.designsystem.icon.IteraIcons.Reflection,
            null,
            tint = Skill.REFLECTION.colors(c.isDark).content,
            modifier = Modifier.size(18.dp).padding(top = 1.dp)
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) {
                    append(label)
                }
                append(" ")
                withStyle(SpanStyle(fontFamily = Itera.type.userText.fontFamily)) { append(text) }
            },
            style = Itera.type.bodySmall,
            color = c.ink2
        )
    }
}

@Composable
private fun Hero(hero: TodayHero, onTarget: (TodayTarget) -> Unit) {
    val c = Itera.colors
    when (hero) {
        is TodayHero.Exercise -> HeroCard(
            eyebrow = stringResource(R.string.today_training),
            eyebrowColor = hero.skill.colors(c.isDark).content,
            badge = if (hero.isNew) stringResource(R.string.badge_new) else null,
            token = { TechniqueToken(hero.skill, techniqueIcon(hero.techniqueId), 60.dp) },
            title = hero.name,
            line = hero.line,
            meta = stringResource(
                R.string.today_about,
                hero.minutes,
                stringResource(hero.skill.title)
            ),
            cta = stringResource(R.string.today_start_exercise),
            onClick = { onTarget(hero.target) }
        )
        is TodayHero.Combination -> HeroCard(
            eyebrow = stringResource(R.string.today_combination),
            eyebrowColor = Skill.PLANNING.colors(c.isDark).content,
            token = { TechniqueToken(Skill.PLANNING, techniqueIcon("pareto_principle"), 60.dp) },
            title = stringResource(
                if (hero.curriculum) R.string.combo_title else R.string.activity_combination_title
            ),
            line = stringResource(
                if (hero.curriculum) R.string.combo_sub else R.string.combo_generated_sub
            ),
            meta = stringResource(
                R.string.today_about,
                hero.minutes,
                stringResource(Skill.PLANNING.title)
            ),
            cta = stringResource(R.string.today_start_exercise),
            onClick = { onTarget(hero.target) }
        )
        is TodayHero.Review -> HeroCard(
            eyebrow = stringResource(R.string.train_review_due),
            eyebrowColor = Skill.LEARNING.colors(c.isDark).content,
            token = { TechniqueToken(hero.skill, techniqueIcon(hero.techniqueId), 60.dp) },
            title = hero.name,
            line = hero.topic?.let { stringResource(R.string.hero_review_line_topic, it) }
                ?: stringResource(R.string.hero_review_line),
            meta = hero.subtitle,
            cta = stringResource(R.string.hero_review_cta),
            onClick = { onTarget(hero.target) }
        )
        is TodayHero.Focus -> HeroCard(
            eyebrow = stringResource(R.string.hero_next_optional),
            eyebrowColor = Skill.FOCUS.colors(c.isDark).content,
            token = { TechniqueToken(Skill.FOCUS, techniqueIcon("deep_work"), 60.dp) },
            title = stringResource(R.string.hero_focus_title, hero.minutes),
            line = stringResource(R.string.hero_focus_line),
            meta = stringResource(
                R.string.hero_minutes_meta,
                hero.minutes,
                stringResource(Skill.FOCUS.title)
            ),
            cta = stringResource(R.string.hero_focus_cta),
            enabled = hero.enabled,
            onClick = { onTarget(hero.target) }
        )
        is TodayHero.Reflection -> HeroCard(
            eyebrow = stringResource(R.string.hero_evening),
            eyebrowColor = Skill.REFLECTION.colors(c.isDark).content,
            token = {
                TechniqueToken(Skill.REFLECTION, techniqueIcon("daily_reflection"), 60.dp)
            },
            title = stringResource(R.string.reflection_label),
            line = stringResource(R.string.hero_reflection_line),
            meta = stringResource(
                R.string.hero_minutes_meta,
                hero.minutes,
                stringResource(Skill.REFLECTION.title)
            ),
            cta = stringResource(R.string.hero_reflection_cta),
            enabled = hero.enabled,
            onClick = { onTarget(hero.target) }
        )
        is TodayHero.DayDone -> HeroCard(
            eyebrow = stringResource(R.string.day_n, hero.programDay),
            eyebrowColor = c.ink2,
            token = {
                TechniqueToken(Skill.HABITS, techniqueIcon("one_percent_improvement"), 60.dp)
            },
            title = stringResource(R.string.hero_done_title),
            line = stringResource(R.string.hero_done_line),
            meta = null,
            // The quiet state offers no new action (docs/ux/03-ux-states.md section 5).
            cta = if (hero.quiet) null else stringResource(R.string.hero_done_cta),
            onClick = { onTarget(hero.target) }
        )
    }
}

@Composable
private fun HeroCard(
    eyebrow: String,
    eyebrowColor: Color,
    token: @Composable () -> Unit,
    title: String,
    line: String,
    meta: String?,
    cta: String?,
    onClick: () -> Unit,
    badge: String? = null,
    enabled: Boolean = true
) {
    val c = Itera.colors
    val large = LocalDensity.current.fontScale > LARGE_FONT_SCALE
    IteraCard(
        radius = 28.dp,
        padding = PaddingValues(22.dp),
        gap = 14.dp,
        modifier = Modifier.testTag("Hero").semantics(mergeDescendants = true) { }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow, eyebrowColor, Modifier.weight(1f))
            if (badge != null) Pill(badge, c.accentSoft, c.accent)
        }
        token()
        Text(title, style = if (large) Itera.type.title else Itera.type.display, color = c.ink)
        Text(line, style = Itera.type.bodyLarge, color = c.ink2)
        if (meta != null) Text(meta, style = Itera.type.bodySmall, color = c.ink2)
        if (cta != null) {
            IteraButton(
                cta,
                onClick,
                enabled = enabled,
                modifier = Modifier.padding(top = 4.dp).testTag("HeroAction")
            )
        }
    }
}

@Composable
private fun Counter(state: TodayUiState) {
    val c = Itera.colors
    val a11y = stringResource(R.string.today_count_a11y, state.completedCount, state.totalCount)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            stringResource(R.string.today_section),
            style = Itera.type.label,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (state.totalCount > 0) {
            Text(
                stringResource(R.string.today_count, state.completedCount, state.totalCount),
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink2,
                modifier = Modifier.testTag("TodayCounter").semantics { contentDescription = a11y }
            )
        }
    }
}

@Composable
private fun Step(step: TodayStep, onTarget: (TodayTarget) -> Unit) {
    val c = Itera.colors
    val context = LocalContext.current
    val locale = currentLocale()
    val subtitle = when (val s = step.subtitle) {
        is StepSubtitle.Planned -> s.text
        is StepSubtitle.Note -> s.text
        StepSubtitle.Done -> stringResource(R.string.step_done)
        StepSubtitle.Skipped -> stringResource(R.string.step_skipped)
        is StepSubtitle.Snoozed ->
            stringResource(R.string.step_snoozed, formatTime(s.until, locale, context))
    }
    StepRow(
        state = step.state,
        title = step.title,
        subtitle = subtitle,
        accent = step.accent?.colors(c.isDark)?.content ?: c.line,
        onClick = step.target?.let { target -> { onTarget(target) } },
        modifier = Modifier.testTag("Step")
    )
}

private const val HERO_SKELETON = 320
private const val LARGE_FONT_SCALE = 1.5f
