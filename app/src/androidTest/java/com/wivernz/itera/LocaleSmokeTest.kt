package com.wivernz.itera

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Use phase=select/assert/restore in separate invocations around an adb force-stop. */
@RunWith(AndroidJUnit4::class)
class LocaleSmokeTest {
    @Test
    fun languageSelectionAndPersistence() {
        val phase = InstrumentationRegistry.getArguments().getString("phase") ?: "roundtrip"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            if (phase == "select" || phase == "roundtrip") {
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
            if (phase != "restore") {
                scenario.onActivity {
                    assertEquals("ru", AppCompatDelegate.getApplicationLocales().toLanguageTags())
                    assertEquals("Итера", it.getString(R.string.app_name))
                }
            }
            if (phase == "restore" || phase == "roundtrip") {
                scenario.onActivity {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
        }
    }
}
