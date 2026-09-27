package com.wivernz.itera.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.DayDot
import com.wivernz.itera.domain.model.ProgressSummary
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.model.SkillProgress
import java.time.LocalDate
import java.time.YearMonth

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun ProgressPreview() {
    val day = LocalDate.of(2026, 9, 22)
    val summary = ProgressSummary(
        1,
        1,
        day,
        listOf(DayDot(day, true, setOf(Skill.FOCUS))),
        Skill.entries.map { SkillProgress(it, SkillLevel.STARTING, 1, 1, 14) },
        5
    )
    IteraTheme { ProgressScreen(ProgressUiState(summary, day, false), {}, {}, {}) }
}

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun HistoryPreview() {
    val day = LocalDate.of(2026, 9, 22)
    IteraTheme {
        HistoryScreen(
            HistoryUiState(
                YearMonth.from(day),
                day,
                listOf(HistoryDay(day, emptyList(), emptyList())),
                loading = false
            ),
            {},
            {},
            {}
        )
    }
}
