@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.voice

import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.runnerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SENTINEL = "zq-private-speech-7731"

/** Speech is ephemeral: never logged, stored, exported or transmitted unless accepted as ordinary text. */
@RunWith(RobolectricTestRunner::class)
class VoicePrivacyTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    private val voiceSources = sourcesUnder("core/voice", "domain/voice", "feature/voice")

    @Test fun voiceCodeLogsOnlyFixedCategories() {
        val offenders = voiceSources.flatMap { file ->
            file.readLines().filter { line ->
                val call =
                    Regex("""\b(?:logger|Log)\.[vdiwe]\(""").find(line) ?: return@filter false
                // a tag and one plain literal, nothing interpolated
                !Regex(
                    """\.[vdiwe]\(TAG, "[^"$]*"\)"""
                ).containsMatchIn(line.substring(call.range.first))
            }.map { "${file.name}: ${it.trim()}" }
        }
        assertEquals(emptyList<String>(), offenders)
        assertTrue(voiceSources.none { "android.util.Log" in it.readText() })
    }

    @Test fun voiceCodeHasNoStorageAudioAnalyticsOrNetworkPath() {
        val forbidden = listOf(
            "MediaRecorder", "AudioRecord", "FileOutputStream", "openFileOutput",
            "SavedStateHandle", "rememberSaveable", "DataStore", "androidx.room",
            "com.wivernz.itera.analytics", "com.wivernz.itera.data.", "java.net.", "HttpURLConnection",
            "EXTRA_AUDIO_SOURCE", "FOREGROUND_SERVICE"
        )
        // ADR-0022 (API 33+ caller audio): only the in-memory capture may record, and only it and the adapter
        // may hand audio to a recogniser. Nothing else in voice code may touch audio.
        val allowed = mapOf(
            "CallerAudio.kt" to setOf("MediaRecorder", "AudioRecord", "EXTRA_AUDIO_SOURCE"),
            "VoiceRecognizer.kt" to setOf("EXTRA_AUDIO_SOURCE")
        )
        val offenders = voiceSources.flatMap { file ->
            val text = file.readText()
            forbidden.filter {
                it in text && it !in allowed[file.name].orEmpty()
            }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test fun sessionAndSheetStateStaysInMemoryAndOutOfLogs() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val host =
            FakeHost(plan = {
                VoicePlan.Reject(VoiceRejection.NoMatch((it as VoiceCommand.CompleteItem).query))
            })
        val flow = VoiceCommandFlow(host, CoroutineScope(Dispatchers.Unconfined))
        voice.start(flow) { flow.onFinal(it, VoiceLanguage.EN) }
        fake.partial(SENTINEL)
        fake.final("complete $SENTINEL", "$SENTINEL alternative")
        assertEquals(VoiceCommandPhase.Rejected(VoiceRejection.NoMatch(SENTINEL)), flow.phase)
        voice.release()
        assertTrue(h.analyticsLog().none { SENTINEL in it })
    }

    @Test fun rejectedSpeechNeverReachesTheDraftAcceptedTextFollowsTypingRules() {
        val id = runBlocking { h.ensureToday() }.activities.first {
            it.techniqueId.value ==
                "two_minute_rule"
        }.id
        val vm = h.runnerViewModel(id)
        vm.state.await { !it.loading }
        // an unmatched query is not data
        vm.planVoice(VoiceCommand.CompleteItem(SENTINEL))
        vm.flushDraft()
        Thread.sleep(200)
        assertFalse(SENTINEL in runBlocking { h.plans.draft(id) }.toString())
        // an accepted item is ordinary draft text, like a typed one
        runBlocking { vm.executeVoice(VoiceAction.AddItem("Pay $SENTINEL")) }
        vm.flushDraft()
        eventually {
            (runBlocking { h.plans.draft(id) } as? ActivityResult.Template)?.takeIf {
                SENTINEL in
                    it.toString()
            }
        }
        assertTrue(h.analyticsLog().none { SENTINEL in it })
    }
}

/** Every stored analytics payload as text. */
private fun EngineHarness.analyticsLog(): List<String> = runBlocking {
    db.eventLogDao().exportSince(0).map { it.toString() }
}
