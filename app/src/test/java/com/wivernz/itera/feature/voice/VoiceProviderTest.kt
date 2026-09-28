package com.wivernz.itera.feature.voice

import android.content.ComponentName
import android.os.Bundle
import android.speech.SpeechRecognizer
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
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.SpeechPlatform
import com.wivernz.itera.core.voice.SystemRecognizerResolver
import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceProvider
import com.wivernz.itera.core.voice.VoiceRecognitionListener
import com.wivernz.itera.core.voice.VoiceRoute
import com.wivernz.itera.data.preferences.DataStorePreferencesRepository
import com.wivernz.itera.data.preferences.PreferencesVoiceConsentStore
import com.wivernz.itera.domain.FakePreferences
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.reduceMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

private val CLAUDE =
    ComponentName("com.anthropic.claude", "com.anthropic.claude.ClaudeRecognitionService")
private val OTHER = ComponentName("com.example.speech", "com.example.speech.Service")
private val GSA =
    ComponentName("com.google.android.googlequicksearchbox", "g.GoogleRecognitionService")

private fun info(component: ComponentName, label: String) =
    RecognitionProviderInfo(component.flattenToString(), label)

internal class Errors : VoiceRecognitionListener {
    val errors = mutableListOf<VoiceError>()
    override fun onPartial(text: String) = Unit
    override fun onFinal(alternatives: List<String>) = Unit
    override fun onError(error: VoiceError) {
        errors += error
    }
}

/**
 * ADR-0022 (amended): ON_DEVICE -> usable SYSTEM DEFAULT -> USER-SELECTED INSTALLED PROVIDER -> UNAVAILABLE. No
 * provider is ever picked for the user, consent is never implied, and a chosen provider never falls through.
 */
