package com.wivernz.itera.feature.voice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.wivernz.itera.core.voice.RecognitionProviderInfo
import com.wivernz.itera.core.voice.VoiceAvailability
import com.wivernz.itera.core.voice.VoiceError
import com.wivernz.itera.core.voice.VoiceRecognitionListener
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.domain.voice.VoiceLanguage

/** Microphone permission as the controller sees it; the Android side lives in the composition. */
interface VoicePermissionGate {
    fun granted(): Boolean

    /** Shows the system dialog; [onResult] gets whether it was granted. */
    fun request(onResult: (Boolean) -> Unit)

    /** Denied and Android will not ask again: only system settings can change it. */
    fun permanentlyDenied(): Boolean
}

/** [LANGUAGE]: the model is missing (speech settings may add it). [LANGUAGE_UNSUPPORTED]: the provider has none. */
enum class VoiceUnavailable { DEVICE, LANGUAGE, LANGUAGE_UNSUPPORTED }

enum class VoiceFailure { NO_SPEECH, BUSY, FAILED }

/** What one field mic or the command sheet shows. Nothing here is saved, logged or restored. */
sealed interface VoiceSessionState {
    val owner: Any?

    data object Idle : VoiceSessionState {
        override val owner: Any? get() = null
    }

    /**
     * No on-device recogniser: explain the system recogniser, "Use system recognition" or Not now. Shown until the
     * user accepts; accepting is stored and never asked again.
     */
    data class NeedsSystemConsent(override val owner: Any) : VoiceSessionState

    /**
     * No on-device or usable system recogniser: installed apps that offer one, for the user to pick from. Nothing
     * is pre-selected. [lost]: a previously chosen app disappeared or failed and was forgotten.
     */
    data class ChooseProvider(
        override val owner: Any,
        val providers: List<RecognitionProviderInfo>,
        val lost: Boolean = false
    ) : VoiceSessionState

    /** The consent naming the picked app: "Use <app>" or Not now. Accepting stores the choice. */
    data class NeedsProviderConsent(
        override val owner: Any,
        val provider: RecognitionProviderInfo
    ) : VoiceSessionState

    /** Before the system dialog: the brief rationale, Continue or Not now. */
    data class NeedsPermission(override val owner: Any) : VoiceSessionState

    data class PermissionDenied(override val owner: Any, val permanent: Boolean) : VoiceSessionState

    data class Unavailable(override val owner: Any, val reason: VoiceUnavailable) :
        VoiceSessionState

    /** [partial] is provisional preview text only; [stopping] waits for the final result. */
    data class Listening(
        override val owner: Any,
        val partial: String? = null,
        val stopping: Boolean = false
    ) : VoiceSessionState

    data class Failed(override val owner: Any, val failure: VoiceFailure) : VoiceSessionState
}

/**
 * The single active recognition session (docs/architecture/07-voice-input.md). A session belongs to one owner (a
 * field or the command sheet), one language and one token; a final result is delivered at most once and only
 * while the session is current. Cancelling, a new session, a language change or release invalidate every
 * callback still in flight. Retry is always an explicit user action.
 */
