package com.wivernz.itera.core.designsystem
import android.app.Application
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.core.designsystem.component.AnimatedCheck
import com.wivernz.itera.core.designsystem.component.CheckCircle
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.component.CircleIconButton
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.EmptyState
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.Group
import com.wivernz.itera.core.designsystem.component.IntervalLadder
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.LanguagePill
import com.wivernz.itera.core.designsystem.component.LanguageSheet
import com.wivernz.itera.core.designsystem.component.LinkRow
import com.wivernz.itera.core.designsystem.component.MasteryDots
import com.wivernz.itera.core.designsystem.component.MasteryLadder
import com.wivernz.itera.core.designsystem.component.NoteField
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ProgressBar
import com.wivernz.itera.core.designsystem.component.RadioDot
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.SectionTitle
import com.wivernz.itera.core.designsystem.component.Segmented
import com.wivernz.itera.core.designsystem.component.Skeleton
import com.wivernz.itera.core.designsystem.component.StepDot
import com.wivernz.itera.core.designsystem.component.StepRow
import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.core.designsystem.component.SwitchRow
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TimePickerSheet
import com.wivernz.itera.core.designsystem.component.TimeRow
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.ValueRow
import com.wivernz.itera.core.designsystem.preview.ComponentGallery
import com.wivernz.itera.core.designsystem.preview.ComponentSample
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class ComponentTest {
    @get:Rule val compose = createComposeRule()

    @Before fun reduceMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Application>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )
    }
    private fun render(sample: ComponentSample) {
        val dark = mutableStateOf(false)
        val scale = mutableStateOf(1f)
        compose.setContent {
            IteraTheme(dark.value) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, scale.value)
                ) { ComponentGallery(sample, Modifier.testTag("gallery")) }
            }
        }
        compose.onNodeWithTag("gallery").assertExists()
        compose.runOnIdle { dark.value = true }
        compose.onNodeWithTag("gallery").assertExists()
        compose.runOnIdle { scale.value = 2f }
        compose.waitForIdle()
        compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true
        ).fetchSemanticsNodes().forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            // Paragraph width may retain the parent constraint after intrinsic text measurement.
            // Check the rendered line bounds, not that unused paragraph space.
            layouts.forEach {
                assertTrue(
                    "$sample text clips at 2x: ${it.layoutInput.text}; size=${it.size}",
                    !it.multiParagraph.didExceedMaxLines &&
                        it.multiParagraph.height <= it.size.height + 1 &&
                        (0 until it.lineCount).all { line ->
                            it.getLineRight(line) - it.getLineLeft(line) <= it.size.width + 1f
                        }
                )
            }
        }
    }

    @Test fun screenColumnRendersInBothThemes() = render(ComponentSample.ScreenColumn)

    @Test fun iteraCardRendersInBothThemes() = render(ComponentSample.IteraCard)

    @Test fun dividerRendersInBothThemes() = render(ComponentSample.Divider)

    @Test fun eyebrowRendersInBothThemes() = render(ComponentSample.Eyebrow)

    @Test fun pillRendersInBothThemes() = render(ComponentSample.Pill)

    @Test fun iteraButtonRendersInBothThemes() = render(ComponentSample.IteraButton)

    @Test fun circleIconButtonRendersInBothThemes() = render(ComponentSample.CircleIconButton)

    @Test fun topBarRendersInBothThemes() = render(ComponentSample.TopBar)

    @Test fun techniqueTokenRendersInBothThemes() = render(ComponentSample.TechniqueToken)

    @Test fun choiceChipRendersInBothThemes() = render(ComponentSample.ChoiceChip)

    @Test fun segmentedRendersInBothThemes() = render(ComponentSample.Segmented)

    @Test fun stepRowRendersInBothThemes() = render(ComponentSample.StepRow)

    @Test fun stepDotRendersInBothThemes() = render(ComponentSample.StepDot)

    @Test fun masteryLadderRendersInBothThemes() = render(ComponentSample.MasteryLadder)

    @Test fun progressBarRendersInBothThemes() = render(ComponentSample.ProgressBar)

    @Test fun noteFieldRendersInBothThemes() = render(ComponentSample.NoteField)

    @Test fun sectionTitleRendersInBothThemes() = render(ComponentSample.SectionTitle)

    @Test fun checkCircleRendersInBothThemes() = render(ComponentSample.CheckCircle)

    @Test fun radioDotRendersInBothThemes() = render(ComponentSample.RadioDot)

    @Test fun linkRowRendersInBothThemes() = render(ComponentSample.LinkRow)

    @Test fun masteryDotsRendersInBothThemes() = render(ComponentSample.MasteryDots)

    @Test fun intervalLadderRendersInBothThemes() = render(ComponentSample.IntervalLadder)

    @Test fun animatedCheckRendersInBothThemes() = render(ComponentSample.AnimatedCheck)

    @Test fun groupRendersInBothThemes() = render(ComponentSample.Group)

    @Test fun valueRowRendersInBothThemes() = render(ComponentSample.ValueRow)

    @Test fun switchRowRendersInBothThemes() = render(ComponentSample.SwitchRow)

    @Test fun timeRowRendersInBothThemes() = render(ComponentSample.TimeRow)

    @Test fun languageSheetRendersInBothThemes() = render(ComponentSample.LanguageSheet)

    @Test fun languagePillRendersInBothThemes() = render(ComponentSample.LanguagePill)

    @Test fun timePickerSheetRendersInBothThemes() = render(ComponentSample.TimePickerSheet)

    @Test fun emptyStateRendersInBothThemes() = render(ComponentSample.EmptyState)

    @Test fun errorStateRendersInBothThemes() = render(ComponentSample.ErrorState)

    @Test fun skeletonRendersInBothThemes() = render(ComponentSample.Skeleton)

    @Test fun callbacksAndSelectedSemantics() {
        var clicks = 0
        val selected = mutableStateOf(false)
        compose.setContent {
            IteraTheme {
                Column {
                    ChoiceChip("choice", selected.value, {
                        selected.value = !selected.value
                        clicks++
                    })
                    IteraButton("button", { clicks++ })
                    IteraButton("disabled", { clicks++ }, enabled = false)
                    SwitchRow("switch", selected.value, Itera.colors.accent, onChange = {
                        selected.value = it
                        clicks++
                    })
                }
            }
        }
        compose.onNodeWithText("choice").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("button").performClick()
        compose.onNodeWithText("disabled").assertIsNotEnabled()
        compose.onNodeWithText("switch").assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertEquals(3, clicks) }
    }

    @Test fun segmentedStacksAtLargeFontScale() {
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Segmented(listOf(0 to "first", 1 to "second"), 0, {})
                }
            }
        }
        val first = compose.onNodeWithText(
            "first"
        ).assertIsSelected().fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText(
            "second"
        ).assertIsNotSelected().fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= first.bottom)
    }

    @Test fun longValuesLeaveRoomForLabelsAtLargeFontScale() {
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    ValueRow("Language", "A long translated setting value that must wrap", {})
                }
            }
        }
        val label = compose.onNodeWithText("Language", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val value = compose.onNodeWithText(
            "A long translated setting value that must wrap",
            useUnmergedTree = true
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(label.width > 80f)
        assertTrue(label.right <= value.left)
        assertTrue(value.width > 80f)
    }

    @Test fun doneAndIntegratedExposeState() {
        compose.setContent {
            IteraTheme {
                Column {
                    StepRow(StepState.Done, "practice", "subtitle", Itera.colors.accent)
                    MasteryLadder(MasteryLevel.INTEGRATED, Itera.colors.accent)
                }
            }
        }
        compose.onNodeWithText(
            "practice"
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"))
        compose.onNodeWithContentDescription("Level Integrated, 4 of 4").assertExists()
    }

    @Test fun languageSelectionAndDismissAreSingleCallbacks() {
        var picked = ""
        var dismiss = 0
        compose.setContent { IteraTheme { LanguageSheet("en", { picked = it }, { dismiss++ }) } }
        compose.onNodeWithText("Deutsch").performClick()
        compose.runOnIdle { assertEquals("de", picked) }
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(1, dismiss) }
    }

    @Test fun timePickerReturnsUnroundedTime() {
        var result: LocalTime? = null
        compose.setContent {
            IteraTheme { TimePickerSheet("Time", LocalTime.of(8, 37), { result = it }, {}) }
        }
        compose.onNodeWithText("Done").performClick()
        compose.runOnIdle { assertEquals(LocalTime.of(8, 37), result) }
    }

    @Test fun timePickerUsesTwelveHourDeviceSetting() {
        Settings.System.putString(
            ApplicationProvider.getApplicationContext<Application>().contentResolver,
            Settings.System.TIME_12_24,
            "12"
        )
        compose.setContent {
            IteraTheme { TimePickerSheet("Time", LocalTime.of(20, 37), {}, {}) }
        }
        compose.onNodeWithText("AM").assertExists()
        compose.onNodeWithText("PM").assertExists()
    }

    @Test fun timePickerUsesTwentyFourHourDeviceSetting() {
        Settings.System.putString(
            ApplicationProvider.getApplicationContext<Application>().contentResolver,
            Settings.System.TIME_12_24,
            "24"
        )
        compose.setContent {
            IteraTheme { TimePickerSheet("Time", LocalTime.of(20, 37), {}, {}) }
        }
        compose.onNodeWithText("AM").assertDoesNotExist()
        compose.onNodeWithText("PM").assertDoesNotExist()
    }

    @Test fun reducedMotionDoesNotScheduleInfiniteFrames() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            IteraTheme {
                Column {
                    StepDot(StepState.Now, Itera.colors.accent)
                    StepDot(StepState.Done, Itera.colors.accent)
                    AnimatedCheck(Itera.colors.surface, Itera.colors.ink)
                    ProgressBar(1f, Itera.colors.accent)
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        // Waiting for idle with the clock paused would time out if a finite animation remained.
        compose.onRoot().assertExists()
    }
}
