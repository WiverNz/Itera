package com.wivernz.itera.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
internal fun SystemBarAppearance(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.activity()?.window ?: return
    DisposableEffect(window, dark) {
        val controller = WindowCompat.getInsetsController(window, view)
        val status = controller.isAppearanceLightStatusBars
        val navigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        onDispose {
            controller.isAppearanceLightStatusBars = status
            controller.isAppearanceLightNavigationBars = navigation
        }
    }
}

/** Focus, evening reflection and day complete always use the night palette. */
@Composable
fun NightSurface(content: @Composable () -> Unit) {
    IteraTheme(dark = true, content = content)
}
