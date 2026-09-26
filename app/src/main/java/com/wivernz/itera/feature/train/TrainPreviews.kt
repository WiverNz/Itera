package com.wivernz.itera.feature.train

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueDefaults
import com.wivernz.itera.domain.model.TechniqueId

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Large text", fontScale = 2f, showBackground = true)
annotation class TrainPreviews

@Composable
private fun previewTechnique() = Technique(
    TechniqueId("pomodoro"), stringResource(R.string.t_pomodoro_name),
    stringResource(R.string.t_pomodoro_short), stringResource(R.string.t_pomodoro_why),
    Skill.FOCUS, 2, ExerciseType.FOCUS_TIMER, 30, false, emptyList(), null,
    TechniqueDefaults(focusMinutes = 25)
)

@TrainPreviews
@Composable
private fun TrainPreview() = IteraTheme {
    TrainScreen(
        TrainUiState(
            loading = false,
            nodes = listOf(TrainNode(1, previewTechnique(), TrainNodeState.TODAY))
        ),
        {
        }
    )
}

@TrainPreviews
@Composable
private fun LibraryPreview() = IteraTheme {
    LibraryScreen(
        LibraryUiState(
            rows = listOf(LibraryRow(previewTechnique(), false, MasteryLevel.NONE))
        ),
        {
        },
        {}
    )
}

@TrainPreviews
@Composable
private fun TechniquePreview() = IteraTheme {
    TechniqueDetailScreen(TechniqueDetailUiState(technique = previewTechnique(), loading = false), {
    }, {})
}
