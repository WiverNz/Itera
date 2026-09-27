package com.wivernz.itera.core.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.wivernz.itera.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether the strictly on-device recogniser exists on this device (ADR-0022). */
enum class VoiceAvailability { AVAILABLE, UNAVAILABLE }

/** Sanitised failure categories; recogniser bundles never leave the adapter. */
enum class VoiceError {
    NO_SPEECH,
    BUSY,
    PERMISSION,
    LANGUAGE_UNAVAILABLE,
    SERVICE_UNAVAILABLE,
    FAILED
}

interface VoiceRecognitionListener {
    fun onPartial(text: String)

    /** Best alternative first; empty when the service returned nothing. */
    fun onFinal(alternatives: List<String>)

    fun onError(error: VoiceError)
}

/**
 * One user-initiated utterance at a time, main thread only. Never retries, never switches
 * recogniser, never falls back to the default platform service. Carries no business logic and
 * cannot reach storage.
 */
interface VoiceRecognizer {
    fun availability(): VoiceAvailability

    fun start(languageTag: String, listener: VoiceRecognitionListener)

    /** Ask for the final result of the current utterance. */
    fun stop()

    /** Discard the current utterance; no callback follows. */
    fun cancel()

    fun release()
}

class AndroidVoiceRecognizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger
) : VoiceRecognizer {
    private var recognizer: SpeechRecognizer? = null

    override fun availability(): VoiceAvailability {
        // API 26-30 and devices without an on-device service: no platform fallback (ADR-0022).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return VoiceAvailability.UNAVAILABLE
        val onDevice = runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
        return if (onDevice.getOrDefault(false)) {
            VoiceAvailability.AVAILABLE
        } else {
            VoiceAvailability.UNAVAILABLE
        }
    }

    override fun start(languageTag: String, listener: VoiceRecognitionListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            availability() != VoiceAvailability.AVAILABLE
        ) {
            listener.onError(VoiceError.SERVICE_UNAVAILABLE)
            return
        }
        release()
        val created = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }
            .getOrNull()
        if (created == null) {
            logger.w(TAG, "On-device recogniser could not be created")
            listener.onError(VoiceError.SERVICE_UNAVAILABLE)
            return
        }
        recognizer = created
        created.setRecognitionListener(Bridge(listener))
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        runCatching { created.startListening(intent) }.onFailure {
            // category only: platform exceptions may carry speech
            logger.w(TAG, "startListening failed")
            listener.onError(VoiceError.FAILED)
        }
    }

    override fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    override fun cancel() {
        runCatching { recognizer?.cancel() }
    }

    override fun release() {
        recognizer?.let { r ->
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
        recognizer = null
    }

    private class Bridge(private val listener: VoiceRecognitionListener) : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let(listener::onPartial)
        }

        override fun onResults(results: Bundle?) {
            val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            listener.onFinal(alternatives.orEmpty())
        }

        override fun onError(error: Int) = listener.onError(errorOf(error))

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
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

        private val NO_SPEECH = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        )
        private val BUSY = setOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS
        )
        private val LANGUAGE = setOf(
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        )
        private val SERVICE = setOf(
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT
        )

        /** Maps a platform error code to a sanitised category. */
        fun errorOf(code: Int): VoiceError = when (code) {
            in NO_SPEECH -> VoiceError.NO_SPEECH
            in BUSY -> VoiceError.BUSY
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PERMISSION
            in LANGUAGE -> VoiceError.LANGUAGE_UNAVAILABLE
            in SERVICE -> VoiceError.SERVICE_UNAVAILABLE
            else -> VoiceError.FAILED
        }
    }
}
