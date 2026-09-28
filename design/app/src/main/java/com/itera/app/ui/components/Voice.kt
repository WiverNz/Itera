package com.itera.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.itera.app.R
import com.itera.app.ui.screens.AppLanguage
import com.itera.app.ui.screens.RadioDot
import com.itera.app.ui.screens.currentLocale
import com.itera.app.ui.theme.Itera
import com.itera.app.ui.theme.IteraTheme

/**
 * Milestone 012 voice affordances (docs/ux/10-voice-input.md). The prototype has no recogniser: the mic and the
 * command sheet walk through the shared states so they can be compared with production.
 */
// ADR-0022 (amended): SystemConsent when only the system default exists; ChooseProvider/ProviderConsent when neither
// exists and installed apps offer recognition. Nothing is pre-selected; the named app needs its own consent.
enum class VoiceDemoState { Idle, Listening, Partial, SystemConsent, ChooseProvider, ProviderConsent, Permission, Denied, Unavailable, NoSpeech }

/** The prototype has no real providers; production shows each installed app's own label and icon. */
const val SAMPLE_PROVIDER = "Speech app"

@Composable
fun VoiceMic(label: String, listening: Boolean, onClick: () -> Unit) {
    val c = Itera.colors
    val state = if (listening) stringResource(R.string.voice_listening) else null
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (listening) c.ink else c.surface)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                if (state != null) stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(IteraIcons.Mic, null, tint = if (listening) c.onInk else c.ink2, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun languageName(): String = AppLanguage.localName(currentLocale().language, currentLocale())

/** The shared listening/permission/failure/unavailable panel under a field or in the command sheet. */
@Composable
fun VoicePanel(state: VoiceDemoState, command: Boolean, onPrimary: () -> Unit, onCancel: () -> Unit) {
    if (state == VoiceDemoState.Idle) return
    val c = Itera.colors
    val language = languageName()
    val keep = stringResource(if (command) R.string.voice_cancel else R.string.voice_keep_typing)
    val (status, detail, actions) = when (state) {
        VoiceDemoState.Listening, VoiceDemoState.Partial -> Triple(
            stringResource(R.string.voice_listening),
            stringResource(if (command) R.string.voice_mode_command else R.string.voice_mode_dictation, language),
            listOf(stringResource(R.string.voice_stop), stringResource(R.string.voice_cancel)),
        )
        VoiceDemoState.SystemConsent -> Triple(
            stringResource(R.string.voice_system_title),
            stringResource(R.string.voice_system_body),
            listOf(stringResource(R.string.voice_system_use), stringResource(R.string.voice_not_now)),
        )
        VoiceDemoState.ChooseProvider -> Triple(
            stringResource(R.string.voice_provider_title),
            stringResource(R.string.voice_provider_body),
            listOf(keep),
        )
        VoiceDemoState.ProviderConsent -> Triple(
            stringResource(R.string.voice_provider_consent_title, SAMPLE_PROVIDER),
            stringResource(R.string.voice_provider_consent_body, SAMPLE_PROVIDER),
            listOf(stringResource(R.string.voice_provider_use, SAMPLE_PROVIDER), stringResource(R.string.voice_not_now)),
        )
        VoiceDemoState.Permission -> Triple(
            stringResource(R.string.voice_permission_title),
            stringResource(R.string.voice_permission_body),
            listOf(stringResource(R.string.voice_permission_continue), stringResource(R.string.voice_not_now)),
        )
        VoiceDemoState.Denied -> Triple(
            stringResource(R.string.voice_permission_denied),
            stringResource(R.string.voice_permission_settings_body),
            listOf(stringResource(R.string.voice_open_settings), keep),
        )
        VoiceDemoState.Unavailable -> Triple(
            stringResource(R.string.voice_unavailable_title),
            stringResource(R.string.voice_unavailable_language, language),
            listOf(stringResource(R.string.voice_speech_settings), keep),
        )
        else -> Triple(stringResource(R.string.voice_no_speech), null, listOf(stringResource(R.string.voice_retry), keep))
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface2).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IteraIcons.Mic, null, tint = c.ink, modifier = Modifier.size(18.dp))
            Text(status, style = Itera.type.label, color = c.ink, modifier = Modifier.padding(start = 8.dp))
        }
        detail?.let { Text(it, style = Itera.type.bodySmall, color = c.ink2) }
        if (state == VoiceDemoState.Partial) {
            Text(stringResource(R.string.voice_preview), style = Itera.type.body.copy(fontStyle = FontStyle.Italic), color = c.ink2)
        }
        if (state == VoiceDemoState.ChooseProvider) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
                    .clickable(role = Role.Button, onClick = onPrimary).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(IteraIcons.Mic, null, tint = c.ink2, modifier = Modifier.size(20.dp))
                Text(SAMPLE_PROVIDER, style = Itera.type.body, color = c.ink)
            }
        }
        if (actions.size == 1) {
            IteraButton(actions[0], onCancel, Modifier.fillMaxWidth().border(1.dp, c.ink2, RoundedCornerShape(18.dp)), kind = ButtonKind.Secondary, height = 44.dp)
        } else if (LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IteraButton(actions[0], onPrimary, height = 44.dp)
                IteraButton(actions[1], onCancel, Modifier.border(1.dp, c.ink2, RoundedCornerShape(18.dp)), kind = ButtonKind.Secondary, height = 44.dp)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IteraButton(actions[0], onPrimary, Modifier.weight(1f), height = 44.dp)
                IteraButton(actions[1], onCancel, Modifier.weight(1f).border(1.dp, c.ink2, RoundedCornerShape(18.dp)), kind = ButtonKind.Secondary, height = 44.dp)
            }
        }
    }
}

