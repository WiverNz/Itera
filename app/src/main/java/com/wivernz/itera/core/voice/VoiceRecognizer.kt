package com.wivernz.itera.core.voice

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.wivernz.itera.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Whether voice can start (ADR-0022). [CONSENT_REQUIRED]: only the system default recogniser exists and the user
 * has not agreed to it yet. [CHOICE_REQUIRED]: no usable on-device or system recogniser, but installed apps offer
 * one and the user has not chosen and agreed to one. Callers never learn which provider will listen.
 */
enum class VoiceAvailability { AVAILABLE, CONSENT_REQUIRED, CHOICE_REQUIRED, UNAVAILABLE }

/** An installed app's recognition service, as the picker shows it. [id] is its flattened ComponentName. */
data class RecognitionProviderInfo(val id: String, val label: String, val icon: Drawable? = null)

/** Which recogniser a session uses. Internal to the adapter; UI and features never see it. */
sealed interface VoiceProvider {
    data object OnDevice : VoiceProvider
    data class SystemDefault(val component: ComponentName) : VoiceProvider
    data class Selected(val component: ComponentName) : VoiceProvider
}

/**
 * ON_DEVICE -> usable SYSTEM DEFAULT (with consent) -> USER-SELECTED INSTALLED PROVIDER (chosen and agreed to)
 * -> UNAVAILABLE. Never picks an installed provider on its own and never falls through from a chosen provider to
 * another one. Nothing infers that a non-on-device recogniser is offline.
 */
sealed interface VoiceRoute {
    data class Use(val provider: VoiceProvider) : VoiceRoute
    data object NeedsSystemConsent : VoiceRoute
    data object NeedsChoice : VoiceRoute
    data object Unavailable : VoiceRoute

    companion object {
        fun select(
            onDevice: Boolean,
            systemDefault: ComponentName?,
            systemConsent: Boolean,
            selected: ComponentName?,
            installed: List<ComponentName>
        ): VoiceRoute = when {
            onDevice -> Use(VoiceProvider.OnDevice)
            systemDefault != null && systemConsent -> Use(
                VoiceProvider.SystemDefault(systemDefault)
            )
            systemDefault != null -> NeedsSystemConsent
            selected != null && selected in installed -> Use(VoiceProvider.Selected(selected))
            installed.isNotEmpty() -> NeedsChoice
            else -> Unavailable
        }
    }
}

/**
 * The user's recogniser choices, persisted locally and changed in Settings. A provider id is stored only after the
 * user accepted the consent naming it, so a stored id is that consent. Consent writes are settings, never
 * recognition output.
 */
interface VoiceConsentStore {
    fun systemGranted(): Boolean

    fun grantSystem()

    /** The chosen installed provider's flattened ComponentName, or null. */
    fun selectedProvider(): String?

    /** Called only after the user accepted the consent for [id]. */
    fun selectProvider(id: String)

    fun clearProvider()
}

/** The platform recognisers, behind a seam so provider selection can be tested. */
interface SpeechPlatform {
    fun onDeviceAvailable(): Boolean

    /** The system default recogniser, only when it is selected and actually resolvable. */
    fun systemDefault(): ComponentName?

    /** Installed, enabled, exported RecognitionServices, sorted by label; never pre-selected. */
    fun providers(): List<RecognitionProviderInfo>

    fun create(provider: VoiceProvider): SpeechRecognizer?

    /** API 33+: external recognisers can take caller-provided audio (`EXTRA_AUDIO_SOURCE`). */
    fun callerAudioSupported(): Boolean

    /** Itera's own microphone capture into a pipe, or null when it cannot be opened. */
    fun openCallerAudio(): CallerAudioStream?

    /** Whether any capture other than [ownSession] is active, e.g. a provider opening its own microphone. */
    fun otherCaptureActive(ownSession: Int): Boolean
}

class AndroidSpeechPlatform @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SpeechPlatform {
    // The on-device factory exists from API 31.
    override fun onDeviceAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

    override fun systemDefault(): ComponentName? = runCatching {
        SystemRecognizerResolver.resolve(secure(VOICE_RECOGNITION_SERVICE), resolvable())
    }.getOrNull()

