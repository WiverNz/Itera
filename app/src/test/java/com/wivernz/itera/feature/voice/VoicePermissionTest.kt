package com.wivernz.itera.feature.voice

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.feature.reduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoicePermissionTest {
    @get:Rule val compose = createComposeRule()
    private val owner = Any()

    @Before fun setUp() = reduceMotion()

    @Test fun firstUseShowsTheRationaleBeforeAnySystemPrompt() {
        val fake = FakeRecognizer()
        val gate = FakeGate(granted = false)
        val voice = voiceController(fake, gate)
        voice.start(owner) { }
        assertEquals(VoiceSessionState.NeedsPermission(owner), voice.state)
        assertEquals(0, gate.requests)
        assertTrue(fake.languages.isEmpty())
        voice.requestPermission()
        assertEquals(1, gate.requests)
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
        assertEquals(listOf("en-US"), fake.languages)
    }

    @Test fun denialKeepsAskingPossibleAndNeverRetriesItself() {
        val fake = FakeRecognizer()
        val gate = FakeGate(granted = false, grantOnRequest = false)
        val voice = voiceController(fake, gate)
        voice.start(owner) { }
        voice.requestPermission()
        assertEquals(VoiceSessionState.PermissionDenied(owner, permanent = false), voice.state)
        assertEquals(1, gate.requests)
        assertTrue(fake.languages.isEmpty())
        // "Try again" is the user's choice; it shows the rationale again, not the prompt
        voice.retry()
        assertEquals(VoiceSessionState.NeedsPermission(owner), voice.state)
        assertEquals(1, gate.requests)
    }

    @Test fun permanentDenialOffersSettings() {
        val gate = FakeGate(granted = false, permanent = true, grantOnRequest = false)
        val voice = voiceController(gate = gate)
        voice.start(owner) { }
        voice.requestPermission()
        assertEquals(VoiceSessionState.PermissionDenied(owner, permanent = true), voice.state)
    }

    @Test fun revocationDuringASessionEndsIt() {
        val fake = FakeRecognizer()
        val gate = FakeGate()
        val voice = voiceController(fake, gate)
        voice.start(owner) { error("nothing may be delivered") }
        gate.granted = false
        fake.error(VoiceError.PERMISSION)
        assertEquals(VoiceSessionState.PermissionDenied(owner, permanent = false), voice.state)
        voice.cancel()
        voice.start(owner) { }
        assertEquals(VoiceSessionState.NeedsPermission(owner), voice.state)
    }

    @Test fun aStaleGrantDoesNotStartACancelledSession() {
        val fake = FakeRecognizer()
        var pending: ((Boolean) -> Unit)? = null
        val gate = object : VoicePermissionGate {
            override fun granted() = false
            override fun request(onResult: (Boolean) -> Unit) {
                pending = onResult
            }
            override fun permanentlyDenied() = false
        }
        val voice = VoiceController(fake, gate, com.wivernz.itera.domain.voice.VoiceLanguage.EN)
        voice.start(owner) { }
        voice.requestPermission()
        voice.cancel()
        pending!!.invoke(true)
        assertEquals(VoiceSessionState.Idle, voice.state)
        assertTrue(fake.languages.isEmpty())
    }

    @Test fun manifestDeclaresOnlyTheMicrophoneForVoice() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val permissions = app.packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions.orEmpty().toList()
        assertTrue(Manifest.permission.RECORD_AUDIO in permissions)
        assertFalse(Manifest.permission.INTERNET in permissions)
        assertFalse("android.permission.FOREGROUND_SERVICE_MICROPHONE" in permissions)
    }

    @Test fun onlyTheVoiceHostEverAsksForTheMicrophone() {
        val askers = sourcesUnder("").filter { "RECORD_AUDIO" in it.readText() }.map { it.name }
        assertEquals(listOf("VoiceHost.kt"), askers)
        // the system recogniser is reachable only through the adapter, which gates it on consent (ADR-0022)
        val platform = sourcesUnder("").filter {
            val text = it.readText()
            "createSpeechRecognizer(" in text || "isRecognitionAvailable(" in text
        }.map { it.name }
        assertEquals(listOf("VoiceRecognizer.kt"), platform)
    }

    @Test fun deniedMicrophoneLeavesTypingComplete() {
        val gate = FakeGate(granted = false, grantOnRequest = false)
        val voice = voiceController(gate = gate)
        var text by mutableStateOf("")
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    VoiceNoteField(text, {
                        text = it
                    }, "Task", maxChars = 100, modifier = Modifier.testTag("Field"))
                }
            }
        }
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithText("Use the microphone?").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Microphone access is off").assertExists()
        compose.onNodeWithText("Keep typing").performClick()
        compose.onNodeWithTag("VoicePanel").assertDoesNotExist()
        compose.onNodeWithTag("Field").performTextInput("typed anyway")
        compose.runOnIdle { assertEquals("typed anyway", text) }
    }
}
