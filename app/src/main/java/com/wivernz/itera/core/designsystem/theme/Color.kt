package com.wivernz.itera.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.wivernz.itera.domain.model.Skill

/** Design tokens. Values match the Itera design canvas. */
@Immutable
data class IteraColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val onInk: Color,
    val scrim: Color,
    val isDark: Boolean
)

val LightColors = IteraColors(
    bg = Color(0xFFF4F2EC),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAE7DF),
    ink = Color(0xFF1A1C19),
    ink2 = Color(0xFF585C55),
    ink3 = Color(0xFFA9ACA4),
    line = Color(0xFFE0DCD2),
    accent = Color(0xFFBF4526),
    accentSoft = Color(0xFFFBE4DA),
    onInk = Color(0xFFF4F2EC),
    scrim = Color(0x6B1A1C19),
    isDark = false
)

val DarkColors = IteraColors(
    bg = Color(0xFF111311),
    surface = Color(0xFF1B1E1B),
    surface2 = Color(0xFF262A26),
    ink = Color(0xFFEDECE6),
    ink2 = Color(0xFFA6AAA2),
    ink3 = Color(0xFF5C605A),
    line = Color(0xFF2D312D),
    accent = Color(0xFFF2825F),
    accentSoft = Color(0xFF3A231B),
    onInk = Color(0xFF111311),
    scrim = Color(0x99000000),
    isDark = true
)

/** Tinted background + strong foreground for each skill. */
@Immutable
data class SkillColors(val container: Color, val content: Color)

fun Skill.colors(dark: Boolean): SkillColors = when (this) {
    Skill.FOCUS -> if (dark) {
        SkillColors(
            Color(0xFF1C2736),
            Color(0xFF93B5E6)
        )
    } else {
        SkillColors(Color(0xFFE0E9F6), Color(0xFF2F5B93))
    }
    Skill.PLANNING -> if (dark) {
        SkillColors(
            Color(0xFF2E2618),
            Color(0xFFE2B669)
        )
    } else {
        SkillColors(Color(0xFFF3E6CD), Color(0xFF835610))
    }
    Skill.LEARNING -> if (dark) {
        SkillColors(
            Color(0xFF261F36),
            Color(0xFFB9A3EC)
        )
    } else {
        SkillColors(Color(0xFFE9E3F6), Color(0xFF5C409A))
    }
    Skill.HABITS -> if (dark) {
        SkillColors(
            Color(0xFF18291E),
            Color(0xFF88C99E)
        )
    } else {
        SkillColors(Color(0xFFDCEEE2), Color(0xFF2D6A42))
    }
    Skill.REFLECTION -> if (dark) {
        SkillColors(
            Color(0xFF321C1F),
            Color(0xFFEA9CA4)
        )
    } else {
        SkillColors(Color(0xFFF5E0E0), Color(0xFF983A45))
    }
}

val LocalIteraColors = staticCompositionLocalOf { LightColors }
