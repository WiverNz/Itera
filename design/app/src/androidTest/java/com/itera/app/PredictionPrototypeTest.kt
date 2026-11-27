package com.itera.app

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.itera.app.data.PredictionState
import com.itera.app.data.PredictionViewModel
import com.itera.app.ui.screens.PredictionScreen
import com.itera.app.ui.theme.IteraTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PredictionPrototypeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun predictionFlowAllowsMixedEvidenceAndUnchangedConfidence() {
        val state = mutableStateOf(PredictionState())
        compose.setContent {
            IteraTheme {
                PredictionScreen(state.value, false, false, { state.value = it },
                    { state.value = state.value.advance("2026-11-26") },
                    { state.value = state.value.previous() }, {}, {}, {}, {}, {})
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("If I outline a report, I will get stuck")
        compose.onNodeWithText("Sure", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("Write headings for two minutes")
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextInput("Number of headings written")
        assertTrue(state.value.action.isNotBlank())
        assertTrue(state.value.evidence.isNotBlank())
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Save and try").performClick()
        compose.waitForIdle()
        assertEquals(2, state.value.page)
        compose.onNodeWithText("Record what happened").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("One heading, then I got stuck")
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Mixed evidence").performScrollTo().performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onAllNodesWithText("Sure").onLast().performScrollTo().performClick()
        compose.onNodeWithText("No clear conclusion yet").performScrollTo().performClick()
        compose.onNodeWithText("Save review").performClick()
        compose.onNodeWithText("Review saved").assertExists()
        assertEquals(2, state.value.comparison)
        assertEquals(3, state.value.confidenceAfter)
        assertTrue(state.value.finished)
    }

    @Test fun localRecordSurvivesViewModelRecreationAndCannotRewritePrediction() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val file = File(app.noBackupFilesDir, "prediction-instrumentation.json")
        val atomic = AtomicFile(file)
        try {
            val vm = PredictionViewModel(app, atomic)
            vm.edit(PredictionState(prediction = "I will get stuck"))
            vm.next()
            vm.edit(vm.state.copy(action = "Two minutes", evidence = "Count headings"))
            vm.next()
            val before = vm.state
            val restored = PredictionViewModel(app, atomic)
            assertEquals(before, restored.state)
            restored.edit(restored.state.copy(prediction = "Changed afterwards"))
            assertEquals(before, restored.state)
            restored.next()
            restored.edit(restored.state.copy(outcome = "One heading"))
            assertEquals("One heading", PredictionViewModel(app, atomic).state.outcome)
            assertEquals(before.preparedDate, restored.state.preparedDate)
        } finally {
            atomic.delete()
        }
    }

    @Test fun everyPageFitsFourLocalesAtLargeTextInBothThemes() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val language = mutableStateOf("en")
        val dark = mutableStateOf(false)
        val page = mutableStateOf(0)
        compose.setContent {
            val config = android.content.res.Configuration(app.resources.configuration).apply {
                setLocale(java.util.Locale.forLanguageTag(language.value))
            }
            val context = app.createConfigurationContext(config)
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides context,
                androidx.compose.ui.platform.LocalConfiguration provides config,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    app.resources.displayMetrics.density, 2f),
            ) {
                IteraTheme(dark = dark.value) {
                    PredictionScreen(PredictionState(page = page.value,
                        prediction = "A small everyday prediction", action = "Try for two minutes",
                        evidence = "Count headings", outcome = "One heading", comparison = 2,
                        lesson = "Try a smaller first step", preparedDate = if (page.value >= 2) "2026-11-26" else ""),
                        false, false, {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        for (locale in listOf("en", "ru", "de", "es")) for (night in listOf(false, true)) {
            for (step in 0..6) {
                compose.runOnIdle { language.value = locale; dark.value = night; page.value = step }
                val config = android.content.res.Configuration(app.resources.configuration).apply {
                    setLocale(java.util.Locale.forLanguageTag(locale))
                }
                val context = app.createConfigurationContext(config)
                compose.onNodeWithText(context.getString(R.string.pt_save_leave)).assertIsDisplayed()
                val texts = compose.onAllNodes(
                    androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(
                        androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult),
                    useUnmergedTree = true)
                repeat(texts.fetchSemanticsNodes().size) { index ->
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    texts[index].performSemanticsAction(
                        androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                    ) { it(layouts) }
                    // The shared TopBar emits an empty title node. It contains no visible text.
                    val visibleText = layouts.filter { it.layoutInput.text.isNotBlank() }
                    // Layout sizes are whole pixels; paragraph metrics are fractional pixels.
                    // Allow rounding only, while still detecting clipped lines or constrained text.
                    // A paragraph keeps the offered width even when Text wraps its measured width
                    // to its content. Check actual line widths, not the paragraph's offered box.
                    assertTrue("Text overflow at $locale / $night / $step",
                        visibleText.all {
                            !it.multiParagraph.didExceedMaxLines &&
                                (0 until it.lineCount).all { line ->
                                    it.getLineRight(line) - it.getLineLeft(line) <= it.size.width + 1f
                                } &&
                                it.multiParagraph.height <= it.size.height + 1f
                        })
                }
            }
        }
    }

    @Test fun saveFailureKeepsEditsAndCorruptRecordIsNotOverwritten() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val parent = File(app.noBackupFilesDir, "prediction-blocked-parent")
        parent.writeText("synthetic blocker")
        try {
            val vm = PredictionViewModel(app, AtomicFile(File(parent, "note.json")))
            vm.edit(PredictionState(prediction = "Synthetic pending note"))
            assertTrue(vm.saveFailed)
            assertEquals("Synthetic pending note", vm.state.prediction)
            vm.next()
            assertEquals(0, vm.state.page)
        } finally { parent.delete() }
        val corrupt = File(app.noBackupFilesDir, "prediction-corrupt-test.json")
        corrupt.writeText("{")
        try {
            val vm = PredictionViewModel(app, AtomicFile(corrupt))
            assertTrue(vm.loadFailed)
            vm.replace()
            assertEquals("{", corrupt.readText())
        } finally { corrupt.delete() }
    }

}
