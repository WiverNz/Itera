package com.wivernz.itera.core.designsystem.preview
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
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.AnimatedCheck
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.CheckCircle
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.CircleIconButton
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.EmptyState
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.Group
import com.wivernz.itera.core.designsystem.component.IntervalLadder
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LanguagePill
import com.wivernz.itera.core.designsystem.component.LanguageSheet
import com.wivernz.itera.core.designsystem.component.LinkRow
import com.wivernz.itera.core.designsystem.component.MasteryDots
import com.wivernz.itera.core.designsystem.component.MasteryLadder
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ProgressBar
import com.wivernz.itera.core.designsystem.component.RadioDot
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.SectionTitle
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.StepDot
import com.wivernz.itera.core.designsystem.component.StepRow
import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.core.designsystem.component.SwitchRow
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TimePickerSheet
import com.wivernz.itera.core.designsystem.component.TimeRow
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.ValueRow
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
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
                    Skill.HABITS,
                    IteraIcons.TwoMinute,
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
            ComponentSample.MasteryLadder -> MasteryLevel.entries.forEach {
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
            ComponentSample.MasteryDots -> MasteryLevel.entries.forEach {
                MasteryDots(
                    it,
                    c.accent
                )
            }
            ComponentSample.IntervalLadder -> (0..4).forEach { IntervalLadder(it, c.accent) }
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
            ComponentSample.LanguageSheet -> LanguageSheet("en", {}, {})
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
