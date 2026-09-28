package com.wivernz.itera.feature.voice

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.voice.AndroidSpeechPlatform
import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.CallerAudioStream
import com.wivernz.itera.core.voice.PipedMicrophone
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceProvider
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.reduceMotion
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers

private val GOOGLE = ComponentName(
    "com.google.android.tts",
    "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
)

/**
 * ADR-0022 (caller audio, API 33+): external recognisers get Itera's own 16 kHz mono PCM16 capture through
 * `EXTRA_AUDIO_SOURCE`; the on-device recogniser and API < 33 keep their normal path; every exit closes the audio.
 */
@RunWith(RobolectricTestRunner::class)
class VoiceCallerAudioTest {
    @get:Rule val compose = createComposeRule()
    private val owner = Any()

    @Before fun setUp() = reduceMotion()

    private val google =
        RecognitionProviderInfo(GOOGLE.flattenToString(), "Speech Recognition & Synthesis")

    private fun selectedPlatform(callerAudio: Boolean = true) =
        FakePlatform(providers = listOf(google), callerAudio = callerAudio)

    private fun chosen() = FakeConsent(selected = GOOGLE.flattenToString())

    private fun start(platform: FakePlatform, consent: FakeConsent = chosen()): VoiceController {
        val voice = VoiceController(
            AndroidVoiceRecognizer(platform, consent, TestLogger()),
            FakeGate(),
            VoiceLanguage.EN
        )
        voice.start(owner) { }
        ShadowLooper.idleMainLooper()
        return voice
    }

    private fun intent(platform: FakePlatform) = shadowOf(platform.last).lastRecognizerIntent

