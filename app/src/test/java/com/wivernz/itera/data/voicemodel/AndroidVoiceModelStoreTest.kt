package com.wivernz.itera.data.voicemodel

import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.voice.VoiceModelImport
import com.wivernz.itera.core.voice.VoiceModelLoader
import com.wivernz.itera.core.voice.VoiceModelSource
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
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

/** The real Vosk load, replaced: records the exact directory it was asked to open; fails with [error] when set. */
private class FakeLoader(var error: Throwable? = null) : VoiceModelLoader {
    val checked = mutableListOf<File>()

    override fun check(path: File): Throwable? {
        checked += path
        return error
    }
}

private fun sha(bytes: ByteArray) =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/**
 * A synthetic model archive: the required files under [top] (an archive's `vosk-model-...` folder; "" for a flat
 * archive, "a/b" for two levels), plus optional extra entries.
 */
private fun archive(
    top: String = "vosk-model-small-ru-test",
    files: List<String> = VoiceModelCatalog.requiredFiles,
    extra: Map<String, String> = emptyMap()
): ByteArray = ByteArrayOutputStream().also { out ->
    ZipOutputStream(out).use { zip ->
        files.forEach {
            zip.putNextEntry(ZipEntry(if (top.isEmpty()) it else "$top/$it"))
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

/** Writes the required files (with [skip] left out) under [dir]. */
private fun layOut(dir: File, skip: String? = null) = VoiceModelCatalog.requiredFiles.filter {
    it !=
        skip
}.forEach {
    File(dir, it).apply { parentFile.mkdirs() }.writeText("data for $it")
}

class AndroidVoiceModelStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val root get() = File(folder.root, "voice-models")
    private val uploads = mutableMapOf<String, ByteArray>()

    /** The RU archive under test is pinned; the other languages have unreachable pins. */
    private fun catalog(ru: ByteArray) = listOf(
        VoiceModelArchive(VoiceLanguage.RU, "vosk-model-small-ru-test", ru.size.toLong(), sha(ru)),
        VoiceModelArchive(VoiceLanguage.EN, "vosk-model-small-en-test", 1, "0".repeat(64)),
        VoiceModelArchive(VoiceLanguage.DE, "vosk-model-small-de-test", 1, "1".repeat(64)),
        VoiceModelArchive(VoiceLanguage.ES, "vosk-model-small-es-test", 1, "2".repeat(64))
    )

    private val ru = archive()

    private fun store(
        loader: FakeLoader = FakeLoader(),
        packs: FakePacks = FakePacks(),
        pinned: ByteArray = ru
    ) = AndroidVoiceModelStore(
        root,
        packs,
        { uri -> uploads[uri]?.let(::ByteArrayInputStream) },
        Dispatchers.Unconfined,
        TestLogger(),
        loader,
        catalog(pinned)
    )

    private fun import(store: AndroidVoiceModelStore, bytes: ByteArray): VoiceModelImport =
        runBlocking {
            uploads["content://picked"] = bytes
            store.import("content://picked")
        }

    private fun manifest() = Properties().apply {
        File(root, "ru/manifest.properties").reader().use(::load)
    }

    private fun ruState(store: AndroidVoiceModelStore) = store.states.value[VoiceLanguage.RU]

    @Test fun nothingIsInstalledByDefault() {
        val models = store()
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.NotInstalled, ruState(models))
        assertFalse("sideloaded: no Play download", models.downloadAvailable.value)
    }

    @Test fun importValidatesImmediatelyAndRecognitionUsesTheValidatedRoot() {
        val loader = FakeLoader()
        val models = store(loader)
        assertEquals(VoiceModelImport.INSTALLED, import(models, ru))
        val validated = loader.checked.single()
        val installed = models.installed(VoiceLanguage.RU)!!
        assertEquals(
            "recognition opens exactly the directory validation loaded",
            validated,
            installed.path
        )
        assertEquals("vosk-model-small-ru-test", installed.version)
        val state = ruState(models) as VoiceModelState.Installed
        assertEquals(VoiceModelSource.IMPORTED, state.source)
        assertEquals(sha(ru), manifest().getProperty("sha256"))
        assertEquals("true", manifest().getProperty("validated"))
        assertEquals(
            VoiceModelLayout.fingerprint(installed.path),
            manifest().getProperty("fingerprint")
        )
        assertEquals("no staging left", listOf("ru"), root.list()!!.toList())

        // persisted and not revalidated on restart: the cheap checks suffice
        val restartLoader = FakeLoader()
        val restarted = store(restartLoader)
        assertEquals(installed.path, restarted.installed(VoiceLanguage.RU)!!.path)
        assertTrue(restartLoader.checked.isEmpty())
    }

    @Test fun theArchiveWrapperFolderIsTheResolvedRoot() {
        val models = store()
        import(models, ru)
        val path = models.installed(VoiceLanguage.RU)!!.path
        assertEquals("vosk-model-small-ru-test", path.name)
        assertTrue(File(path, "am/final.mdl").isFile && File(path, "ivector/final.ie").isFile)
    }

    @Test fun aFlatArchiveResolvesToItsOwnFolder() {
        val flat = archive(top = "")
        val models = store(pinned = flat)
        assertEquals(VoiceModelImport.INSTALLED, import(models, flat))
        assertEquals(
            File(root, "ru/model").canonicalPath,
            models.installed(VoiceLanguage.RU)!!.path.canonicalPath
        )
    }

    @Test fun aModelNestedTwoLevelsDeepIsNotFlattened() {
        val deep = archive(top = "outer/vosk-model-small-ru-test")
        val loader = FakeLoader()
        val models = store(loader, pinned = deep)
        assertEquals(VoiceModelImport.NOT_A_MODEL, import(models, deep))
        assertNull(models.installed(VoiceLanguage.RU))
        assertTrue(loader.checked.isEmpty())
    }

    @Test fun missingRequiredFilesAreRejected() {
        val incomplete = archive(files = VoiceModelCatalog.requiredFiles - "ivector/final.ie")
        val loader = FakeLoader()
        val models = store(loader, pinned = incomplete)
        assertEquals(VoiceModelImport.NOT_A_MODEL, import(models, incomplete))
        assertNull(models.installed(VoiceLanguage.RU))
        assertTrue("no model load for an incomplete package", loader.checked.isEmpty())
    }

    @Test fun unpinnedAndUnsafeArchivesAreRejectedAndLeaveNothing() {
        val models = store()
        assertEquals(
            VoiceModelImport.NOT_A_MODEL,
            import(models, archive(top = "someone-elses-model"))
        )
        assertEquals(
            VoiceModelImport.NOT_A_MODEL,
            import(models, archive(extra = mapOf("top/../../escape.txt" to "x")))
        )
        assertFalse(File(folder.root, "escape.txt").exists())
        assertNull(models.installed(VoiceLanguage.RU))
        assertTrue(root.list().orEmpty().isEmpty())
        assertEquals(VoiceModelImport.FAILED, runBlocking { models.import("content://missing") })
    }

    @Test fun aChecksumRecordThatNoLongerMatchesIsDamaged() {
        val models = store()
        import(models, ru)
        val file = File(root, "ru/manifest.properties")
        file.writeText(file.readText().replace(sha(ru), "f".repeat(64)))
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.Damaged, ruState(models))
    }

    @Test fun aRuntimeFailureDuringValidationKeepsTheFilesAndIsNotDamage() {
        val loader =
            FakeLoader(
                UnsatisfiedLinkError("Can't obtain peer field ID for class com.sun.jna.Pointer")
            )
        val packs = FakePacks()
        val models = store(loader, packs)
        assertEquals(VoiceModelImport.LOAD_FAILED, import(models, ru))
        assertEquals(VoiceModelState.Unusable, ruState(models))
        assertNull("not offered for recognition", models.installed(VoiceLanguage.RU))
        assertTrue("files kept", File(root, "ru/manifest.properties").isFile)
        assertTrue(packs.removed.isEmpty())
    }

    @Test fun aLoadFailureAtUseDoesNotMarkIntactFilesDamaged() {
        val packs = FakePacks()
        val models = store(packs = packs)
        import(models, ru)
        models.reportLoadFailure(VoiceLanguage.RU, IOException("Failed to create a model"))
        assertEquals(VoiceModelState.Unusable, ruState(models))
        assertTrue("files kept", VoiceModelLayout.resolveRoot(File(root, "ru/model")) != null)
        assertTrue("nothing removed", packs.removed.isEmpty())
        models.reportLoadFailure(VoiceLanguage.RU, OutOfMemoryError())
        assertEquals(VoiceModelState.Unusable, ruState(models))
    }

    @Test fun aLoadFailureWithMissingFilesIsDamageAndReinstallRepairsIt() {
        val models = store()
        import(models, ru)
        val modelRoot = models.installed(VoiceLanguage.RU)!!.path
        File(modelRoot, "graph/Gr.fst").delete()
        models.reportLoadFailure(VoiceLanguage.RU, IOException("Failed to create a model"))
        assertEquals(VoiceModelState.Damaged, ruState(models))
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelImport.INSTALLED, import(models, ru))
        assertTrue(ruState(models) is VoiceModelState.Installed)
    }

    @Test fun theCheapPreUseCheckCatchesMissingOrChangedFilesWithoutLoading() {
        val loader = FakeLoader()
        val models = store(loader)
        import(models, ru)
        val modelRoot = models.installed(VoiceLanguage.RU)!!.path
        loader.checked.clear()
        File(modelRoot, "am/final.mdl").appendText("truncated or replaced")
        assertNull("changed since validation", models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.Damaged, ruState(models))
        assertTrue("no model load before use", loader.checked.isEmpty())
    }

    @Test fun anUnusableModelRevalidatesBackToReadyWithoutReinstalling() {
        val loader = FakeLoader(UnsatisfiedLinkError("jna"))
        val models = store(loader)
        import(models, ru)
        assertEquals(VoiceModelState.Unusable, ruState(models))
        loader.error = null // for example after an app update with the missing keep rules
        models.revalidate(VoiceLanguage.RU)
        assertTrue(ruState(models) is VoiceModelState.Installed)
        assertTrue(models.installed(VoiceLanguage.RU) != null)
    }

    @Test fun aGenuinelyDamagedModelStaysDamagedOnRevalidation() {
        val loader = FakeLoader()
        val models = store(loader)
        import(models, ru)
        File(models.installed(VoiceLanguage.RU)!!.path, "conf/model.conf").delete()
        loader.checked.clear()
        models.revalidate(VoiceLanguage.RU)
        assertEquals(VoiceModelState.Damaged, ruState(models))
        assertTrue("no load attempt for a structurally broken model", loader.checked.isEmpty())
    }

    @Test fun anInstallationFromAnEarlierBuildIsValidatedOnceAtStartup() {
        // milestone 013's first builds stripped the archive folder and wrote no fingerprint
        layOut(File(root, "ru/model"))
        File(
            root,
            "ru/manifest.properties"
        ).writeText("version=vosk-model-small-ru-test\nsha256=${sha(ru)}\nbytes=1\n")
        val loader = FakeLoader()
        val models = store(loader)
        assertEquals(listOf(File(root, "ru/model")), loader.checked)
        assertTrue(ruState(models) is VoiceModelState.Installed)
        assertEquals("true", manifest().getProperty("validated"))
    }

    @Test fun playPacksValidateOnDeliveryAndUseTheResolvedRoot() {
        val packs = FakePacks(available = true)
        val loader = FakeLoader()
        val models = store(loader, packs)
        models.download(VoiceLanguage.RU)
        assertEquals(listOf("voice_model_ru"), packs.fetched)
        packs.emit(PackState("voice_model_ru", PackStatus.DOWNLOADING, 40))
        assertEquals(VoiceModelState.Downloading(40), ruState(models))
        packs.emit(PackState("voice_model_ru", PackStatus.WAITING_FOR_WIFI))
        assertEquals(VoiceModelState.WaitingForWifi, ruState(models))

        val assets = folder.newFolder("pack")
        layOut(File(assets, "voice_model_ru"))
        packs.folders["voice_model_ru"] = assets
        packs.emit(PackState("voice_model_ru", PackStatus.COMPLETED, 100))
        assertEquals(listOf(File(assets, "voice_model_ru")), loader.checked)
        val state = ruState(models) as VoiceModelState.Installed
        assertEquals(VoiceModelSource.PLAY, state.source)
        assertEquals(loader.checked.single(), models.installed(VoiceLanguage.RU)!!.path)

        packs.emit(PackState("voice_model_en", PackStatus.FAILED))
        assertEquals(VoiceModelState.DownloadFailed, models.states.value[VoiceLanguage.EN])
    }

    @Test fun aPlayPackThatWillNotLoadIsUnusableNotDamaged() {
        val packs = FakePacks(available = true)
        val models = store(FakeLoader(IOException("Failed to create a model")), packs)
        val assets = folder.newFolder("pack")
        layOut(File(assets, "voice_model_ru"))
        packs.folders["voice_model_ru"] = assets
        packs.emit(PackState("voice_model_ru", PackStatus.COMPLETED, 100))
        assertEquals(VoiceModelState.Unusable, ruState(models))
        assertTrue("the pack is kept", packs.removed.isEmpty())
    }

    @Test fun anIncompletePlayPackIsDamaged() {
        val packs = FakePacks(available = true)
        val loader = FakeLoader()
        val models = store(loader, packs)
        val assets = folder.newFolder("pack")
        layOut(File(assets, "voice_model_ru"), skip = "graph/HCLr.fst")
        packs.folders["voice_model_ru"] = assets
        packs.emit(PackState("voice_model_ru", PackStatus.COMPLETED, 100))
        assertEquals(VoiceModelState.Damaged, ruState(models))
        assertTrue(loader.checked.isEmpty())
    }

    @Test fun removeClearsTheModel() {
        val packs = FakePacks()
        val models = store(packs = packs)
        import(models, ru)
        models.remove(VoiceLanguage.RU)
        assertNull(models.installed(VoiceLanguage.RU))
        assertEquals(VoiceModelState.NotInstalled, ruState(models))
        assertFalse(File(root, "ru").exists())
        assertEquals(listOf("voice_model_ru"), packs.removed)
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
        val props = Properties().apply {
            File("../voicemodels/models.properties").reader().use(::load)
        }
        assertEquals(4, VoiceModelCatalog.archives.size)
        VoiceModelCatalog.archives.forEach { archive ->
            val (name, bytes, sha) = props.getProperty(
                archive.language.name.lowercase()
            )!!.split("|")
            assertEquals(name, archive.name)
            assertEquals(bytes.toLong(), archive.zipBytes)
            assertEquals(sha, archive.sha256)
        }
        assertEquals(
            VoiceLanguage.entries.toSet(),
            VoiceModelCatalog.archives.map {
                it.language
            }.toSet()
        )
    }

    @Test fun eachLanguageHasItsOwnOnDemandPack() {
        val packs = VoiceModelCatalog.archives.map { it.pack }
        assertEquals(
            listOf("voice_model_en", "voice_model_ru", "voice_model_de", "voice_model_es"),
            packs
        )
        packs.forEach { pack ->
            val build = File("../voicemodels/$pack/build.gradle.kts").readText()
            assertTrue(
                pack,
                "packName.set(\"$pack\")" in build && "deliveryType.set(\"on-demand\")" in build
            )
        }
        val app = File("build.gradle.kts").readText()
        packs.forEach { assertTrue("app bundles $it", "\":$it\"" in app) }
    }
}
