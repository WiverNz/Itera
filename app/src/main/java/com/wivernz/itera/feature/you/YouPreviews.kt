package com.wivernz.itera.feature.you

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.wivernz.itera.core.designsystem.theme.IteraTheme

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun YouPreview() {
    IteraTheme { YouScreen(YouUiState(loading = false), {}, true) }
}

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun ExportPreview() {
    IteraTheme { ExportSheet(ExportUiState(), {}, {}, {}) }
}

@PreviewLightDark
@Preview(fontScale = 2f)
@Composable
private fun TopicsPreview() {
    IteraTheme { TopicsScreen(emptyList(), {}, {}) }
}
