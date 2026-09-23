package com.itera.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.Skill
import com.itera.app.model.ThemeMode
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.Segmented
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val c = Itera.colors
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()
    val since = vm.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    val habits = Skill.Habits.colors(c.isDark).content

    ScreenColumn(gap = 22.dp) {
        Text(stringResource(R.string.you_title), style = Itera.type.display, color = c.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val sc = Skill.Focus.colors(c.isDark)
            Box(Modifier.size(56.dp).clip(CircleShape).background(sc.container), contentAlignment = Alignment.Center) {
                Icon(IteraIcons.You, null, tint = sc.content, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(stringResource(R.string.your_name), style = Itera.type.label, color = c.ink)
                Text(stringResource(R.string.training_since, since, vm.programDay), style = Itera.type.bodySmall, color = c.ink2)
            }
        }

        Group(stringResource(R.string.sec_rhythm)) {
            ValueRow(stringResource(R.string.rhythm_morning), formatTime(vm.morningTime)) { vm.morningTime = vm.morningTime.plusMinutes(30) }
            Divider()
            ValueRow(stringResource(R.string.rhythm_evening), formatTime(vm.eveningTime)) { vm.eveningTime = vm.eveningTime.plusMinutes(30) }
            Divider()
            ValueRow(stringResource(R.string.time_most_days), stringResource(R.string.minutes_short, vm.dailyMinutes)) {}
        }

        Group(stringResource(R.string.sec_program)) {
            Column(Modifier.padding(top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.pace), style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
                Text(stringResource(R.string.pace_hint), style = Itera.type.caption, color = c.ink2)
                Segmented(
                    listOf(0 to stringResource(R.string.pace_gentle), 1 to stringResource(R.string.pace_standard), 2 to stringResource(R.string.pace_intense)),
                    vm.pace, { vm.pace = it }, Modifier.padding(top = 4.dp),
                )
            }
            Divider()
            val areas = vm.focusSkills.map { stringResource(it.title) }.joinToString(", ")
            ValueRow(stringResource(R.string.focus_areas), areas) {}
        }

        Group(stringResource(R.string.sec_language)) {
            ValueRow(stringResource(R.string.language_title), AppLanguage.nativeName(locale.language)) { showLanguage = true }
            Divider()
            ValueRow(stringResource(R.string.time_format), stringResource(R.string.time_format_value)) {}
        }

        Group(stringResource(R.string.sec_appearance)) {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.theme), style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
                Segmented(
                    listOf(
                        ThemeMode.System to stringResource(R.string.theme_system),
                        ThemeMode.Light to stringResource(R.string.theme_light),
                        ThemeMode.Dark to stringResource(R.string.theme_dark),
                    ),
                    vm.themeMode, { vm.themeMode = it },
                )
            }
        }

        Group(stringResource(R.string.sec_notifications)) {
            listOf(R.string.notif_morning, R.string.notif_focus, R.string.notif_review, R.string.notif_evening).forEachIndexed { i, res ->
                if (i > 0) Divider()
                SwitchRow(stringResource(res), vm.notifications[i], habits) { vm.notifications[i] = it }
            }
        }

        Group(stringResource(R.string.sec_coach)) {
            SwitchRow(stringResource(R.string.coach_ai), false, habits, sub = stringResource(R.string.coming_later), enabled = false) {}
        }

        Group(stringResource(R.string.sec_data)) {
            ValueRow(stringResource(R.string.export_journal), "Markdown") {}
            Divider()
            ValueRow(stringResource(R.string.privacy), "") {}
        }

        IteraButton(stringResource(R.string.load_demo), onClick = { vm.loadDemo() }, kind = ButtonKind.Secondary, height = 48.dp)
    }
    if (showLanguage) LanguageSheet { showLanguage = false }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Itera.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = Itera.type.eyebrow, color = c.ink2, modifier = Modifier.padding(start = 4.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val c = Itera.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = Itera.type.bodySmall, color = c.ink2)
        Icon(IteraIcons.Chevron, null, tint = c.ink3, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    sub: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = Itera.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).alpha(if (enabled) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
            if (sub != null) Text(sub, style = Itera.type.caption, color = c.ink2)
        }
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent, checkedThumbColor = c.surface,
                uncheckedTrackColor = c.surface2, uncheckedThumbColor = c.ink3, uncheckedBorderColor = c.line,
            ),
        )
    }
}
