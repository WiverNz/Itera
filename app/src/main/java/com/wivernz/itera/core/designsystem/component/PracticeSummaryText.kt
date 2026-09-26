package com.wivernz.itera.core.designsystem.component
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wivernz.itera.R
import com.wivernz.itera.domain.model.PracticeKind
import com.wivernz.itera.domain.model.PracticeSummary
import com.wivernz.itera.domain.model.RecallGrade
@Composable
fun summaryText(summary: PracticeSummary): String = when (summary) {
    is PracticeSummary.Review -> stringResource(
        when (summary.grade) {
            com.wivernz.itera.domain.model.RecallGrade.FORGOT -> R.string.review_forgot
            com.wivernz.itera.domain.model.RecallGrade.PARTIAL -> R.string.review_partial
            com.wivernz.itera.domain.model.RecallGrade.SOLID -> R.string.review_solid
        }
    )
    is PracticeSummary.Text -> summary.value
    is PracticeSummary.Topic -> stringResource(R.string.detail_explained, summary.title)
    is PracticeSummary.Habit -> stringResource(
        R.string.detail_habit_summary,
        summary.anchor,
        summary.habit
    )
    is PracticeSummary.Count -> pluralStringResource(
        when (summary.kind) {
            PracticeKind.MINUTES -> R.plurals.detail_minutes
            PracticeKind.TASKS -> R.plurals.detail_tasks
            PracticeKind.REASONS -> R.plurals.detail_reasons
            PracticeKind.STEPS -> R.plurals.detail_steps
            PracticeKind.ITEMS -> R.plurals.detail_items
        },
        summary.count,
        summary.count
    )
}
