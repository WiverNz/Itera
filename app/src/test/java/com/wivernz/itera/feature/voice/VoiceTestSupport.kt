package com.wivernz.itera.feature.voice

import android.content.ComponentName
import android.os.ParcelFileDescriptor
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.voice.AndroidVoiceRecognizer
import com.wivernz.itera.core.voice.CallerAudioStream
import com.wivernz.itera.core.voice.OfflineModel
import com.wivernz.itera.core.voice.OfflineModels
import com.wivernz.itera.core.voice.OfflineSpeechEngine
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.SpeechPlatform
import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceConsentStore
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceProvider
import com.wivernz.itera.core.voice.VoiceRecognitionListener
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.File

/** Deterministic recogniser: tests drive partial/final/error callbacks, including late ones. */
class FakeRecognizer(
    var available: Boolean = true,
    var consentRequired: Boolean = false,
    var providers: List<RecognitionProviderInfo> = emptyList()
) : VoiceRecognizer {
    val languages = mutableListOf<String>()
    var consents = 0
    var selected: String? = null
    var lost = false
    val listeners = mutableListOf<VoiceRecognitionListener>()
    var stops = 0
    var cancels = 0
    var releases = 0

    val listener: VoiceRecognitionListener get() = listeners.last()

    val grammars = mutableListOf<List<String>?>()
    val availabilityLanguages = mutableListOf<String>()
    var trims = 0

    override fun trim() {
        trims++
    }

    override fun availability(languageTag: String): VoiceAvailability {
        availabilityLanguages += languageTag
        return availabilityNow()
    }

    private fun availabilityNow() = when {
        !available -> VoiceAvailability.UNAVAILABLE
        consentRequired -> VoiceAvailability.CONSENT_REQUIRED
        providers.isNotEmpty() && selected == null -> VoiceAvailability.CHOICE_REQUIRED
        else -> VoiceAvailability.AVAILABLE
    }

    override fun providers() = providers

    override fun selectProvider(id: String) {
        selected = id
    }

    override fun consumeProviderLost() = lost.also { lost = false }

    override fun allowSystemRecognition() {
        consents++
        consentRequired = false
    }

    override fun start(
        languageTag: String,
        listener: VoiceRecognitionListener,
        grammar: List<String>?
    ) {
        languages += languageTag
        listeners += listener
        grammars += grammar
    }

    override fun stop() {
        stops++
    }

    override fun cancel() {
        cancels++
    }

    override fun release() {
        releases++
    }

    fun partial(text: String) = listener.onPartial(text)

    fun final(vararg alternatives: String) = listener.onFinal(alternatives.toList())

    fun error(error: VoiceError) = listener.onError(error)
}

class FakeConsent(var granted: Boolean = false, var selected: String? = null) : VoiceConsentStore {
    var grants = 0
    var clears = 0

    override fun systemGranted() = granted

    override fun grantSystem() {
        grants++
        granted = true
    }

    override fun selectedProvider() = selected

    override fun selectProvider(id: String) {
        selected = id
    }

    override fun clearProvider() {
        clears++
        selected = null
    }
}

class FakeGate(
    var granted: Boolean = true,
    var permanent: Boolean = false,
    var grantOnRequest: Boolean = true
) : VoicePermissionGate {
    var requests = 0

    override fun granted() = granted

    override fun request(onResult: (Boolean) -> Unit) {
        requests++
        if (grantOnRequest) granted = true
        onResult(granted)
    }

    override fun permanentlyDenied() = !granted && permanent
}

fun voiceController(
    recognizer: FakeRecognizer = FakeRecognizer(),
    gate: FakeGate = FakeGate(),
    language: VoiceLanguage = VoiceLanguage.EN
) = VoiceController(recognizer, gate, language)

