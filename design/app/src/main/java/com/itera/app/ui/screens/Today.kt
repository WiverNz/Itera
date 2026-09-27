package com.itera.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.DayStep
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.ui.components.VoiceCommandPill
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.SectionTitle
import com.itera.app.ui.components.StepRow
import com.itera.app.ui.components.StepState
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.LocalTime

/**
 * The main screen. Answers one question: what should I do right now?
 * One hero card with a single action, then a short list of today's three steps.
 */
@Composable
fun TodayScreen(
    vm: AppViewModel,
    onExercise: (Technique) -> Unit,
    onCombination: () -> Unit,
    onFocus: (Int) -> Unit,
    onReflection: () -> Unit,
    onWrapUp: () -> Unit,
) {
    val c = Itera.colors
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greeting_morning
            hour < 18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    val technique = vm.todaysTechnique
    val exerciseDone = DayStep.Exercise in vm.completed
    val focusDone = DayStep.Focus in vm.completed
    val reflectionDone = DayStep.Reflection in vm.completed
    val focusSkill = Skill.Focus.colors(c.isDark).content
    val reflectionColor = Skill.Reflection.colors(c.isDark).content
    val exerciseColor = (technique?.skill ?: Skill.Planning).colors(c.isDark).content
    val startExercise = { if (technique != null) onExercise(technique) else onCombination() }

    ScreenColumn(gap = 22.dp) {
        // header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(greeting, style = Itera.type.display, color = c.ink, modifier = Modifier.weight(1f))
            Pill(stringResource(R.string.day_n, vm.programDay), c.surface2, c.ink)
        }

        // last night's plan, carried into the morning
        if (vm.carriedChange.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(IteraIcons.Reflection, null, tint = reflectionColor, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) {
                            append(stringResource(R.string.today_plan_label))
                        }
                        append(" ")
                        append(vm.carriedChange)
                    },
                    style = Itera.type.bodySmall,
                    color = c.ink2,
                )
            }
        }

        // hero: exactly one suggested action
        when {
            !exerciseDone -> {
                if (technique != null) {
                    HeroCard(
                        eyebrow = stringResource(R.string.today_training),
                        eyebrowColor = exerciseColor,
                        badge = stringResource(R.string.badge_new),
                        token = { TechniqueToken(technique, 60.dp) },
                        title = stringResource(technique.title),
                        line = stringResource(technique.short),
                        meta = stringResource(R.string.today_about, technique.minutes, stringResource(technique.skill.title)),
                        cta = stringResource(R.string.today_start_exercise),
                        onClick = startExercise,
                    )
                } else {
                    HeroCard(
                        eyebrow = stringResource(R.string.today_combination),
                        eyebrowColor = exerciseColor,
                        token = { TechniqueToken(Technique.Pareto, 60.dp) },
                        title = stringResource(R.string.combo_title),
                        line = stringResource(R.string.combo_sub),
                        meta = stringResource(R.string.today_about, 70, stringResource(Skill.Planning.title)),
                        cta = stringResource(R.string.today_start_exercise),
                        onClick = startExercise,
                    )
                }
            }
            !focusDone && hour < 18 -> HeroCard(
                eyebrow = stringResource(R.string.hero_next_optional),
                eyebrowColor = focusSkill,
                token = { TechniqueToken(Technique.DeepWork, 60.dp) },
                title = stringResource(R.string.hero_focus_title, vm.focusMinutes),
                line = stringResource(R.string.hero_focus_line),
                meta = stringResource(R.string.minutes_short, vm.focusMinutes) + " · " + stringResource(Skill.Focus.title),
                cta = stringResource(R.string.hero_focus_cta),
                onClick = { onFocus(vm.focusMinutes) },
            )
            !reflectionDone -> HeroCard(
                eyebrow = stringResource(R.string.hero_evening),
                eyebrowColor = reflectionColor,
                token = { TechniqueToken(Technique.DailyReflection, 60.dp) },
                title = stringResource(R.string.reflection_label),
                line = stringResource(R.string.hero_reflection_line),
                meta = stringResource(R.string.minutes_short, 2) + " · " + stringResource(Skill.Reflection.title),
                cta = stringResource(R.string.hero_reflection_cta),
                onClick = onReflection,
            )
            else -> HeroCard(
                eyebrow = stringResource(R.string.day_n, vm.programDay),
                eyebrowColor = c.ink2,
                token = { TechniqueToken(Technique.OnePercent, 60.dp) },
                title = stringResource(R.string.hero_done_title),
                line = stringResource(R.string.hero_done_line),
                meta = null,
                cta = stringResource(R.string.hero_done_cta),
                onClick = onWrapUp,
            )
        }

        // the day at a glance
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle(stringResource(R.string.today_section), stringResource(R.string.today_count, vm.completed.size, 3))
            val exerciseTitle = technique?.let { stringResource(it.title) } ?: stringResource(R.string.today_combination)
            val exerciseMinutes = technique?.minutes ?: 70
            val doneLabel = stringResource(R.string.step_done)
            StepRow(
                state = if (exerciseDone) StepState.Done else StepState.Now,
                title = exerciseTitle,
                subtitle = if (exerciseDone) vm.lastNote.ifBlank { doneLabel } else stringResource(R.string.step_now, exerciseMinutes),
                accent = exerciseColor,
                onClick = if (exerciseDone) null else startExercise,
            )
            StepRow(
                state = when {
                    focusDone -> StepState.Done
                    exerciseDone -> StepState.Now
                    else -> StepState.Next
                },
                title = stringResource(R.string.step_focus_title, vm.focusMinutes),
                subtitle = if (focusDone) stringResource(R.string.step_done) else stringResource(R.string.step_focus_sub, formatTime(LocalTime.of(11, 0))),
                accent = focusSkill,
                onClick = if (focusDone) null else ({ onFocus(vm.focusMinutes) }),
            )
            StepRow(
                state = when {
                    reflectionDone -> StepState.Done
                    exerciseDone && (focusDone || hour >= 18) -> StepState.Now
                    else -> StepState.Next
                },
                title = stringResource(R.string.reflection_label),
                subtitle = if (reflectionDone) stringResource(R.string.step_done) else stringResource(R.string.step_reflection_sub, formatTime(vm.eveningTime)),
                accent = reflectionColor,
                onClick = if (reflectionDone) null else onReflection,
            )
        }
        VoiceCommandPill()
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
    cta: String,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val c = Itera.colors
    IteraCard(radius = 28.dp, padding = PaddingValues(22.dp), gap = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(eyebrow, eyebrowColor, Modifier.weight(1f))
            if (badge != null) Pill(badge, c.accentSoft, c.accent)
        }
        token()
        Text(title, style = Itera.type.display, color = c.ink)
        Text(line, style = Itera.type.bodyLarge, color = c.ink2)
        if (meta != null) Text(meta, style = Itera.type.bodySmall, color = c.ink2)
        IteraButton(cta, onClick, modifier = Modifier.padding(top = 4.dp))
    }
}
