package com.wivernz.itera.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel
import kotlinx.coroutines.delay
@Composable
fun EmptyState(
    message: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = Itera.type.bodySmall, color = Itera.colors.ink2)
        if (action != null) IteraButton(action, onAction, kind = ButtonKind.Ghost)
    }
}

@Composable
fun ErrorState(
    message: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    EmptyState(message, action, onAction, modifier)
}

@Composable
fun Skeleton(loading: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        visible = false
        if (loading) {
            delay(150)
            visible = true
        }
    }
    Box(
        modifier.then(
            if (visible) {
                Modifier.clip(
                    RoundedCornerShape(16.dp)
                ).background(Itera.colors.surface2)
            } else {
                Modifier
            }
        )
    )
}
