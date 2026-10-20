@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.quality

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.feature.reduceMotion
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * One golden per golden variant: English light/dark at 1x and 2x, plus Russian light 1x (its Cyrillic face pair,
 * ADR-0019). Pinned device and density; fixtures from [AuditScreen], never the clock or a database.
 * `./gradlew recordRoborazziDebug` re-records, `verifyRoborazziDebug` fails on drift (04-visual-regression.md).
 */
internal data class GoldenVariant(
    val locale: String,
    val dark: Boolean,
    val scale: Float,
    val folder: String
) {
    /** The same variant under a separate golden set, e.g. `components/en/light`. */
    fun inFolder(root: String) = copy(folder = "$root/$folder")

    companion object {
        val LIGHT = GoldenVariant("en", false, 1f, "en/light")
        val DARK = GoldenVariant("en", true, 1f, "en/dark")
        val LIGHT_2X = GoldenVariant("en", false, 2f, "en/light-2x")
        val DARK_2X = GoldenVariant("en", true, 2f, "en/dark-2x")
        val RU_LIGHT = GoldenVariant("ru", false, 1f, "ru/light")
        val entries = listOf(LIGHT, DARK, LIGHT_2X, DARK_2X, RU_LIGHT)
    }
}

internal const val GOLDEN_QUALIFIERS = "en-rUS-w411dp-h891dp-mdpi"

internal fun goldenPath(folder: String, name: String) = "src/test/screenshots/$folder/$name.png"

/**
 * Text anti-aliasing differs by a few levels (at most 4/255 per channel, distance ~0.027) between host systems,
 * e.g. goldens recorded on Windows and verified on Linux CI. A per-pixel colour distance tolerance absorbs that;
 * a moved or recoloured element differs by far more and still fails. No share of pixels may differ beyond it.
 */
@OptIn(ExperimentalRoborazziApi::class)
internal val goldenOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(
        imageComparator = SimpleImageComparator(maxDistance = 0.04f)
    )
)

/**
 * Renders [content] for each variant in turn on one compose host and captures the root, or with [windows] every
 * window (bottom sheets and dialogs live in their own). [prepare] runs before each capture, e.g. to open a sheet.
 */
@OptIn(ExperimentalRoborazziApi::class)
internal fun captureVariants(
    compose: ComposeContentTestRule,
    name: String,
    variants: List<GoldenVariant> = GoldenVariant.entries,
    windows: Boolean = false,
    prepare: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var variant by mutableStateOf(variants.first())
    var host: Context? = null
    compose.setContent {
        host = LocalView.current.context
        val current = variant
        val configuration = Configuration(LocalConfiguration.current).apply {
            setLocale(Locale.forLanguageTag(current.locale))
            fontScale = current.scale
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (current.dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val context = LocalContext.current.createConfigurationContext(configuration)
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalDensity provides Density(LocalDensity.current.density, current.scale),
            LocalReduceMotion provides true
        ) {
            key(current) { IteraTheme(dark = current.dark) { content() } }
        }
    }
    variants.forEach {
        // Sheets and dialogs are separate windows whose density comes from the activity's configuration.
        if (windows) host?.setFontScale(it.scale)
        compose.runOnIdle { variant = it }
        compose.waitForIdle()
        prepare()
        compose.waitForIdle()
        if (windows) {
            captureScreenRoboImage(goldenPath(it.folder, name), goldenOptions)
        } else {
            compose.onRoot().captureRoboImage(goldenPath(it.folder, name), goldenOptions)
        }
    }
    if (windows) host?.setFontScale(1f)
}

@Suppress("DEPRECATION")
private fun Context.setFontScale(scale: Float) {
    val configuration = Configuration(resources.configuration).apply { fontScale = scale }
    resources.updateConfiguration(configuration, resources.displayMetrics)
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = GOLDEN_QUALIFIERS)
class ScreenGoldenTest(private val screen: String) {
    @get:Rule val compose = createComposeRule()

    @Before fun setUp() = reduceMotion()

    @Test fun matchesGoldens() = captureVariants(compose, screen) { AuditScreen(screen) }

    companion object {
        @JvmStatic
        @Suppress("unused") // called reflectively by the parameterized runner
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun screens(): List<Array<Any>> = auditScreens.map { arrayOf(it) }
    }
}
