package com.wivernz.itera.feature.voice

import android.os.Build
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.voice.AndroidSpeechPlatform
import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceRecognitionListener
import com.wivernz.itera.domain.voice.VoiceLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSpeechRecognizer
import org.robolectric.util.ReflectionHelpers

private class Recording : VoiceRecognitionListener {
    val errors = mutableListOf<VoiceError>()
    val finals = mutableListOf<List<String>>()
    override fun onPartial(text: String) = Unit
    override fun onFinal(alternatives: List<String>) {
        finals += alternatives
    }
    override fun onError(error: VoiceError) {
        errors += error
    }
}

@RunWith(RobolectricTestRunner::class)
class VoiceRecognitionTest {
    private val owner = Any()

    private fun android(consent: FakeConsent = FakeConsent()) = AndroidVoiceRecognizer(
        AndroidSpeechPlatform(ApplicationProvider.getApplicationContext()),
        consent,
        TestLogger()
    )

    @Test fun api26To30WithoutAnyServiceIsUnavailable() {
        val real = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.R
        )
        try {
            val recognizer = android(FakeConsent(granted = true))
            assertEquals(VoiceAvailability.UNAVAILABLE, recognizer.availability())
            val listener = Recording()
            recognizer.start("en-US", listener)
            assertEquals(listOf(VoiceError.SERVICE_UNAVAILABLE), listener.errors)
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", real)
        }
    }

    @Test fun noRecognitionServiceAtAllIsUnavailable() {
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(false)
        try {
            val recognizer = android(FakeConsent(granted = true))
            assertEquals(VoiceAvailability.UNAVAILABLE, recognizer.availability())
            val listener = Recording()
            recognizer.start("de-DE", listener)
            assertEquals(listOf(VoiceError.SERVICE_UNAVAILABLE), listener.errors)
            assertEquals(null, ShadowSpeechRecognizer.getLatestSpeechRecognizer())
        } finally {
            ShadowSpeechRecognizer.reset()
        }
    }

    @Test fun anOnDeviceServiceIsUsedAndReleased() {
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(true)
        try {
            val consent = FakeConsent()
            val recognizer = android(consent)
            assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability())
            val listener = Recording()
            recognizer.start("es-ES", listener)
            assertTrue(listener.errors.isEmpty())
            assertTrue(ShadowSpeechRecognizer.getLatestSpeechRecognizer() != null)
            assertEquals("no consent needed on-device", 0, consent.grants)
            recognizer.stop()
            recognizer.cancel()
            recognizer.release()
        } finally {
            ShadowSpeechRecognizer.reset()
        }
    }

    @Test fun platformErrorsMapToSanitisedCategories() {
        val map = AndroidVoiceRecognizer.Companion::errorOf
        assertEquals(VoiceError.NO_SPEECH, map(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(VoiceError.NO_SPEECH, map(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(VoiceError.BUSY, map(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(VoiceError.BUSY, map(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS))
        assertEquals(VoiceError.PERMISSION, map(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(
            VoiceError.LANGUAGE_UNSUPPORTED,
            map(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        )
        assertEquals(
            VoiceError.LANGUAGE_UNAVAILABLE,
            map(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        )
        assertEquals(VoiceError.SERVICE_UNAVAILABLE, map(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(
            VoiceError.SERVICE_UNAVAILABLE,
            map(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        )
        assertEquals(VoiceError.FAILED, map(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(VoiceError.FAILED, map(SpeechRecognizer.ERROR_AUDIO))
    }

    @Test fun unavailableDeviceShowsUnavailableWithoutStarting() {
        val fake = FakeRecognizer(available = false)
        val voice = voiceController(fake)
        voice.start(owner) { error("no result expected") }
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE), voice.state)
        assertTrue(fake.languages.isEmpty())
    }

    @Test fun partialThenFinalIsDeliveredExactlyOnce() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val results = mutableListOf<List<String>>()
        voice.start(owner) { results += it }
        assertEquals(listOf("en-US"), fake.languages)
        fake.partial("buy")
        assertEquals(VoiceSessionState.Listening(owner, partial = "buy"), voice.state)
        assertTrue("no command or text from partials", results.isEmpty())
        fake.final("buy milk", "by milk")
        assertEquals(listOf(listOf("buy milk", "by milk")), results)
        assertEquals(VoiceSessionState.Idle, voice.state)
        // duplicate and late callbacks are ignored
        fake.final("buy milk")
        fake.partial("late")
        fake.error(VoiceError.FAILED)
        assertEquals(1, results.size)
        assertEquals(VoiceSessionState.Idle, voice.state)
    }

    @Test fun servicesWithoutPartialsStillWork() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        var result: List<String>? = null
        voice.start(owner) { result = it }
        voice.stop()
        assertEquals(1, fake.stops)
        assertEquals(VoiceSessionState.Listening(owner, stopping = true), voice.state)
        voice.stop()
        assertEquals("Stop is idempotent", 1, fake.stops)
        fake.final("hello")
        assertEquals(listOf("hello"), result)
    }

    @Test fun cancelDiscardsAndLateResultsNeverArrive() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        var delivered = false
        voice.start(owner) { delivered = true }
        voice.cancel()
        assertEquals(1, fake.cancels)
        assertEquals(VoiceSessionState.Idle, voice.state)
        fake.final("too late")
        assertTrue(!delivered)
    }

    @Test fun releaseDestroysTheRecogniser() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        voice.start(owner) { }
        voice.release()
        assertEquals(1, fake.releases)
        assertEquals(VoiceSessionState.Idle, voice.state)
    }

    @Test fun aNewSessionEndsTheOldOneAndItsCallbacks() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val other = Any()
        val first = mutableListOf<List<String>>()
        voice.start(owner) { first += it }
        val stale = fake.listener
        voice.start(other) { }
        stale.onFinal(listOf("for the old field"))
        assertTrue(first.isEmpty())
        assertTrue(voice.isActive(other))
        voice.cancelIfOwner(owner)
        assertTrue("another owner cannot cancel", voice.isActive(other))
    }

    @Test fun failuresMapToStatesAndRetryIsExplicit() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        var result: List<String>? = null
        voice.start(owner) { result = it }
        fake.error(VoiceError.NO_SPEECH)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.NO_SPEECH), voice.state)
        assertEquals("no automatic retry", 1, fake.languages.size)
        voice.retry()
        assertEquals(2, fake.languages.size)
        fake.error(VoiceError.BUSY)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.BUSY), voice.state)
        voice.retry()
        fake.error(VoiceError.FAILED)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.FAILED), voice.state)
        voice.retry()
        fake.error(VoiceError.LANGUAGE_UNAVAILABLE)
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.LANGUAGE), voice.state)
        voice.retry()
        fake.error(VoiceError.SERVICE_UNAVAILABLE)
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE), voice.state)
        voice.retry()
        fake.final("   ")
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.NO_SPEECH), voice.state)
        voice.retry()
        fake.final("finally")
        assertEquals(listOf("finally"), result)
        assertEquals(7, fake.languages.size)
    }

    @Test fun competingAlternativesReachTheSheetAsAChoice() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val host = FakeHost()
        val flow = VoiceCommandFlow(host, kotlinx.coroutines.MainScope())
        voice.start(flow) { flow.onFinal(it, VoiceLanguage.EN) }
        fake.final("pause", "end focus")
        assertTrue(flow.phase is VoiceCommandPhase.Competing)
        assertTrue("nothing planned or run yet", host.planned.isEmpty() && host.executed.isEmpty())
    }
}
