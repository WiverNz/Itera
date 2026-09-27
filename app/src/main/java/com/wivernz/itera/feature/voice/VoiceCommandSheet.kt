// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.RadioDot
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceParse
import com.wivernz.itera.feature.exercise.runner.ConfirmDialog

/**
 * The labelled "Voice command" action and its shared sheet (docs/ux/10-voice-input.md), for the screen's
 * [LocalVoiceCommandHost]. Hidden where voice is not wired; keyboard and touch stay the primary path everywhere.
 */
@Composable
fun VoiceCommandAction(modifier: Modifier = Modifier) {
    val voice = LocalVoiceController.current ?: return
    val host = LocalVoiceCommandHost.current ?: return
    val c = Itera.colors
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.voice_command)
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = { open = true })
            .padding(horizontal = 14.dp)
            .testTag("VoiceCommand"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(IteraIcons.Mic, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink
        )
    }
    if (open) VoiceCommandSheet(voice, host, onClose = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandSheet(voice: VoiceController, host: VoiceCommandHost, onClose: () -> Unit) {
    val c = Itera.colors
    val scope = rememberCoroutineScope()
    val flow = remember(host) { VoiceCommandFlow(host, scope) }
    val close = {
        voice.cancelIfOwner(flow)
        flow.reset()
        onClose()
    }
    // Background ends listening and any pending choice; nothing is replayed on return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { close() }
    val phase = flow.phase
    LaunchedEffect(phase) {
        if (phase is VoiceCommandPhase.Done && phase.feedback == VoiceFeedback.Handover) close()
    }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        tonalElevation = 0.dp,
        containerColor = c.surface,
        scrimColor = c.scrim,
        modifier = Modifier.testTag("VoiceSheet")
    ) {
        Column(
            Modifier.verticalScroll(
                rememberScrollState()
            ).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.voice_command), style = Itera.type.title, color = c.ink)
            Text(
                stringResource(R.string.voice_mode_command, languageName(voice.language)),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
            val listen = {
                flow.reset()
                voice.start(flow) { flow.onFinal(it, voice.language) }
            }
            if (voice.isActive(flow)) {
                VoiceStatusPanel(voice, voice.state, command = true, onDismiss = {})
            } else {
                PhaseContent(flow, listen, close)
            }
        }
    }
    (phase as? VoiceCommandPhase.Confirming)?.let { confirming ->
        ConfirmationDialog(confirming, onConfirm = flow::confirm, onDismiss = flow::reset)
    }
}

