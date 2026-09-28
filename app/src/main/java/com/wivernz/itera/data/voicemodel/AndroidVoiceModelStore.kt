package com.wivernz.itera.data.voicemodel

import android.content.Context
import android.net.Uri
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.core.voice.OfflineModel
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelLoader
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Offline voice models (milestone 013, ADR-0022). Two sources, one per language at a time:
 * - **Imported** through the document picker: extracted into `noBackupFilesDir/voice-models/<lang>/model` with a
 *   manifest (version, SHA-256, size, fingerprint, validated). Only the pinned official archives are accepted; the
 *   checksum identifies the language.
 * - **Play**: the on-demand `voice_model_<lang>` pack, extracted by the Play Store; its validation record lives in
 *   `voice-models/<lang>/play.properties`.
 *
 * Lifecycle: installed files → **Validating** (structure, pinned checksum, a real Vosk load of the resolved root) →
 * **Installed**, or **Damaged** when there is evidence the files are incomplete or changed, or **Unusable** when the
 * files look intact but the model would not load (kept; "Check again" revalidates). Before each use only the cheap
 * checks run (required files, recorded fingerprint). Nothing is deleted except by Remove or a reinstall. No audio or
 * transcript passes through here.
 */
@Singleton
class AndroidVoiceModelStore internal constructor(
    private val root: File,
    private val packs: ModelPacks,
    private val open: (String) -> InputStream?,
    private val io: CoroutineDispatcher,
    private val logger: Logger,
    private val loader: VoiceModelLoader,
    // the pinned archives; tests pass small synthetic ones
    private val archives: List<VoiceModelArchive> = VoiceModelCatalog.archives
) : VoiceModelStore {
    @Inject constructor(
        @ApplicationContext context: Context,
        packs: ModelPacks,
        @IoDispatcher io: CoroutineDispatcher,
        logger: Logger,
        loader: VoiceModelLoader
    ) : this(
        File(context.noBackupFilesDir, "voice-models"),
        packs,
        { uri -> context.contentResolver.openInputStream(Uri.parse(uri)) },
        io,
        logger,
        loader
    )

    /** What the local files say, without loading the model. */
    private sealed interface Local {
        data object None : Local

        data class Damaged(val reason: String) : Local

        data class NeedsValidation(val root: File, val source: VoiceModelSource) : Local

        data class Ready(val root: File, val source: VoiceModelSource, val bytes: Long) : Local
    }

    private val scope = CoroutineScope(SupervisorJob() + io)
    private val validating = mutableSetOf<VoiceLanguage>() // guarded by this
    private val unusable = mutableSetOf<VoiceLanguage>() // guarded by this
    private val packStatus = mutableMapOf<VoiceLanguage, PackState>() // guarded by this
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
        // installations never validated (for example imported by an earlier build) are validated once now
        VoiceLanguage.entries.forEach(::refresh)
    }

    private fun archiveOf(language: VoiceLanguage) = archives.first { it.language == language }

    private fun dir(language: VoiceLanguage) = File(root, language.name.lowercase())

    private fun manifest(language: VoiceLanguage) = File(dir(language), MANIFEST)

    private fun playRecord(language: VoiceLanguage) = File(dir(language), PLAY_RECORD)

    private fun playDir(language: VoiceLanguage): File? =
        archiveOf(language).pack.let { pack -> packs.assets(pack)?.let { File(it, pack) } }

    private fun props(file: File) = Properties().apply { file.reader().use(::load) }

    private fun onPack(state: PackState) {
        val language = archives.firstOrNull { it.pack == state.pack }?.language ?: return
        synchronized(this) { packStatus[language] = state }
        if (state.status == PackStatus.COMPLETED) {
            logger.i(TAG, "Play pack delivered for $language; validating")
            launchValidation(language)
        } else {
            publish(language)
        }
    }

    private fun publish(language: VoiceLanguage) = _states.update {
        it +
            (language to stateOf(language))
    }

    /** Publishes, and starts validation for an installation that has never been validated. */
    private fun refresh(language: VoiceLanguage) {
        val needs = local(language) is Local.NeedsValidation
        val busy = synchronized(this) { language in validating || language in unusable }
        if (needs && !busy) launchValidation(language) else publish(language)
    }

    /** The cheap checks: pinned checksum record, resolvable root, recorded fingerprint. No model load. */
    private fun local(language: VoiceLanguage): Local {
        val archive = archiveOf(language)
        if (manifest(language).isFile) {
            val record = props(manifest(language))
            if (record.getProperty("sha256") != archive.sha256) {
                return Local.Damaged(
                    "imported archive checksum does not match the pinned ${archive.name}"
                )
            }
            val model = File(dir(language), MODEL)
            val root = VoiceModelLayout.resolveRoot(model)
                ?: return Local.Damaged(
                    "no model root in ${model.path}: ${VoiceModelLayout.missing(model)}"
                )
            val recorded = record.getProperty("fingerprint")
            if (recorded == null || record.getProperty("validated") != "true") {
                return Local.NeedsValidation(root, VoiceModelSource.IMPORTED)
            }
            if (VoiceModelLayout.fingerprint(root) != recorded) {
                return Local.Damaged("files under ${root.path} changed since validation")
            }
            return Local.Ready(
                root,
                VoiceModelSource.IMPORTED,
                record.getProperty("bytes")?.toLongOrNull() ?: 0
            )
        }
        val pack = playDir(language)?.takeIf { it.isDirectory } ?: return Local.None
        val root = VoiceModelLayout.resolveRoot(pack)
            ?: return Local.Damaged(
                "no model root in Play pack ${pack.path}: ${VoiceModelLayout.missing(pack)}"
            )
        val record = playRecord(language).takeIf { it.isFile }?.let(::props)
        // a different fingerprint may be a new pack version: validate again rather than call it damaged
        return if (record?.getProperty("validated") == "true" &&
            record.getProperty("fingerprint") == VoiceModelLayout.fingerprint(root)
        ) {
            Local.Ready(root, VoiceModelSource.PLAY, archive.zipBytes)
        } else {
            Local.NeedsValidation(root, VoiceModelSource.PLAY)
        }
    }

    private fun stateOf(language: VoiceLanguage): VoiceModelState {
        val (isValidating, isUnusable, pack) = synchronized(this) {
            Triple(language in validating, language in unusable, packStatus[language])
        }
        if (isValidating) return VoiceModelState.Validating
        val archive = archiveOf(language)
        return when (val local = local(language)) {
            is Local.Damaged -> VoiceModelState.Damaged
            is Local.Ready ->
                if (isUnusable) {
                    VoiceModelState.Unusable
                } else {
                    VoiceModelState.Installed(archive.name, local.bytes, local.source)
                }
            // validation is started by refresh(); until it reports, the model is not usable
            is Local.NeedsValidation ->
                if (isUnusable) VoiceModelState.Unusable else VoiceModelState.Validating
            Local.None -> when (pack?.status) {
                PackStatus.DOWNLOADING -> VoiceModelState.Downloading(pack.percent)
                PackStatus.WAITING_FOR_WIFI -> VoiceModelState.WaitingForWifi
                PackStatus.FAILED -> VoiceModelState.DownloadFailed
                else -> VoiceModelState.NotInstalled
            }
        }
    }

    override fun installed(language: VoiceLanguage): OfflineModel? {
        if (synchronized(this) { language in validating || language in unusable }) return null
        return when (val local = local(language)) {
            is Local.Ready -> OfflineModel(language, local.root, archiveOf(language).name)
            is Local.Damaged -> {
                if (_states.value[language] != VoiceModelState.Damaged) {
                    logger.w(TAG, "Model for $language is damaged before use: ${local.reason}")
                    publish(language)
                }
                null
            }
            is Local.NeedsValidation -> {
                refresh(language)
                null
            }
            Local.None -> null
        }
    }

    override fun reportLoadFailure(language: VoiceLanguage, error: Throwable) {
        logger.w(
            TAG,
            "Vosk could not open the $language model: ${error.javaClass.name}: ${error.message}"
        )
        when (val local = local(language)) {
            is Local.Damaged -> logger.w(TAG, "Model for $language marked damaged: ${local.reason}")
            else -> {
                // intact files: a runtime/native/memory failure is not evidence of damage; keep them
                logger.w(
                    TAG,
                    "Model for $language kept and marked unusable until it is checked again"
                )
                synchronized(this) { unusable += language }
            }
        }
        publish(language)
    }

    override fun revalidate(language: VoiceLanguage) {
        logger.i(TAG, "Revalidating the $language model on request")
        launchValidation(language)
    }

    private fun launchValidation(language: VoiceLanguage) {
        if (!synchronized(this) { validating.add(language) }) return
        publish(language)
        scope.launch { validate(language) }
    }

    /** The full validation; [validating] is already set. Returns the resulting state. */
    private fun validate(language: VoiceLanguage): VoiceModelState {
        try {
            val archive = archiveOf(language)
            val target: Pair<File, VoiceModelSource>? = when (val local = local(language)) {
                is Local.Ready -> local.root to local.source
                is Local.NeedsValidation -> local.root to local.source
                is Local.Damaged -> {
                    logger.w(TAG, "Validation of $language: marked damaged: ${local.reason}")
                    null
                }
                Local.None -> null
            }
            if (target != null) {
                val (modelRoot, source) = target
                logger.i(
                    TAG,
                    "Validating $language: source=$source dir=${dir(
                        language
                    ).path} root=${modelRoot.path}"
                )
                val error = loader.check(modelRoot)
                if (error == null) {
                    record(language, source, modelRoot)
                    synchronized(this) { unusable -= language }
                    logger.i(TAG, "Validation of $language passed: ${archive.name}")
                } else {
                    synchronized(this) { unusable += language }
                    logger.w(
                        TAG,
                        "Validation of $language: Vosk Model init failed at ${modelRoot.path}: " +
                            "${error.javaClass.name}: ${error.message}; files kept"
                    )
                }
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            synchronized(this) { unusable += language }
            logger.w(TAG, "Validation of $language failed: ${e.javaClass.name}; files kept")
        } finally {
            synchronized(this) { validating -= language }
        }
        publish(language)
        return stateOf(language)
    }

    /** Remembers that the files under [modelRoot] validated, and their fingerprint for the cheap checks. */
    private fun record(language: VoiceLanguage, source: VoiceModelSource, modelRoot: File) {
        val fingerprint = VoiceModelLayout.fingerprint(modelRoot)
        val file = if (source ==
            VoiceModelSource.IMPORTED
        ) {
            manifest(language)
        } else {
            playRecord(language)
        }
        val values = if (file.isFile) props(file) else Properties()
        values.setProperty("fingerprint", fingerprint)
        values.setProperty("validated", "true")
        file.parentFile?.mkdirs()
        file.writer().use { values.store(it, null) }
    }

    override fun info(language: VoiceLanguage): VoiceModelInfo =
        archiveOf(language).let { VoiceModelInfo("${it.name}.zip", it.zipBytes) }

    override fun download(language: VoiceLanguage) {
        synchronized(this) { unusable -= language }
        packs.fetch(archiveOf(language).pack)
    }

    override fun remove(language: VoiceLanguage) {
        logger.i(TAG, "Removing the $language model")
        dir(language).deleteRecursively()
        packs.remove(archiveOf(language).pack)
        synchronized(this) {
            packStatus.remove(language)
            unusable -= language
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
            if (archive == null) {
                logger.w(TAG, "Import rejected: not a pinned model archive")
                return@withContext VoiceModelImport.NOT_A_MODEL
            }
            val model = File(staging, MODEL)
            if (VoiceModelLayout.resolveRoot(model) == null) {
                logger.w(
                    TAG,
                    "Import of ${archive.name} rejected: ${VoiceModelLayout.missing(model)}"
                )
                return@withContext VoiceModelImport.NOT_A_MODEL
            }
            File(staging, MANIFEST).writer().use { out ->
                Properties().apply {
                    setProperty("version", archive.name)
                    setProperty("sha256", sha)
                    setProperty("bytes", extracted.toString())
                    setProperty("validated", "false")
                }.store(out, null)
            }
            val target = dir(archive.language)
            target.deleteRecursively()
            if (!staging.renameTo(target)) return@withContext VoiceModelImport.FAILED
            logger.i(TAG, "Imported ${archive.name} into ${target.path}; validating")
            val language = archive.language
            synchronized(this@AndroidVoiceModelStore) {
                unusable -= language
                validating += language
            }
            publish(language)
            when (validate(language)) {
                is VoiceModelState.Installed -> VoiceModelImport.INSTALLED
                VoiceModelState.Unusable -> VoiceModelImport.LOAD_FAILED
                else -> VoiceModelImport.NOT_A_MODEL
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception
        ) {
            logger.w(TAG, "Model import failed: ${e.javaClass.simpleName}")
            VoiceModelImport.FAILED
        } finally {
            staging.deleteRecursively()
        }
    }

    /**
     * Unpacks [zip] under [into] with its paths intact (the model root is resolved afterwards, by
     * [VoiceModelLayout.resolveRoot]). Null for anything unsafe or oversized.
     */
    private fun extract(zip: ZipInputStream, into: File): Long? {
        var total = 0L
        var entries = 0
        val base = into.canonicalFile
        while (true) {
            val entry = zip.nextEntry ?: break
            if (++entries > MAX_ENTRIES) return null
            val relative = entry.name.replace('\\', '/').trimStart('/')
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
        const val PLAY_RECORD = "play.properties"
        const val BUFFER = 1 shl 16
        const val MAX_ENTRIES = 200
        const val MAX_BYTES = 200L * 1024 * 1024
    }
}
