@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.quality

import androidx.compose.ui.test.junit4.createComposeRule
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.preview.ComponentGallery
import com.wivernz.itera.core.designsystem.preview.ComponentSample
import com.wivernz.itera.feature.reduceMotion
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** One golden per design-system component, light and dark. The loading skeleton is timing-dependent and excluded. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = GOLDEN_QUALIFIERS)
class ComponentGoldenTest(private val sample: ComponentSample) {
    @get:Rule val compose = createComposeRule()

    @Before fun setUp() = reduceMotion()

    @Test fun matchesGoldens() = captureVariants(
        compose,
        sample.name,
        listOf(GoldenVariant.LIGHT, GoldenVariant.DARK).map { it.inFolder("components") },
        windows = true
    ) { ScreenColumn { ComponentGallery(sample) } }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun samples(): List<Array<Any>> =
            ComponentSample.entries.filter { it != ComponentSample.Skeleton }.map { arrayOf(it) }
    }
}
