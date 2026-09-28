@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.you

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.LanguageChoice
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.analytics.SettingKey
import com.wivernz.itera.analytics.SettingValue
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatDateMedium
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.common.time.is24Hour
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Group
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LanguageSheet
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.SwitchRow
import com.wivernz.itera.core.designsystem.component.TimePickerSheet
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.ValueRow
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.feature.onboarding.FocusAreaRows
import com.wivernz.itera.feature.voice.RecognizerSheet
import com.wivernz.itera.feature.voice.recognitionProviders

@Composable
fun YouRoute(
    vm: YouViewModel,
    resetNavigation: (Boolean) -> Unit,
    export: @Composable (() -> Unit) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resume by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.onEvent(
            YouUiEvent.Permission(NotificationManagerCompat.from(context).areNotificationsEnabled())
        )
        resume++
    }
    ObserveEffects(vm.effects) {
        when (it) {
            is YouEffect.ResetDone -> {
                Toast.makeText(
                    context,
                    if (it.erased) R.string.settings_erased else R.string.settings_reset_done,
                    Toast.LENGTH_LONG
                ).show()
                resetNavigation(it.erased)
            }
            YouEffect.DemoDone -> resetNavigation(false)
        }
    }
    YouScreen(state, vm::onEvent, is24Hour(context), resume, {
        context.startActivity(
            Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }, export)
}

