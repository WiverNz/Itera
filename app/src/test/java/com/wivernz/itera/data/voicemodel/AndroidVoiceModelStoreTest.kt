package com.wivernz.itera.data.voicemodel

import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelSource
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Play Asset Delivery without Play: tests set availability, pack states and extracted folders. */
private class FakePacks(var available: Boolean = false) : ModelPacks {
    private var listener: ((PackState) -> Unit)? = null
    val fetched = mutableListOf<String>()
    val removed = mutableListOf<String>()
    val folders = mutableMapOf<String, File>()

    override fun listen(onUpdate: (PackState) -> Unit) {
        listener = onUpdate
    }

    override fun refresh(packs: List<String>, onResult: (Boolean, List<PackState>) -> Unit) =
        onResult(available, emptyList())

    override fun fetch(pack: String) {
        fetched += pack
    }

    override fun remove(pack: String) {
        removed += pack
        folders.remove(pack)
    }

    override fun assets(pack: String): File? = folders[pack]

    fun emit(state: PackState) = listener?.invoke(state)
}

private fun sha(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** A synthetic model archive: a top folder with the required files (or [files]) plus optional extra entries. */
private fun archive(
    top: String = "vosk-model-small-ru-test",
    files: List<String> = VoiceModelCatalog.requiredFiles,
    extra: Map<String, String> = emptyMap()
): ByteArray = ByteArrayOutputStream().also { out ->
    ZipOutputStream(out).use { zip ->
        files.forEach {
            zip.putNextEntry(ZipEntry("$top/$it"))
            zip.write("data for $it".toByteArray())
            zip.closeEntry()
        }
        extra.forEach { (name, body) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(body.toByteArray())
            zip.closeEntry()
        }
    }
}.toByteArray()

class AndroidVoiceModelStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val ru = archive()
    private val catalog = listOf(
        VoiceModelArchive(VoiceLanguage.RU, "vosk-model-small-ru-test", ru.size.toLong(), sha(ru)),
        VoiceModelArchive(VoiceLanguage.EN, "vosk-model-small-en-test", 1, "0".repeat(64))
    )
    private val uploads = mutableMapOf<String, ByteArray>()

    private fun store(root: File = File(folder.root, "voice-models"), packs: FakePacks = FakePacks()) =
        AndroidVoiceModelStore(
            root,
            packs,
            { uri -> uploads[uri]?.let(::ByteArrayInputStream) },
            Dispatchers.Unconfined,
            TestLogger(),
            catalog
        )

    private fun import(store: AndroidVoiceModelStore, bytes: ByteArray): VoiceModelImport = runBlocking {
        uploads["content://picked"] = bytes
        store.import("content://picked")
    }

    @Test fun nothingIsInstalledByDefault() {
        val models = store()
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.NotInstalled, models.states.value[VoiceLanguage.RU])
        assertFalse("sideloaded: no Play download", models.downloadAvailable.value)
    }

    @Test fun aPinnedArchiveImportsForTheLanguageItsChecksumNames() {
        val root = File(folder.root, "voice-models")
        val models = store(root)
        assertEquals(VoiceModelImport.INSTALLED, import(models, ru))
        val installed = models.installed(VoiceLanguage.RU)!!
        assertEquals("vosk-model-small-ru-test", installed.version)
        assertTrue(File(installed.path, "am/final.mdl").isFile)
        val state = models.states.value[VoiceLanguage.RU] as VoiceModelState.Installed
        assertEquals(VoiceModelSource.IMPORTED, state.source)
        val manifest = Properties().apply { File(root, "ru/manifest.properties").reader().use(::load) }
        assertEquals(sha(ru), manifest.getProperty("sha256"))
        assertEquals("no staging left", listOf("ru"), root.list()!!.toList())
        // persisted: a new store (app restart) finds it
        assertTrue(store(root).installed(VoiceLanguage.RU) != null)
    }

    @Test fun unknownArchivesAreRejectedAndLeaveNothing() {
        val root = File(folder.root, "voice-models")
        val models = store(root)
        assertEquals(VoiceModelImport.NOT_A_MODEL, import(models, archive(top = "someone-elses-model")))
        assertNull(models.installed(VoiceLanguage.RU))
        assertTrue(root.list().orEmpty().isEmpty())
    }

    @Test fun unsafeOrIncompleteArchivesAreRejected() {
        val models = store()
        val escaping = archive(extra = mapOf("top/../../escape.txt" to "x"))
        assertEquals(VoiceModelImport.NOT_A_MODEL, import(models, escaping))
        assertFalse(File(folder.root, "escape.txt").exists())

        // pinned checksum but missing a required file: incompatible
        val incomplete = archive(files = VoiceModelCatalog.requiredFiles.drop(1))
        val pinned = listOf(VoiceModelArchive(VoiceLanguage.RU, "x", incomplete.size.toLong(), sha(incomplete)))
        val strict = AndroidVoiceModelStore(
            File(folder.root, "strict"),
            FakePacks(),
            { ByteArrayInputStream(incomplete) },
            Dispatchers.Unconfined,
            TestLogger(),
            pinned
        )
        assertEquals(VoiceModelImport.NOT_A_MODEL, runBlocking { strict.import("content://x") })
        assertNull(strict.installed(VoiceLanguage.RU))
    }

    @Test fun anUnreadableUriFails() {
        assertEquals(VoiceModelImport.FAILED, runBlocking { store().import("content://missing") })
    }

    @Test fun aTamperedModelIsNotInstalled() {
        val root = File(folder.root, "voice-models")
        val models = store(root)
        import(models, ru)
        File(root, "ru/model/graph/Gr.fst").delete()
        assertNull("integrity check on every lookup", models.installed(VoiceLanguage.RU))
    }

    @Test fun removeAndDamageClearTheModel() {
        val root = File(folder.root, "voice-models")
        val packs = FakePacks()
        val models = store(root, packs)
        import(models, ru)
        models.remove(VoiceLanguage.RU)
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.NotInstalled, models.states.value[VoiceLanguage.RU])
        assertFalse(File(root, "ru").exists())

        import(models, ru)
        models.reportUnusable(VoiceLanguage.RU)
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.Damaged, models.states.value[VoiceLanguage.RU])
        assertEquals(listOf("voice_model_ru", "voice_model_ru"), packs.removed)
        // installing again clears the damaged state
        import(models, ru)
        assertTrue(models.states.value[VoiceLanguage.RU] is VoiceModelState.Installed)
    }

    @Test fun playPacksDownloadOnlyOnRequestAndInstallFromTheirFolder() {
        val packs = FakePacks(available = true)
        val models = store(packs = packs)
        assertTrue(models.downloadAvailable.value)
        assertTrue("never automatic", packs.fetched.isEmpty())
        models.download(VoiceLanguage.RU)
        assertEquals(listOf("voice_model_ru"), packs.fetched)
        packs.emit(PackState("voice_model_ru", PackStatus.DOWNLOADING, 40))
        assertEquals(VoiceModelState.Downloading(40), models.states.value[VoiceLanguage.RU])
        packs.emit(PackState("voice_model_ru", PackStatus.WAITING_FOR_WIFI))
        assertEquals(VoiceModelState.WaitingForWifi, models.states.value[VoiceLanguage.RU])

        val assets = folder.newFolder("pack")
        VoiceModelCatalog.requiredFiles.forEach { File(assets, "model/$it").apply { parentFile.mkdirs() }.writeText("x") }
        packs.folders["voice_model_ru"] = assets
        packs.emit(PackState("voice_model_ru", PackStatus.COMPLETED, 100))
        val state = models.states.value[VoiceLanguage.RU] as VoiceModelState.Installed
        assertEquals(VoiceModelSource.PLAY, state.source)
        assertEquals(File(assets, "model"), models.installed(VoiceLanguage.RU)!!.path)

        packs.emit(PackState("voice_model_en", PackStatus.FAILED))
        assertEquals(VoiceModelState.DownloadFailed, models.states.value[VoiceLanguage.EN])
    }

    @Test fun theDownloadSizeAndFileNameComeFromTheCatalog() {
        val info = store().info(VoiceLanguage.RU)
        assertEquals("vosk-model-small-ru-test.zip", info.file)
        assertEquals(ru.size.toLong(), info.downloadBytes)
    }
}

