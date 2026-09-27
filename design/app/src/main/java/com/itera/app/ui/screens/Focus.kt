package com.itera.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.model.Skill
import com.itera.app.ui.components.VoiceCommandPill
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.components.screenInsets
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.IteraTheme
import com.itera.app.ui.theme.colors
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

/**
 * Minimal focus screen: big timer, the current task, three controls.
 * Always dark, whatever the app theme, and keeps the screen on while running.
 */
@Composable
fun FocusScreen(minutes: Int, onEnd: () -> Unit) {
    IteraTheme(dark = true) { FocusContent(minutes, onEnd) }
}

@Composable
private fun FocusContent(minutes: Int, onEnd: () -> Unit) {
    val c = Itera.colors
    val accent = Skill.Focus.colors(true).content
    var total by rememberSaveable { mutableIntStateOf(minutes * 60) }
    var left by rememberSaveable { mutableIntStateOf(minutes * 60) }
    var running by rememberSaveable { mutableStateOf(true) }

    val view = LocalView.current
    // Light system-bar icons on the dark focus screen, restored when leaving.
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLight = controller?.isAppearanceLightStatusBars ?: false
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = wasLight
            controller?.isAppearanceLightNavigationBars = wasLight
        }
    }
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(running) {
        while (running && left > 0) {
            delay(1000)
            left -= 1
        }
        if (left == 0) onEnd()
    }

    val progress by animateFloatAsState(1f - left.toFloat() / total, tween(900), label = "ring")
    val time = "%02d:%02d".format(left / 60, left % 60)

    val scrollTimer = LocalDensity.current.fontScale > 1.3f
    val timerScroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().background(c.bg).screenInsets()
            .then(if (scrollTimer) Modifier.verticalScroll(timerScroll) else Modifier).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(IteraIcons.DeepWork, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.focus_label), style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = accent, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Text(stringResource(R.string.focus_silenced), style = Itera.type.caption, color = c.ink2)
        }
        VoiceCommandPill(Modifier.align(Alignment.End).padding(top = 8.dp))
        Spacer(if (scrollTimer) Modifier.height(24.dp) else Modifier.weight(1f))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(accent, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, style = Itera.type.timer, color = c.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(if (running) R.string.focus_remaining else R.string.focus_paused), style = Itera.type.body, color = c.ink2)
            }
        }
        Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.focus_working_on).uppercase(), style = Itera.type.eyebrow, color = c.ink2)
            Text(stringResource(R.string.focus_default_task), style = Itera.type.headline, color = c.ink, textAlign = TextAlign.Center)
        }
        Spacer(if (scrollTimer) Modifier.height(24.dp) else Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            TextControl(stringResource(R.string.focus_plus5)) {
                total += 300
                left += 300
            }
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(c.ink)
                    .clickable(role = Role.Button, onClickLabel = stringResource(if (running) R.string.focus_pause else R.string.focus_resume)) { running = !running },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (running) IteraIcons.Pause else IteraIcons.Play, stringResource(if (running) R.string.focus_pause else R.string.focus_resume), tint = c.bg, modifier = Modifier.size(30.dp))
            }
            TextControl(stringResource(R.string.focus_end), onEnd)
        }
        Text(
            stringResource(R.string.focus_break_hint),
            style = Itera.type.bodySmall, color = c.ink2, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Composable
private fun TextControl(text: String, onClick: () -> Unit) {
    Box(
        Modifier.widthIn(min = 72.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold), color = Itera.colors.ink2) }
}
