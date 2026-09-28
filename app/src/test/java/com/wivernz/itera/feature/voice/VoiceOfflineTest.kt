package com.wivernz.itera.feature.voice

import android.content.ComponentName
import com.wivernz.itera.core.voice.OfflineModel
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceProvider
import com.wivernz.itera.core.voice.VoiceRoute
import com.wivernz.itera.core.voice.VoskSpeechEngine
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceCommandParser
import com.wivernz.itera.domain.voice.VoiceGrammar
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.domain.voice.VoiceParse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val GOOGLE = ComponentName("com.google.android.tts", "g.GoogleTTSRecognitionService")

/**
 * Milestone 013: ON_DEVICE -> ITERA OFFLINE -> SYSTEM DEFAULT -> USER-SELECTED -> UNAVAILABLE. A missing model never
 * blocks; the offline session is Itera's own (no service, consent or caller audio).
 */
@RunWith(RobolectricTestRunner::class)
class VoiceOfflineTest {
    private val owner = Any()
    private val google = RecognitionProviderInfo(GOOGLE.flattenToString(), "Google")

    @Test fun routeOrder() {
        val model = OfflineModel(VoiceLanguage.RU, File("m"), "v")
        val installed = listOf(GOOGLE)
        assertEquals(
            "true on-device first",
            VoiceRoute.Use(VoiceProvider.OnDevice),
            VoiceRoute.select(true, GOOGLE, true, GOOGLE, installed, model)
        )
        assertEquals(
            "then Itera offline, before any external provider",
            VoiceRoute.Use(VoiceProvider.Offline(model)),
            VoiceRoute.select(false, GOOGLE, true, GOOGLE, installed, model)
        )
        assertEquals(
            "no model: the chain continues unchanged",
            VoiceRoute.NeedsSystemConsent,
            VoiceRoute.select(false, GOOGLE, false, null, installed, null)
        )
        assertEquals(VoiceRoute.Unavailable, VoiceRoute.select(false, null, false, null, emptyList(), null))
    }

