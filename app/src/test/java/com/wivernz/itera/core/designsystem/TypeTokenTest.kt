package com.wivernz.itera.core.designsystem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.theme.createIteraType
import com.wivernz.itera.core.designsystem.theme.fontResources
import com.wivernz.itera.core.designsystem.theme.resourceFamily
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class TypeTokenTest {
    @Test fun everyMetricAndFamilyInEveryLanguage() {
        listOf("en", "ru", "de", "es").forEach { language ->
            val locale = Locale.forLanguageTag(language)
            val t = createIteraType(locale)
            val styles = listOf(
                t.hero,
                t.display,
                t.title,
                t.headline,
                t.bodyLarge,
                t.body,
                t.bodySmall,
                t.caption,
                t.label,
                t.eyebrow,
                t.timer
            )
            val sizes = listOf(42, 34, 30, 22, 18, 16, 14, 13, 17, 13, 80)
            val heights = listOf(44, 36, 33, 27, 26, 23, 20, 18, 22, 16, 84)
            val weights = listOf(700, 700, 700, 700, 400, 400, 400, 400, 600, 600, 600)
            val spacing = listOf(
                (-0.02).em,
                (-0.02).em,
                (-0.02).em,
                (-0.01).em,
                TextUnit.Unspecified,
                TextUnit.Unspecified,
                TextUnit.Unspecified,
                TextUnit.Unspecified,
                TextUnit.Unspecified,
                0.06.em,
                (-0.04).em
            )
            styles.forEachIndexed { i, style ->
                assertEquals(
                    sizes[i].sp,
                    style.fontSize
                )
                assertEquals(
                    heights[i].sp,
                    style.lineHeight
                )
                assertEquals(
                    FontWeight(weights[i]),
                    style.fontWeight
                )
                assertEquals(
                    spacing[i],
                    style.letterSpacing
                )
            }
            val pair = fontResources(locale)
            assertEquals(
                if (language ==
                    "ru"
                ) {
                    R.font.inter_tight to R.font.inter
                } else {
                    R.font.bricolage_grotesque to
                        R.font.instrument_sans
                },
                pair
            )
            assertEquals(resourceFamily(R.font.inter), t.userText.fontFamily)
            assertEquals(resourceFamily(R.font.inter), t.userTextLarge.fontFamily)
        }
        assertEquals(
            R.font.inter_tight to R.font.inter,
            fontResources(Locale.forLanguageTag("sr-Cyrl"))
        )
    }
}
