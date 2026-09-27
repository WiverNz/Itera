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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.itera.app.ui.components.NoteField
import com.itera.app.ui.components.TopBar
import com.itera.app.ui.components.TimePickerSheet
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
fun ProfileScreen(vm: AppViewModel, onReset: (Boolean) -> Unit = {}) {
    val c = Itera.colors
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var addingTopic by rememberSaveable { mutableStateOf(false) }
    var newTopic by rememberSaveable { mutableStateOf("") }
    val topics = remember { mutableStateListOf<String>() }
    var range by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current
    val use24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val locale = currentLocale()
    val timeFormat = DateTimeFormatter.ofPattern(
        android.text.format.DateFormat.getBestDateTimePattern(locale, if (use24Hour) "Hm" else "hm"), locale
    )
    val since = vm.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    val habits = Skill.Habits.colors(c.isDark).content

    if (panel == "privacy" || panel == "topics") {
        androidx.activity.compose.BackHandler { panel = null }
        ScreenColumn {
            TopBar("", { panel = null }, IteraIcons.Back)
            Text(stringResource(if (panel == "privacy") R.string.privacy else R.string.settings_topics), style = Itera.type.display, color = c.ink)
            if (panel == "privacy") Text(stringResource(R.string.privacy_body), style = Itera.type.body, color = c.ink)
            if (panel == "privacy") Text(stringResource(R.string.privacy_voice), style = Itera.type.body, color = c.ink)
            else {
                if (topics.isEmpty()) Text(stringResource(R.string.settings_topics_empty), style = Itera.type.body, color = c.ink2)
                topics.toList().forEachIndexed { i, topic ->
                    NoteField(topic, { topics[i] = it }, stringResource(R.string.settings_topic_title))
                    IteraButton(stringResource(R.string.settings_archive), { topics.removeAt(i) }, kind = ButtonKind.Ghost)
                    Divider()
                }
                IteraButton(stringResource(R.string.settings_add_topic), { addingTopic = true })
            }
        }
    } else ScreenColumn(gap = 22.dp) {
        Text(stringResource(R.string.you_title), style = Itera.type.display, color = c.ink)
        Row(Modifier.fillMaxWidth().clickable { panel = "name" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val sc = Skill.Focus.colors(c.isDark)
            Box(Modifier.size(56.dp).clip(CircleShape).background(sc.container), contentAlignment = Alignment.Center) {
                if (name.isBlank()) Icon(IteraIcons.You, null, tint = sc.content, modifier = Modifier.size(26.dp)) else Text(name.take(1), style = Itera.type.userTextLarge, color = sc.content)
            }
            Column {
                Text(name.ifBlank { stringResource(R.string.settings_add_name) }, style = Itera.type.label, color = c.ink)
                Text(stringResource(R.string.training_since, since, vm.programDay), style = Itera.type.bodySmall, color = c.ink2)
            }
        }

        Group(stringResource(R.string.sec_rhythm)) {
            ValueRow(stringResource(R.string.rhythm_morning), vm.morningTime.format(timeFormat)) { panel = "morning" }
            Divider()
            ValueRow(stringResource(R.string.rhythm_evening), vm.eveningTime.format(timeFormat)) { panel = "evening" }
            Divider()
            ValueRow(stringResource(R.string.time_most_days), stringResource(R.string.minutes_short, vm.dailyMinutes)) { panel = "budget" }
        }

        Group(stringResource(R.string.sec_program)) {
            Column(Modifier.padding(top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.pace), style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
                Text(stringResource(when(vm.pace) { 0 -> R.string.pace_gentle_hint; 2 -> R.string.pace_intense_hint; else -> R.string.pace_standard_hint }), style = Itera.type.caption, color = c.ink2)
                Segmented(
                    listOf(0 to stringResource(R.string.pace_gentle), 1 to stringResource(R.string.pace_standard), 2 to stringResource(R.string.pace_intense)),
                    vm.pace, { vm.pace = it }, Modifier.padding(top = 4.dp),
                )
            }
            Divider()
            val areas = vm.focusSkills.map { stringResource(it.title) }.joinToString(", ")
            ValueRow(stringResource(R.string.focus_areas), areas) { panel = "areas" }
            Divider()
            ValueRow(stringResource(R.string.settings_topics), pluralStringResource(R.plurals.settings_topic_count, topics.size, topics.size)) { panel = "topics" }
        }

        Group(stringResource(R.string.sec_language)) {
            ValueRow(stringResource(R.string.language_title), AppLanguage.nativeName(locale.language)) { showLanguage = true }
            Divider()
            Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.time_format), style = Itera.type.body, color = c.ink, modifier = Modifier.weight(1f))
                    Text(stringResource(if (use24Hour) R.string.time_24 else R.string.time_12), style = Itera.type.bodySmall, color = c.ink2)
                }
                Text(stringResource(R.string.time_system_hint), style = Itera.type.caption, color = c.ink2)
            }
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
            val permission = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!permission) {
                ValueRow(stringResource(R.string.notif_denied), stringResource(R.string.notif_open_settings)) {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
                Divider()
            }
            listOf(R.string.notif_morning, R.string.notif_focus, R.string.notif_review, R.string.notif_evening).forEachIndexed { i, res ->
                if (i > 0) Divider()
                SwitchRow(stringResource(res), permission && vm.notifications[i], habits, enabled = permission) { vm.notifications[i] = it }
            }
            Text(stringResource(R.string.notif_system_hint), style = Itera.type.caption, color = c.ink2, modifier = Modifier.padding(bottom = 12.dp))
        }

        Group(stringResource(R.string.sec_coach)) {
            SwitchRow(stringResource(R.string.coach_ai), false, habits, sub = stringResource(R.string.coming_later), enabled = false) {}
        }

        Group(stringResource(R.string.sec_data)) {
            ValueRow(stringResource(R.string.export_journal), stringResource(R.string.export_markdown)) { panel = "export" }
            Divider()
            ValueRow(stringResource(R.string.privacy), "") { panel = "privacy" }
            Divider()
            ValueRow(stringResource(R.string.settings_reset), "") { panel = "reset" }
            Divider()
            ValueRow(stringResource(R.string.settings_erase), "") { panel = "erase" }
        }

        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) IteraButton(stringResource(R.string.load_demo), onClick = { vm.loadDemo() }, kind = ButtonKind.Secondary, height = 48.dp)
    }
    if (addingTopic) ProfileSheet(stringResource(R.string.settings_add_topic), { addingTopic = false }, false) {
        NoteField(newTopic, { newTopic = it }, stringResource(R.string.settings_topic_title))
        IteraButton(stringResource(R.string.settings_add_topic), { topics.add(newTopic); newTopic = ""; addingTopic = false }, enabled = newTopic.isNotBlank())
    }
    if (showLanguage) LanguageSheet { showLanguage = false }
    when(panel) {
        "morning", "evening" -> TimePickerSheet(stringResource(if (panel == "morning") R.string.rhythm_morning else R.string.rhythm_evening), if (panel == "morning") vm.morningTime else vm.eveningTime,
            { if (panel == "morning") vm.morningTime = it else vm.eveningTime = it; panel = null }, { panel = null })
        "name" -> ProfileSheet(stringResource(R.string.settings_add_name), { panel = null }) { NoteField(name, { name = it }, stringResource(R.string.settings_add_name)) }
        "budget" -> ProfileSheet(stringResource(R.string.time_most_days), { panel = null }) { Segmented(listOf(5, 15, 30).map { it to stringResource(R.string.minutes_short, it) }, vm.dailyMinutes, { vm.dailyMinutes = it }) }
        "areas" -> ProfileSheet(stringResource(R.string.focus_areas), { panel = null }) {
            Text(stringResource(R.string.goals_sub), style = Itera.type.bodySmall, color = c.ink2)
            FocusAreaRows(vm)
        }
        "reset", "erase" -> ProfileSheet(stringResource(if (panel == "erase") R.string.settings_erase else R.string.settings_reset), { panel = null }, false) {
            Text(stringResource(if (panel == "erase") R.string.settings_erase_body else R.string.settings_reset_body), style = Itera.type.body, color = c.ink)
            IteraButton(stringResource(R.string.action_cancel), { panel = null }, kind = ButtonKind.Secondary)
            IteraButton(stringResource(if (panel == "erase") R.string.settings_erase else R.string.settings_reset), { val erase = panel == "erase"; vm.resetProgram(erase); panel = null; onReset(erase) })
        }
        "export" -> ProfileSheet(stringResource(R.string.export_journal), { panel = null }, false) {
            Text(stringResource(R.string.export_note), style = Itera.type.bodySmall, color = c.ink2)
            Segmented(listOf(0 to stringResource(R.string.export_month), 1 to stringResource(R.string.export_year), 2 to stringResource(R.string.export_all)), range, { range = it })
            IteraButton(stringResource(R.string.export_share), { panel = null })
            IteraButton(stringResource(R.string.export_save), { panel = null }, kind = ButtonKind.Secondary)
        }
    }
}

@Composable
fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Itera.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = Itera.type.eyebrow, color = c.ink2, modifier = Modifier.padding(start = 4.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 16.dp), content = content)
    }
}

@Composable
fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val c = Itera.colors
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val valueMaxWidth = maxWidth * 0.55f
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(label, style = Itera.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.ink, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) Text(value, style = Itera.type.bodySmall, color = c.ink2,
                modifier = Modifier.widthIn(max = valueMaxWidth))
            Icon(IteraIcons.Chevron, null, tint = c.ink3, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    sub: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = Itera.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).alpha(if (enabled) 1f else 0.55f).toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
            if (sub != null) Text(sub, style = Itera.type.caption, color = c.ink2)
        }
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent, checkedThumbColor = c.surface,
                uncheckedTrackColor = c.surface2, uncheckedThumbColor = c.ink3, uncheckedBorderColor = c.line,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(title: String, dismiss: () -> Unit, done: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Itera.colors.surface, scrimColor = Itera.colors.scrim) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = Itera.type.title, color = Itera.colors.ink)
            content()
            if (done) IteraButton(stringResource(R.string.action_done), dismiss)
        }
    }
}