    override fun providers(): List<RecognitionProviderInfo> = runCatching {
        val pm = context.packageManager
        services().filter { it.packageName != context.packageName }.map { service ->
            RecognitionProviderInfo(
                id = ComponentName(service.packageName, service.name).flattenToString(),
                label = service.applicationInfo.loadLabel(pm).toString(),
                icon = runCatching { service.loadIcon(pm) }.getOrNull()
            )
        }.sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    override fun create(provider: VoiceProvider): SpeechRecognizer? = runCatching {
        when (provider) {
            VoiceProvider.OnDevice ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    null
                }
            is VoiceProvider.SystemDefault ->
                SpeechRecognizer.createSpeechRecognizer(context, provider.component)
            is VoiceProvider.Selected ->
                SpeechRecognizer.createSpeechRecognizer(context, provider.component)
        }
    }.getOrNull()

    override fun callerAudioSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun openCallerAudio(): CallerAudioStream? = PipedMicrophone.open(context)

    override fun otherCaptureActive(ownSession: Int): Boolean = runCatching {
        context.getSystemService(AudioManager::class.java).activeRecordingConfigurations
            .any { it.clientAudioSessionId != ownSession }
    }.getOrDefault(false)

    private fun services() = context.packageManager
        .queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
        .mapNotNull { it.serviceInfo }
        .filter { it.exported && it.enabled }

    private fun resolvable(): List<ComponentName> =
        services().map { ComponentName(it.packageName, it.name) }

    private fun secure(key: String): String? =
        runCatching { Settings.Secure.getString(context.contentResolver, key) }.getOrNull()

    private companion object {
        // Readable Settings.Secure key without a public constant.
        const val VOICE_RECOGNITION_SERVICE = "voice_recognition_service"
    }
}

/**
 * The system default recogniser is the one selected in `Settings.Secure.voice_recognition_service`, and only when
 * it resolves. An empty selection (some OEM builds) or one the OEM hides from queries is absent or unusable; the
 * assistant app is never treated as the default.
 */
object SystemRecognizerResolver {
    fun resolve(selected: String?, resolvable: List<ComponentName>): ComponentName? =
        selected?.takeIf { it.isNotBlank() }
            ?.let(ComponentName::unflattenFromString)
            ?.takeIf { it in resolvable }
}

/** Sanitised failure categories; recogniser bundles never leave the adapter. */
enum class VoiceError {
    NO_SPEECH,
    BUSY,
    PERMISSION,

    /** The language model is missing or not installed; it may be added in speech settings. */
    LANGUAGE_UNAVAILABLE,

    /** The provider does not support the language at all. */
    LANGUAGE_UNSUPPORTED,
    SERVICE_UNAVAILABLE,

    /** The chosen installed provider could not be bound or refused the request; its choice was cleared. */
    PROVIDER_UNUSABLE,
    FAILED
}

interface VoiceRecognitionListener {
    fun onPartial(text: String)

    /** Best alternative first; empty when the service returned nothing. */
    fun onFinal(alternatives: List<String>)

    fun onError(error: VoiceError)
}

/**
 * One user-initiated utterance at a time, main thread only. Never retries and never switches
 * recogniser mid-session; anything but the on-device recogniser is used only after explicit
 * consent. Carries no business logic and cannot reach storage.
 */
interface VoiceRecognizer {
    fun availability(): VoiceAvailability

    /** The user chose "Use system recognition"; stored so they are not asked again. */
    fun allowSystemRecognition()

    /** Installed providers for the picker; empty unless a choice can be offered. */
    fun providers(): List<RecognitionProviderInfo>

    /** The user picked [id] and accepted the consent naming it. */
    fun selectProvider(id: String)

    /** True once after a chosen provider disappeared or failed, so the picker can say why it is back. */
    fun consumeProviderLost(): Boolean

    fun start(languageTag: String, listener: VoiceRecognitionListener)

    /** Ask for the final result of the current utterance. */
    fun stop()

    /** Discard the current utterance; no callback follows. */
    fun cancel()

    fun release()
}

