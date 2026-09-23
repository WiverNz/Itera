package com.wivernz.itera.core.designsystem
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class UiTextTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resourceAndPluralResolveInsideComposition() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val values = mutableListOf<String>()
        compose.setContent {
            values.clear()
            values += UiText.Res(R.string.activity_program, listOf(5)).asString()
            for (count in 0..2) {
                values +=
                    UiText.Plural(
                        R.plurals.practice_count,
                        count,
                        listOf(count)
                    )
                        .asString()
            }
        }
        compose.runOnIdle {
            assertEquals(
                listOf(
                    "Now · 5 min",
                    "0 practices",
                    "1 practice",
                    "2 practices"
                ),
                values
            )
            assertEquals("private", UiText.Raw("private").asString(context))
        }
    }
}