/** The mic beside an exercise field: tap to listen, Stop/Cancel, and the failure state a real session can end in. */
@Composable
fun DictationMic(placeholder: String, state: VoiceDemoState, onState: (VoiceDemoState) -> Unit) {
    VoiceMic(stringResource(R.string.voice_dictate_a11y, placeholder), state == VoiceDemoState.Listening) {
        onState(if (state == VoiceDemoState.Idle) VoiceDemoState.Listening else VoiceDemoState.Idle)
    }
}

/** The labelled "Voice command" action; opens the shared sheet. */
@Composable
fun VoiceCommandPill(modifier: Modifier = Modifier) {
    val c = Itera.colors
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = { open = true })
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(IteraIcons.Mic, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.voice_command), style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
    }
    if (open) VoiceCommandSheet { open = false }
}

private enum class SheetPhase { Ready, Listening, Choosing, Confirming, Done }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandSheet(onDismiss: () -> Unit) {
    val c = Itera.colors
    var phase by remember { mutableStateOf(SheetPhase.Ready) }
    var selected by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        tonalElevation = 0.dp,
        containerColor = c.surface,
        scrimColor = c.scrim,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.voice_command), style = Itera.type.title, color = c.ink)
            Text(stringResource(R.string.voice_mode_command, languageName()), style = Itera.type.bodySmall, color = c.ink2)
            when (phase) {
                SheetPhase.Ready, SheetPhase.Confirming -> {
                    Text(stringResource(R.string.voice_command_sub), style = Itera.type.body, color = c.ink2)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.voice_command_here), style = Itera.type.label, color = c.ink)
                        listOf(
                            R.string.voice_example_add, R.string.voice_example_complete, R.string.voice_example_complete_exercise,
                            R.string.voice_example_recommendation,
                        ).forEach { Text(stringResource(it), style = Itera.type.body.copy(fontFamily = Itera.type.userText.fontFamily), color = c.ink2) }
                    }
                    IteraButton(stringResource(R.string.voice_speak), { phase = SheetPhase.Listening }, icon = IteraIcons.Mic)
                }
                SheetPhase.Listening -> VoicePanel(VoiceDemoState.Listening, command = true, onPrimary = { phase = SheetPhase.Choosing }, onCancel = { phase = SheetPhase.Ready })
                SheetPhase.Choosing -> {
                    Text(stringResource(R.string.voice_choose_item), style = Itera.type.label, color = c.ink)
                    Column {
                        listOf(1, 3).forEachIndexed { i, position ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected == i, role = Role.RadioButton) { selected = i },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.voice_sample_item), style = Itera.type.label.copy(fontFamily = Itera.type.userText.fontFamily), color = c.ink)
                                    Text(stringResource(R.string.voice_item_position, position), style = Itera.type.bodySmall, color = c.ink2)
                                }
                                RadioDot(selected == i)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IteraButton(stringResource(R.string.voice_confirm), { phase = SheetPhase.Done }, Modifier.weight(1f), enabled = selected != null, height = 44.dp)
                        IteraButton(stringResource(R.string.voice_cancel), { phase = SheetPhase.Ready }, Modifier.weight(1f), kind = ButtonKind.Secondary, height = 44.dp)
                    }
                }
                SheetPhase.Done -> {
                    Text(
                        stringResource(R.string.voice_done_completed, stringResource(R.string.voice_sample_item)),
                        style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink,
                    )
                    IteraButton(stringResource(R.string.action_done), onDismiss)
                }
            }
        }
    }
    if (phase == SheetPhase.Confirming) {
        AlertDialog(
            onDismissRequest = { phase = SheetPhase.Ready },
            containerColor = c.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.voice_complete_title), style = Itera.type.headline, color = c.ink) },
            text = { Text(stringResource(R.string.voice_complete_body), style = Itera.type.body, color = c.ink2) },
            confirmButton = { TextButton({ phase = SheetPhase.Done }) { Text(stringResource(R.string.voice_complete), style = Itera.type.label, color = c.ink) } },
            dismissButton = { TextButton({ phase = SheetPhase.Ready }) { Text(stringResource(R.string.voice_cancel), style = Itera.type.label, color = c.ink2) } },
        )
    }
}

/** Every shared voice state in one place, for side-by-side comparison with production. */
@Preview(showBackground = true, heightDp = 1400)
@Preview(showBackground = true, heightDp = 1400, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun VoiceStatesPreview() {
    IteraTheme {
        Column(Modifier.background(Itera.colors.bg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NoteField("", {}, stringResource(R.string.voice_preview), dictation = true)
            VoiceDemoState.entries.drop(1).forEach { VoicePanel(it, command = false, onPrimary = {}, onCancel = {}) }
            VoiceCommandPill()
        }
    }
}
