package com.wivernz.itera.core.common
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AppLanguageTest {
    @Test fun allFiveTagsRoundTrip() {
        try {
            AppLanguage.tags.forEach {
                AppLanguage.set(it)
                assertEquals(it, AppLanguage.current())
            }
        } finally {
            AppLanguage.set("")
        }
        assertEquals("", AppLanguage.current())
        assertEquals("Русский", AppLanguage.nativeName("ru"))
        assertEquals("German", AppLanguage.localName("de", Locale.ENGLISH))
    }
}
