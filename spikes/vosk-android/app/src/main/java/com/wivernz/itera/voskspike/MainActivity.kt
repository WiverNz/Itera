package com.wivernz.itera.voskspike

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.wivernz.itera.domain.voice.VoiceCommandParser
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.File
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Spike only. Itera captures the microphone itself (AudioRecord, 16 kHz mono PCM16) and feeds it to an offline
 * Vosk model in memory; nothing is written to disk. Transcripts appear on screen only; the log carries numbers.
 * Model: side-loaded into internal files/model with adb + run-as (see README).
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var dictate: Button
    private lateinit var command: Button
    private lateinit var stop: Button

    private var model: Model? = null
    private var worker: Thread? = null
    @Volatile private var recording = false
    private val language = VoiceLanguage.RU

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 15f }
        output = TextView(this).apply { textSize = 18f; setTextIsSelectable(true) }
        dictate = Button(this).apply { text = "Dictate"; setOnClickListener { record(commandMode = false) } }
        command = Button(this).apply { text = "Command"; setOnClickListener { record(commandMode = true) } }
        stop = Button(this).apply { text = "Stop"; isEnabled = false; setOnClickListener { recording = false } }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 120, 40, 40)
            listOf(status, dictate, command, stop, output).forEach(::addView)
        }
        // target SDK 35 draws edge to edge: keep the controls below the status bar
        column.setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            view.setPadding(40, insets.systemWindowInsetTop + 40, 40, insets.systemWindowInsetBottom + 40)
            insets
        }
        setContentView(ScrollView(this).apply { addView(column) })
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        loadModel()
    }

    private fun loadModel() {
        // debug sideload: /data/local/tmp -> run-as copy into internal files/model (see README)
        val dir = listOf(File(filesDir, "model"), File(getExternalFilesDir(null), "model"))
            .firstOrNull { File(it, "am").isDirectory } ?: File(filesDir, "model")
        if (!File(dir, "conf").isDirectory && !File(dir, "am").isDirectory) {
            status.text = "No model at ${dir.path}"
            dictate.isEnabled = false
            command.isEnabled = false
            return
        }
        status.text = "Loading model…"
        dictate.isEnabled = false
        command.isEnabled = false
        Thread {
            val rssBefore = rssKb()
            val t0 = SystemClock.elapsedRealtime()
            val loaded = runCatching { Model(dir.path) }
            val loadMs = SystemClock.elapsedRealtime() - t0
            runOnUiThread {
                loaded.onSuccess {
                    model = it
                    val line = "model ${sizeMb(dir)} MB · load ${loadMs} ms · RSS ${rssBefore / 1024}→${rssKb() / 1024} MB"
                    Log.i(TAG, "METRIC $line")
                    status.text = line
                    dictate.isEnabled = true
                    command.isEnabled = true
                }.onFailure { status.text = "Model failed: ${it.javaClass.simpleName}" }
            }
        }.start()
    }

    @SuppressLint("MissingPermission") // requested in onCreate; the tap fails harmlessly without it
    private fun record(commandMode: Boolean) {
        val loaded = model ?: return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        dictate.isEnabled = false
        command.isEnabled = false
        stop.isEnabled = true
        output.text = "Listening…"
        recording = true
        worker = Thread {
            val t0 = SystemClock.elapsedRealtime()
            val recognizer = Recognizer(loaded, SAMPLE_RATE.toFloat())
            val createMs = SystemClock.elapsedRealtime() - t0
            val minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val mic = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, CHUNK * 4)
            )
            val buffer = ByteArray(CHUNK)
            var audioBytes = 0L
            var decodeNs = 0L
            mic.startRecording()
            while (recording && audioBytes < MAX_BYTES) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                audioBytes += n
                val d0 = System.nanoTime()
                val endpoint = recognizer.acceptWaveForm(buffer, n)
                decodeNs += System.nanoTime() - d0
                if (!endpoint) {
                    val partial = JSONObject(recognizer.partialResult).optString("partial")
                    runOnUiThread { output.text = "… $partial" }
                }
            }
            mic.stop()
            mic.release()
            val s0 = SystemClock.elapsedRealtime()
            val text = JSONObject(recognizer.finalResult).optString("text")
            val finalMs = SystemClock.elapsedRealtime() - s0
            recognizer.close()
            val audioSec = audioBytes / 2.0 / SAMPLE_RATE
            val rtf = decodeNs / 1e9 / audioSec
            val metrics = "recognizer ${createMs} ms · audio %.1f s · RTF %.2f · final after stop ${finalMs} ms · peak RSS ${peakKb() / 1024} MB · native heap ${Debug.getNativeHeapAllocatedSize() / 1048576} MB"
                .format(audioSec, rtf)
            Log.i(TAG, "METRIC $metrics") // numbers only; never the transcript
            val parsed = if (commandMode) "\nparser: ${VoiceCommandParser.parse(text, language)}" else ""
            runOnUiThread {
                output.text = "text: «$text»$parsed\n\n$metrics"
                dictate.isEnabled = true
                command.isEnabled = true
                stop.isEnabled = false
            }
        }.also { it.start() }
    }

    override fun onStop() {
        super.onStop()
        recording = false
    }

    override fun onDestroy() {
        recording = false
        worker?.join(1000)
        model?.close()
        super.onDestroy()
    }

    private fun sizeMb(dir: File) = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1_048_576

    private fun statusKb(key: String): Long = File("/proc/self/status").readLines()
        .firstOrNull { it.startsWith(key) }?.filter { it.isDigit() }?.toLongOrNull() ?: 0

    private fun rssKb() = statusKb("VmRSS")

    private fun peakKb() = statusKb("VmHWM")

    private companion object {
        const val TAG = "VoskSpike"
        const val SAMPLE_RATE = 16_000
        const val CHUNK = 3_200 // 100 ms
        const val MAX_BYTES = SAMPLE_RATE * 2L * 30 // 30 s cap
    }
}
