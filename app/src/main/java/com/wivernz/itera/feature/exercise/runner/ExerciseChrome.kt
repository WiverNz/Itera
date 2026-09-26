package com.wivernz.itera.feature.exercise.runner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.feature.exercise.template.CompletionGate
import com.wivernz.itera.feature.exercise.template.CompletionRules
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Draft autosave shared by every exercise body: a 2 s debounce, plus [flush] on `ON_STOP` so a swipe-away never
 * loses the last seconds of typing. Drafts are user text and are never logged.
 */
class DraftAutosave<T : Any>(
    private val scope: CoroutineScope,
    private val debounce: Duration = DEBOUNCE,
    private val save: suspend (T) -> Unit
) {
    private var pending: T? = null
    private var job: Job? = null

    fun schedule(draft: T) {
        pending = draft
        job?.cancel()
        job = scope.launch {
            delay(debounce)
            write()
        }
    }

    /** Writes a pending draft now. */
    fun flush() {
        job?.cancel()
        job = scope.launch { write() }
    }

    /** Drops a pending draft, e.g. once the exercise is completed. */
    fun cancel() {
        job?.cancel()
        pending = null
    }

    private suspend fun write() {
        val draft = pending ?: return
        pending = null
        save(draft)
    }

    companion object {
        val DEBOUNCE: Duration = 2.seconds
    }
}

/** Caps free text at 4 000 characters. */
fun capped(text: String): String = text.take(CompletionRules.MAX_CHARS)

/** A [NoteField] capped at 4 000 characters, with a counter once past 3 500. */
@Composable
fun CappedNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 2,
    textStyle: TextStyle = Itera.type.userText,
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NoteField(
            value,
            { onValueChange(capped(it)) },
            placeholder,
            minLines = minLines,
            textStyle = textStyle,
            bordered = bordered,
            modifier = modifier,
            onDone = onDone
        )
        if (value.length > CompletionRules.COUNTER_FROM) {
            Text(
                stringResource(R.string.exercise_chars, value.length, CompletionRules.MAX_CHARS),
                style = Itera.type.caption,
                color = Itera.colors.ink2,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Why the primary is disabled, for its `stateDescription`; null when it is enabled. */
@Composable
fun gateReason(gate: CompletionGate): String? = when (gate) {
    CompletionGate.Ready -> null
    is CompletionGate.TickMore ->
        pluralStringResource(R.plurals.gate_tick, gate.missing, gate.missing)
    CompletionGate.FillBlocks -> stringResource(R.string.gate_fill)
    CompletionGate.PickOne -> stringResource(R.string.gate_pick)
}

/** A disabled button is never silent: its reason is exposed to TalkBack. */
fun Modifier.gated(reason: String?): Modifier =
    if (reason == null) this else semantics { stateDescription = reason }

/** The exercise confirmation dialog (leave, end session, discard review). */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = Itera.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ConfirmDialog"),
        containerColor = c.surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = Itera.type.headline, color = c.ink) },
        text = { Text(body, style = Itera.type.body, color = c.ink2) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, style = Itera.type.label, color = c.ink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismiss, style = Itera.type.label, color = c.ink2)
            }
        }
    )
}

/** "Leave this exercise?" - shown only when there is something to lose. */
@Composable
fun LeaveExerciseDialog(onLeave: () -> Unit, onStay: () -> Unit) = ConfirmDialog(
    title = stringResource(R.string.exercise_leave_title),
    body = stringResource(R.string.exercise_leave_body),
    confirm = stringResource(R.string.exercise_leave),
    dismiss = stringResource(R.string.exercise_keep_going),
    onConfirm = onLeave,
    onDismiss = onStay
)
