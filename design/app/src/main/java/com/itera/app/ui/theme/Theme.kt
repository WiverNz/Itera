package com.itera.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.itera.app.model.ThemeMode

object Itera {
    val colors: IteraColors
        @Composable get() = LocalIteraColors.current
    val type: IteraType
        @Composable get() = LocalIteraType.current
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun IteraTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.ink, onPrimary = colors.onInk, background = colors.bg, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, surfaceVariant = colors.surface2, onSurfaceVariant = colors.ink2,
            surfaceContainerLow = colors.surface, surfaceContainerHigh = colors.surface, outline = colors.line,
            secondary = colors.accent, scrim = colors.scrim,
        )
    } else {
        lightColorScheme(
            primary = colors.ink, onPrimary = colors.onInk, background = colors.bg, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, surfaceVariant = colors.surface2, onSurfaceVariant = colors.ink2,
            surfaceContainerLow = colors.surface, surfaceContainerHigh = colors.surface, outline = colors.line,
            secondary = colors.accent, scrim = colors.scrim,
        )
    }
    val type = rememberIteraType()
    CompositionLocalProvider(LocalIteraColors provides colors, LocalIteraType provides type) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