    @Test fun anInstalledModelIsUsedWithoutConsentOrExternalProvider() {
        val platform = FakePlatform(systemDefault = GOOGLE, providers = listOf(google), callerAudio = true)
        val consent = FakeConsent()
        val engine = FakeOfflineEngine()
        val recognizer = androidRecognizer(platform, consent, engine, FakeOfflineModels(VoiceLanguage.RU))
        assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability("ru-RU"))
        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.RU)
        voice.start(owner) { }
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
        assertEquals(VoiceLanguage.RU, engine.started.single().first.language)
        assertNull("dictation is free-form", engine.started.single().second)
        assertTrue("no platform recogniser", platform.created.isEmpty())
        assertTrue("no caller-audio pipe", platform.streams.isEmpty())
        assertEquals(0, consent.grants)
    }

    @Test fun theModelIsPerLanguageAndAMissingOneFallsThrough() {
        val platform = FakePlatform(systemDefault = GOOGLE)
        val engine = FakeOfflineEngine()
        val recognizer = androidRecognizer(platform, FakeConsent(), engine, FakeOfflineModels(VoiceLanguage.RU))
        assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability("ru-RU"))
        assertEquals("EN has no model: system consent next", VoiceAvailability.CONSENT_REQUIRED, recognizer.availability("en-US"))
        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.EN)
        voice.start(owner) { }
        assertEquals(VoiceSessionState.NeedsSystemConsent(owner), voice.state)
        assertTrue(engine.started.isEmpty())
    }

    @Test fun resultsFlowThroughTheUnchangedController() {
        val engine = FakeOfflineEngine()
        val recognizer = androidRecognizer(FakePlatform(), offline = engine, models = FakeOfflineModels(VoiceLanguage.RU))
        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.RU)
        val results = mutableListOf<List<String>>()
        voice.start(owner) { results += it }
        engine.listener!!.onPartial("позвонить")
        assertEquals(VoiceSessionState.Listening(owner, partial = "позвонить"), voice.state)
        engine.listener!!.onFinal(listOf("позвонить сантехнику завтра утром"))
        assertEquals(listOf(listOf("позвонить сантехнику завтра утром")), results)
        assertEquals(VoiceSessionState.Idle, voice.state)
    }

    @Test fun stopCancelAndBackgroundReachTheEngine() {
        val engine = FakeOfflineEngine()
        val recognizer = androidRecognizer(FakePlatform(), offline = engine, models = FakeOfflineModels(VoiceLanguage.EN))
        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.EN)
        voice.start(owner) { }
        voice.stop()
        assertEquals(1, engine.stops)
        voice.cancel()
        assertTrue(engine.cancels >= 1)
        voice.trim()
        assertEquals(1, engine.trims)
    }

    @Test fun aModelThatFailsToLoadIsReportedAndTheNextTapFallsThrough() {
        val platform = FakePlatform(systemDefault = GOOGLE)
        val engine = FakeOfflineEngine()
        val models = FakeOfflineModels(VoiceLanguage.DE)
        val voice = VoiceController(androidRecognizer(platform, FakeConsent(), engine, models), FakeGate(), VoiceLanguage.DE)
        voice.start(owner) { }
        engine.modelFailed!!.invoke()
        engine.listener!!.onError(VoiceError.FAILED)
        assertEquals(listOf(VoiceLanguage.DE), models.unusable)
        assertEquals(VoiceSessionState.Failed(owner, VoiceFailure.FAILED), voice.state)
        voice.retry()
        assertEquals(VoiceSessionState.NeedsSystemConsent(owner), voice.state)
    }

    @Test fun commandContextsWithoutFreeTextGetAGrammar() {
        val engine = FakeOfflineEngine()
        val recognizer = androidRecognizer(FakePlatform(), offline = engine, models = FakeOfflineModels(VoiceLanguage.RU))
        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.RU)
        val focus = setOf(
            VoiceCommandKind.START_FOCUS,
            VoiceCommandKind.PAUSE_FOCUS,
            VoiceCommandKind.RESUME_FOCUS,
            VoiceCommandKind.END_FOCUS,
            VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION
        )
        val grammar = VoiceGrammar.forCommands(focus, VoiceLanguage.RU)!!
        voice.start(owner, grammar) { }
        assertEquals(grammar, engine.started.last().second)
        voice.cancel()
        // retry keeps the grammar
        voice.start(owner, grammar) { }
        engine.listener!!.onError(VoiceError.NO_SPEECH)
        voice.retry()
        assertEquals(grammar, engine.started.last().second)
    }

    @Test fun grammarsContainOnlyPhrasesTheParserAccepts() {
        val fixed = VoiceCommandKind.entries.toSet() - VoiceCommandKind.ADD_ITEM - VoiceCommandKind.COMPLETE_ITEM
        for (language in VoiceLanguage.entries) {
            val grammar = VoiceGrammar.forCommands(fixed, language)!!
            assertTrue(grammar.isNotEmpty())
            grammar.forEach { phrase ->
                assertTrue("$language: $phrase", VoiceCommandParser.parse(phrase, language) is VoiceParse.Recognised)
            }
        }
        val ru = VoiceGrammar.forCommands(setOf(VoiceCommandKind.START_FOCUS), VoiceLanguage.RU)!!
        assertTrue("начни фокус на двадцать пять минут" in ru)
        assertNull("free-text commands need free-form", VoiceGrammar.forCommands(VoiceCommandKind.entries.toSet(), VoiceLanguage.EN))
        assertNull(VoiceGrammar.forCommands(emptySet(), VoiceLanguage.EN))
    }

    @Test fun voskJsonIsParsedToAlternatives() {
        assertEquals(
            listOf("start focus", "stark focus"),
            VoskSpeechEngine.alternativesOf(
                """{"alternatives":[{"confidence":0.9,"text":"start focus"},{"text":"stark focus"},{"text":""}]}"""
            )
        )
        assertEquals(listOf("позвонить сантехнику"), VoskSpeechEngine.alternativesOf("""{"text":"позвонить сантехнику"}"""))
        assertEquals("grammar unknowns dropped", listOf("pause"), VoskSpeechEngine.alternativesOf("""{"text":"[unk] pause"}"""))
        assertTrue(VoskSpeechEngine.alternativesOf("""{"text":""}""").isEmpty())
        assertTrue(VoskSpeechEngine.alternativesOf("not json").isEmpty())
        assertEquals("начни", VoskSpeechEngine.partialOf("""{"partial":"начни"}"""))
    }

    @Test fun onlyTheCoreAdapterTouchesVosk() {
        val users = sourcesUnder("").filter { "org.vosk" in it.readText() }.map { it.name }
        assertEquals(listOf("VoskSpeechEngine.kt"), users)
        val engine = sourcesUnder("core/voice").first { it.name == "VoskSpeechEngine.kt" }.readText()
        for (forbidden in listOf("FileOutputStream", "openFileOutput", "cacheDir", "filesDir", "Log.")) {
            assertFalse("no audio file or raw log: $forbidden", forbidden in engine)
        }
        val calls = Regex("""logger\.\w\(([^)]*)\)""").findAll(engine).map { it.groupValues[1].trim() }.toList()
        calls.forEach { assertTrue("literal-only log: $it", Regex("""TAG,\s*"[^"$]*"""").matches(it)) }
    }
}
