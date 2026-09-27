package com.itera.app.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.Program
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.ui.components.ButtonKind
import com.itera.app.ui.components.CircleIconButton
import com.itera.app.ui.components.IteraButton
import com.itera.app.ui.components.IteraCard
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.Pill
import com.itera.app.ui.components.ScreenColumn
import com.itera.app.ui.components.Segmented
import com.itera.app.ui.components.TechniqueToken
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.colors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// ------------------------------------------------------------------ language

object AppLanguage {
    /** Tags offered in the picker. Empty = follow the device. */
    val tags = listOf("", "en", "ru", "de", "es")

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    fun set(tag: String) {
        val list = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(list)
    }

    /** Language name in that language: "Deutsch", "Русский"… */
    fun nativeName(tag: String): String {
        val l = Locale.forLanguageTag(tag)
        return l.getDisplayLanguage(l).replaceFirstChar { it.titlecase(l) }
    }

    fun localName(tag: String, inLocale: Locale): String =
        Locale.forLanguageTag(tag).getDisplayLanguage(inLocale).replaceFirstChar { it.titlecase(inLocale) }
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val c = Itera.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = currentLocale()
    var selected by remember { mutableStateOf(AppLanguage.current()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, tonalElevation = 0.dp, containerColor = c.surface, scrimColor = c.scrim) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.language_title), style = Itera.type.title, color = c.ink)
            Text(stringResource(R.string.language_sub), style = Itera.type.bodySmall, color = c.ink2)
            Column {
                AppLanguage.tags.forEachIndexed { i, tag ->
                    val on = tag == selected
                    val title = if (tag.isEmpty()) stringResource(R.string.language_match_device) else AppLanguage.nativeName(tag)
                    val sub = if (tag.isEmpty()) {
                        stringResource(R.string.language_current, AppLanguage.localName(android.content.res.Resources.getSystem().configuration.locales[0].language, locale))
                    } else {
                        AppLanguage.localName(tag, locale)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(selected = on, role = Role.RadioButton) {
                                selected = tag
                                AppLanguage.set(tag)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
                            if (tag.isEmpty()) Icon(IteraIcons.Globe, null, tint = c.ink, modifier = Modifier.size(20.dp))
                            else Text(tag.uppercase(), style = Itera.type.caption.copy(fontWeight = FontWeight.Bold), color = c.ink)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, style = Itera.type.label.copy(fontFamily = Itera.type.userText.fontFamily), color = c.ink)
                            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
                        }
                        RadioDot(on)
                    }
                    if (i == 0) Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Divider() }
                }
            }
            IteraButton(stringResource(R.string.action_done), onClick = onDismiss)
        }
    }
}

@Composable
fun RadioDot(on: Boolean) {
    val c = Itera.colors
    Box(
        Modifier.size(26.dp).clip(CircleShape)
            .background(if (on) c.ink else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, if (on) c.ink else c.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(IteraIcons.Check, null, tint = c.onInk, modifier = Modifier.size(15.dp))
    }
}

@Composable
fun LanguagePill(onClick: () -> Unit) {
    val c = Itera.colors
    val code = currentLocale().language.uppercase()
    val label = stringResource(R.string.language_a11y, AppLanguage.nativeName(currentLocale().language))
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(IteraIcons.Globe, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Text(code, style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
    }
}

// ------------------------------------------------------------------ welcome

@Composable
fun WelcomeScreen(onStart: () -> Unit, onDemo: () -> Unit) {
    val c = Itera.colors
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    ScreenColumn(
        bottom = {
            IteraButton(stringResource(R.string.welcome_get_started), onStart)
            IteraButton(stringResource(R.string.welcome_demo), onDemo, kind = ButtonKind.Ghost)
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.ink), contentAlignment = Alignment.Center) {
                Icon(IteraIcons.OnePercent, null, tint = c.onInk, modifier = Modifier.size(18.dp))
            }
            Text(stringResource(R.string.app_name), style = Itera.type.headline, color = c.ink, modifier = Modifier.padding(start = 10.dp).weight(1f))
            LanguagePill { showLanguage = true }
        }
        RisingPath()
        Text(stringResource(R.string.welcome_title), style = Itera.type.hero, color = c.ink)
        Text(stringResource(R.string.app_description), style = Itera.type.bodyLarge, color = c.ink2)
    }
    if (showLanguage) LanguageSheet { showLanguage = false }
}

