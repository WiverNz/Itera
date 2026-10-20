@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.quality

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelSource
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.voice.VoiceItem
import com.wivernz.itera.domain.voice.VoiceLanguage
import com.wivernz.itera.feature.exercise.runner.ConfirmDialog
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.voice.FakeHost
import com.wivernz.itera.feature.voice.FakeRecognizer
import com.wivernz.itera.feature.voice.LocalVoiceController
import com.wivernz.itera.feature.voice.OfflineModelSheet
import com.wivernz.itera.feature.voice.ProvideVoiceCommands
import com.wivernz.itera.feature.voice.RecognizerSheet
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandAction
import com.wivernz.itera.feature.voice.VoiceFailure
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceSessionState
import com.wivernz.itera.feature.voice.VoiceStatusPanel
import com.wivernz.itera.feature.voice.VoiceUnavailable
import com.wivernz.itera.feature.voice.voiceController
import com.wivernz.itera.feature.you.YouScreen
import com.wivernz.itera.feature.you.YouUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val providers = listOf(
    RecognitionProviderInfo("synthetic.one", "Speech App One"),
    RecognitionProviderInfo("synthetic.two", "Speech App Two")
)

/** Every voice panel state from fake recognition: normal host (both themes, 2x, Russian) and the always-dark one. */
private val panelStates: Map<String, VoiceSessionState> = mapOf(
    "Listening" to VoiceSessionState.Listening(Unit),
    "Partial" to VoiceSessionState.Listening(Unit, partial = "Call the plumber tomorrow"),
    "Processing" to VoiceSessionState.Listening(Unit, stopping = true),
    "FailedNoSpeech" to VoiceSessionState.Failed(Unit, VoiceFailure.NO_SPEECH),
    "FailedBusy" to VoiceSessionState.Failed(Unit, VoiceFailure.BUSY),
    "Failed" to VoiceSessionState.Failed(Unit, VoiceFailure.FAILED),
    "Permission" to VoiceSessionState.NeedsPermission(Unit),
    "Denied" to VoiceSessionState.PermissionDenied(Unit, false),
    "DeniedPermanently" to VoiceSessionState.PermissionDenied(Unit, true),
    "UnavailableDevice" to VoiceSessionState.Unavailable(Unit, VoiceUnavailable.DEVICE),
    "UnavailableLanguage" to VoiceSessionState.Unavailable(Unit, VoiceUnavailable.LANGUAGE),
    "UnsupportedLanguage" to
        VoiceSessionState.Unavailable(Unit, VoiceUnavailable.LANGUAGE_UNSUPPORTED),
    "SystemConsent" to VoiceSessionState.NeedsSystemConsent(Unit),
    "ChooseProvider" to VoiceSessionState.ChooseProvider(Unit, providers),
    "ChooseProviderLost" to VoiceSessionState.ChooseProvider(Unit, providers, lost = true),
    "ProviderConsent" to VoiceSessionState.NeedsProviderConsent(Unit, providers.first())
)

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = GOLDEN_QUALIFIERS)
class VoicePanelGoldenTest(private val name: String) {
    @get:Rule val compose = createComposeRule()
    private val voice = voiceController(FakeRecognizer(), language = VoiceLanguage.EN)

    @Before fun setUp() = reduceMotion()

    @Test fun normalHost() = captureVariants(
        compose,
        name,
        listOf(
            GoldenVariant.LIGHT,
            GoldenVariant.DARK,
            GoldenVariant.LIGHT_2X,
            GoldenVariant.RU_LIGHT
        )
            .map { it.inFolder("voice") }
    ) { ScreenColumn { VoiceStatusPanel(voice, panelStates.getValue(name), false, {}) } }

