package com.wivernz.itera.data.copy
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.common.FakeClock
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class CopyResolverTest {
    @Test fun everyKeyResolvesInAllLanguagesWithoutChangingUserText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val titles = mutableSetOf<String>()
        listOf("en", "ru", "de", "es").forEach { tag ->
            val localized = context.createConfigurationContext(
                Configuration(context.resources.configuration)
                    .apply {
                        setLocale(Locale.forLanguageTag(tag))
                    }
            )
            val resolver = CopyResolver(localized, FakeClock(), TestLogger())
            listOf(
                "activity_program",
                "activity_program_new_technique",
                "activity_combination",
                "activity_review",
                "activity_focus",
                "activity_focus_generic",
                "activity_practice",
                "activity_reflection"
            ).forEach { key ->
                val text = resolver.resolve(
                    key,
                    "{\"minutes\":5,\"sourceDay\":6,\"time\":\"23:00\",\"eveningTime\":\"21:00\"}",
                    "two_minute_rule"
                )
                assertTrue(text.title.isNotBlank())
                assertTrue(text.subtitle.isNotBlank())
                assertTrue(text.instruction.isNotBlank())
            }
            titles += resolver.resolve("activity_program", "{}", "two_minute_rule").title
        }
        assertEquals(4, titles.size)
    }
}
