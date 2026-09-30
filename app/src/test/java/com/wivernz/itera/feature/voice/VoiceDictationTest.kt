package com.wivernz.itera.feature.voice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.feature.reduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceDictationTest {
    @get:Rule val compose = createComposeRule()
    private val fake = FakeRecognizer()
    private val voice = voiceController(fake)
    private var text by mutableStateOf("")
    private var edits = 0
    private var shown by mutableStateOf(true)

    @Before fun setUp() = reduceMotion()

    private fun field(initial: String, maxChars: Int = 200, onAdd: (() -> Unit)? = null) {
        text = initial
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    if (shown) {
                        VoiceNoteField(
                            text,
                            {
                                text = it
                                edits++
                            },
                            "Task",
                            maxChars = maxChars,
                            modifier = Modifier.testTag("Field"),
                            onAdd = onAdd
                        )
                    }
                }
            }
        }
    }

    private fun final(vararg alternatives: String) = compose.runOnIdle { fake.final(*alternatives) }

    @Test fun finalTextIsInsertedOnceAtTheCaretThroughTheNormalEdit() {
        field("Buy milk")
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithText("Listening").assertExists()
        compose.onNodeWithText("Dictation · English").assertExists()
        compose.runOnIdle { fake.partial("and") }
        compose.onNodeWithTag("VoicePartial").assertExists()
        compose.runOnIdle { assertEquals("partials are never inserted", "Buy milk", text) }
        final("and bread", "and bred")
        compose.runOnIdle {
            assertEquals("Buy milk and bread", text)
            assertEquals(1, edits)
        }
        final("and bread")
        compose.runOnIdle { assertEquals("Buy milk and bread", text) }
        compose.onNodeWithTag("VoicePanel").assertDoesNotExist()
    }

    @Test fun onlyTheCapturedSelectionIsReplaced() {
        field("Buy cow milk today")
        compose.onNodeWithTag("Field").performTextInputSelection(TextRange(4, 7))
        compose.onNodeWithTag("VoiceMic").performClick()
        final("oat")
        compose.runOnIdle { assertEquals("Buy oat milk today", text) }
    }

    @Test fun commandLookingDictationIsJustText() {
        field("")
        compose.onNodeWithTag("VoiceMic").performClick()
        final("end focus")
        compose.runOnIdle { assertEquals("end focus", text) }
        compose.onNodeWithTag("VoiceMic").performClick()
        final("complete exercise")
        compose.runOnIdle { assertEquals("end focus complete exercise", text) }
        compose.onNodeWithTag("VoiceSheet").assertDoesNotExist()
    }

    @Test fun typingWhileListeningCancelsAndALateResultIsDropped() {
        field("Plan")
        compose.onNodeWithTag("VoiceMic").performClick()
        val stale = fake.listener
        compose.onNodeWithTag("Field").performTextInput(" trip")
        compose.runOnIdle {
            assertEquals(VoiceSessionState.Idle, voice.state)
            stale.onFinal(listOf("late words"))
        }
        compose.runOnIdle { assertEquals("Plan trip", text) }
    }

    @Test fun addingWhileListeningStopsDictation() {
        var added = 0
        field("Call Anna", onAdd = { added++ })
        compose.onNodeWithTag("VoiceMic").performClick()
        val stale = fake.listener
        compose.onNodeWithTag("FieldAdd").performClick()
        compose.runOnIdle {
            assertEquals(1, added)
            assertEquals(VoiceSessionState.Idle, voice.state)
            stale.onFinal(listOf("late words"))
        }
        compose.runOnIdle { assertEquals("Call Anna", text) }
        compose.onNodeWithTag("VoicePanel").assertDoesNotExist()
    }

    @Test fun cancelKeepsTheFieldUntouched() {
        field("Keep me")
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithText("Cancel").performClick()
        final("ignored")
        compose.runOnIdle {
            assertEquals("Keep me", text)
            assertEquals(0, edits)
        }
    }

    @Test fun overLimitResultsStayEditableInsteadOfLosingWords() {
        field("Buy milk", maxChars = 14)
        compose.onNodeWithTag("VoiceMic").performClick()
        final("and fresh bread")
        compose.onNodeWithTag("VoiceOverflow").assertExists()
        compose.onNodeWithTag("VoiceInsert").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("Buy milk", text) }
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithTag("VoiceOverflow").assertDoesNotExist()
        compose.onNodeWithTag("VoiceMic").performClick()
        final("eggs")
        compose.runOnIdle { assertEquals("Buy milk eggs", text) }
    }

    @Test fun leavingTheScreenCancelsListening() {
        field("x")
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.runOnIdle { shown = false }
        compose.runOnIdle {
            assertEquals(VoiceSessionState.Idle, voice.state)
            assertTrue(fake.cancels >= 1)
        }
    }

    @Test fun micIsALabelledTouchTarget() {
        field("")
        compose.onNodeWithTag("VoiceMic")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Dictate: Task")
                )
            )
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(44.dp)
            .assertHeightIsAtLeast(44.dp)
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithTag("VoiceMic")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Listening"))
        compose.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test fun everyExerciseTextFieldIsADictationTarget() {
        // bare NoteField is left only where dictation is not a target (You/settings) or inside the voice field
        val offenders = sourcesUnder("feature/exercise", "feature/reflection", "feature/focus")
            .flatMap { file ->
                file.readLines().filter {
                    Regex("""(?<![A-Za-z])NoteField\(""").containsMatchIn(it)
                }
                    .map { "${file.name}: ${it.trim()}" }
            }
        assertEquals(emptyList<String>(), offenders)
    }
}
