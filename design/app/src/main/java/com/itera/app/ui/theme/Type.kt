package com.itera.app.ui.theme

import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Fonts are loaded from app/src/main/assets/fonts/ when present (see README),
 * otherwise the system font is used. Cyrillic falls back to the system font
 * automatically, because both brand fonts are Latin-only.
 */
private val DISPLAY_FILES = listOf(
    "BricolageGrotesque-Medium.ttf" to FontWeight.Medium,
    "BricolageGrotesque-SemiBold.ttf" to FontWeight.SemiBold,
    "BricolageGrotesque-Bold.ttf" to FontWeight.Bold,
)
private val BODY_FILES = listOf(
    "InstrumentSans-Regular.ttf" to FontWeight.Normal,
    "InstrumentSans-Medium.ttf" to FontWeight.Medium,
    "InstrumentSans-SemiBold.ttf" to FontWeight.SemiBold,
    "InstrumentSans-Bold.ttf" to FontWeight.Bold,
)

@OptIn(ExperimentalTextApi::class)
private fun assetFamily(assets: AssetManager, files: List<Pair<String, FontWeight>>): FontFamily? {
    val present = runCatching { assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
    val fonts = files.filter { it.first in present }.map { (file, weight) ->
        Font(path = "fonts/$file", assetManager = assets, weight = weight)
    }
    return if (fonts.isEmpty()) null else FontFamily(fonts)
}

@Immutable
data class IteraType(
    val hero: TextStyle,       // 42 — welcome
    val display: TextStyle,    // 34 — screen titles
    val title: TextStyle,      // 28–30 — exercise titles
    val headline: TextStyle,   // 22 — section headlines
    val bodyLarge: TextStyle,  // 18
    val body: TextStyle,       // 16
    val bodySmall: TextStyle,  // 14
    val caption: TextStyle,    // 13
    val label: TextStyle,      // 17 semibold — buttons, row titles
    val eyebrow: TextStyle,    // 13 caps
    val timer: TextStyle,      // 80 — focus timer
)

@Composable
fun rememberIteraType(): IteraType {
    val assets = LocalContext.current.assets
    return remember(assets) {
        val display = assetFamily(assets, DISPLAY_FILES) ?: FontFamily.Default
        val body = assetFamily(assets, BODY_FILES) ?: FontFamily.Default
        val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        IteraType(
            hero = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em, lineHeightStyle = tight),
            display = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em, lineHeightStyle = tight),
            title = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.02).em, lineHeightStyle = tight),
            headline = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = (-0.01).em),
            bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
            body = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
            bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            caption = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
            label = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
            eyebrow = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
            timer = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 80.sp, lineHeight = 84.sp, letterSpacing = (-0.04).em),
        )
    }
}

val LocalIteraType = staticCompositionLocalOf<IteraType> { error("IteraType not provided") }
