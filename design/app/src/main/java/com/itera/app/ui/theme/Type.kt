package com.itera.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import com.itera.app.R
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Variable fonts are bundled in res/font; no optional external font installation. */
@OptIn(ExperimentalTextApi::class)
private fun resourceFamily(resource: Int): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(resource, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
)

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
    return remember {
        val display = resourceFamily(R.font.bricolage_grotesque)
        val body = resourceFamily(R.font.instrument_sans)
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
