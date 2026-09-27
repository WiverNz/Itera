@file:Suppress("ktlint:standard:no-wildcard-imports")
package com.wivernz.itera.quality

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

open class ScreenAuditBase {
    @get:Rule val compose = createComposeRule()
    protected fun audit(locales: List<String>, scales: List<Float>, controls: Boolean = false) {
        var selection by mutableStateOf(Triple("Welcome", "en", 1f))
        var dark by mutableStateOf(false)
        val failures = linkedSetOf<String>()
        var density = 1f
        compose.setContent {
            val (screen, locale, scale) = selection
            val base = LocalContext.current
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(locale)); fontScale = scale }
            val context = base.createConfigurationContext(configuration)
            density = LocalDensity.current.density
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration, LocalDensity provides Density(density, scale), LocalReduceMotion provides true) {
                key(selection, dark) { IteraTheme(dark = dark) { AuditScreen(screen) } }
            }
        }
        for (language in locales) for (scale in scales) for (night in listOf(false, true)) for (screen in auditScreens) {
            compose.runOnIdle { selection = Triple(screen, language, scale); dark = night }
            compose.waitForIdle()
            val label = "$screen/$language/$scale/${if (night) "dark" else "light"}"
            fun inspect() {
                compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                    layouts.filter { layout ->
                        // MultiParagraph keeps the max constraint width for short text; TextLayoutResult
                        // shrinks its reported size. Inspect actual line extents, not that allocation.
                        layout.multiParagraph.didExceedMaxLines || layout.multiParagraph.height > layout.size.height + 1 ||
                            (0 until layout.lineCount).any { line -> layout.isLineEllipsized(line) || layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1 }
                    }.forEach { failures += "$label clipped: ${it.layoutInput.text.text} [${it.multiParagraph.width}x${it.multiParagraph.height} / ${it.size}, lines=${it.lineCount}, exceeded=${it.multiParagraph.didExceedMaxLines}]" }
                }
                if (controls) compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                    val tag = node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                    if (!tag.startsWith("decorative:")) {
                        val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
                        val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
                        if (text.isBlank() && description.isBlank()) failures += "$label unlabelled: $tag"
                        if (node.size.width / density < 43.9f || node.size.height / density < 43.9f) failures += "$label small: $text $description ${node.size}"
                    }
                }
            }
            inspect()
            // Exercise each scroll container to its end; lazy lists materialize later rows on demand.
            repeat(12) {
                var moved = false
                compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy)).fetchSemanticsNodes().forEach { node ->
                    compose.runOnIdle { if (node.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(0f, 400f * density) == true) moved = true }
                }
                compose.waitForIdle()
                inspect()
                if (!moved) return@repeat
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
class FontScaleTest : ScreenAuditBase() {
    @Test fun englishAndGermanAtDoubleSize() = audit(listOf("en", "de"), listOf(2f))
}

@RunWith(RobolectricTestRunner::class)
class AccessibilityAuditTest : ScreenAuditBase() {
    @Test fun labelledControlsHaveMinimumTargets() = audit(listOf("en"), listOf(1f), controls = true)
}

@RunWith(RobolectricTestRunner::class)
class LocalizedRenderTest : ScreenAuditBase() {
    @Test fun fourLanguagesAtNormalAndLargeScale() = audit(listOf("en", "ru", "de", "es"), listOf(1f, 1.5f))
}

@RunWith(RobolectricTestRunner::class)
class PseudoLocaleTest : ScreenAuditBase() {
    @Test fun expandedAndRtlLocales() = audit(listOf("en-XA", "ar-XB"), listOf(1f))
}