class AndroidVoiceRecognizer @Inject constructor(
    private val platform: SpeechPlatform,
    private val consent: VoiceConsentStore,
    private val logger: Logger
) : VoiceRecognizer {
    private var recognizer: SpeechRecognizer? = null
    private var providerLost = false

    private fun route(): VoiceRoute {
        val installed = platform.providers().mapNotNull { ComponentName.unflattenFromString(it.id) }
        val selected = consent.selectedProvider()?.let(ComponentName::unflattenFromString)
        val onDevice = platform.onDeviceAvailable()
        val systemDefault = if (onDevice) null else platform.systemDefault()
        // A chosen provider that is gone or disabled is forgotten, and the user chooses again.
        if (!onDevice && systemDefault == null && selected != null && selected !in installed) {
            forgetProvider()
        }
        return VoiceRoute.select(
            onDevice = onDevice,
            systemDefault = systemDefault,
            systemConsent = consent.systemGranted(),
            selected = selected?.takeIf { it in installed },
            installed = installed
        )
    }

    override fun availability(): VoiceAvailability = when (route()) {
        is VoiceRoute.Use -> VoiceAvailability.AVAILABLE
        VoiceRoute.NeedsSystemConsent -> VoiceAvailability.CONSENT_REQUIRED
        VoiceRoute.NeedsChoice -> VoiceAvailability.CHOICE_REQUIRED
        VoiceRoute.Unavailable -> VoiceAvailability.UNAVAILABLE
    }

    override fun allowSystemRecognition() = consent.grantSystem()

    override fun providers(): List<RecognitionProviderInfo> = platform.providers()

    override fun selectProvider(id: String) {
        if (platform.providers().any { it.id == id }) consent.selectProvider(id)
    }

    override fun consumeProviderLost(): Boolean = providerLost.also { providerLost = false }

    override fun start(languageTag: String, listener: VoiceRecognitionListener) {
        val provider = (route() as? VoiceRoute.Use)?.provider
        if (provider == null) {
            listener.onError(VoiceError.SERVICE_UNAVAILABLE)
            return
        }
        release()
        val created = platform.create(provider)
        if (created == null) {
            logger.w(TAG, "Recogniser could not be created")
            if (provider is VoiceProvider.Selected) {
                forgetProvider()
                listener.onError(VoiceError.PROVIDER_UNUSABLE)
            } else {
                listener.onError(VoiceError.SERVICE_UNAVAILABLE)
            }
            return
        }
        recognizer = created
        // API 33+ external providers get Itera's own capture: theirs may be silenced in the background.
        val injected = provider !is VoiceProvider.OnDevice && platform.callerAudioSupported()
        val stream = if (injected) platform.openCallerAudio() else null
        if (injected && stream == null) {
            logger.w(TAG, "Caller audio could not be opened")
            release()
            listener.onError(VoiceError.FAILED)
            return
        }
        audio = stream
        val bridge = Bridge(
            listener,
            selected = provider is VoiceProvider.Selected,
            callerAudio = stream != null,
            hooks = object : Bridge.Hooks {
                override fun heard() = stopWatchdog()

                override fun finished() = closeAudio()

                override fun unusable() {
                    logger.w(TAG, "Chosen provider failed before listening")
                    forgetProvider()
                }

                // A provider that ignores the supplied audio opens its own microphone: end rather than compete.
                override fun competingCapture(): Boolean {
                    val competing = stream != null && platform.otherCaptureActive(stream.sessionId)
                    if (competing) {
                        logger.w(TAG, "Provider opened its own microphone; session ended")
                        release()
                    }
                    return competing
                }
            }
        )
        created.setRecognitionListener(bridge)
        // EXTRA_PREFER_OFFLINE is a hint to the provider, never a privacy guarantee.
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        stream?.let { withCallerAudio(intent, it) }
        runCatching { created.startListening(intent) }.onFailure {
            // category only: platform exceptions may carry speech
            logger.w(TAG, "startListening failed")
            release()
            listener.onError(VoiceError.FAILED)
            return
        }
        // A chosen app that never answers (blocked from starting, silently refusing) counts as failing to bind.
        if (provider is VoiceProvider.Selected) {
            watchdog = Runnable {
                watchdog = null
                if (bridge.timeOut()) release()
            }.also { main.postDelayed(it, READY_TIMEOUT_MS) }
        }
    }

    @SuppressLint("InlinedApi") // only reached on API 33+
    private fun withCallerAudio(intent: Intent, stream: CallerAudioStream) {
        intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, stream.descriptor)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, CallerAudioStream.CHANNELS)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, CallerAudioStream.ENCODING)
            .putExtra(
                RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE,
                CallerAudioStream.SAMPLE_RATE
            )
    }

    private val main = Handler(Looper.getMainLooper())
    private var watchdog: Runnable? = null
    private var audio: CallerAudioStream? = null

    private fun stopWatchdog() {
        watchdog?.let(main::removeCallbacks)
        watchdog = null
    }

    private fun closeAudio() {
        audio?.close()
        audio = null
    }

    private fun forgetProvider() {
        consent.clearProvider()
        providerLost = true
    }

    /** Stop: the recogniser is asked for its final result and the supplied audio ends. */
    override fun stop() {
        runCatching { recognizer?.stopListening() }
        audio?.endOfAudio()
    }

    override fun cancel() {
        stopWatchdog()
        runCatching { recognizer?.cancel() }
        closeAudio()
    }

    override fun release() {
        stopWatchdog()
        recognizer?.let { r ->
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
        recognizer = null
        closeAudio()
    }

    /**
     * For a chosen installed provider, a bind/service failure before it is ready means the provider cannot serve
     * Itera: the choice is cleared and the listener hears [VoiceError.PROVIDER_UNUSABLE]. A final result or error
     * ends the supplied audio.
     */
    private class Bridge(
        private val listener: VoiceRecognitionListener,
        private val selected: Boolean,
        private val callerAudio: Boolean,
        private val hooks: Hooks
    ) : RecognitionListener {
        interface Hooks {
            fun heard()

            fun finished()

            fun unusable()

            fun competingCapture(): Boolean
        }

        private var ready = false
        private var done = false

        /** The chosen provider never answered: unusable. False when it already answered. */
        fun timeOut(): Boolean {
            if (ready || done) return false
            done = true
            hooks.finished()
            hooks.unusable()
            listener.onError(VoiceError.PROVIDER_UNUSABLE)
            return true
        }

        private fun heard(): Boolean {
            if (done) return false
            hooks.heard()
            return true
        }

        private fun finish() {
            done = true
            hooks.finished()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!heard()) return
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let(listener::onPartial)
        }

        override fun onResults(results: Bundle?) {
            if (!heard()) return
            finish()
            val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            listener.onFinal(alternatives.orEmpty())
        }

        override fun onError(error: Int) {
            if (!heard()) return
            finish()
            // With caller audio Itera already holds and uses the microphone: a permission error is the provider's.
            val providerPermission =
                callerAudio && error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
            when {
                selected && (providerPermission || (!ready && error in UNUSABLE)) -> {
                    hooks.unusable()
                    listener.onError(VoiceError.PROVIDER_UNUSABLE)
                }
                providerPermission -> listener.onError(VoiceError.SERVICE_UNAVAILABLE)
                else -> listener.onError(errorOf(error))
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            if (!heard()) return
            ready = true
            if (hooks.competingCapture()) {
                finish()
                listener.onError(VoiceError.FAILED)
            }
        }

        override fun onBeginningOfSpeech() {
            if (heard()) ready = true
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    // Error codes are compile-time constants; the recogniser only exists on API 31+ anyway.
    @SuppressLint("InlinedApi")
    companion object {
        private const val TAG = "Voice"
        private const val MAX_RESULTS = 3
        internal const val READY_TIMEOUT_MS = 8_000L

        private val NO_SPEECH = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        )
        private val BUSY = setOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS
        )
        private val SERVICE = setOf(
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT
        )

        /**
         * Before the provider is ready, these mean it could not be bound or will not serve this client (a failed
         * bind reports ERROR_TOO_MANY_REQUESTS on Android 16).
         */
        internal val UNUSABLE = setOf(
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS
        )

        /** Maps a platform error code to a sanitised category. */
        fun errorOf(code: Int): VoiceError = when (code) {
            in NO_SPEECH -> VoiceError.NO_SPEECH
            in BUSY -> VoiceError.BUSY
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> VoiceError.LANGUAGE_UNSUPPORTED
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> VoiceError.LANGUAGE_UNAVAILABLE
            in SERVICE -> VoiceError.SERVICE_UNAVAILABLE
            else -> VoiceError.FAILED
        }
    }
}
