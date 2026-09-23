package com.wivernz.itera.core.designsystem
import androidx.compose.ui.graphics.Color
import com.wivernz.itera.core.designsystem.theme.DarkColors
import com.wivernz.itera.core.designsystem.theme.IteraColors
import com.wivernz.itera.core.designsystem.theme.LightColors
import com.wivernz.itera.core.designsystem.theme.SkillColors
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.Skill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class ColorTokenTest {
    @Test fun exactPalettes() {
        fun tokens(c: IteraColors) = listOf(
            c.bg,
            c.surface,
            c.surface2,
            c.ink,
            c.ink2,
            c.ink3,
            c.line,
            c.accent,
            c.accentSoft,
            c.onInk,
            c.scrim
        )
        assertEquals(
            listOf(
                0xFFF4F2EC,
                0xFFFFFFFF,
                0xFFEAE7DF,
                0xFF1A1C19,
                0xFF585C55,
                0xFFA9ACA4,
                0xFFE0DCD2,
                0xFFBF4526,
                0xFFFBE4DA,
                0xFFF4F2EC,
                0x6B1A1C19
            ).map {
                Color(it)
            },
            tokens(LightColors)
        )
        assertEquals(
            listOf(
                0xFF111311,
                0xFF1B1E1B,
                0xFF262A26,
                0xFFEDECE6,
                0xFFA6AAA2,
                0xFF5C605A,
                0xFF2D312D,
                0xFFF2825F,
                0xFF3A231B,
                0xFF111311,
                0x99000000
            ).map {
                Color(it)
            },
            tokens(DarkColors)
        )
        assertFalse(LightColors.isDark)
        assertTrue(DarkColors.isDark)
    }

    @Test fun exactSkillPairs() {
        val light = listOf(
            0xFFE0E9F6 to 0xFF2F5B93,
            0xFFF3E6CD to 0xFF835610,
            0xFFE9E3F6 to 0xFF5C409A,
            0xFFDCEEE2 to 0xFF2D6A42,
            0xFFF5E0E0 to 0xFF983A45
        )
        val dark = listOf(
            0xFF1C2736 to 0xFF93B5E6,
            0xFF2E2618 to 0xFFE2B669,
            0xFF261F36 to 0xFFB9A3EC,
            0xFF18291E to 0xFF88C99E,
            0xFF321C1F to 0xFFEA9CA4
        )
        Skill.entries.forEachIndexed { i, skill ->
            assertEquals(
                SkillColors(
                    Color(light[i].first),
                    Color(light[i].second)
                ),
                skill.colors(false)
            )
            assertEquals(
                SkillColors(Color(dark[i].first), Color(dark[i].second)),
                skill.colors(true)
            )
        }
    }
}
