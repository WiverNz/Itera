package com.wivernz.itera

import android.app.Application
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontLoadingTest {
    @Test
    fun allFourBundledFontsLoad() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        listOf(
            R.font.bricolage_grotesque,
            R.font.instrument_sans,
            R.font.inter_tight,
            R.font.inter
        ).forEach {
            assertNotNull(ResourcesCompat.getFont(app, it))
        }
    }
}
