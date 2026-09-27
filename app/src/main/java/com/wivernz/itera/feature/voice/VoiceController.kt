package com.wivernz.itera.feature.voice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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

enum class VoiceUnavailable { DEVICE, LANGUAGE }

enum class VoiceFailure { NO_SPEECH, BUSY, FAILED }

/** What one field mic or the command sheet shows. Nothing here is saved, logged or restored. */
sealed interface VoiceSessionState {
    val owner: Any?

    data object Idle : VoiceSessionState {
        override val owner: Any? get() = null
    }

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
        when {
            recognizer.availability() != VoiceAvailability.AVAILABLE ->
                state = VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE)
            !permission.granted() -> state = VoiceSessionState.NeedsPermission(owner)
            else -> listen(owner)
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
                        VoiceError.SERVICE_UNAVAILABLE ->
                            VoiceSessionState.Unavailable(owner, VoiceUnavailable.DEVICE)
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