/** Records executions; [plan] decides what each command becomes. */
class FakeHost(
    override val voiceCommands: Set<VoiceCommandKind> = VoiceCommandKind.entries.toSet(),
    var plan: (VoiceCommand) -> VoicePlan = { VoicePlan.Reject(VoiceRejection.NotHere) },
    var outcome: suspend (VoiceAction) -> VoiceOutcome = {
        VoiceOutcome.Done(VoiceFeedback.FocusPaused)
    }
) : VoiceCommandHost {
    val planned = mutableListOf<VoiceCommand>()
    val executed = mutableListOf<VoiceAction>()

    override fun planVoice(command: VoiceCommand): VoicePlan {
        planned += command
        return plan(command)
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome {
        executed += action
        return outcome(action)
    }
}

/** Production sources, relative to the app module (the unit-test working directory). */
val mainSources = File("src/main/java/com/wivernz/itera")

fun sourcesUnder(vararg dirs: String): List<File> = dirs.flatMap { dir ->
    File(mainSources, dir).walkTopDown().filter { it.extension == "kt" }.toList()
}

/** Records which recogniser the adapter asked for; the instances are Robolectric's shadows. */
class FakePlatform(
    var onDevice: Boolean = false,
    var systemDefault: ComponentName? = null,
    var providers: List<RecognitionProviderInfo> = emptyList(),
    var callerAudio: Boolean = false,
    var micOpens: Boolean = true,
    var otherCapture: Boolean = false
) : SpeechPlatform {
    val created = mutableListOf<VoiceProvider>()
    val streams = mutableListOf<FakeAudioStream>()
    var last: SpeechRecognizer? = null

    override fun onDeviceAvailable() = onDevice

    override fun systemDefault() = systemDefault

    override fun providers() = providers

    override fun create(provider: VoiceProvider): SpeechRecognizer {
        created += provider
        return SpeechRecognizer.createSpeechRecognizer(ApplicationProvider.getApplicationContext())
            .also { last = it }
    }

    override fun callerAudioSupported() = callerAudio

    override fun openCallerAudio(): CallerAudioStream? =
        if (micOpens) FakeAudioStream().also { streams += it } else null

    override fun otherCaptureActive(ownSession: Int) = otherCapture
}

/** Milestone 013: the offline engine without Vosk; tests drive its callbacks. */
class FakeOfflineEngine : OfflineSpeechEngine {
    val started = mutableListOf<Pair<OfflineModel, List<String>?>>()
    var listener: VoiceRecognitionListener? = null
    var modelFailed: ((Throwable) -> Unit)? = null
    var stops = 0
    var cancels = 0
    var trims = 0

    override fun start(
        model: OfflineModel,
        grammar: List<String>?,
        listener: VoiceRecognitionListener,
        onModelFailed: (Throwable) -> Unit
    ) {
        started += model to grammar
        this.listener = listener
        modelFailed = onModelFailed
    }

    override fun stop() {
        stops++
    }

    override fun cancel() {
        cancels++
    }

    override fun trim() {
        trims++
    }
}

class FakeOfflineModels(vararg installed: VoiceLanguage) : OfflineModels {
    val installed = installed.toMutableSet()
    val failures = mutableListOf<Pair<VoiceLanguage, Throwable>>()

    override fun installed(language: VoiceLanguage) = if (language in
        installed
    ) {
        OfflineModel(language, File("models/${language.tag}"), "test-${language.tag}")
    } else {
        null
    }

    override fun reportLoadFailure(language: VoiceLanguage, error: Throwable) {
        failures += language to error
        // the real store keeps the files but stops offering the model until it is checked again
        installed -= language
    }
}

/** The real adapter over fakes. */
fun androidRecognizer(
    platform: SpeechPlatform,
    consent: VoiceConsentStore = FakeConsent(),
    offline: OfflineSpeechEngine = FakeOfflineEngine(),
    models: OfflineModels = FakeOfflineModels()
) = AndroidVoiceRecognizer(platform, consent, offline, models, TestLogger())

/** Caller audio without a microphone: a real pipe, counting end-of-audio and close calls. */
class FakeAudioStream : CallerAudioStream {
    private val pipe = ParcelFileDescriptor.createPipe()
    override val descriptor: ParcelFileDescriptor = pipe[0]
    override val sessionId = 42
    var ends = 0
    var closes = 0

    override fun endOfAudio() {
        ends++
    }

    override fun close() {
        closes++
        pipe.forEach { runCatching { it.close() } }
    }
}