/** The runtime pins and the build-time pins are the same four archives. */
class VoiceModelCatalogTest {
    @Test fun catalogMatchesTheBuildPins() {
        val props = Properties().apply { File("../voicemodels/models.properties").reader().use(::load) }
        assertEquals(4, VoiceModelCatalog.archives.size)
        VoiceModelCatalog.archives.forEach { archive ->
            val (name, bytes, sha) = props.getProperty(archive.language.name.lowercase())!!.split("|")
            assertEquals(name, archive.name)
            assertEquals(bytes.toLong(), archive.zipBytes)
            assertEquals(sha, archive.sha256)
        }
        assertEquals(VoiceLanguage.entries.toSet(), VoiceModelCatalog.archives.map { it.language }.toSet())
    }

    @Test fun eachLanguageHasItsOwnOnDemandPack() {
        val packs = VoiceModelCatalog.archives.map { it.pack }
        assertEquals(listOf("voice_model_en", "voice_model_ru", "voice_model_de", "voice_model_es"), packs)
        packs.forEach { pack ->
            val build = File("../voicemodels/$pack/build.gradle.kts").readText()
            assertTrue(pack, "packName.set(\"$pack\")" in build && "deliveryType.set(\"on-demand\")" in build)
        }
        val app = File("build.gradle.kts").readText()
        packs.forEach { assertTrue("app bundles $it", "\":$it\"" in app) }
    }
}
