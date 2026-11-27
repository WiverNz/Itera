package com.itera.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itera.app.R
import com.itera.app.data.PredictionState
import com.itera.app.data.PredictionViewModel
import com.itera.app.ui.components.*
import com.itera.app.ui.theme.Itera
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PredictionRoute(onClose: () -> Unit, vm: PredictionViewModel = viewModel()) {
    PredictionScreen(vm.state, vm.saveFailed, vm.loadFailed, vm::edit, vm::next,
        vm::previous, { if (vm.save()) onClose() }, onClose, vm::reload,
        vm::closeWithoutOutcome, vm::replace)
}

@Composable
fun PredictionScreen(
    state: PredictionState,
    saveFailed: Boolean,
    loadFailed: Boolean,
    onEdit: (PredictionState) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSaveClose: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onEnd: () -> Unit,
    onReplace: () -> Unit,
) {
    val c = Itera.colors
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (loadFailed) onClose() else onSaveClose() }
    if (confirmReplace || confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false; confirmEnd = false },
            title = { Text(stringResource(if (confirmReplace) R.string.pt_replace else R.string.pt_end)) },
            text = { Text(stringResource(if (confirmReplace) R.string.pt_replace_body else R.string.pt_end_body)) },
            confirmButton = { TextButton(onClick = {
                if (confirmReplace) onReplace() else onEnd()
                confirmReplace = false; confirmEnd = false
            }) { Text(stringResource(R.string.action_done)) } },
            dismissButton = { TextButton(onClick = { confirmReplace = false; confirmEnd = false }) {
                Text(stringResource(R.string.pt_cancel))
            } },
        )
    }
    androidx.compose.runtime.key(state.page, state.closed) {
        ScreenColumn(gap = 22.dp, bottom = {
            if (!loadFailed) {
                if (!state.finished) {
                    IteraButton(stringResource(when (state.page) {
                        1 -> R.string.pt_save_try
                        2 -> R.string.pt_record
                        5 -> R.string.pt_finish
                        else -> R.string.action_next
                    }), onNext, enabled = state.canAdvance)
                }
                IteraButton(stringResource(R.string.pt_save_leave), onSaveClose, kind = ButtonKind.Ghost)
            }
        }) {
            TopBar("", if (loadFailed) onClose else onSaveClose)
            Text(stringResource(R.string.pt_title), style = Itera.type.bodySmall, color = c.ink2)
            if (loadFailed) {
                Text(stringResource(R.string.pt_load_failed), style = Itera.type.body, color = c.ink)
                IteraButton(stringResource(R.string.pt_retry), onRetry)
            } else {
                if (saveFailed) Text(stringResource(R.string.pt_save_failed), style = Itera.type.body, color = c.ink)
                val title = when {
                    state.closed -> R.string.pt_closed
                    state.finished -> R.string.pt_reviewed
                    else -> listOf(R.string.pt_predict, R.string.pt_plan, R.string.pt_ready,
                        R.string.pt_actual, R.string.pt_compare, R.string.pt_learn)[state.page]
                }
                Text(stringResource(title), style = Itera.type.display, color = c.ink,
                    modifier = Modifier.semantics { heading() })
                when {
                    state.finished -> {
                        PredictionSummary(state)
                        if (state.outcome.isNotBlank()) ReadAnswer(R.string.pt_actual, state.outcome)
                        if (state.comparison in 0..3) ReadAnswer(R.string.pt_compare, stringResource(comparisonLabels[state.comparison]))
                        ConfidenceRead(R.string.pt_after, state.confidenceAfter)
                        if (state.lesson.isNotBlank()) ReadAnswer(R.string.pt_lesson, state.lesson)
                        else if (state.noConclusion) ReadAnswer(R.string.pt_lesson, stringResource(R.string.pt_no_conclusion))
                        Text(stringResource(R.string.pt_no_credit), style = Itera.type.bodySmall, color = c.ink2)
                        IteraButton(stringResource(R.string.pt_replace), { confirmReplace = true }, kind = ButtonKind.Secondary)
                    }
                    state.page == 0 -> {
                        Text(stringResource(R.string.pt_intro), style = Itera.type.body, color = c.ink2)
                        AnswerField(R.string.pt_prediction, state.prediction) { onEdit(state.copy(prediction = it)) }
                        ConfidenceChoice(R.string.pt_before, state.confidenceBefore) { onEdit(state.copy(confidenceBefore = it)) }
                    }
                    state.page == 1 -> {
                        ReadAnswer(R.string.pt_prediction, state.prediction)
                        Text(stringResource(R.string.pt_safe), style = Itera.type.bodySmall, color = c.ink2)
                        AnswerField(R.string.pt_action, state.action) { onEdit(state.copy(action = it)) }
                        AnswerField(R.string.pt_evidence, state.evidence) { onEdit(state.copy(evidence = it)) }
                    }
                    state.page == 2 -> {
                        PredictionSummary(state)
                        Text(stringResource(R.string.pt_return), style = Itera.type.body, color = c.ink2)
                        IteraButton(stringResource(R.string.pt_end), { confirmEnd = true }, kind = ButtonKind.Ghost)
                    }
                    state.page == 3 -> {
                        ReadAnswer(R.string.pt_action, state.action)
                        ReadAnswer(R.string.pt_evidence, state.evidence)
                        AnswerField(R.string.pt_outcome, state.outcome) { onEdit(state.copy(outcome = it)) }
                    }
                    state.page == 4 -> {
                        ReadAnswer(R.string.pt_prediction, state.prediction)
                        ReadAnswer(R.string.pt_actual, state.outcome)
                        Text(stringResource(R.string.pt_neutral), style = Itera.type.bodySmall, color = c.ink2)
                        Choices(comparisonLabels, state.comparison) { onEdit(state.copy(comparison = it)) }
                    }
                    state.page == 5 -> {
                        ConfidenceRead(R.string.pt_before, state.confidenceBefore)
                        ConfidenceChoice(R.string.pt_after, state.confidenceAfter) { onEdit(state.copy(confidenceAfter = it)) }
                        AnswerField(R.string.pt_lesson, state.lesson) { onEdit(state.copy(lesson = it)) }
                        ChoiceChip(stringResource(R.string.pt_no_conclusion), state.noConclusion,
                            { onEdit(state.copy(noConclusion = !state.noConclusion)) })
                        Text(stringResource(R.string.pt_learning_hint), style = Itera.type.bodySmall, color = c.ink2)
                    }
                }
                if (!state.finished && (state.page == 1 || state.page in 4..5)) {
                    IteraButton(stringResource(R.string.action_back), onPrevious, kind = ButtonKind.Ghost)
                }
            }
        }
    }
}

