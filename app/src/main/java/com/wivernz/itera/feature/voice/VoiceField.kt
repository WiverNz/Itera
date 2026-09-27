// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.domain.voice.VoiceText

/**
 * A note field with push-to-talk dictation (docs/ux/10-voice-input.md). The mic captures the field and its
 * selection; the final transcript replaces only that selection, once, through the same [onValueChange] typing
 * uses, so limits, validation and autosave are unchanged. Dictation never reaches the command parser. Any edit,
 * caret move or leaving the screen while listening cancels, so a late result cannot overwrite newer text.
 */
@Composable
fun VoiceNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxChars: Int,
    minLines: Int = 2,
    textStyle: TextStyle = Itera.type.userText,
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    val voice = LocalVoiceController.current
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (field.text == value) {
        field
    } else {
        TextFieldValue(value, TextRange(value.length)).also { field = it }
    }
    val owner = remember { Any() }
    var captured by remember { mutableStateOf<TextFieldValue?>(null) }
    var overflow by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val latest by rememberUpdatedState(shown)
    val emit by rememberUpdatedState(onValueChange)

    fun apply(transcript: String): Boolean {
        val snapshot = captured ?: return true
        val now = latest
        if (now.text != snapshot.text || now.selection != snapshot.selection) {
            captured = null
            return true
        }
        val inserted = VoiceText.insert(
            snapshot.text,
            snapshot.selection.min,
            snapshot.selection.max,
            transcript
        )
        if (inserted.text.length > maxChars) return false
        captured = null
        field = TextFieldValue(inserted.text, TextRange(inserted.caret))
        emit(inserted.text)
        return true
    }

    DisposableEffect(voice, owner) { onDispose { voice?.cancelIfOwner(owner) } }

    val active = voice?.isActive(owner) == true
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteField(
            shown,
            { next ->
                val edited = next.text != shown.text || next.selection != shown.selection
                if (active && edited) {
                    voice?.cancelIfOwner(owner)
                    captured = null
                }
                field = next
                if (next.text != value) onValueChange(next.text)
            },
            placeholder,
            minLines = minLines,
            textStyle = textStyle,
            bordered = bordered,
            modifier = modifier.focusRequester(focus),
            onDone = onDone,
            trailing = voice?.let { controller ->
                {
                    MicButton(
                        stringResource(R.string.voice_dictate_a11y, placeholder),
                        listening = active && controller.state is VoiceSessionState.Listening,
                        onClick = {
                            if (active) {
                                controller.cancel()
                                captured = null
                            } else {
                                overflow = null
                                runCatching { focus.requestFocus() }
                                captured = latest
                                controller.start(owner) { alternatives ->
                                    val best = alternatives.first { it.isNotBlank() }
                                    if (!apply(best)) overflow = best.trim()
                                }
                            }
                        }
                    )
                }
            }
        )
        if (voice != null && active) {
            VoiceStatusPanel(voice, voice.state, command = false, onDismiss = { captured = null })
        }
        overflow?.let { text ->
            OverflowPreview(
                text,
                fits = {
                    VoiceText.insert(
                        latest.text,
                        latest.selection.min,
                        latest.selection.max,
                        it
                    ).text.length <=
                        maxChars
                },
                onEdit = { overflow = it },
                onInsert = {
                    captured = latest
                    if (apply(it)) overflow = null
                },
                onDiscard = {
                    overflow = null
                    captured = null
                }
            )
        }
    }
}

@Composable
internal fun MicButton(
    label: String,
    listening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val state = if (listening) stringResource(R.string.voice_listening) else null
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (listening) c.ink else c.surface)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                if (state != null) stateDescription = state
            }
            .testTag("VoiceMic"),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            IteraIcons.Mic,
            contentDescription = null,
            tint = if (listening) c.onInk else c.ink2,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** "English", in the current UI language. */
@Composable
internal fun languageName(language: VoiceLanguage): String =
    AppLanguage.localName(language.tag.substringBefore('-'), currentLocale())

/**
 * The shared listening/permission/failure/unavailable states, for a field ([command] false) or the command
 * sheet. Static text and icon: nothing animates, so reduced motion needs no variant.
 */
