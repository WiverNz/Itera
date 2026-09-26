package com.wivernz.itera.feature.focus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.screenInsets
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.ConfirmDialog

class FocusNavigation(
    val showResult: (Long, String) -> Unit,
    val returnToChain: () -> Unit,
    val close: () -> Unit,
    val toToday: () -> Unit
)

@Composable
fun FocusSessionRoute(vm: FocusViewModel, navigation: FocusNavigation) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is FocusEffect.Completed -> {
                if (effect.natural) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    chime(context)
                }
                if (effect.chainStep) {
                    navigation.returnToChain()
                } else {
                    navigation.showResult(effect.activityId, effect.techniqueId)
                }
            }
            FocusEffect.TooShort -> navigation.toToday()
        }
    }
    FocusScreen(
        state = state,
        notificationsAllowed = notificationsAllowed(context),
        onTask = vm::setTask,
        onMinutes = vm::setMinutes,
        onStart = vm::start,
        onClose = navigation.close,
        onExtend = vm::extend,
        onTogglePause = vm::togglePause,
        onEnd = vm::requestEnd,
        onConfirmEnd = vm::confirmEnd,
        onDismissEnd = vm::dismissEnd
    )
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** A short completion chime that respects the ringer mode. */
private fun chime(context: Context) {
    val audio = context.getSystemService(AudioManager::class.java)
    if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
    runCatching {
        RingtoneManager.getRingtone(
            context,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )?.play()
    }
}

/** The pre-timer sheet, then the prototype's `FocusScreen` (Focus.kt). Always on the night surface. */
@Composable
fun FocusScreen(
    state: FocusUiState,
    notificationsAllowed: Boolean,
    onTask: (String) -> Unit,
    onMinutes: (Int) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
    onExtend: () -> Unit,
    onTogglePause: () -> Unit,
    onEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onDismissEnd: () -> Unit
) {
    val timer = state.timer
    if (timer == null) {
        FocusSetup(state, notificationsAllowed, onTask, onMinutes, onStart, onClose)
    } else {
        FocusTimerContent(timer, onExtend, onTogglePause, onEnd)
    }
    if (state.confirmingEnd) {
        ConfirmDialog(
            title = stringResource(R.string.focus_end_title),
            body = stringResource(R.string.focus_end_body),
            confirm = stringResource(R.string.focus_end),
            dismiss = stringResource(R.string.exercise_keep_going),
            onConfirm = onConfirmEnd,
            onDismiss = onDismissEnd
        )
    }
}

@Composable
private fun FocusSetup(
    state: FocusUiState,
    notificationsAllowed: Boolean,
    onTask: (String) -> Unit,
    onMinutes: (Int) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    val c = Itera.colors
    val minutesLabel = state.minuteOptions.map { it to stringResource(R.string.minutes_short, it) }
    ScreenColumn(
        gap = 18.dp,
        modifier = Modifier.testTag("FocusSetup"),
        bottom = {
            IteraButton(
                stringResource(R.string.focus_start),
                onStart,
                icon = IteraIcons.Play,
                enabled = state.task.isNotBlank() && !state.busy && !state.loading,
                modifier = Modifier.testTag("FocusStart")
            )
        }
    ) {
        TopBar(stringResource(R.string.focus_label), onClose)
        Text(stringResource(R.string.focus_setup_title), style = Itera.type.title, color = c.ink)
        Text(stringResource(R.string.focus_setup_sub), style = Itera.type.body, color = c.ink2)
        NoteField(
            state.task,
            onTask,
            stringResource(R.string.focus_task_hint),
            minLines = 1,
            bordered = true,
            modifier = Modifier.testTag("FocusTask")
        )
        Text(
            stringResource(R.string.focus_length),
            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink
        )
        Segmented(minutesLabel, state.minutes, onMinutes)
        if (!notificationsAllowed) {
            Text(
                stringResource(R.string.focus_permission_note),
                style = Itera.type.bodySmall,
                color = c.ink2,
                modifier = Modifier.testTag("FocusPermissionNote")
            )
        }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
}

@Composable
private fun FocusTimerContent(
    timer: FocusTimerUi,
    onExtend: () -> Unit,
    onTogglePause: () -> Unit,
    onEnd: () -> Unit
) {
    val c = Itera.colors
    val accent = Skill.FOCUS.colors(true).content
    val running = !timer.paused
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    val reduced = LocalReduceMotion.current
    val progress by animateFloatAsState(
        timer.progress,
        if (reduced) snap() else tween(900),
        label = "ring"
    )
    val left = timer.remainingSeconds
    val time = "%02d:%02d".format(left / 60, left % 60)
    val announcement = when {
        left <= 0 -> stringResource(R.string.focus_announce_done)
        left <= ONE_MINUTE -> pluralStringResource(R.plurals.focus_announce_minutes, 1, 1)
        left <= FIVE_MINUTES -> pluralStringResource(R.plurals.focus_announce_minutes, 5, 5)
        else -> ""
    }
    val status = stringResource(if (running) R.string.focus_remaining else R.string.focus_paused)
    Column(
        Modifier.fillMaxSize().background(c.bg).screenInsets()
            .padding(horizontal = 24.dp, vertical = 16.dp).testTag("FocusTimer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(IteraIcons.DeepWork, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.focus_label),
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = accent,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            Text(
                stringResource(R.string.focus_own_paused),
                style = Itera.type.caption,
                color = c.ink2
            )
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().testTag("FocusRing")) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    c.surface2,
                    0f,
                    360f,
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke)
                )
                drawArc(
                    accent,
                    -90f,
                    360f * progress,
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The readout itself is not a live region: TalkBack hears only 5:00, 1:00 and 0:00.
                Text(
                    time,
                    style = Itera.type.timer,
                    color = c.ink,
                    modifier = Modifier.testTag("FocusReadout").semantics {
                        contentDescription = listOf(time, status).joinToString(" ")
                    }
                )
                Text(status, style = Itera.type.body, color = c.ink2)
                Box(
                    Modifier.size(1.dp).testTag("FocusAnnouncement").semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announcement
                    }
                )
            }
        }
        Column(
            Modifier.padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.focus_working_on).uppercase(),
                style = Itera.type.eyebrow,
                color = c.ink2
            )
            Text(
                timer.taskLabel,
                style = Itera.type.headline.copy(fontFamily = Itera.type.userText.fontFamily),
                color = c.ink,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextControl(
                stringResource(R.string.focus_plus5),
                onExtend,
                Modifier.testTag("FocusExtend")
            )
            val toggle =
                stringResource(if (running) R.string.focus_pause else R.string.focus_resume)
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(c.ink)
                    .clickable(role = Role.Button, onClickLabel = toggle, onClick = onTogglePause)
                    .testTag("FocusToggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (running) IteraIcons.Pause else IteraIcons.Play,
                    toggle,
                    tint = c.bg,
                    modifier = Modifier.size(30.dp)
                )
            }
            TextControl(stringResource(R.string.focus_end), onEnd, Modifier.testTag("FocusEnd"))
        }
        Text(
            pluralStringResource(R.plurals.focus_break_hint_n, BREAK_MINUTES, BREAK_MINUTES),
            style = Itera.type.bodySmall,
            color = c.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
private fun TextControl(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.widthIn(min = 72.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = Itera.colors.ink2
        )
    }
}

private const val ONE_MINUTE = 60
private const val FIVE_MINUTES = 300
private const val BREAK_MINUTES = 5