private val confidenceLabels = listOf(R.string.pt_c0, R.string.pt_c1, R.string.pt_c2, R.string.pt_c3, R.string.pt_c4)
private val comparisonLabels = listOf(R.string.pt_supported, R.string.pt_contradicted, R.string.pt_mixed, R.string.pt_inconclusive)

@Composable
private fun AnswerField(label: Int, value: String, onChange: (String) -> Unit) {
    val text = stringResource(label)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = Itera.type.label, color = Itera.colors.ink)
        NoteField(value, onChange, text,
            modifier = Modifier.semantics { contentDescription = text })
    }
}

@Composable
private fun ReadAnswer(label: Int, value: String) {
    IteraCard(gap = 8.dp) {
        Text(stringResource(label), style = Itera.type.bodySmall, color = Itera.colors.ink2)
        Text(value, style = Itera.type.userText, color = Itera.colors.ink)
    }
}

@Composable
private fun ConfidenceRead(label: Int, value: Int) {
    ReadAnswer(label, stringResource(confidenceLabels.getOrElse(value) { R.string.pt_not_rated }))
}

@Composable
private fun ConfidenceChoice(label: Int, value: Int, onChange: (Int) -> Unit) {
    Text(stringResource(label), style = Itera.type.label, color = Itera.colors.ink)
    Choices(confidenceLabels + R.string.pt_not_rated, if (value < 0) 5 else value) {
        onChange(if (it == 5) -1 else it)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(labels: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            ChoiceChip(stringResource(label), selected == i, { onSelect(i) }, singleSelect = true)
        }
    }
}

@Composable
private fun PredictionSummary(state: PredictionState) {
    if (state.preparedDate.isNotEmpty()) {
        val date = LocalDate.parse(state.preparedDate).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()))
        Text(stringResource(R.string.pt_prepared_on, date), style = Itera.type.bodySmall, color = Itera.colors.ink2)
    }
    ReadAnswer(R.string.pt_prediction, state.prediction)
    ConfidenceRead(R.string.pt_before, state.confidenceBefore)
    ReadAnswer(R.string.pt_action, state.action)
    ReadAnswer(R.string.pt_evidence, state.evidence)
}



@androidx.compose.ui.tooling.preview.Preview(name = "Prediction · light", showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Prediction · large text", fontScale = 2f, showBackground = true)
@Composable
private fun PredictionPreview() {
    com.itera.app.ui.theme.IteraTheme(dark = false) {
        PredictionScreen(PredictionState(), false, false, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Prediction · dark", showBackground = true)
@Composable
private fun PredictionDarkPreview() {
    com.itera.app.ui.theme.IteraTheme(dark = true) {
        PredictionScreen(PredictionState(), false, false, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
