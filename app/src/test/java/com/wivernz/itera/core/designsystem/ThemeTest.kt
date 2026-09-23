package com.wivernz.itera.core.designsystem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.wivernz.itera.core.designsystem.theme.DarkColors
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.LightColors
import com.wivernz.itera.core.designsystem.theme.colors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchesPaletteLive() {
        val dark = mutableStateOf(false)
        var colors = LightColors
        var background = LightColors.bg
        compose.setContent {
            IteraTheme(dark.value) {
                colors = Itera.colors
                background = MaterialTheme.colorScheme.background
            }
        }
        compose.runOnIdle {
            assertEquals(LightColors, colors)
            assertEquals(LightColors.bg, background)
            dark.value =
                true
        }
        compose.runOnIdle {
            assertEquals(DarkColors, colors)
            assertEquals(DarkColors.bg, background)
        }
    }
}