class VoiceController(
    private val recognizer: VoiceRecognizer,
    private val permission: VoicePermissionGate,
    language: VoiceLanguage
) {
    var state: VoiceSessionState by mutableStateOf(VoiceSessionState.Idle)
        private set

    var language: VoiceLanguage = language
        private set

    private var token = 0L
    private var onFinal: ((List<String>) -> Unit)? = null

    /** A mic or "Speak" tap. Ends any other session first. */
    fun start(owner: Any, onFinal: (List<String>) -> Unit) {
        cancel()
        this.onFinal = onFinal
        when (recognizer.availability()) {
            VoiceAvailability.UNAVAILABLE ->
                state = VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE)
            VoiceAvailability.CONSENT_REQUIRED ->
                state =
                    VoiceSessionState.NeedsSystemConsent(owner)
            VoiceAvailability.CHOICE_REQUIRED -> chooseAgain(owner)
            VoiceAvailability.AVAILABLE -> microphoneThenListen(owner)
        }
    }

    /** "Use system recognition": stored once, then the usual microphone step. */
    fun acceptSystemRecognition() {
        val current = state as? VoiceSessionState.NeedsSystemConsent ?: return
        recognizer.allowSystemRecognition()
        microphoneThenListen(current.owner)
    }

    /** Installed recognition apps, for Settings. */
    fun providers(): List<RecognitionProviderInfo> = recognizer.providers()

    /** A row in the picker: show the consent for that app. Nothing is stored yet. */
    fun pickProvider(provider: RecognitionProviderInfo) {
        val current = state as? VoiceSessionState.ChooseProvider ?: return
        if (provider !in current.providers) return
        state = VoiceSessionState.NeedsProviderConsent(current.owner, provider)
    }

    /** "Use <app>": the choice and its consent are stored, then the usual microphone step. */
    fun acceptProvider() {
        val current = state as? VoiceSessionState.NeedsProviderConsent ?: return
        recognizer.selectProvider(current.provider.id)
        microphoneThenListen(current.owner)
    }

    private fun chooseAgain(owner: Any) {
        val lost = recognizer.consumeProviderLost()
        val providers = recognizer.providers()
        state = if (providers.isEmpty()) {
            VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE)
        } else {
            VoiceSessionState.ChooseProvider(owner, providers, lost)
        }
    }

    private fun microphoneThenListen(owner: Any) {
        if (permission.granted()) {
            listen(owner)
        } else {
            state =
                VoiceSessionState.NeedsPermission(owner)
        }
    }

    /** "Continue" on the rationale: the one system prompt. */
    fun requestPermission() {
        val current = state as? VoiceSessionState.NeedsPermission ?: return
        val expected = token
        permission.request { granted ->
            if (token != expected || state != current) return@request
            if (granted) {
                listen(current.owner)
            } else {
                state =
                    VoiceSessionState.PermissionDenied(
                        current.owner,
                        permission.permanentlyDenied()
                    )
            }
        }
    }

    /** "Try again" after a failure or a denial the system may still show. */
    fun retry() {
        val owner = state.owner ?: return
        val callback = onFinal ?: return
        start(owner, callback)
    }

    fun stop() {
        val current = state as? VoiceSessionState.Listening ?: return
        if (current.stopping) return
        state = current.copy(stopping = true)
        recognizer.stop()
    }

    /** Cancel, Keep typing, dismiss, navigation, background. Nothing pending survives. */
    fun cancel() {
        token++
        onFinal = null
        if (state is VoiceSessionState.Listening) recognizer.cancel()
        state = VoiceSessionState.Idle
    }

    /** Cancels only when [owner] holds the session, e.g. a field leaving the composition. */
    fun cancelIfOwner(owner: Any) {
        if (state.owner === owner) cancel()
    }

    fun isActive(owner: Any): Boolean = state.owner === owner

    /** The app language changed: the old session ends; the next tap uses [language]. */
    fun onLanguage(language: VoiceLanguage) {
        if (language == this.language) return
        cancel()
        this.language = language
    }

    fun release() {
        cancel()
        recognizer.release()
    }

    private fun listen(owner: Any) {
        val session = ++token
        state = VoiceSessionState.Listening(owner)
        recognizer.start(
            language.tag,
            object : VoiceRecognitionListener {
                private fun current(): VoiceSessionState.Listening? =
                    (state as? VoiceSessionState.Listening)?.takeIf {
                        token == session &&
                            it.owner === owner
                    }

                override fun onPartial(text: String) {
                    val listening = current() ?: return
                    state = listening.copy(partial = text)
                }

                override fun onFinal(alternatives: List<String>) {
                    current() ?: return
                    val deliver = this@VoiceController.onFinal
                    token++
                    this@VoiceController.onFinal = null
                    if (alternatives.none { it.isNotBlank() }) {
                        this@VoiceController.onFinal = deliver
                        state = VoiceSessionState.Failed(owner, VoiceFailure.NO_SPEECH)
                        return
                    }
                    state = VoiceSessionState.Idle
                    deliver?.invoke(alternatives)
                }

                override fun onError(error: VoiceError) {
                    current() ?: return
                    token++
                    state = when (error) {
                        VoiceError.PERMISSION ->
                            VoiceSessionState.PermissionDenied(
                                owner,
                                permission.permanentlyDenied()
                            )
                        VoiceError.LANGUAGE_UNAVAILABLE ->
                            VoiceSessionState.Unavailable(owner, VoiceUnavailable.LANGUAGE)
                        VoiceError.LANGUAGE_UNSUPPORTED ->
                            VoiceSessionState.Unavailable(
                                owner,
                                VoiceUnavailable.LANGUAGE_UNSUPPORTED
                            )
                        VoiceError.SERVICE_UNAVAILABLE ->
                            VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE)
                        VoiceError.PROVIDER_UNUSABLE -> {
                            chooseAgain(owner)
                            return
                        }
                        VoiceError.NO_SPEECH -> VoiceSessionState.Failed(
                            owner,
                            VoiceFailure.NO_SPEECH
                        )
                        VoiceError.BUSY -> VoiceSessionState.Failed(owner, VoiceFailure.BUSY)
                        VoiceError.FAILED -> VoiceSessionState.Failed(owner, VoiceFailure.FAILED)
                    }
                }
            }
        )
    }
}

/** Null where voice is not wired (previews, tests without it): every field stays a plain text field. */
val LocalVoiceController = staticCompositionLocalOf<VoiceController?> { null }
