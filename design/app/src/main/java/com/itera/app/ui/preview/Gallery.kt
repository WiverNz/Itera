package com.itera.app.ui.preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.ui.screens.AnimatedCheck
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.screens.CheckCircle
import com.itera.app.ui.components.ChoiceChip
import com.itera.app.ui.components.CircleIconButton
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.EmptyState
import com.itera.app.ui.components.ErrorState
import com.itera.app.ui.components.Eyebrow
import com.itera.app.ui.screens.Group
import com.itera.app.ui.screens.IntervalLadder
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.screens.LanguagePill
import com.itera.app.ui.screens.LanguageSheet
import com.itera.app.ui.screens.LinkRow
import com.itera.app.ui.screens.MasteryDots
import com.itera.app.ui.components.MasteryLadder
import com.itera.app.ui.components.NoteField
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ProgressBar
import com.itera.app.ui.screens.RadioDot
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.SectionTitle
import com.itera.app.ui.components.Segmented
import com.itera.app.ui.components.Skeleton
import com.itera.app.ui.components.StepDot
import com.itera.app.ui.components.StepRow
import com.itera.app.ui.components.StepState
import com.itera.app.ui.screens.SwitchRow
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.components.TimePickerSheet
import com.itera.app.ui.screens.TimeRow
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.screens.ValueRow
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.IteraTheme
import com.itera.app.ui.theme.colors
import com.itera.app.model.Mastery
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import java.time.LocalTime

@PreviewLightDark
@Preview(fontScale = 2f, showBackground = true)
@Composable
fun ComponentPreview(@PreviewParameter(Samples::class) sample: ComponentSample) {
    IteraTheme { ComponentGallery(sample) }
}

@Composable
fun ComponentGallery(sample: ComponentSample, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.app_name)
    val description = stringResource(R.string.language_sub)
    val c = Itera.colors
    ScreenColumn(modifier = modifier) {
        when (sample) {
            ComponentSample.ScreenColumn -> Text(label, style = Itera.type.body, color = c.ink)
            ComponentSample.IteraCard -> IteraCard {
                Text(
                    description,
                    style = Itera.type.body,
                    color = c.ink
                )
            }
            ComponentSample.Divider -> Divider()
            ComponentSample.Eyebrow -> Eyebrow(label)
            ComponentSample.Pill -> Pill(label, c.accentSoft, c.accent)
            ComponentSample.IteraButton -> ButtonKind.entries.forEach { kind ->
                IteraButton(label, {}, kind = kind, icon = IteraIcons.Check)
                IteraButton(label, {}, kind = kind, enabled = false)
            }
            ComponentSample.CircleIconButton -> CircleIconButton(IteraIcons.Close, label, {})
            ComponentSample.TopBar -> TopBar(label, {})
            ComponentSample.TechniqueToken -> listOf(
                36,
                40,
                44,
                48,
                56,
                60,
                64
            ).forEach {
                TechniqueToken(
                    Technique.TwoMinute,
                    it.dp
                )
            }
            ComponentSample.ChoiceChip -> {
                ChoiceChip(
                    label,
                    true,
                    {
                    }
                )
                ChoiceChip(
                    label,
                    false,
                    {
                    }
                )
            }
            ComponentSample.Segmented -> Segmented(listOf(0 to label, 1 to description), 0, {})
            ComponentSample.StepRow -> StepState.entries.forEach {
                StepRow(
                    it,
                    label,
                    description,
                    c.accent,
                    {
                    }
                )
            }
            ComponentSample.StepDot -> StepState.entries.forEach { StepDot(it, c.accent) }
            ComponentSample.MasteryLadder -> (listOf(null) + Mastery.entries).forEach {
                MasteryLadder(
                    it,
                    c.accent
                )
            }
            ComponentSample.ProgressBar -> ProgressBar(0.5f, c.accent)
            ComponentSample.NoteField -> NoteField("", {}, description)
            ComponentSample.SectionTitle -> SectionTitle(label, "1 / 3")
            ComponentSample.CheckCircle -> {
                CheckCircle(
                    true,
                    c.accent
                )
                CheckCircle(
                    false,
                    c.accent
                )
            }
            ComponentSample.RadioDot -> {
                RadioDot(true)
                RadioDot(false)
            }
            ComponentSample.LinkRow -> LinkRow(IteraIcons.Today, label, description, {})
            ComponentSample.MasteryDots -> (listOf(null) + Mastery.entries).forEach {
                MasteryDots(
                    it,
                    c.accent
                )
            }
            ComponentSample.IntervalLadder -> (0..4).forEach { IntervalLadder(c.accent, it) }
            ComponentSample.AnimatedCheck -> AnimatedCheck(c.accentSoft, c.accent)
            ComponentSample.Group -> Group(label) { ValueRow(label, description, {}) }
            ComponentSample.ValueRow -> ValueRow(label, description, {})
            ComponentSample.SwitchRow -> SwitchRow(
                label,
                true,
                c.accent,
                sub = description,
                onChange = {
                }
            )
            ComponentSample.TimeRow -> TimeRow(IteraIcons.Today, label, description, "08:30", {})
            ComponentSample.LanguageSheet -> LanguageSheet({})
            ComponentSample.LanguagePill -> LanguagePill({})
            ComponentSample.TimePickerSheet -> TimePickerSheet(label, LocalTime.of(8, 30), {}, {})
            ComponentSample.EmptyState -> EmptyState(description)
            ComponentSample.ErrorState -> ErrorState(description, label, {})
            ComponentSample.Skeleton -> Skeleton(true, Modifier.fillMaxWidth().height(60.dp))
        }
    }
}