@Composable
fun YouScreen(
    state: YouUiState,
    onEvent: (YouUiEvent) -> Unit,
    use24Hour: Boolean,
    resume: Int = 0,
    systemSettings: () -> Unit = {},
    export: @Composable (() -> Unit) -> Unit = {}
) {
    val c = Itera.colors
    val p = state.preferences
    val locale = currentLocale()
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var reset by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(panel) {
        when (panel) {
            "privacy" -> onEvent(YouUiEvent.Open(ScreenRoute.PRIVACY))
            "topics" -> onEvent(YouUiEvent.Open(ScreenRoute.TOPICS))
        }
    }
    fun change(key: SettingKey, value: SettingValue) = onEvent(YouUiEvent.Change(key, value))
    val providers = recognitionProviders()
    if (panel == "privacy" || panel == "topics") {
        BackHandler { panel = null }
        if (panel == "privacy") {
            ScreenColumn {
                TopBar("", { panel = null }, IteraIcons.Back)
                Text(stringResource(R.string.privacy), style = Itera.type.display, color = c.ink)
                Text(stringResource(R.string.privacy_body), style = Itera.type.body, color = c.ink)
                Text(stringResource(R.string.privacy_voice), style = Itera.type.body, color = c.ink)
            }
        } else {
            TopicsScreen(state.topics, onEvent) { panel = null }
        }
    } else {
        ScreenColumn(gap = 22.dp) {
            Text(stringResource(R.string.you_title), style = Itera.type.display, color = c.ink)
            if (state.loading) Skeleton(true, Modifier.fillMaxWidth().height(56.dp))
            if (state.failed) {
                ErrorState(
                    stringResource(
                        R.string.settings_failed
                    ),
                    stringResource(R.string.action_retry),
                    {
                        onEvent(YouUiEvent.Retry)
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) {
                    panel = "name"
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val sc = Skill.FOCUS.colors(c.isDark)
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(sc.container),
                    contentAlignment = Alignment.Center
                ) {
                    if (p.displayName.isBlank()) {
                        Icon(
                            IteraIcons.You,
                            null,
                            tint = sc.content,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Text(
                            p.displayName.take(1),
                            style = Itera.type.userTextLarge,
                            color = sc.content
                        )
                    }
                }
                Column {
                    Text(
                        p.displayName.ifBlank {
                            stringResource(R.string.settings_add_name)
                        },
                        style = if (p.displayName.isBlank()) Itera.type.label else Itera.type.userTextLarge,
                        color = c.ink
                    )
                    p.programStartedOn?.let {
                        Text(
                            stringResource(
                                R.string.training_since,
                                formatDateMedium(it, locale),
                                p.currentProgramDay
                            ),
                            style = Itera.type.bodySmall,
                            color = c.ink2
                        )
                    }
                }
            }
            Group(stringResource(R.string.sec_rhythm)) {
                ValueRow(
                    stringResource(
                        R.string.rhythm_morning
                    ),
                    formatTime(p.morningTime, locale, use24Hour),
                    {
                        panel =
                            "morning"
                    }
                )
                Divider()
                ValueRow(
                    stringResource(
                        R.string.rhythm_evening
                    ),
                    formatTime(p.eveningTime, locale, use24Hour),
                    {
                        panel =
                            "evening"
                    }
                )
                Divider()
                ValueRow(
                    stringResource(
                        R.string.time_most_days
                    ),
                    stringResource(R.string.minutes_short, p.timeBudget.minutes),
                    {
                        panel =
                            "budget"
                    }
                )
            }
            Group(stringResource(R.string.sec_program)) {
                Column(
                    Modifier.padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.pace),
                        style = Itera.type.body.copy(fontWeight = FontWeight.Medium),
                        color = c.ink
                    )
                    Text(
                        stringResource(
                            when (p.pace) {
                                ProgramPace.GENTLE -> R.string.pace_gentle_hint
                                ProgramPace.STANDARD -> R.string.pace_standard_hint
                                ProgramPace.INTENSE -> R.string.pace_intense_hint
                            }
                        ),
                        style = Itera.type.caption,
                        color = c.ink2
                    )
                    Segmented(
                        listOf(
                            ProgramPace.GENTLE to stringResource(R.string.pace_gentle),
                            ProgramPace.STANDARD to stringResource(R.string.pace_standard),
                            ProgramPace.INTENSE to stringResource(R.string.pace_intense)
                        ),
                        p.pace,
                        {
                            change(SettingKey.PACE, SettingValue.Pace(it))
                        },
                        Modifier.padding(top = 4.dp)
                    )
                }
                Divider()
                ValueRow(
                    stringResource(R.string.focus_areas),
                    p.focusAreas.map {
                        stringResource(it.title)
                    }.joinToString(", "),
                    {
                        panel =
                            "areas"
                    }
                )
                Divider()
                ValueRow(
                    stringResource(
                        R.string.settings_topics
                    ),
                    pluralStringResource(
                        R.plurals.settings_topic_count,
                        state.topics.size,
                        state.topics.size
                    ),
                    {
                        panel =
                            "topics"
                    }
                )
            }
            Group(stringResource(R.string.sec_language)) {
                ValueRow(
                    stringResource(
                        R.string.language_title
                    ),
                    AppLanguage.nativeName(locale.language),
                    {
                        panel =
                            "language"
                    }
                )
                Divider()
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.time_format),
                            style = Itera.type.body,
                            color = c.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(if (use24Hour) R.string.time_24 else R.string.time_12),
                            style = Itera.type.bodySmall,
                            color = c.ink2
                        )
                    }
                    Text(
                        stringResource(R.string.time_system_hint),
                        style = Itera.type.caption,
                        color = c.ink2
                    )
                }
            }
            Group(stringResource(R.string.sec_appearance)) {
                Column(
                    Modifier.padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.theme), style = Itera.type.body, color = c.ink)
                    Segmented(
                        listOf(
                            ThemePreference.SYSTEM to stringResource(R.string.theme_system),
                            ThemePreference.LIGHT to stringResource(R.string.theme_light),
                            ThemePreference.DARK to stringResource(R.string.theme_dark)
                        ),
                        p.theme,
                        {
                            change(SettingKey.THEME, SettingValue.Theme(it))
                        }
                    )
                }
            }
            Group(stringResource(R.string.sec_notifications)) {
                if (!state.permission) {
                    ValueRow(
                        stringResource(R.string.notif_denied),
                        stringResource(R.string.notif_open_settings),
                        systemSettings
                    )
                    Divider()
                }
                listOf(
                    Triple(R.string.notif_morning, p.notifyMorning, SettingKey.NOTIFY_MORNING),
                    Triple(R.string.notif_focus, p.notifyFocus, SettingKey.NOTIFY_FOCUS),
                    Triple(R.string.notif_review, p.notifyReviews, SettingKey.NOTIFY_REVIEWS),
                    Triple(R.string.notif_evening, p.notifyEvening, SettingKey.NOTIFY_EVENING)
                ).forEachIndexed {
                        index,
                        (label, checked, key)
                    ->
                    if (index > 0) Divider()
                    SwitchRow(
                        stringResource(
                            label
                        ),
                        state.permission && checked,
                        Skill.HABITS.colors(
                            c.isDark
                        ).content,
                        enabled = state.permission,
                        onChange = {
                            change(key, SettingValue.Toggle(it))
                        }
                    )
                }
                Text(
                    stringResource(R.string.notif_system_hint),
                    style = Itera.type.caption,
                    color = c.ink2,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Group(stringResource(R.string.sec_voice)) {
                SwitchRow(
                    stringResource(R.string.voice_system_setting),
                    p.systemRecognitionAllowed,
                    Skill.HABITS.colors(c.isDark).content,
                    sub = stringResource(R.string.voice_system_setting_sub),
                    onChange = { onEvent(YouUiEvent.SystemRecognition(it)) }
                )
                if (providers.isNotEmpty() || p.selectedRecognizer != null) {
                    Divider()
                    ValueRow(
                        stringResource(R.string.voice_provider_setting),
                        providers.firstOrNull { it.id == p.selectedRecognizer }?.label
                            ?: stringResource(R.string.voice_provider_none),
                        { panel = "recognizer" }
                    )
                }
            }
            Group(stringResource(R.string.sec_coach)) {
                SwitchRow(
                    stringResource(
                        R.string.coach_ai
                    ),
                    false,
                    Skill.HABITS.colors(
                        c.isDark
                    ).content,
                    sub = stringResource(R.string.coming_later),
                    enabled = false,
                    onChange = {
                    }
                )
            }
            Group(stringResource(R.string.sec_data)) {
                ValueRow(
                    stringResource(
                        R.string.export_journal
                    ),
                    stringResource(R.string.export_markdown),
                    {
                        panel =
                            "export"
                    }
                )
                Divider()
                ValueRow(stringResource(R.string.privacy), "", { panel = "privacy" })
                Divider()
                ValueRow(stringResource(R.string.settings_reset), "", { reset = false })
                Divider()
                ValueRow(stringResource(R.string.settings_erase), "", { reset = true })
            }
            if (state.demoAvailable) {
                IteraButton(stringResource(R.string.load_demo), {
                    onEvent(YouUiEvent.Demo)
                }, kind = ButtonKind.Secondary, height = 48.dp)
            }
        }
    }
    when (panel) {
        "recognizer" -> RecognizerSheet(
            providers,
            p.selectedRecognizer,
            { onEvent(YouUiEvent.Recognizer(it)) },
            { panel = null }
        )
        "language" -> LanguageSheet(AppLanguage.current(), { tag ->
            change(
                SettingKey.LANGUAGE,
                SettingValue.Language(
                    LanguageChoice.entries.first {
                        it.name ==
                            tag.uppercase().ifBlank { "SYSTEM" }
                    }
                )
            )
            AppLanguage.set(tag)
        }, { panel = null })
        "morning", "evening" -> key(panel, resume) {
            TimePickerSheet(
                stringResource(
                    if (panel ==
                        "morning"
                    ) {
                        R.string.rhythm_morning
                    } else {
                        R.string.rhythm_evening
                    }
                ),
                if (panel ==
                    "morning"
                ) {
                    p.morningTime
                } else {
                    p.eveningTime
                },
                { time ->
                    onEvent(
                        YouUiEvent.Change(
                            if (panel ==
                                "morning"
                            ) {
                                SettingKey.MORNING_TIME
                            } else {
                                SettingKey.EVENING_TIME
                            },
                            SettingValue.Hour(time.hour),
                            time.minute
                        )
                    )
                    panel =
                        null
                },
                { panel = null }
            )
        }
        "name" -> SettingsSheet(stringResource(R.string.settings_add_name), { panel = null }) {
            NoteField(p.displayName, {
                onEvent(YouUiEvent.Name(it))
            }, stringResource(R.string.settings_add_name))
        }
        "budget" -> SettingsSheet(stringResource(R.string.time_most_days), { panel = null }) {
            Segmented(
                TimeBudget.entries.map {
                    it to
                        stringResource(R.string.minutes_short, it.minutes)
                },
                p.timeBudget,
                { change(SettingKey.TIME_BUDGET, SettingValue.Budget(it)) }
            )
        }
        "areas" -> SettingsSheet(stringResource(R.string.focus_areas), { panel = null }) {
            Text(stringResource(R.string.goals_sub), style = Itera.type.bodySmall, color = c.ink2)
            FocusAreaRows(p.focusAreas.toList()) {
                change(SettingKey.FOCUS_AREAS, SettingValue.Area(it))
            }
        }
        "export" -> export { panel = null }
    }
    reset?.let { erased ->
        SettingsSheet(
            stringResource(
                if (erased) R.string.settings_erase else R.string.settings_reset
            ),
            {
                reset =
                    null
            },
            done = false
        ) {
            Text(
                stringResource(
                    if (erased) R.string.settings_erase_body else R.string.settings_reset_body
                ),
                style = Itera.type.body,
                color = c.ink
            )
            IteraButton(stringResource(R.string.action_cancel), {
                reset = null
            }, kind = ButtonKind.Secondary)
            IteraButton(
                stringResource(
                    if (erased) R.string.settings_erase else R.string.settings_reset
                ),
                {
                    reset =
                        null
                    onEvent(YouUiEvent.Reset(erased))
                }
            )
        }
    }
    if (state.busy) {
        Dialog(
            {
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            IteraCard {
                Text(
                    stringResource(R.string.settings_working),
                    style = Itera.type.body,
                    color = c.ink
                )
                CircularProgressIndicator(color = c.ink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    title: String,
    dismiss: () -> Unit,
    done: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Itera.colors.surface,
        scrimColor = Itera.colors.scrim
    ) {
        Column(
            Modifier.verticalScroll(
                rememberScrollState()
            ).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = Itera.type.title, color = Itera.colors.ink)
            content()
            if (done) IteraButton(stringResource(R.string.action_done), dismiss)
        }
    }
}

@Composable
fun TopicsScreen(topics: List<LearningTopic>, onEvent: (YouUiEvent) -> Unit, back: () -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    ScreenColumn {
        TopBar("", back, IteraIcons.Back)
        Text(
            stringResource(R.string.settings_topics),
            style = Itera.type.display,
            color = Itera.colors.ink
        )
        if (topics.isEmpty()) {
            Text(
                stringResource(R.string.settings_topics_empty),
                style = Itera.type.body,
                color = Itera.colors.ink2
            )
        }
        topics.forEach { topic ->
            key(topic.id) {
                var draft by rememberSaveable { mutableStateOf(topic.title) }
                NoteField(draft, {
                    draft = it
                    onEvent(YouUiEvent.Topic(topic.id, it))
                }, stringResource(R.string.settings_topic_title))
            }
            IteraButton(stringResource(R.string.settings_archive), {
                onEvent(YouUiEvent.Archive(topic.id))
            }, kind = ButtonKind.Ghost)
            Divider()
        }
        IteraButton(stringResource(R.string.settings_add_topic), { adding = true })
    }
    if (adding) {
        SettingsSheet(stringResource(R.string.settings_add_topic), {
            adding = false
        }, done = false) {
            NoteField(title, { title = it }, stringResource(R.string.settings_topic_title))
            IteraButton(stringResource(R.string.settings_add_topic), {
                onEvent(YouUiEvent.Topic(null, title))
                title =
                    ""
                adding = false
            }, enabled = title.isNotBlank())
        }
    }
}
