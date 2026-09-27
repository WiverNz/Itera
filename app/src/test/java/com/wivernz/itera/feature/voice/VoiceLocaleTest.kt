package com.wivernz.itera.feature.voice

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceCommandParser
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.domain.voice.VoiceParse
import com.wivernz.itera.feature.reduceMotion
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceLocaleTest {
    @get:Rule val compose = createComposeRule()
    private val owner = Any()

    @Before fun setUp() = reduceMotion()

    @Test fun appLanguageSelectsRecognitionAndParserLanguage() {
        assertEquals(VoiceLanguage.EN, VoiceLanguage.of("en"))
        assertEquals(VoiceLanguage.RU, VoiceLanguage.of("ru"))
        assertEquals(VoiceLanguage.DE, VoiceLanguage.of("DE"))
        assertEquals(VoiceLanguage.ES, VoiceLanguage.of("es"))
        // Match device with an unsupported device language resolves like the UI: English
        assertEquals(VoiceLanguage.EN, VoiceLanguage.of("fr"))
        assertEquals(VoiceLanguage.EN, VoiceLanguage.of(null))
        assertEquals(
            listOf("en-US", "ru-RU", "de-DE", "es-ES"),
            VoiceLanguage.entries.map {
                it.tag
            }
        )
    }

    @Test fun switchingLanguageCancelsAndTheNextTapUsesTheNewOne() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake, language = VoiceLanguage.EN)
        var delivered = false
        voice.start(owner) { delivered = true }
        val stale = fake.listener
        voice.onLanguage(VoiceLanguage.RU)
        assertEquals(VoiceSessionState.Idle, voice.state)
        stale.onFinal(listOf("late english"))
        assertTrue(!delivered)
        voice.start(owner) { }
        assertEquals(listOf("en-US", "ru-RU"), fake.languages)
        // the same language is not a change
        voice.onLanguage(VoiceLanguage.RU)
        assertTrue(voice.isActive(owner))
    }

    @Test fun pendingCommandsDoNotSurviveALanguageChange() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        val host = FakeHost()
        val flow = VoiceCommandFlow(host, kotlinx.coroutines.MainScope())
        voice.start(flow) { flow.onFinal(it, voice.language) }
        voice.onLanguage(VoiceLanguage.DE)
        fake.listeners.first().onFinal(listOf("pause"))
        assertEquals(VoiceCommandPhase.Ready, flow.phase)
        assertTrue(host.planned.isEmpty())
    }

    @Test fun savedTextSurvivesALanguageSwitchWhileListening() {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        var text by mutableStateOf("Позвонить маме")
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    VoiceNoteField(text, { text = it }, "Task", maxChars = 100)
                }
            }
        }
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.runOnIdle { voice.onLanguage(VoiceLanguage.ES) }
        compose.onNodeWithTag("VoicePanel").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Позвонить маме", text) }
    }

    /** Translation check: every voice_* key ships in all four catalogues, in app and prototype alike. */
    @Test fun voiceCopyShipsInFourLanguagesAndMatchesThePrototype() {
        val locales = listOf("values", "values-ru", "values-de", "values-es")
        val app = locales.associateWith { strings(File("src/main/res/$it/milestone012.xml")) }
        val keys = app.getValue("values").keys
        assertTrue(keys.size > 60)
        locales.forEach { locale ->
            assertEquals(locale, keys, app.getValue(locale).keys)
            app.getValue(locale).forEach { (key, value) ->
                assertTrue("$locale/$key", value.isNotBlank())
            }
            assertEquals(
                "prototype $locale",
                app.getValue(locale),
                strings(File("../design/app/src/main/res/$locale/milestone012.xml"))
            )
        }
    }

    /** Examples shown in the sheet must be commands in their own language. */
    @Test fun localisedExamplesParseInTheirLanguage() {
        val examples = mapOf(
            "voice_example_add" to VoiceCommandKind.ADD_ITEM,
            "voice_example_complete" to VoiceCommandKind.COMPLETE_ITEM,
            "voice_example_start" to VoiceCommandKind.START_FOCUS,
            "voice_example_pause" to VoiceCommandKind.PAUSE_FOCUS,
            "voice_example_resume" to VoiceCommandKind.RESUME_FOCUS,
            "voice_example_end" to VoiceCommandKind.END_FOCUS,
            "voice_example_complete_exercise" to VoiceCommandKind.COMPLETE_CURRENT_EXERCISE,
            "voice_example_recommendation" to VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION
        )
        mapOf(
            "values" to VoiceLanguage.EN,
            "values-ru" to VoiceLanguage.RU,
            "values-de" to VoiceLanguage.DE,
            "values-es" to VoiceLanguage.ES
        ).forEach { (locale, language) ->
            val catalogue = strings(File("src/main/res/$locale/milestone012.xml"))
            examples.forEach { (key, kind) ->
                val parsed = VoiceCommandParser.parse(catalogue.getValue(key), language)
                assertEquals(
                    "$locale/$key",
                    kind,
                    (parsed as? VoiceParse.Recognised)?.command?.kind
                )
            }
        }
    }

    private fun strings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