/** Five technique tokens climbing a dotted path; each pops in with a spring. */
@Composable
private fun RisingPath() {
    val c = Itera.colors
    val nodes = listOf(
        Technique.TwoMinute to Offset(0.05f, 0.73f),
        Technique.Pomodoro to Offset(0.26f, 0.56f),
        Technique.Feynman to Offset(0.47f, 0.39f),
        Technique.HabitStack to Offset(0.68f, 0.26f),
        Technique.DailyReflection to Offset(0.86f, 0.06f),
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.3f)
            .drawBehind {
                val p = Path().apply {
                    moveTo(size.width * 0.13f, size.height * 0.83f)
                    cubicTo(size.width * 0.3f, size.height * 0.75f, size.width * 0.4f, size.height * 0.55f, size.width * 0.55f, size.height * 0.49f)
                    cubicTo(size.width * 0.7f, size.height * 0.42f, size.width * 0.8f, size.height * 0.3f, size.width * 0.95f, size.height * 0.18f)
                }
                drawPath(
                    p, c.ink3,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 14f))),
                )
            },
    ) {
        val w = maxWidth
        val h = maxHeight
        nodes.forEachIndexed { i, (t, pos) ->
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(120L * i)
                shown = true
            }
            val scale by animateFloatAsState(if (shown) 1f else 0.4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pop$i")
            Box(
                Modifier
                    .offset(x = w * pos.x, y = h * pos.y)
                    .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (shown) 1f else 0f },
            ) { TechniqueToken(t, if (i == 4) 64.dp else 56.dp, 20.dp) }
        }
    }
}

// ------------------------------------------------------------------ onboarding steps

@Composable
private fun OnboardingHeader(step: Int, onBack: () -> Unit) {
    val c = Itera.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CircleIconButton(IteraIcons.Back, stringResource(R.string.action_back), onBack)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                val f by animateFloatAsState(if (i < step) 1f else 0f, label = "seg$i")
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(c.line).drawBehind {
                    drawRect(c.ink, size = size.copy(width = size.width * f))
                })
            }
        }
        Text(stringResource(R.string.onb_step, step, 3), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
    }
}

@Composable
fun GoalsScreen(vm: AppViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val c = Itera.colors
    ScreenColumn(bottom = { IteraButton(stringResource(R.string.action_continue), onNext, enabled = vm.focusSkills.isNotEmpty()) }) {
        OnboardingHeader(1, onBack)
        Text(stringResource(R.string.goals_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.goals_sub), style = Itera.type.body, color = c.ink2)
        FocusAreaRows(vm)
    }
}

@Composable
fun FocusAreaRows(vm: AppViewModel) {
    val c = Itera.colors
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Skill.entries.forEach { skill ->
                val on = skill in vm.focusSkills
                val sc = skill.colors(c.isDark)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .border(2.dp, if (on) c.ink else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(20.dp))
                        .toggleable(value = on, role = Role.Checkbox) { vm.toggleFocusSkill(skill) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(sc.container), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(sc.content))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(skill.title), style = Itera.type.label, color = c.ink)
                        Text(stringResource(skill.description), style = Itera.type.bodySmall, color = c.ink2)
                    }
                    if (on) RadioDot(true)
                }
            }
        }
}