@Composable
private fun PhaseContent(flow: VoiceCommandFlow, listen: () -> Unit, close: () -> Unit) {
    val c = Itera.colors
    when (val phase = flow.phase) {
        VoiceCommandPhase.Ready, is VoiceCommandPhase.Confirming -> {
            Text(
                stringResource(R.string.voice_command_sub),
                style = Itera.type.body,
                color = c.ink2
            )
            Examples(flow.commands)
            IteraButton(
                stringResource(R.string.voice_speak),
                listen,
                icon = IteraIcons.Mic,
                modifier = Modifier.testTag("VoiceSpeak")
            )
        }
        VoiceCommandPhase.Working -> Message(stringResource(R.string.voice_processing))
        is VoiceCommandPhase.Done -> {
            Message(feedbackText(phase.feedback))
            IteraButton(
                stringResource(R.string.action_done),
                close,
                modifier = Modifier.testTag("VoiceDone")
            )
        }
        is VoiceCommandPhase.Rejected -> {
            Message(rejectionText(phase.reason))
            Examples(flow.commands)
            PanelActions(
                listOf(
                    stringResource(R.string.voice_retry) to listen,
                    stringResource(R.string.voice_cancel) to close
                )
            )
        }
        is VoiceCommandPhase.NotUnderstood -> {
            Text(
                stringResource(R.string.voice_heard, phase.heard),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
            Message(notUnderstoodText(phase.parse))
            Examples(flow.commands)
            PanelActions(
                listOf(
                    stringResource(R.string.voice_retry) to listen,
                    stringResource(R.string.voice_cancel) to close
                )
            )
        }
        is VoiceCommandPhase.Competing -> Choice(
            stringResource(R.string.voice_choose_command),
            phase.commands.map { commandText(it) to null },
            phase.selected,
            flow::select,
            flow::confirm,
            flow::reset
        )
        is VoiceCommandPhase.Choosing -> Choice(
            stringResource(R.string.voice_choose_item),
            phase.candidates.map {
                it.label to
                    stringResource(R.string.voice_item_position, it.position)
            },
            phase.selected,
            flow::select,
            flow::confirm,
            flow::reset
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
        color = Itera.colors.ink,
        modifier = Modifier.testTag("VoiceMessage").semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
private fun Examples(commands: Set<VoiceCommandKind>) {
    val c = Itera.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.voice_command_here), style = Itera.type.label, color = c.ink)
        VoiceCommandKind.entries.filter { it in commands }.forEach { kind ->
            Text(
                exampleText(kind),
                style = Itera.type.body.copy(fontFamily = Itera.type.userText.fontFamily),
                color = c.ink2
            )
        }
    }
}

/** Radio rows in the language sheet's treatment, then an explicit Confirm. */
@Composable
private fun Choice(
    title: String,
    rows: List<Pair<String, String?>>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val c = Itera.colors
    Text(title, style = Itera.type.label, color = c.ink)
    Column {
        rows.forEachIndexed { index, (label, sub) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected == index, role = Role.RadioButton, onClick = {
                        onSelect(index)
                    })
                    .testTag("VoiceOption"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = Itera.type.label.copy(fontFamily = Itera.type.userText.fontFamily),
                        color = c.ink
                    )
                    sub?.let { Text(it, style = Itera.type.bodySmall, color = c.ink2) }
                }
                RadioDot(selected == index)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IteraButton(
            stringResource(R.string.voice_confirm),
            onConfirm,
            enabled = selected != null,
            height = 44.dp,
            modifier = Modifier.weight(1f).testTag("VoiceConfirm")
        )
        IteraButton(
            stringResource(R.string.voice_cancel),
            onCancel,
            kind = ButtonKind.Secondary,
            height = 44.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConfirmationDialog(
    confirming: VoiceCommandPhase.Confirming,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    when (confirming.action) {
        VoiceAction.ShowRecommendation -> ConfirmDialog(
            title = stringResource(R.string.exercise_leave_title),
            body = stringResource(R.string.voice_leave_body),
            confirm = stringResource(R.string.exercise_leave),
            dismiss = stringResource(R.string.exercise_keep_going),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
        else -> ConfirmDialog(
            title = stringResource(R.string.voice_complete_title),
            body = listOfNotNull(
                stringResource(R.string.voice_complete_body),
                confirming.note?.let { stringResource(R.string.voice_complete_mitigation_note) }
            ).joinToString(" "),
            confirm = stringResource(R.string.voice_complete),
            dismiss = stringResource(R.string.exercise_keep_going),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun exampleText(kind: VoiceCommandKind): String = stringResource(
    when (kind) {
        VoiceCommandKind.ADD_ITEM -> R.string.voice_example_add
        VoiceCommandKind.COMPLETE_ITEM -> R.string.voice_example_complete
        VoiceCommandKind.START_FOCUS -> R.string.voice_example_start
        VoiceCommandKind.PAUSE_FOCUS -> R.string.voice_example_pause
        VoiceCommandKind.RESUME_FOCUS -> R.string.voice_example_resume
        VoiceCommandKind.END_FOCUS -> R.string.voice_example_end
        VoiceCommandKind.COMPLETE_CURRENT_EXERCISE -> R.string.voice_example_complete_exercise
        VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION -> R.string.voice_example_recommendation
    }
)

@Composable
internal fun commandText(command: VoiceCommand): String = when (command) {
    is VoiceCommand.AddItem -> stringResource(R.string.voice_action_add, command.text)
    is VoiceCommand.CompleteItem -> stringResource(R.string.voice_action_complete, command.query)
    is VoiceCommand.StartFocus -> command.minutes?.let {
        stringResource(
            R.string.voice_action_start_minutes,
            stringResource(R.string.minutes_short, it)
        )
    } ?: stringResource(R.string.voice_action_start)
    VoiceCommand.PauseFocus -> stringResource(R.string.voice_action_pause)
    VoiceCommand.ResumeFocus -> stringResource(R.string.voice_action_resume)
    VoiceCommand.EndFocus -> stringResource(R.string.voice_action_end)
    VoiceCommand.CompleteCurrentExercise -> stringResource(R.string.voice_action_complete_exercise)
    VoiceCommand.ShowCurrentRecommendation -> stringResource(R.string.voice_action_recommendation)
}

@Composable
internal fun feedbackText(feedback: VoiceFeedback): String = when (feedback) {
    is VoiceFeedback.Added -> stringResource(R.string.voice_done_added, feedback.text)
    is VoiceFeedback.Completed -> stringResource(R.string.voice_done_completed, feedback.label)
    VoiceFeedback.FocusStarted -> stringResource(R.string.voice_done_focus_started)
    is VoiceFeedback.FocusSetup ->
        stringResource(
            R.string.voice_done_focus_setup,
            stringResource(R.string.minutes_short, feedback.minutes)
        )
    VoiceFeedback.FocusPaused -> stringResource(R.string.voice_done_paused)
    VoiceFeedback.FocusResumed -> stringResource(R.string.voice_done_resumed)
    VoiceFeedback.Recommendation, VoiceFeedback.Handover -> stringResource(
        R.string.voice_done_recommendation
    )
}

@Composable
internal fun rejectionText(reason: VoiceRejection): String = when (reason) {
    VoiceRejection.NotHere -> stringResource(R.string.voice_not_here)
    VoiceRejection.NoList -> stringResource(R.string.voice_no_list)
    VoiceRejection.ListFull -> stringResource(R.string.voice_list_full)
    is VoiceRejection.NoMatch -> stringResource(R.string.voice_no_match, reason.query)
    VoiceRejection.FocusNotRunning -> stringResource(R.string.voice_focus_not_running)
    VoiceRejection.FocusAlreadyPaused -> stringResource(R.string.voice_focus_already_paused)
    VoiceRejection.FocusNotPaused -> stringResource(R.string.voice_focus_not_paused)
    VoiceRejection.FocusActive -> stringResource(R.string.voice_focus_active)
    is VoiceRejection.FocusDuration -> stringResource(
        R.string.voice_focus_duration,
        stringResource(R.string.minutes_short, reason.minutes),
        reason.options.map { stringResource(R.string.minutes_short, it) }.joinToString(" · ")
    )
    VoiceRejection.NoFocusEntry -> stringResource(R.string.voice_focus_no_entry)
    VoiceRejection.ExerciseNotReady -> stringResource(R.string.voice_exercise_not_ready)
    VoiceRejection.Stale -> stringResource(R.string.voice_stale)
    VoiceRejection.Failed -> stringResource(R.string.voice_error)
}

@Composable
private fun notUnderstoodText(parse: VoiceParse): String = when (parse) {
    is VoiceParse.MissingArgument -> stringResource(
        if (parse.kind ==
            VoiceCommandKind.ADD_ITEM
        ) {
            R.string.voice_missing_text
        } else {
            R.string.voice_missing_query
        }
    )
    is VoiceParse.InvalidArgument -> stringResource(
        if (parse.kind ==
            VoiceCommandKind.START_FOCUS
        ) {
            R.string.voice_invalid_focus
        } else {
            R.string.voice_invalid_command
        }
    )
    else -> stringResource(R.string.voice_not_understood)
}

/** The current screen's command host; screens place [VoiceCommandAction] without knowing their view model. */
val LocalVoiceCommandHost = staticCompositionLocalOf<VoiceCommandHost?> { null }

/** Where ShowCurrentRecommendation goes: Today's hero. Provided by the navigation layer. */
val LocalVoiceToToday = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun ProvideVoiceCommands(host: VoiceCommandHost, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalVoiceCommandHost provides host, content = content)
