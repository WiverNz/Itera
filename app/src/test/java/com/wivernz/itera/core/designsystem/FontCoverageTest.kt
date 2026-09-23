package com.wivernz.itera.core.designsystem
import android.app.Application
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.theme.fontResources
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontCoverageTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val punctuation = "-·“”‘’%"
    private val sets = mapOf(
        "en" to (('a'..'z') + ('A'..'Z')).joinToString(""),
        "de" to "äöüÄÖÜß",
        "es" to "áéíóúüñÁÉÍÓÚÑ¿¡",
        "ru" to (('а'..'я') + ('А'..'Я') + listOf('ё', 'Ё')).joinToString("")
    )

    @Test fun everyResolvedFontCoversItsLanguageWithoutSystemFallback() {
        sets.forEach { (tag, required) ->
            val pair = fontResources(Locale.forLanguageTag(tag))
            listOf(pair.first, pair.second, R.font.inter).forEach { resource ->
                val paint = Paint().apply {
                    typeface =
                        requireNotNull(ResourcesCompat.getFont(app, resource))
                }
                (required + punctuation).forEach { character ->
                    assertTrue(
                        "$tag $resource $character native glyph",
                        paint.hasGlyph(character.toString())
                    )
                    assertTrue(
                        "$tag $resource $character bundled glyph",
                        containsGlyph(resource, character.code)
                    )
                }
            }
        }
    }

    @Test fun latinFontSwapIsDetectedEvenWhenAndroidCouldFallback() {
        assertFalse(containsGlyph(R.font.bricolage_grotesque, 'Ж'.code))
        assertFalse(containsGlyph(R.font.instrument_sans, 'Ж'.code))
        assertTrue(containsGlyph(R.font.inter, 'Ж'.code))
        assertTrue(containsGlyph(R.font.inter_tight, 'Ж'.code))
    }

    /** Check the bundled cmap too: Paint.hasGlyph alone permits Android's fallback. */
    private fun containsGlyph(resource: Int, code: Int): Boolean {
        val buffer = ByteBuffer.wrap(
            app.resources.openRawResource(resource).use {
                it.readBytes()
            }
        ).order(ByteOrder.BIG_ENDIAN)
        fun u16(offset: Int) = buffer.getShort(offset).toInt() and 65535
        var cmap = -1
        repeat(u16(4)) { i ->
            val offset = 12 + 16 * i
            if (buffer.getInt(offset) == 0x636d6170) cmap = buffer.getInt(offset + 8)
        }
        check(cmap >= 0)
        repeat(u16(cmap + 2)) { i ->
            val table = cmap + buffer.getInt(cmap + 4 + i * 8 + 4)
            when (u16(table)) {
                12 -> {
                    repeat(buffer.getInt(table + 12)) { group ->
                        val offset = table + 16 + group * 12
                        val start = buffer.getInt(offset)
                        val end = buffer.getInt(offset + 4)
                        if (code in start..end) return buffer.getInt(offset + 8) + code - start != 0
                    }
                }
                4 -> {
                    val count = u16(table + 6) / 2
                    val ends = table + 14
                    val starts = ends + 2 * count + 2
                    val deltas = starts + 2 * count
                    val offsets = deltas + 2 * count
                    repeat(count) { segment ->
                        if (code in u16(starts + segment * 2)..u16(ends + segment * 2)) {
                            val delta = u16(deltas + segment * 2)
                            val range = u16(offsets + segment * 2)
                            val glyph = if (range == 0) {
                                (code + delta) and 65535
                            } else {
                                val raw = u16(
                                    offsets + segment * 2 + range +
                                        (code - u16(starts + segment * 2)) * 2
                                )
                                if (raw == 0) 0 else (raw + delta) and 65535
                            }
                            if (glyph != 0) return true
                        }
                    }
                }
            }
        }
        return false
    }
}