@RunWith(RobolectricTestRunner::class)
class VoiceProviderTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val folder = TemporaryFolder()
    private val owner = Any()

    @Before fun setUp() = reduceMotion()

    private fun adapter(platform: FakePlatform, consent: FakeConsent) =
        androidRecognizer(platform, consent)

    private fun controller(
        platform: FakePlatform,
        consent: FakeConsent,
        gate: FakeGate = FakeGate()
    ) = VoiceController(adapter(platform, consent), gate, VoiceLanguage.EN)

    @Test fun selectionOrder() {
        val installed = listOf(CLAUDE, OTHER)
        fun route(
            onDevice: Boolean = false,
            default: ComponentName? = null,
            consent: Boolean = false,
            selected: ComponentName? = null,
            available: List<ComponentName> = installed
        ) = VoiceRoute.select(onDevice, default, consent, selected, available)
        assertEquals(
            VoiceRoute.Use(VoiceProvider.OnDevice),
            route(onDevice = true, default = GSA, selected = CLAUDE)
        )
        assertEquals(
            VoiceRoute.Use(VoiceProvider.SystemDefault(GSA)),
            route(default = GSA, consent = true, selected = CLAUDE)
        )
        assertEquals(
            "a usable default needs its consent",
            VoiceRoute.NeedsSystemConsent,
            route(default = GSA, selected = CLAUDE)
        )
        assertEquals(VoiceRoute.Use(VoiceProvider.Selected(CLAUDE)), route(selected = CLAUDE))
        assertEquals("never the first installed provider", VoiceRoute.NeedsChoice, route())
        assertEquals(
            "a chosen provider that is gone does not fall through",
            VoiceRoute.NeedsChoice,
            route(selected = GSA)
        )
        assertEquals(VoiceRoute.Unavailable, route(consent = true, available = emptyList()))
    }

    @Test fun systemDefaultIsOnlyTheResolvableSelectionNeverTheAssistant() {
        val resolvable = listOf(CLAUDE, OTHER)
        assertEquals(OTHER, SystemRecognizerResolver.resolve(OTHER.flattenToString(), resolvable))
        // empty selection (the vivo case)
        assertNull(SystemRecognizerResolver.resolve("", resolvable))
        assertNull(SystemRecognizerResolver.resolve(null, resolvable))
        // selected but hidden from queries and unbindable (vivo hides the Google app): unusable
        assertNull(SystemRecognizerResolver.resolve(GSA.flattenToString(), resolvable))
    }

    @Test fun onDeviceAvailableUsesTheOnDeviceRecognizerWithoutConsent() {
        val platform = FakePlatform(
            onDevice = true,
            systemDefault = GSA,
            providers = listOf(info(CLAUDE, "Claude"))
        )
        val consent = FakeConsent()
        val recognizer = adapter(platform, consent)
        assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability("en-US"))
        val listener = Errors()
        recognizer.start("en-US", listener)
        assertEquals(listOf<VoiceProvider>(VoiceProvider.OnDevice), platform.created)
        assertTrue(listener.errors.isEmpty())
        assertEquals(0, consent.grants)
        recognizer.release()
    }

    @Test fun systemDefaultNeedsConsentThenIsUsed() {
        val platform = FakePlatform(systemDefault = GSA, providers = listOf(info(CLAUDE, "Claude")))
        val consent = FakeConsent()
        val gate = FakeGate(granted = false)
        val voice = controller(platform, consent, gate)
        voice.start(owner) { }
        assertEquals(VoiceSessionState.NeedsSystemConsent(owner), voice.state)
        assertEquals("no microphone prompt before consent", 0, gate.requests)
        assertTrue(platform.created.isEmpty())
        voice.acceptSystemRecognition()
        assertEquals(1, consent.grants)
        assertEquals(VoiceSessionState.NeedsPermission(owner), voice.state)
        voice.requestPermission()
        assertEquals(listOf<VoiceProvider>(VoiceProvider.SystemDefault(GSA)), platform.created)
        voice.release()
    }

    @Test fun declinedSystemConsentStartsNothing() {
        val platform = FakePlatform(systemDefault = GSA)
        val consent = FakeConsent()
        val voice = controller(platform, consent)
        voice.start(owner) { error("nothing may be delivered") }
        voice.cancel() // "Not now"
        voice.acceptSystemRecognition() // stale
        assertEquals(VoiceSessionState.Idle, voice.state)
        assertEquals(0, consent.grants)
        assertTrue(platform.created.isEmpty())
    }

    @Test fun noDefaultOffersAPickerAndNeverPreselects() {
        val providers = listOf(info(CLAUDE, "Claude"), info(OTHER, "Speech"))
        val platform = FakePlatform(providers = providers)
        val consent = FakeConsent()
        val recognizer = adapter(platform, consent)
        assertEquals(VoiceAvailability.CHOICE_REQUIRED, recognizer.availability("en-US"))
        // even a direct start cannot reach an installed provider without a choice
        val listener = Errors()
        recognizer.start("ru-RU", listener)
        assertEquals(listOf(VoiceError.SERVICE_UNAVAILABLE), listener.errors)
        assertTrue(platform.created.isEmpty())

        val voice = VoiceController(recognizer, FakeGate(), VoiceLanguage.RU)
        voice.start(owner) { }
        assertEquals(VoiceSessionState.ChooseProvider(owner, providers), voice.state)
        assertNull(consent.selected)
        assertTrue(platform.created.isEmpty())
    }

    @Test fun pickingAProviderShowsItsConsentAndOnlyAcceptStoresIt() {
        val claude = info(CLAUDE, "Claude")
        val platform = FakePlatform(providers = listOf(claude, info(OTHER, "Speech")))
        val consent = FakeConsent()
        val voice = controller(platform, consent, FakeGate(granted = false))
        voice.start(owner) { }
        voice.pickProvider(claude)
        assertEquals(VoiceSessionState.NeedsProviderConsent(owner, claude), voice.state)
        assertNull("picking is not consent", consent.selected)
        voice.acceptProvider()
        assertEquals(CLAUDE.flattenToString(), consent.selected)
        assertEquals(VoiceSessionState.NeedsPermission(owner), voice.state)
        voice.requestPermission()
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
        assertEquals(listOf<VoiceProvider>(VoiceProvider.Selected(CLAUDE)), platform.created)
        voice.release()
        // persisted: the next tap listens with the same provider, no picker
        val next = controller(platform, consent)
        next.start(owner) { }
        assertEquals(VoiceSessionState.Listening(owner), next.state)
        assertEquals(VoiceProvider.Selected(CLAUDE), platform.created.last())
        next.release()
    }

    @Test fun notNowAtTheProviderConsentStoresNothing() {
        val claude = info(CLAUDE, "Claude")
        val platform = FakePlatform(providers = listOf(claude))
        val consent = FakeConsent()
        val voice = controller(platform, consent)
        voice.start(owner) { error("nothing may be delivered") }
        voice.pickProvider(claude)
        voice.cancel()
        voice.acceptProvider() // stale
        assertNull(consent.selected)
        assertTrue(platform.created.isEmpty())
        voice.start(owner) { }
        assertEquals(VoiceSessionState.ChooseProvider(owner, listOf(claude)), voice.state)
    }

    @Test fun aChosenProviderThatDisappearsIsForgottenAndTheUserChoosesAgain() {
        val other = info(OTHER, "Speech")
        val platform = FakePlatform(providers = listOf(other))
        val consent = FakeConsent(selected = CLAUDE.flattenToString())
        val voice = controller(platform, consent)
        voice.start(owner) { }
        assertNull(consent.selected)
        assertEquals(
            VoiceSessionState.ChooseProvider(owner, listOf(other), lost = true),
            voice.state
        )
        assertTrue("never falls through to another provider", platform.created.isEmpty())
    }

    @Test fun aChosenProviderThatFailsToBindIsForgotten() {
        val platform =
            FakePlatform(providers = listOf(info(CLAUDE, "Claude"), info(OTHER, "Speech")))
        val consent = FakeConsent(selected = CLAUDE.flattenToString())
        val voice = controller(platform, consent)
        voice.start(owner) { error("nothing may be delivered") }
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
        // Android 16 reports a failed bind as ERROR_TOO_MANY_REQUESTS before the service is ready
        ShadowLooper.idleMainLooper()
        shadowOf(platform.last).triggerOnError(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS)
        assertNull(consent.selected)
        val state = voice.state as VoiceSessionState.ChooseProvider
        assertTrue(state.lost)
        assertEquals("no other provider was tried", 1, platform.created.size)
    }

    @Test fun aChosenProviderThatNeverAnswersIsForgotten() {
        val platform = FakePlatform(providers = listOf(info(CLAUDE, "Claude")))
        val consent = FakeConsent(selected = CLAUDE.flattenToString())
        val voice = controller(platform, consent)
        voice.start(owner) { error("nothing may be delivered") }
        ShadowLooper.idleMainLooper(7, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
        ShadowLooper.idleMainLooper(2, java.util.concurrent.TimeUnit.SECONDS)
        assertNull(consent.selected)
        assertTrue((voice.state as VoiceSessionState.ChooseProvider).lost)
    }

    @Test fun aProviderThatAnswersInTimeIsKept() {
        val platform = FakePlatform(providers = listOf(info(CLAUDE, "Claude")))
        val consent = FakeConsent(selected = CLAUDE.flattenToString())
        val voice = controller(platform, consent)
        voice.start(owner) { }
        ShadowLooper.idleMainLooper()
        shadowOf(platform.last).triggerOnReadyForSpeech(Bundle())
        ShadowLooper.idleMainLooper(20, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(CLAUDE.flattenToString(), consent.selected)
        assertEquals(VoiceSessionState.Listening(owner), voice.state)
    }

    @Test fun errorsAfterTheProviderIsReadyKeepTheChoice() {
        val platform = FakePlatform(providers = listOf(info(CLAUDE, "Claude")))
        val consent = FakeConsent(selected = CLAUDE.flattenToString())
        val voice = controller(platform, consent)
        voice.start(owner) { }
        ShadowLooper.idleMainLooper()
        val shadow = shadowOf(platform.last)
        shadow.triggerOnReadyForSpeech(Bundle())
        shadow.triggerOnError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        assertEquals(CLAUDE.flattenToString(), consent.selected)
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE), voice.state)
    }

    @Test fun noRecognitionServiceIsUnavailable() {
        val platform = FakePlatform()
        val voice = controller(platform, FakeConsent(granted = true))
        voice.start(owner) { }
        assertEquals(VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE), voice.state)
        assertTrue(platform.created.isEmpty())
    }

    @Test fun choicesPersistAcrossRestarts() = runBlocking {
        val file = folder.root.resolve("user.preferences_pb")
        val first = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(scope = first, produceFile = { file }),
            TestLogger()
        )
        val store = PreferencesVoiceConsentStore(repo, Dispatchers.IO)
        assertFalse(store.systemGranted())
        assertNull(store.selectedProvider())
        store.grantSystem()
        store.selectProvider(CLAUDE.flattenToString())
        assertTrue("takes effect at once", store.systemGranted())
        assertEquals(CLAUDE.flattenToString(), store.selectedProvider())
        withTimeout(5_000) {
            while (repo.preferences.first().let {
                    !it.systemRecognitionAllowed || it.selectedRecognizer == null
                }
            ) {
                delay(10)
            }
        }
        first.cancel()
        delay(50)

        val second = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = DataStorePreferencesRepository(
                PreferenceDataStoreFactory.create(scope = second, produceFile = { file }),
                TestLogger()
            )
            val restored = PreferencesVoiceConsentStore(reopened, Dispatchers.IO)
            withTimeout(5_000) {
                while (!restored.systemGranted() || restored.selectedProvider() == null) delay(10)
            }
            assertEquals(CLAUDE.flattenToString(), restored.selectedProvider())
        } finally {
            second.cancel()
        }
    }

    @Test fun choicesCanBeChangedOrRevokedInSettings() {
        val prefs = FakePreferences()
        val store = PreferencesVoiceConsentStore(prefs, Dispatchers.Unconfined)
        val platform =
            FakePlatform(providers = listOf(info(CLAUDE, "Claude"), info(OTHER, "Speech")))
        val recognizer = androidRecognizer(platform, store)
        recognizer.selectProvider(CLAUDE.flattenToString())
        assertEquals(CLAUDE.flattenToString(), prefs.state.value.selectedRecognizer)
        assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability("en-US"))
        // Settings: another app (after its consent in the sheet), then None
        prefs.state.value = prefs.state.value.copy(selectedRecognizer = OTHER.flattenToString())
        recognizer.start("en-US", Errors())
        assertEquals(VoiceProvider.Selected(OTHER), platform.created.last())
        prefs.state.value = prefs.state.value.copy(selectedRecognizer = null)
        assertEquals(VoiceAvailability.CHOICE_REQUIRED, recognizer.availability("en-US"))
        // the system-default consent is revoked the same way
        platform.systemDefault = GSA
        prefs.state.value = prefs.state.value.copy(systemRecognitionAllowed = true)
        assertEquals(VoiceAvailability.AVAILABLE, recognizer.availability("en-US"))
        prefs.state.value = prefs.state.value.copy(systemRecognitionAllowed = false)
        assertEquals(VoiceAvailability.CONSENT_REQUIRED, recognizer.availability("en-US"))
        // an id that is not an installed provider is never stored
        recognizer.selectProvider("com.evil/.Service")
        assertNull(prefs.state.value.selectedRecognizer)
    }

    @Test fun pickerAndNamedConsentKeepTypingComplete() {
        val claude = info(CLAUDE, "Claude")
        val fake = FakeRecognizer(providers = listOf(claude))
        val voice = voiceController(fake, FakeGate())
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
        compose.onNodeWithText("Choose a speech recognition app").assertExists()
        compose.onNodeWithText("Claude").performClick()
        compose.onNodeWithText("Use speech recognition from Claude?").assertExists()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithTag("VoicePanel").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(fake.selected)
            assertTrue(fake.languages.isEmpty())
        }
        compose.onNodeWithTag("Field").performTextInput("typed anyway")
        compose.runOnIdle { assertEquals("typed anyway", text) }

        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithText("Claude").performClick()
        compose.onNodeWithText("Use Claude").performClick()
        compose.runOnIdle {
            assertEquals(CLAUDE.flattenToString(), fake.selected)
            assertEquals(listOf("en-US"), fake.languages)
        }
    }

    @Test fun systemConsentPanelOffersSystemRecognition() {
        val fake = FakeRecognizer(consentRequired = true)
        val voice = voiceController(fake, FakeGate())
        compose.setContent {
            IteraTheme {
                CompositionLocalProvider(LocalVoiceController provides voice) {
                    VoiceNoteField("", {}, "Task", maxChars = 100)
                }
            }
        }
        compose.onNodeWithTag("VoiceMic").performClick()
        compose.onNodeWithText("Use system speech recognition?").assertExists()
        compose.onNodeWithText("Use system recognition").performClick()
        compose.runOnIdle {
            assertEquals(1, fake.consents)
            assertEquals(listOf("en-US"), fake.languages)
        }
    }
}
