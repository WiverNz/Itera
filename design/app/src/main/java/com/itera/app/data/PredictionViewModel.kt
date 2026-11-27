package com.itera.app.data

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Bounded UX prototype storage, excluded from Android backup by its directory.
 * Small synchronous atomic writes make leaving the app safe after every edit.
 * Production must use the approved repository/Room design, not this adapter.
 */
class PredictionViewModel internal constructor(application: Application, private val file: AtomicFile) : AndroidViewModel(application) {
    constructor(application: Application) : this(application,
        AtomicFile(File(application.noBackupFilesDir, "prediction-prototype.json")))
    var state by mutableStateOf(PredictionState())
        private set
    var saveFailed by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    init { reload() }

    fun reload() {
        try {
            if (file.baseFile.exists()) {
                val j = JSONObject(file.openRead().bufferedReader().use { it.readText() })
                state = PredictionState(
                    j.getInt("page"), j.getString("prediction"), j.getInt("before"),
                    j.getString("action"), j.getString("evidence"), j.getString("outcome"),
                    j.getInt("comparison"), j.getInt("after"), j.getString("lesson"),
                    j.getBoolean("noConclusion"), j.getString("date"), j.getBoolean("closed"),
                )
                require(state.page in 0..6)
                require(state.confidenceBefore in -1..4 && state.confidenceAfter in -1..4)
                require(state.comparison in -1..3)
                if (state.preparedDate.isNotEmpty()) LocalDate.parse(state.preparedDate)
            }
            loadFailed = false
        } catch (_: Exception) {
            // Do not overwrite an unreadable record and never log authored text.
            loadFailed = true
        }
    }

    private fun persist(next: PredictionState): Boolean {
        if (loadFailed) return false
        val j = JSONObject().apply {
            put("page", next.page); put("prediction", next.prediction)
            put("before", next.confidenceBefore); put("action", next.action)
            put("evidence", next.evidence); put("outcome", next.outcome)
            put("comparison", next.comparison); put("after", next.confidenceAfter)
            put("lesson", next.lesson); put("noConclusion", next.noConclusion)
            put("date", next.preparedDate); put("closed", next.closed)
        }
        var stream: java.io.FileOutputStream? = null
        return try {
            stream = file.startWrite()
            stream.write(j.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
            saveFailed = false
            true
        } catch (_: Exception) {
            file.failWrite(stream)
            saveFailed = true
            false
        }
    }

    fun edit(next: PredictionState) {
        if (state.finished || loadFailed) return
        // The pre-action snapshot cannot be rewritten while reviewing evidence.
        if (state.frozen && (next.prediction != state.prediction ||
                next.action != state.action || next.evidence != state.evidence ||
                next.confidenceBefore != state.confidenceBefore)) return
        state = next
        persist(next)
    }

    fun save(): Boolean = persist(state)
    fun next() {
        val next = state.advance(LocalDate.now().toString())
        if (persist(next)) state = next
    }
    fun previous() {
        val previous = state.previous()
        if (persist(previous)) state = previous
    }
    fun closeWithoutOutcome() {
        val next = state.copy(closed = true)
        if (persist(next)) state = next
    }
    /** Called only after an explicit replacement confirmation. */
    fun replace() {
        if (persist(PredictionState())) state = PredictionState()
    }

    /** The profile already confirms both prototype reset actions. */
    fun erase() {
        file.delete()
        state = PredictionState()
        saveFailed = false
        loadFailed = false
    }
}

