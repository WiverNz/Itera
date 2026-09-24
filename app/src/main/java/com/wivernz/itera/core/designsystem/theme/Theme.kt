package com.wivernz.itera.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.wivernz.itera.domain.model.ThemePreference

private val LocalThemeInstalled = androidx.compose.runtime.staticCompositionLocalOf { false }

object Itera {
    val colors: IteraColors
        @Composable get() = LocalIteraColors.current
    val type: IteraType
        @Composable get() = LocalIteraType.current
}

@Composable
fun ThemePreference.isDark(): Boolean = when (this) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}

@Composable
fun IteraTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val reduceMotion = if (LocalThemeInstalled.current) {
        LocalReduceMotion.current
    } else {
        rememberReduceMotion()
    }
    SystemBarAppearance(dark)
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.ink,
            onPrimary = colors.onInk,
            background = colors.bg,
            onBackground = colors.ink,

            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.ink2,

            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surface,
            outline = colors.line,

            secondary = colors.accent, scrim = colors.scrim
        )
    } else {
        lightColorScheme(
            primary = colors.ink,
            onPrimary = colors.onInk,
            background = colors.bg,
            onBackground = colors.ink,

            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.ink2,

            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surface,
            outline = colors.line,

            secondary = colors.accent, scrim = colors.scrim
        )
    }
    val type = rememberIteraType()
    CompositionLocalProvider(
        LocalThemeInstalled provides true,
        LocalIteraColors provides colors,
        LocalIteraType provides type,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = androidx.compose.material3.Typography(
                bodyLarge = type.body,
                bodyMedium = type.bodySmall,
                bodySmall = type.caption,
                labelLarge = type.label,
                titleLarge = type.headline,
                headlineLarge = type.display
            ),
            content = content
        )
    }
}