@Composable
fun RhythmScreen(vm: AppViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val c = Itera.colors
    ScreenColumn(bottom = { IteraButton(stringResource(R.string.action_continue), onNext) }) {
        OnboardingHeader(2, onBack)
        Text(stringResource(R.string.rhythm_title), style = Itera.type.title, color = c.ink)
        IteraCard(padding = androidx.compose.foundation.layout.PaddingValues(16.dp), gap = 8.dp) {
            TimeRow(IteraIcons.Today, stringResource(R.string.rhythm_morning), stringResource(R.string.rhythm_morning_sub), formatTime(vm.morningTime)) {
                vm.morningTime = vm.morningTime.plusMinutes(30)
            }
            Divider()
            TimeRow(IteraIcons.Reflection, stringResource(R.string.rhythm_evening), stringResource(R.string.rhythm_evening_sub), formatTime(vm.eveningTime)) {
                vm.eveningTime = vm.eveningTime.plusMinutes(30)
            }
        }
        if (!notificationsGranted()) NotificationRationaleCard()
        Text(stringResource(R.string.rhythm_time_q), style = Itera.type.label, color = c.ink)
        Segmented(
            options = listOf(5 to stringResource(R.string.minutes_short, 5), 15 to stringResource(R.string.minutes_short, 15), 30 to stringResource(R.string.minutes_plus, 30)),
            selected = vm.dailyMinutes,
            onSelect = { vm.dailyMinutes = it },
        )
        Text(stringResource(R.string.rhythm_time_hint), style = Itera.type.bodySmall, color = c.ink2)
    }
}

/** Production addition: Android 13+ asks for POST_NOTIFICATIONS on Continue; this card explains why first. */
@Composable
private fun notificationsGranted(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

@Composable
fun NotificationRationaleCard() {
    val c = Itera.colors
    IteraCard(color = c.surface2, padding = androidx.compose.foundation.layout.PaddingValues(16.dp), gap = 6.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(IteraIcons.Bell, null, tint = c.ink, modifier = Modifier.size(18.dp).padding(top = 1.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.rhythm_notif_title), style = Itera.type.body, fontWeight = FontWeight.SemiBold, color = c.ink)
                Text(stringResource(R.string.rhythm_notif_body), style = Itera.type.bodySmall, color = c.ink2)
            }
        }
    }
}

/** A setting row with a tappable time chip. Tapping steps the time by 30 minutes (a real picker comes later). */
@Composable
fun TimeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, value: String, onClick: () -> Unit) {
    val c = Itera.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Itera.type.body, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
        }
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(c.surface2)
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text(value, style = Itera.type.headline.copy(fontWeight = FontWeight.SemiBold), color = c.ink) }
    }
}

@Composable
fun FirstWeekScreen(onBack: () -> Unit, onStart: () -> Unit) {
    val c = Itera.colors
    ScreenColumn(gap = 18.dp, bottom = { IteraButton(stringResource(R.string.week_start), onStart) }) {
        OnboardingHeader(3, onBack)
        Text(stringResource(R.string.week_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.week_sub), style = Itera.type.body, color = c.ink2)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Program.days.take(7).forEachIndexed { i, t ->
                val day = i + 1
                val sc = t.skill.colors(c.isDark)
                if (day == 1) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(stringResource(R.string.day_n, day), style = Itera.type.caption.copy(fontWeight = FontWeight.Bold), color = c.accent, modifier = Modifier.width(52.dp))
                        TechniqueToken(t, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(t.title), style = Itera.type.label, color = c.ink)
                            Text(stringResource(t.skill.title), style = Itera.type.caption, color = c.ink2)
                        }
                        Pill(stringResource(R.string.nav_today), c.accentSoft, c.accent)
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(stringResource(R.string.day_n, day), style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2, modifier = Modifier.width(52.dp))
                        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(8.dp).clip(CircleShape).background(sc.content)) }
                        Text(
                            stringResource(t.title) + if (day == 4) " " + stringResource(R.string.week_review_suffix) else "",
                            style = Itera.type.body, color = c.ink,
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(IteraIcons.Reflection, null, tint = c.ink2, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.week_every_evening), style = Itera.type.bodySmall, color = c.ink2)
        }
    }
}