@PreviewLightDark
@Composable
fun TokenGalleryPreview() {
    IteraTheme { TokenGallery() }
}

@Composable
fun TokenGallery() {
    val c = Itera.colors
    val t = Itera.type
    val label = stringResource(R.string.app_name)
    ScreenColumn {
        listOf(
            c.bg,
            c.surface,
            c.surface2,
            c.ink,
            c.ink2,
            c.ink3,
            c.line,
            c.accent,
            c.accentSoft,
            c.onInk,
            c.scrim
        ).forEach {
            Box(Modifier.fillMaxWidth().height(32.dp).background(it))
        }
        Skill.entries.forEach { skill ->
            val pair = skill.colors(c.isDark)
            Row {
                Box(Modifier.size(40.dp).background(pair.container))
                Box(Modifier.size(40.dp).background(pair.content))
            }
        }
        listOf(
            t.hero,
            t.display,
            t.title,
            t.headline,
            t.bodyLarge,
            t.body,
            t.bodySmall,
            t.caption,
            t.label,
            t.eyebrow,
            t.timer
        ).forEach {
            Text(label, style = it, color = c.ink)
        }
        listOf(28, 24, 22, 20, 18, 16, 14, 13, 12, 8).forEach {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(it.dp)).background(c.ink))
        }
        allIcons.forEach { Icon(it, null, tint = c.ink, modifier = Modifier.size(24.dp)) }
    }
}

val allIcons = listOf(
    IteraIcons.TwoMinute,
    IteraIcons.Pomodoro,
    IteraIcons.Eisenhower,
    IteraIcons.Feynman,
    IteraIcons.FiveSecond,
    IteraIcons.InfoDiet,
    IteraIcons.OnePercent,
    IteraIcons.DeepWork,
    IteraIcons.Premortem,
    IteraIcons.HabitStack,
    IteraIcons.Pareto,
    IteraIcons.TwoList,
    IteraIcons.Reflection,
    IteraIcons.Spaced,
    IteraIcons.Today,
    IteraIcons.Train,
    IteraIcons.Progress,
    IteraIcons.You,
    IteraIcons.Close,
    IteraIcons.Back,
    IteraIcons.Chevron,
    IteraIcons.Check,
    IteraIcons.Plus,
    IteraIcons.Lock,
    IteraIcons.Play,
    IteraIcons.Pause,
    IteraIcons.Spark,
    IteraIcons.Bell,
    IteraIcons.ArrowDown,
    IteraIcons.Eye,
    IteraIcons.Globe
)