@Composable
internal fun VoiceStatusPanel(
    voice: VoiceController,
    state: VoiceSessionState,
    command: Boolean,
    onDismiss: () -> Unit
) {
    val c = Itera.colors
    val context = LocalContext.current
    val language = languageName(voice.language)
    val cancel = {
        voice.cancel()
        onDismiss()
    }
    val status: String
    val detail: String?
    val actions: List<Pair<String, () -> Unit>>
    when (state) {
        VoiceSessionState.Idle -> return
        is VoiceSessionState.Listening -> {
            status = stringResource(
                if (state.stopping) R.string.voice_processing else R.string.voice_listening
            )
            detail = stringResource(
                if (command) R.string.voice_mode_command else R.string.voice_mode_dictation,
                language
            )
            actions = listOfNotNull(
                (stringResource(R.string.voice_stop) to voice::stop).takeUnless { state.stopping },
                stringResource(R.string.voice_cancel) to cancel
            )
        }
        is VoiceSessionState.NeedsPermission -> {
            status = stringResource(R.string.voice_permission_title)
            detail = stringResource(R.string.voice_permission_body)
            actions = listOf(
                stringResource(R.string.voice_permission_continue) to voice::requestPermission,
                stringResource(R.string.voice_not_now) to cancel
            )
        }
        is VoiceSessionState.PermissionDenied -> {
            status = stringResource(R.string.voice_permission_denied)
            detail =
                if (state.permanent) {
                    stringResource(
                        R.string.voice_permission_settings_body
                    )
                } else {
                    null
                }
            actions = listOfNotNull(
                (stringResource(R.string.voice_open_settings) to { openAppSettings(context) })
                    .takeIf { state.permanent },
                stringResource(
                    if (command) R.string.voice_cancel else R.string.voice_keep_typing
                ) to
                    cancel
            )
        }
        is VoiceSessionState.Unavailable -> {
            status = stringResource(R.string.voice_unavailable_title)
            detail = when (state.reason) {
                VoiceUnavailable.DEVICE -> stringResource(R.string.voice_unavailable_device)
                VoiceUnavailable.LANGUAGE -> stringResource(
                    R.string.voice_unavailable_language,
                    language
                )
            }
            actions = listOf(
                stringResource(R.string.voice_speech_settings) to { openSpeechSettings(context) },
                stringResource(
                    if (command) R.string.voice_cancel else R.string.voice_keep_typing
                ) to
                    cancel
            )
        }
        is VoiceSessionState.Failed -> {
            status = stringResource(
                when (state.failure) {
                    VoiceFailure.NO_SPEECH -> R.string.voice_no_speech
                    VoiceFailure.BUSY -> R.string.voice_busy
                    VoiceFailure.FAILED -> R.string.voice_failed
                }
            )
            detail = null
            actions = listOf(
                stringResource(R.string.voice_retry) to voice::retry,
                stringResource(
                    if (command) R.string.voice_cancel else R.string.voice_keep_typing
                ) to
                    cancel
            )
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface2)
            .padding(14.dp)
            .testTag("VoicePanel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(IteraIcons.Mic, null, tint = c.ink, modifier = Modifier.size(18.dp))
            Text(
                status,
                style = Itera.type.label,
                color = c.ink,
                modifier = Modifier.padding(start = 8.dp).semantics {
                    liveRegion =
                        LiveRegionMode.Polite
                }
            )
        }
        detail?.let { Text(it, style = Itera.type.bodySmall, color = c.ink2) }
        (state as? VoiceSessionState.Listening)?.partial?.let { partial ->
            val provisional = stringResource(R.string.voice_provisional)
            Text(
                partial,
                style = Itera.type.body.copy(fontStyle = FontStyle.Italic),
                color = c.ink2,
                modifier = Modifier.testTag("VoicePartial").semantics {
                    stateDescription =
                        provisional
                }
            )
        }
        PanelActions(actions)
    }
}

@Composable
internal fun PanelActions(actions: List<Pair<String, () -> Unit>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEachIndexed { index, (label, action) ->
            IteraButton(
                label,
                action,
                kind = if (index == 0) ButtonKind.Primary else ButtonKind.Secondary,
                height = 44.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** A result that would push the field past its limit stays editable here instead of losing words. */
@Composable
private fun OverflowPreview(
    text: String,
    fits: (String) -> Boolean,
    onEdit: (String) -> Unit,
    onInsert: (String) -> Unit,
    onDiscard: () -> Unit
) {
    val c = Itera.colors
    val ok = fits(text)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface2)
            .padding(14.dp)
            .testTag("VoiceOverflow"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.voice_too_long),
            style = Itera.type.bodySmall,
            color = c.ink2,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        NoteField(text, onEdit, stringResource(R.string.voice_preview), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IteraButton(
                stringResource(R.string.voice_insert),
                { onInsert(text) },
                enabled = ok,
                height = 44.dp,
                modifier = Modifier.weight(1f).testTag("VoiceInsert")
            )
            IteraButton(
                stringResource(R.string.voice_discard),
                onDiscard,
                kind = ButtonKind.Secondary,
                height = 44.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
