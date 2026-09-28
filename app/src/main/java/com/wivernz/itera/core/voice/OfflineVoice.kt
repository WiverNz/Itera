package com.wivernz.itera.core.voice

import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.File
import kotlinx.coroutines.flow.StateFlow

/** An installed, verified offline model for one language (milestone 013, ADR-0022). */
data class OfflineModel(val language: VoiceLanguage, val path: File, val version: String)

/** What the recogniser needs from the model store. */
interface OfflineModels {
    /** The verified model for [language], or null: then voice continues down the provider chain. */
    fun installed(language: VoiceLanguage): OfflineModel?

    /** The model failed to load: mark it damaged and stop offering it until it is installed again. */
    fun reportUnusable(language: VoiceLanguage)
}

/** Where a model came from; shown in Settings. */
enum class VoiceModelSource { PLAY, IMPORTED }

/** One language's offline model as Settings shows it. */
sealed interface VoiceModelState {
    data object NotInstalled : VoiceModelState

    data class Downloading(val percent: Int) : VoiceModelState

    /** Play waits for Wi-Fi or the user's confirmation for a large mobile-data download. */
    data object WaitingForWifi : VoiceModelState

    data class Installed(val version: String, val bytes: Long, val source: VoiceModelSource) :
        VoiceModelState

    /** Failed to verify or load; removed so it can be installed again. */
    data object Damaged : VoiceModelState

    /** A download failed; nothing was installed. */
    data object DownloadFailed : VoiceModelState
}

enum class VoiceModelImport { INSTALLED, NOT_A_MODEL, FAILED }

/** The official archive for a language: what sideload users download and import, and its size. */
data class VoiceModelInfo(val file: String, val downloadBytes: Long)

/**
 * The per-language offline model store (milestone 013): installed state, path/version/checksum, Play download,
 * document-picker import and removal. Models never ship in the base APK.
 */
interface VoiceModelStore : OfflineModels {
    val states: StateFlow<Map<VoiceLanguage, VoiceModelState>>

    /** True when Play Asset Delivery can serve packs (the app came from Play, or local testing). */
    val downloadAvailable: StateFlow<Boolean>

    fun info(language: VoiceLanguage): VoiceModelInfo

    fun download(language: VoiceLanguage)

    /** A user-picked archive (content URI string); installed for whichever language its checksum identifies. */
    suspend fun import(uri: String): VoiceModelImport

    fun remove(language: VoiceLanguage)
}

/**
 * Itera's own recogniser: it opens the microphone and decodes on-device in memory. Nothing is written to disk and
 * no audio or transcript is logged. Callbacks arrive on the main thread; a cancelled session stays silent.
 */
interface OfflineSpeechEngine {
    /** [grammar] restricts recognition to those phrases (command mode); null is free-form dictation. */
    fun start(
        model: OfflineModel,
        grammar: List<String>?,
        listener: VoiceRecognitionListener,
        onModelFailed: () -> Unit
    )

    fun stop()

    fun cancel()

    /** Free the loaded model once idle (the app went to the background). */
    fun trim()
}
