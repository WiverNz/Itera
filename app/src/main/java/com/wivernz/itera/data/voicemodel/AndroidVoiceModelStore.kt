package com.wivernz.itera.data.voicemodel

import android.content.Context
import android.net.Uri
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.core.voice.OfflineModel
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelSource
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.core.voice.VoiceModelStore
import com.wivernz.itera.domain.voice.VoiceLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Offline voice models (milestone 013, ADR-0022). Two sources, one per language at a time:
 * - **Imported** through the document picker: extracted into `noBackupFilesDir/voice-models/<lang>/model` with a
 *   manifest (version, SHA-256, size). Only the pinned official archives are accepted; the checksum identifies the
 *   language.
 * - **Play**: the on-demand `voice_model_<lang>` pack, extracted by the Play Store.
 *
 * A model counts as installed only if its required files are present. One that fails to load is removed and shown
 * as damaged. Nothing here reads or writes audio or transcripts.
 */
@Singleton
class AndroidVoiceModelStore internal constructor(
    private val root: File,
    private val packs: ModelPacks,
    private val open: (String) -> InputStream?,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    // the pinned archives; tests pass small synthetic ones
    private val archives: List<VoiceModelArchive> = VoiceModelCatalog.archives
) : VoiceModelStore {
    @Inject constructor(
        @ApplicationContext context: Context,
        packs: ModelPacks,
        @IoDispatcher io: CoroutineDispatcher,
        logger: Logger
    ) : this(
        File(context.noBackupFilesDir, "voice-models"),
        packs,
        { uri -> context.contentResolver.openInputStream(Uri.parse(uri)) },
        io,
        logger
    )

    private fun archiveOf(language: VoiceLanguage) = archives.first { it.language == language }

    private val damaged = mutableSetOf<VoiceLanguage>()
    private val packStatus = mutableMapOf<VoiceLanguage, PackState>()
    private val _states = MutableStateFlow(VoiceLanguage.entries.associateWith { stateOf(it) })
    override val states: StateFlow<Map<VoiceLanguage, VoiceModelState>> = _states.asStateFlow()
    private val _downloadAvailable = MutableStateFlow(false)
    override val downloadAvailable: StateFlow<Boolean> = _downloadAvailable.asStateFlow()

    init {
        packs.listen(::onPack)
        packs.refresh(archives.map { it.pack }) { available, current ->
            _downloadAvailable.value = available
            current.forEach(::onPack)
        }
    }

    private fun onPack(state: PackState) {
        val language = archives.firstOrNull { it.pack == state.pack }?.language ?: return
        synchronized(this) {
            packStatus[language] = state
            if (state.status == PackStatus.COMPLETED) damaged -= language
        }
        publish(language)
    }

    private fun publish(language: VoiceLanguage) = _states.update {
        it +
            (language to stateOf(language))
    }

    private fun stateOf(language: VoiceLanguage): VoiceModelState = synchronized(this) {
        val archive = archiveOf(language)
        imported(language)?.let {
            return VoiceModelState.Installed(archive.name, it.second, VoiceModelSource.IMPORTED)
        }
        val pack = packStatus[language]
        when {
            playModel(language) != null ->
                VoiceModelState.Installed(archive.name, archive.zipBytes, VoiceModelSource.PLAY)
            pack?.status == PackStatus.DOWNLOADING -> VoiceModelState.Downloading(pack.percent)
            pack?.status == PackStatus.WAITING_FOR_WIFI -> VoiceModelState.WaitingForWifi
            language in damaged -> VoiceModelState.Damaged
            pack?.status == PackStatus.FAILED -> VoiceModelState.DownloadFailed
            else -> VoiceModelState.NotInstalled
        }
    }

    private fun dir(language: VoiceLanguage) = File(root, language.name.lowercase())

    private fun complete(model: File) = VoiceModelCatalog.requiredFiles.all {
        File(model, it).isFile
    }

    /** The imported model folder and its size, when intact and pinned. */
    private fun imported(language: VoiceLanguage): Pair<File, Long>? {
        val manifest = File(dir(language), MANIFEST).takeIf { it.isFile } ?: return null
        val props = Properties().apply { manifest.reader().use(::load) }
        val model = File(dir(language), MODEL)
        val pinned = props.getProperty("sha256") == archiveOf(language).sha256
        return if (pinned &&
            complete(model)
        ) {
            model to (props.getProperty("bytes")?.toLongOrNull() ?: 0)
        } else {
            null
        }
    }

    // asset packs share one namespace: each pack keeps its model under assets/<pack name>/
    private fun playModel(language: VoiceLanguage): File? = archiveOf(language).pack.let { pack ->
        packs.assets(pack)?.let { File(it, pack) }?.takeIf(::complete)
    }

    override fun installed(language: VoiceLanguage): OfflineModel? {
        val archive = archiveOf(language)
        val path = imported(language)?.first ?: playModel(language) ?: return null
        return OfflineModel(language, path, archive.name)
    }

    override fun reportUnusable(language: VoiceLanguage) {
        logger.w(TAG, "Offline model marked damaged")
        dir(language).deleteRecursively()
        packs.remove(archiveOf(language).pack)
        synchronized(this) {
            packStatus.remove(language)
            damaged += language
        }
        publish(language)
    }

    override fun info(language: VoiceLanguage): VoiceModelInfo =
        archiveOf(language).let { VoiceModelInfo("${it.name}.zip", it.zipBytes) }

    override fun download(language: VoiceLanguage) {
        synchronized(this) { damaged -= language }
        packs.fetch(archiveOf(language).pack)
    }

    override fun remove(language: VoiceLanguage) {
        dir(language).deleteRecursively()
        packs.remove(archiveOf(language).pack)
        synchronized(this) {
            packStatus.remove(language)
            damaged -= language
        }
        publish(language)
    }

    override suspend fun import(uri: String): VoiceModelImport = withContext(io) {
        val staging = File(root, ".import-${System.nanoTime()}")
        try {
            val input =
                runCatching { open(uri) }.getOrNull() ?: return@withContext VoiceModelImport.FAILED
            val digest = MessageDigest.getInstance("SHA-256")
            val extracted = DigestInputStream(input.buffered(), digest).use { stream ->
                val bytes = extract(ZipInputStream(stream), File(staging, MODEL))
                // the central directory after the last entry must be hashed too
                val buffer = ByteArray(BUFFER)
                while (stream.read(buffer) >= 0) Unit
                bytes
            } ?: return@withContext VoiceModelImport.NOT_A_MODEL
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            val archive = archives.firstOrNull { it.sha256 == sha }
            if (archive == null ||
                !complete(File(staging, MODEL))
            ) {
                return@withContext VoiceModelImport.NOT_A_MODEL
            }
            File(staging, MANIFEST).writer().use { out ->
                Properties().apply {
                    setProperty("version", archive.name)
                    setProperty("sha256", sha)
                    setProperty("bytes", extracted.toString())
                }.store(out, null)
            }
            val target = dir(archive.language)
            target.deleteRecursively()
            if (!staging.renameTo(target)) return@withContext VoiceModelImport.FAILED
            synchronized(this@AndroidVoiceModelStore) { damaged -= archive.language }
            publish(archive.language)
            VoiceModelImport.INSTALLED
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.w(TAG, "Model import failed: ${e.javaClass.simpleName}")
            VoiceModelImport.FAILED
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Unpacks [zip] under [into], dropping the archive's top folder. Null for anything unsafe or oversized. */
    private fun extract(zip: ZipInputStream, into: File): Long? {
        var total = 0L
        var entries = 0
        val base = into.canonicalFile
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > MAX_ENTRIES) return null
            val relative = entry.name.replace('\\', '/').substringAfter('/', "")
            if (relative.isEmpty() || entry.isDirectory) continue
            val target = File(base, relative).canonicalFile
            if (!target.path.startsWith(base.path + File.separator)) return null
            target.parentFile?.mkdirs()
            target.outputStream().use { out ->
                val buffer = ByteArray(BUFFER)
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) return null
                    out.write(buffer, 0, n)
                }
            }
        }
        return total.takeIf { entries > 0 }
    }

    private companion object {
        const val TAG = "VoiceModels"
        const val MODEL = "model"
        const val MANIFEST = "manifest.properties"
        const val BUFFER = 1 shl 16
        const val MAX_ENTRIES = 200
        const val MAX_BYTES = 200L * 1024 * 1024
    }
}
