package com.wivernz.itera

import android.app.Application
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BootstrapPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun disableAnimations() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test fun bricolagePreviewRenders() = render(R.font.bricolage_grotesque)

    @Test fun instrumentPreviewRenders() = render(R.font.instrument_sans)

    @Test fun interTightPreviewRenders() = render(R.font.inter_tight)

    @Test fun interPreviewRenders() = render(R.font.inter)

    @Test
    @Config(qualifiers = "+night")
    fun bricolageDarkPreviewRenders() = render(R.font.bricolage_grotesque)

    @Test
    @Config(qualifiers = "+night")
    fun instrumentDarkPreviewRenders() = render(R.font.instrument_sans)

    @Test
    @Config(qualifiers = "+night")
    fun interTightDarkPreviewRenders() = render(R.font.inter_tight)

    @Test
    @Config(qualifiers = "+night")
    fun interDarkPreviewRenders() = render(R.font.inter)

    private fun render(font: Int) {
        compose.setContent { BootstrapPreview(font) }
        compose.onNodeWithText("Itera").assertIsDisplayed()
    }
}
