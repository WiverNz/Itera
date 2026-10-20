package com.wivernz.itera.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

/**
 * Widths of a label on one line and of its longest word, at the current font scale. A label given less
 * than the longest word splits that word across lines; rows use this to stack at large text sizes instead.
 */
internal data class TextFit(val full: Dp, val longestWord: Dp)

@Composable
internal fun rememberTextFit(text: String, style: TextStyle): TextFit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, density, measurer) {
        fun width(part: String) = with(density) {
            measurer.measure(part, style, softWrap = false, maxLines = 1).size.width.toDp()
        }
        val words = text.split(' ', '\n').filter { it.isNotEmpty() }
        TextFit(width(text), words.maxOfOrNull(::width) ?: width(text))
    }
}

/**
 * [style], scaled down in 5 % steps (to half at most) until [text] fits [maxWidth] on one line. For the rare label
 * that must stay on one line in a fixed slot, such as a bottom-bar tab at the largest text and display sizes.
 */
@Composable
internal fun rememberFittingStyle(text: String, style: TextStyle, maxWidth: Dp): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, maxWidth, density, measurer) {
        fun fits(candidate: TextStyle) = with(density) {
            measurer.measure(text, candidate, softWrap = false, maxLines = 1).size.width.toDp() <=
                maxWidth
        }
        var scale = 1f
        var fitted = style
        while (scale > 0.5f && !fits(fitted)) {
            scale -= 0.05f
            fitted = style.copy(fontSize = style.fontSize * scale)
        }
        fitted
    }
}

/**
 * [style], scaled down only as far as needed for the longest word of [text] to fit [maxWidth]; the text still wraps
 * between words. For labels in a fixed slot (top-bar titles, matrix cells) at the largest text and display sizes.
 */
@Composable
internal fun rememberWordFittingStyle(text: String, style: TextStyle, maxWidth: Dp): TextStyle {
    val longest = text.split(' ', '\n').filter {
        it.isNotEmpty()
    }.maxByOrNull { it.length }.orEmpty()
    return rememberFittingStyle(longest, style, maxWidth)
}
