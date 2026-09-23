package com.wivernz.itera.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import com.wivernz.itera.core.designsystem.theme.DarkColors
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.LightColors
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.core.designsystem.theme.colors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NightSurfaceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overridesOnlyItsSubtree() {
        var outer = DarkColors
        var inner = LightColors
        var after = DarkColors
        compose.setContent {
            IteraTheme(false) {
                outer = Itera.colors
                NightSurface {
                    inner = Itera.colors
                    assertEquals(DarkColors.bg, MaterialTheme.colorScheme.background)
                }
                after = Itera.colors
            }
        }
        compose.runOnIdle {
            assertEquals(LightColors, outer)
            assertEquals(DarkColors, inner)
            assertEquals(LightColors, after)
        }
    }
}