    @Test fun alwaysDarkHost() = captureVariants(
        compose,
        name,
        listOf(GoldenVariant.LIGHT, GoldenVariant.LIGHT_2X).map { it.inFolder("voice-night") }
    ) {
        NightSurface {
            ScreenColumn { VoiceStatusPanel(voice, panelStates.getValue(name), true, {}) }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun states(): List<Array<Any>> = panelStates.keys.map { arrayOf(it) }
    }
}

/** The command sheet's phases and the Settings voice section and sheets; English, both themes and 2x. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = GOLDEN_QUALIFIERS)
class VoiceSheetGoldenTest {
    @get:Rule val compose = createComposeRule()
    private val variants =
        listOf(GoldenVariant.LIGHT, GoldenVariant.DARK, GoldenVariant.LIGHT_2X).map {
            it.inFolder("voice")
        }
    private val anna = VoiceItem("1", "Call Anna", 1)
    private val annaAgain = VoiceItem("3", "Call Anna", 3)

    @Before fun setUp() = reduceMotion()

    private fun command(name: String, host: FakeHost, vararg heard: String) {
        val fake = FakeRecognizer()
        val voice = voiceController(fake)
        captureVariants(compose, name, variants, windows = true, prepare = {
            compose.runOnIdle { voice.cancel() }
            compose.onNodeWithTag("VoiceCommand").performClick()
            if (heard.isNotEmpty()) {
                compose.onNodeWithTag("VoiceSpeak").performClick()
                compose.runOnIdle { fake.final(*heard) }
            }
        }) {
            CompositionLocalProvider(LocalVoiceController provides voice) {
                ProvideVoiceCommands(host) { ScreenColumn { VoiceCommandAction() } }
            }
        }
    }

    @Test fun commandReady() = command("CommandReady", FakeHost())

    @Test fun commandAmbiguousItem() = command(
        "CommandAmbiguous",
        FakeHost(plan = {
            VoicePlan.Choose(listOf(anna, annaAgain))
        }),
        "complete call anna"
    )

    @Test fun commandCompeting() = command(
        "CommandCompeting",
        FakeHost(plan = {
            VoicePlan.Run(VoiceAction.PauseFocus)
        }),
        "pause",
        "end focus"
    )

    /** Screen capture does not keep window z-order over the sheet, so the confirmation is captured on its own. */
    @Test fun commandConfirmation() =
        captureVariants(compose, "CommandConfirmation", variants, windows = true) {
            ConfirmDialog(
                title = stringResource(R.string.voice_complete_title),
                body = stringResource(R.string.voice_complete_body),
                confirm = stringResource(R.string.voice_complete),
                dismiss = stringResource(R.string.exercise_keep_going),
                onConfirm = {},
                onDismiss = {}
            )
        }

    @Test fun commandNotUnderstood() = command("CommandNotUnderstood", FakeHost(), "sing a song")

    @Test fun commandDone() = command(
        "CommandDone",
        FakeHost(
            plan = { VoicePlan.Run(VoiceAction.CompleteItem(anna)) },
            outcome = { VoiceOutcome.Done(VoiceFeedback.Completed(anna.label)) }
        ),
        "complete call anna"
    )

    @Test fun settingsVoiceSection() = captureVariants(compose, "SettingsVoice", variants, prepare = {
        // bring the Voice group to the top of the screen, not merely into view at the bottom edge
        val voice = compose.onNodeWithText("Voice", ignoreCase = true).performScrollTo()
        val top = voice.fetchSemanticsNode().boundsInRoot.top
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(
                0f,
                top - 24f
            )
        }
    }) {
        YouScreen(
            YouUiState(
                preferences = UserPreferences(systemRecognitionAllowed = true),
                loading = false,
                voiceModels = mapOf(
                    VoiceLanguage.EN to
                        VoiceModelState.Installed("0.22", 68_000_000, VoiceModelSource.IMPORTED)
                )
            ),
            {},
            true
        )
    }

    private fun offline(name: String, state: VoiceModelState, play: Boolean = false) =
        captureVariants(compose, name, variants, windows = true) {
            OfflineModelSheet(
                VoiceLanguage.EN,
                state,
                VoiceModelInfo("vosk-model-small-en-us-0.15.zip", 41_200_000),
                play,
                {},
                {},
                {},
                {}
            )
        }

    @Test fun offlineNotInstalled() =
        offline("OfflineNotInstalled", VoiceModelState.NotInstalled, play = true)

    @Test fun offlineDownloading() =
        offline("OfflineDownloading", VoiceModelState.Downloading(40), play = true)

    @Test fun offlineWaitingForWifi() =
        offline("OfflineWaitingForWifi", VoiceModelState.WaitingForWifi, play = true)

    @Test fun offlineInstalled() = offline(
        "OfflineInstalled",
        VoiceModelState.Installed("0.15", 68_000_000, VoiceModelSource.IMPORTED)
    )

    @Test fun offlineDamaged() = offline("OfflineDamaged", VoiceModelState.Damaged)

    @Test fun recognizerSheet() = captureVariants(
        compose,
        "RecognizerSheet",
        variants,
        windows = true
    ) {
        RecognizerSheet(providers, "synthetic.two", {}, {})
    }
}
