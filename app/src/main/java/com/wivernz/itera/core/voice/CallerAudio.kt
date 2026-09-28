package com.wivernz.itera.core.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import java.io.IOException

/**
 * Microphone audio Itera captures itself and hands to an external recogniser through
 * `RecognizerIntent.EXTRA_AUDIO_SOURCE` (API 33+, ADR-0022). Needed where the provider's own capture is silenced
 * because it runs in the background. PCM stays in memory: nothing is written to a file, kept or logged.
 */
interface CallerAudioStream {
    /** The read end of the pipe, passed to the recogniser. */
    val descriptor: ParcelFileDescriptor

    /** Itera's own capture session, to tell it apart from any capture the provider starts. */
    val sessionId: Int

    /** Stop capturing and close the write end: the recogniser sees end of audio. */
    fun endOfAudio()

    /** Stop and release everything. Idempotent. */
    fun close()

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Hard cap on one utterance's capture; the recogniser then sees end of audio. */
        const val MAX_CAPTURE_MS = 60_000L
    }
}

/**
 * 16 kHz mono PCM16 from `VOICE_RECOGNITION`, pumped into a pipe on a background thread. The thread ends at the
 * cap, on [endOfAudio]/[close], or when the recogniser closes its end; it then closes the write end.
 */
class PipedMicrophone private constructor(
    private val record: AudioRecord,
    override val descriptor: ParcelFileDescriptor,
    private val sink: ParcelFileDescriptor
) : CallerAudioStream {
    @Volatile private var running = true
    private var closed = false

    override val sessionId: Int get() = record.audioSessionId

    private val pump = Thread({ pumpAudio() }, "itera-voice-audio").apply { isDaemon = true }

    private fun start() {
        record.startRecording()
        pump.start()
    }

    private fun pumpAudio() {
        val buffer = ByteArray(BUFFER_BYTES)
        val deadline = System.nanoTime() + CallerAudioStream.MAX_CAPTURE_MS * NANOS_PER_MILLI
        ParcelFileDescriptor.AutoCloseOutputStream(sink).use { out ->
            try {
                while (running && System.nanoTime() < deadline) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read > 0) out.write(buffer, 0, read)
                }
            } catch (_: IOException) {
                // the recogniser closed its end: nothing more to send
            }
        }
        runCatching { record.stop() }
    }

    override fun endOfAudio() {
        running = false
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        running = false
        runCatching { record.stop() }
        runCatching { pump.join(JOIN_MS) }
        runCatching { record.release() }
        runCatching { descriptor.close() }
        runCatching { sink.close() }
    }

    companion object {
        private const val BUFFER_BYTES = 3_200 // 100 ms of 16 kHz mono PCM16
        private const val JOIN_MS = 500L
        private const val NANOS_PER_MILLI = 1_000_000L

        /** Null without RECORD_AUDIO or when the microphone cannot be opened. */
        @SuppressLint("MissingPermission") // checked just below
        fun open(context: Context): CallerAudioStream? {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }
            val minimum = AudioRecord.getMinBufferSize(
                CallerAudioStream.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                CallerAudioStream.ENCODING
            )
            if (minimum <= 0) return null
            val record = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    CallerAudioStream.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    CallerAudioStream.ENCODING,
                    maxOf(minimum, BUFFER_BYTES * 4)
                )
            }.getOrNull() ?: return null
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return null
            }
            val pipe = runCatching { ParcelFileDescriptor.createPipe() }.getOrNull()
            if (pipe == null) {
                record.release()
                return null
            }
            return runCatching { PipedMicrophone(record, pipe[0], pipe[1]).also { it.start() } }
                .getOrElse {
                    record.release()
                    pipe.forEach { runCatching { it.close() } }
                    null
                }
        }
    }
}
