package com.wivernz.itera.feature.voice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceItem
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.reduceMotion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceConfirmationTest {
    @get:Rule val compose = createComposeRule()
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val anna = VoiceItem("1", "Call Anna", 1)
    private val annaAgain = VoiceItem("3", "Call Anna", 3)

    @Before fun setUp() = reduceMotion()

    @Test fun candidatesNeedASelectionAndAnExplicitConfirm() {
        val host = FakeHost(plan = { VoicePlan.Choose(listOf(anna, annaAgain)) })
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("complete call anna"), VoiceLanguage.EN)
        assertEquals(listOf<VoiceCommand>(VoiceCommand.CompleteItem("call anna")), host.planned)
        flow.confirm()
        assertTrue("no selection, nothing runs", host.executed.isEmpty())
        flow.select(1)
        flow.confirm()
        assertEquals(listOf<VoiceAction>(VoiceAction.CompleteItem(annaAgain)), host.executed)
        flow.confirm()
        assertEquals("a repeated confirm does nothing", 1, host.executed.size)
    }

    @Test fun cancelBackAndDismissNeverExecute() {
        val host = FakeHost(plan = { VoicePlan.Confirm(VoiceAction.CompleteExercise) })
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("complete exercise"), VoiceLanguage.EN)
        assertEquals(VoiceCommandPhase.Confirming(VoiceAction.CompleteExercise, null), flow.phase)
        flow.reset()
        assertEquals(VoiceCommandPhase.Ready, flow.phase)
        flow.confirm()
        assertTrue(host.executed.isEmpty())
    }

    @Test fun duplicateConfirmsWhileRunningExecuteOnce() {
        val gate = CompletableDeferred<Unit>()
        val host = FakeHost(
            plan = { VoicePlan.Confirm(VoiceAction.CompleteExercise) },
            outcome = {
                gate.await()
                VoiceOutcome.Done(VoiceFeedback.Handover)
            }
        )
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("complete exercise"), VoiceLanguage.EN)
        flow.confirm()
        flow.confirm()
        flow.onFinal(listOf("complete exercise"), VoiceLanguage.EN)
        assertEquals(VoiceCommandPhase.Working, flow.phase)
        gate.complete(Unit)
        assertEquals(1, host.executed.size)
        assertEquals(VoiceCommandPhase.Done(VoiceFeedback.Handover), flow.phase)
    }

    @Test fun aStaleTargetIsRejectedAtConfirmation() {
        val host = FakeHost(
            plan = { VoicePlan.Choose(listOf(anna)) },
            outcome = { VoiceOutcome.Rejected(VoiceRejection.Stale) }
        )
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("complete anna"), VoiceLanguage.EN)
        flow.select(0)
        flow.confirm()
        assertEquals(VoiceCommandPhase.Rejected(VoiceRejection.Stale), flow.phase)
    }

    @Test fun commandsNotOfferedHereAreRejectedBeforePlanning() {
        val host = FakeHost(voiceCommands = emptySet())
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("end focus"), VoiceLanguage.EN)
        assertEquals(VoiceCommandPhase.Rejected(VoiceRejection.NotHere), flow.phase)
        assertTrue(host.planned.isEmpty())
    }

    @Test fun competingCommandsAreChosenThenPlanned() {
        val host = FakeHost(plan = { VoicePlan.Run(VoiceAction.PauseFocus) })
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("pause", "end focus"), VoiceLanguage.EN)
        assertTrue(host.planned.isEmpty())
        flow.select(0)
        flow.confirm()
        assertEquals(listOf<VoiceCommand>(VoiceCommand.PauseFocus), host.planned)
        assertEquals(listOf<VoiceAction>(VoiceAction.PauseFocus), host.executed)
    }

    @Test fun recommendationFromAnExerciseAsksBeforeLeaving() {
        var left = 0
        val host = NoVoiceCommands.withRecommendation(confirm = true) { left++ }
        val flow = VoiceCommandFlow(host, scope)
        flow.onFinal(listOf("what should I do now"), VoiceLanguage.EN)
        assertEquals(VoiceCommandPhase.Confirming(VoiceAction.ShowRecommendation, null), flow.phase)
        assertEquals(0, left)
        flow.confirm()
        assertEquals(1, left)
    }

    @Test fun sheetShowsRadioRowsAndTheConfirmationDialog() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val host = FakeHost(
            plan = { command ->
                when (command) {
                    is VoiceCommand.CompleteItem -> VoicePlan.Choose(listOf(anna, annaAgain))
                    else -> VoicePlan.Confirm(VoiceAction.CompleteExercise)
                }
            },
            outcome = { VoiceOutcome.Done(VoiceFeedback.Completed(anna.label)) }
        )
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    ProvideVoiceCommands(host) { VoiceCommandAction() }
                }
            }
        }
        compose.onNodeWithText("Voice command").performClick()
        compose.onNodeWithText("Voice command · English").assertExists()
        compose.onNodeWithText("“Complete call Anna”").assertExists()
        compose.onNodeWithTag("VoiceSpeak").performClick()
        compose.onNodeWithText("Listening").assertExists()
        compose.runOnIdle { fake.final("complete call anna") }
        compose.onNodeWithText("Which item did you mean?").assertExists()
        compose.onNodeWithText("Item 3").assertExists()
        compose.onNodeWithTag("VoiceConfirm").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertTrue(host.executed.isEmpty()) }

        compose.onNodeWithTag("VoiceSpeak").performClick()
        compose.runOnIdle { fake.final("complete exercise") }
        compose.onNodeWithText("Complete this exercise?").assertExists()
        compose.onNodeWithText("Keep going").performClick()
        compose.runOnIdle { assertTrue(host.executed.isEmpty()) }

        compose.onNodeWithTag("VoiceSpeak").performClick()
        compose.runOnIdle { fake.final("complete call anna") }
        compose.onAllNodesWithTag("VoiceOption")[0].performClick()
        compose.onNodeWithTag("VoiceConfirm").performClick()
        compose.onNodeWithText("Ticked off “Call Anna”").assertExists()
        compose.runOnIdle {
            assertEquals(listOf<VoiceAction>(VoiceAction.CompleteItem(anna)), host.executed)
        }
    }

    @Test fun unsupportedSpeechExplainsWithExamplesValidHere() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val host =
            FakeHost(
                voiceCommands = setOf(com.wivernz.itera.domain.voice.VoiceCommandKind.PAUSE_FOCUS)
            )
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    ProvideVoiceCommands(host) { VoiceCommandAction() }
                }
            }
        }
        compose.onNodeWithText("Voice command").performClick()
        compose.onNodeWithText("“Add task call Anna”").assertDoesNotExist()
        compose.onNodeWithTag("VoiceSpeak").performClick()
        compose.runOnIdle { fake.final("sing a song") }
        compose.onNodeWithText("Heard: “sing a song”").assertExists()
        compose.onNodeWithText("That isn't a command Itera knows.").assertExists()
        compose.onNodeWithText("“Pause”").assertExists()
        compose.runOnIdle { assertTrue(host.planned.isEmpty()) }
    }
}
