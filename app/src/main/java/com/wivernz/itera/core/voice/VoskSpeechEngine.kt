package com.wivernz.itera.core.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.wivernz.itera.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Vosk behind [OfflineSpeechEngine] (milestone 013). One model stays loaded app-wide for the active language, so
 * repeated taps and activity recreation reuse it; it is closed when another language is needed,
 * on [trim] (app in background) and never while a session decodes. Audio: 16 kHz mono PCM16 from Itera's own
 * `AudioRecord`, in memory only. The record buffer holds two seconds, so speech during a cold model load is kept.
 */
@Singleton
class VoskSpeechEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger
) : OfflineSpeechEngine {
    private val main = Handler(Looper.getMainLooper())

    init {
        // native diagnostics only (paths, decoder settings); never transcripts, but keep them out of logcat
        runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS) }
    }
    private val lock = Any()
    private var loaded: Pair<String, Model>? = null // guarded by lock
    private var decoding = 0 // guarded by lock
    private var trimWhenIdle = false // guarded by lock

    @Volatile private var session: Session? = null

    override fun start(
        model: OfflineModel,
        grammar: List<String>?,
        listener: VoiceRecognitionListener,
        onModelFailed: (Throwable) -> Unit
    ) {
        cancel()
        val next = Session(model, grammar, listener, onModelFailed)
        session = next
        Thread(next, "itera-offline-voice").apply { isDaemon = true }.start()
    }

    override fun stop() {
        session?.stopRequested = true
    }

    override fun cancel() {
        session?.let {
            it.cancelled = true
            it.stopRequested = true
        }
        session = null
    }

    override fun trim() {
        synchronized(lock) {
            if (decoding > 0) {
                trimWhenIdle = true
            } else {
                closeModel()
            }
        }
    }

    private fun closeModel() {
        loaded?.second?.let { runCatching { it.close() } }
        loaded = null
        trimWhenIdle = false
    }

    /**
     * Loads [model] at its resolved root unless it is the one already loaded. A failure carries the actual error: it
     * is not proof of damage (a native, linkage or memory error looks the same from here), so the store decides.
     */
    private fun acquire(model: OfflineModel): Result<Model> = synchronized(lock) {
        val path = model.path.absolutePath
        val cached = loaded?.takeIf { it.first == path }?.second
        val result = if (cached != null) {
            Result.success(cached)
        } else {
            closeModel()
            runCatching { Model(path) }.onSuccess { loaded = path to it }
        }
        result.onSuccess {
            decoding++
            trimWhenIdle = false
        }
    }

    private fun releaseDecoder() = synchronized(lock) {
        decoding--
        if (decoding == 0 && trimWhenIdle) closeModel()
    }

    private inner class Session(
        private val model: OfflineModel,
        private val grammar: List<String>?,
        private val listener: VoiceRecognitionListener,
        private val onModelFailed: (Throwable) -> Unit
    ) : Runnable {
        @Volatile var stopRequested = false

        @Volatile var cancelled = false

        private fun post(block: () -> Unit) {
            main.post { if (!cancelled && session === this) block() }
        }

        private fun finish(block: () -> Unit) = post {
            session = null
            block()
        }

        override fun run() {
            val mic = openMicrophone() ?: return finish {
                logger.w(TAG, "Microphone could not be opened")
                listener.onError(VoiceError.FAILED)
            }
            val t0 = SystemClock.elapsedRealtime()
            val vosk = acquire(model).getOrElse { error ->
                mic.release()
                return finish {
                    logger.w(TAG, "Offline model could not be loaded")
                    onModelFailed(error)
                    listener.onError(VoiceError.FAILED)
                }
            }
            try {
                decode(mic, vosk, t0)
            } finally {
                runCatching { mic.stop() }
                mic.release()
                releaseDecoder()
            }
        }

        private fun decode(mic: AudioRecord, vosk: Model, t0: Long) {
            val recognizer = if (grammar != null) {
                Recognizer(vosk, SAMPLE_RATE.toFloat(), JSONArray(grammar + UNKNOWN).toString())
            } else {
                Recognizer(vosk, SAMPLE_RATE.toFloat())
            }
            recognizer.use {
                // n-best only for a command grammar (competing interpretations); free-form keeps the one-best
                // result, which is more accurate for dictation (vivo, 2026-09-28)
                if (grammar != null) it.setMaxAlternatives(MAX_ALTERNATIVES)
                val buffer = ByteArray(CHUNK_BYTES)
                var heardSpeech = false
                var lastPartial = ""
                var result: List<String>? = null
                while (!stopRequested) {
                    val elapsed = SystemClock.elapsedRealtime() - t0
                    if (!heardSpeech && elapsed > NO_SPEECH_MS) {
                        return finish { listener.onError(VoiceError.NO_SPEECH) }
                    }
                    if (elapsed > MAX_UTTERANCE_MS) break
                    val read = mic.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    if (it.acceptWaveForm(buffer, read)) {
                        // an endpoint: one utterance is done as soon as it holds words
                        val alternatives = alternativesOf(it.result)
                        if (alternatives.isNotEmpty()) {
                            result = alternatives
                            break
                        }
                    } else {
                        val partial = partialOf(it.partialResult)
                        if (partial.isNotEmpty() && partial != lastPartial) {
                            heardSpeech = true
                            lastPartial = partial
                            post { listener.onPartial(partial) }
                        }
                    }
                }
                if (cancelled) return
                val alternatives = result ?: alternativesOf(it.finalResult)
                finish { listener.onFinal(alternatives) }
            }
        }
    }

    @SuppressLint("MissingPermission") // checked just below
    private fun openMicrophone(): AudioRecord? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(
                    AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ),
                    LOAD_BUFFER_BYTES
                )
            )
        }.getOrNull() ?: return null
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return null
        }
        return runCatching { record.also { it.startRecording() } }.getOrElse {
            record.release()
            null
        }
    }

    companion object {
        private const val TAG = "Voice"
        private const val SAMPLE_RATE = 16_000
        private const val CHUNK_BYTES = 3_200 // 100 ms
        private const val LOAD_BUFFER_BYTES = SAMPLE_RATE * 2 * 2 // 2 s: covers a cold model load
        private const val MAX_ALTERNATIVES = 3
        private const val NO_SPEECH_MS = 8_000L
        private const val MAX_UTTERANCE_MS = 30_000L
        private const val UNKNOWN = "[unk]"

        /** Best first, blanks and the grammar's unknown marker dropped. */
        fun alternativesOf(json: String): List<String> {
            val result = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
            val texts = result.optJSONArray("alternatives")?.let { array ->
                (0 until array.length()).map {
                    array.optJSONObject(it)?.optString("text").orEmpty()
                }
            } ?: listOf(result.optString("text"))
            return texts.map { it.replace(UNKNOWN, " ").trim().replace(Regex("\\s+"), " ") }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        fun partialOf(json: String): String =
            runCatching { JSONObject(json).optString("partial") }.getOrDefault("")
                .replace(UNKNOWN, " ").trim()
    }
}

/**
 * Validation's real model load (milestone 013): opens the Vosk model at the resolved root the engine will later use,
 * then closes it. Any throwable is returned, never thrown; the caller classifies it.
 */
class VoskModelLoader @Inject constructor() : VoiceModelLoader {
    init {
        runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS) }
    }

    override fun check(path: File): Throwable? = runCatching {
        Model(path.absolutePath).close()
    }.exceptionOrNull()
}