    @Test fun anExternalProviderGetsCallerAudioWithItsFormat() {
        val platform = selectedPlatform()
        start(platform)
        assertEquals(VoiceProvider.Selected(GOOGLE), platform.created.single())
        val sent = intent(platform)
        val stream = platform.streams.single()
        val descriptor = sent.getParcelableExtra(
            RecognizerIntent.EXTRA_AUDIO_SOURCE,
            ParcelFileDescriptor::class.java
        )
        assertTrue(descriptor != null)
        assertEquals(1, sent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, -1))
        assertEquals(
            AudioFormat.ENCODING_PCM_16BIT,
            sent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, -1)
        )
        assertEquals(
            16_000,
            sent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, -1)
        )
        assertEquals(0, stream.closes)
        // the existing extras are unchanged
        assertEquals("en-US", sent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(sent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    }

    @Test fun theSystemDefaultAlsoGetsCallerAudio() {
        val platform = FakePlatform(systemDefault = GOOGLE, callerAudio = true)
        start(platform, FakeConsent(granted = true))
        assertEquals(VoiceProvider.SystemDefault(GOOGLE), platform.created.single())
        assertTrue(intent(platform).hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE))
    }

    @Test fun theOnDeviceRecognizerKeepsItsOwnMicrophone() {
        val platform = FakePlatform(onDevice = true, providers = listOf(google), callerAudio = true)
        start(platform)
        assertEquals(VoiceProvider.OnDevice, platform.created.single())
        assertTrue(platform.streams.isEmpty())
        assertFalse(intent(platform).hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE))
    }

    @Test fun belowApi33NothingIsInjected() {
        val platform = selectedPlatform(callerAudio = false)
        start(platform)
        assertTrue(platform.streams.isEmpty())
        assertFalse(intent(platform).hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE))
    }

    @Test fun callerAudioIsOfferedFromApi33Only() {
        val android = AndroidSpeechPlatform(ApplicationProvider.getApplicationContext())
        val real = Build.VERSION.SDK_INT
        try {
            ReflectionHelpers.setStaticField(
                Build.VERSION::class.java,
                "SDK_INT",
                Build.VERSION_CODES.S_V2
            )
            assertFalse(android.callerAudioSupported())
            ReflectionHelpers.setStaticField(
                Build.VERSION::class.java,
                "SDK_INT",
                Build.VERSION_CODES.TIRAMISU
            )
            assertTrue(android.callerAudioSupported())
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", real)
        }
    }

    @Test fun aFinalResultClosesTheAudio() {
        val platform = selectedPlatform()
        val results = mutableListOf<List<String>>()
        val voice = VoiceController(
            AndroidVoiceRecognizer(platform, chosen(), TestLogger()),
            FakeGate(),
            VoiceLanguage.EN
        )
        voice.start(owner) { results += it }
        ShadowLooper.idleMainLooper()
        val shadow = shadowOf(platform.last)
        shadow.triggerOnReadyForSpeech(Bundle())
        shadow.triggerOnResults(
            Bundle().apply {
                putStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION,
                    arrayListOf("call the plumber")
                )
            }
        )
        assertEquals(listOf(listOf("call the plumber")), results)
        assertEquals(1, platform.streams.single().closes)
    }

    @Test fun anErrorClosesTheAudio() {
        val platform = selectedPlatform()
        val voice = start(platform)
        val shadow = shadowOf(platform.last)
        shadow.triggerOnReadyForSpeech(Bundle())
        shadow.triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.NO_SPEECH), voice.state)
        assertEquals(1, platform.streams.single().closes)
    }

    @Test fun cancelAndReleaseCloseTheAudioAndStopEndsIt() {
        val platform = selectedPlatform()
        val voice = start(platform)
        shadowOf(platform.last).triggerOnReadyForSpeech(Bundle())
        voice.stop()
        assertEquals("Stop sends end of audio", 1, platform.streams.single().ends)
        voice.cancel()
        assertEquals(1, platform.streams.single().closes)
        voice.start(owner) { }
        ShadowLooper.idleMainLooper()
        voice.release()
        assertEquals(1, platform.streams[1].closes)
    }

    @Test fun theNoAnswerTimeoutClosesTheAudio() {
        val platform = selectedPlatform()
        val consent = chosen()
        start(platform, consent)
        ShadowLooper.idleMainLooper(9, TimeUnit.SECONDS)
        assertEquals(1, platform.streams.single().closes)
        assertNull(consent.selected)
    }

    @Test fun aMicrophoneThatCannotOpenFailsWithoutAnotherProvider() {
        val platform = selectedPlatform().apply { micOpens = false }
        val voice = start(platform)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.FAILED), voice.state)
        assertEquals(1, platform.created.size)
    }

    @Test fun aProviderThatOpensItsOwnMicrophoneIsStoppedNotRaced() {
        val platform = selectedPlatform().apply { otherCapture = true }
        val consent = chosen()
        val voice = start(platform, consent)
        shadowOf(platform.last).triggerOnReadyForSpeech(Bundle())
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.FAILED), voice.state)
        assertEquals(1, platform.streams.single().closes)
        assertTrue(shadowOf(platform.last).isDestroyed)
        assertEquals("no provider switch", 1, platform.created.size)
        assertEquals(GOOGLE.flattenToString(), consent.selected)
    }

    @Test fun aProviderSidePermissionFailureIsNotShownAsItera() {
        // the vivo case: the provider reads Itera's audio, then Android rejects its own background microphone access
        val platform = selectedPlatform()
        val consent = chosen()
        val voice = start(platform, consent)
        shadowOf(platform.last).triggerOnError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        assertTrue((voice.state as VoiceSessionState.ChooseProvider).lost)
        assertNull(consent.selected)

        val system = FakePlatform(systemDefault = GOOGLE, callerAudio = true)
        val onSystem = start(system, FakeConsent(granted = true))
        shadowOf(system.last).triggerOnError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE), onSystem.state)
    }

    @Test fun withoutCallerAudioAPermissionErrorStaysItera() {
        val platform = selectedPlatform(callerAudio = false)
        val voice = start(platform)
        shadowOf(platform.last).triggerOnError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        assertEquals(VoiceSessionState.PermissionDenied(owner, permanent = false), voice.state)
    }

    @Test fun unsupportedLanguageAndMissingModelAreDifferentStates() {
        assertEquals(
            VoiceError.LANGUAGE_UNSUPPORTED,
            AndroidVoiceRecognizer.errorOf(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        )
        assertEquals(
            VoiceError.LANGUAGE_UNAVAILABLE,
            AndroidVoiceRecognizer.errorOf(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        )
        val platform = selectedPlatform()
        val voice = start(platform)
        shadowOf(platform.last).triggerOnReadyForSpeech(Bundle())
        shadowOf(platform.last).triggerOnError(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        assertEquals(
            VoiceSessionState.Unavailable(owner, VoiceUnavailable.LANGUAGE_UNSUPPORTED),
            voice.state
        )
        voice.retry()
        ShadowLooper.idleMainLooper()
        shadowOf(platform.last).triggerOnReadyForSpeech(Bundle())
        shadowOf(platform.last).triggerOnError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.LANGUAGE), voice.state)
    }

    @Test fun unsupportedLanguageNeverSuggestsDownloadingIt() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake, FakeGate(), VoiceLanguage.RU)
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    VoiceNoteField("", {}, "Task", maxChars = 100)
                }
            }
        }
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.runOnIdle { fake.error(VoiceError.LANGUAGE_UNSUPPORTED) }
        compose.onNodeWithText("Speech settings").assertDoesNotExist()
        compose.onNodeWithText("Keep typing").assertExists()
        compose.onNodeWithText("You can add the language", substring = true).assertDoesNotExist()
        compose.onNodeWithText("doesn't support", substring = true).assertExists()

        compose.onNodeWithText("Keep typing").performClick()
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.runOnIdle { fake.error(VoiceError.LANGUAGE_UNAVAILABLE) }
        compose.onNodeWithText("Speech settings").assertExists()
    }

    @Test fun theRealCaptureNeedsThePermissionAndClosesCleanly() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)
        assertNull("no permission, no capture", PipedMicrophone.open(app))
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val stream = PipedMicrophone.open(app) ?: return // no audio input in this environment
        val input = ParcelFileDescriptor.AutoCloseInputStream(stream.descriptor)
        stream.endOfAudio()
        // the pump closes its end: the reader reaches end of stream rather than blocking
        while (input.read(ByteArray(CallerAudioStream.SAMPLE_RATE)) >= 0) Unit
        stream.close()
        stream.close() // idempotent
    }

    @Test fun callerAudioCodeKeepsPcmInMemoryAndOutOfLogs() {
        val source = sourcesUnder("core/voice").first { it.name == "CallerAudio.kt" }.readText()
        for (forbidden in listOf(
            "File(",
            "openFileOutput",
            "FileOutputStream(",
            "setOutputFile",
            "cacheDir",
            "filesDir"
        )) {
            assertFalse("no file-backed audio: $forbidden", forbidden in source)
        }
        assertFalse("capture never logs", "logger" in source || "Log." in source)
        // every adapter log line is a fixed literal: no buffer, transcript or variable ever reaches the logger
        val adapter = sourcesUnder("core/voice").first {
            it.name == "VoiceRecognizer.kt"
        }.readText()
        val calls = Regex("""logger\.\w\(([^)]*)\)""").findAll(adapter).map {
            it.groupValues[1].trim()
        }.toList()
        assertTrue(calls.isNotEmpty())
        calls.forEach { args ->
            assertTrue("literal-only log: $args", Regex("""TAG,\s*"[^"$]*"""").matches(args))
        }
    }
}
