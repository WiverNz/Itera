// Shared by every voice_model_<language> asset pack. Fills src/main/assets/<pack name>/ (git-ignored) with the
// pinned, checksum-verified Vosk model before the pack is bundled. Asset packs share one asset namespace, so each
// pack's files live under its own folder. Only bundle tasks need it; APK and test builds do not.
// Archives are cached in <gradle user home>/caches/itera-voice-models, or read from -Pitera.voiceModels.dir=<dir>.
import java.net.URI
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipInputStream

val fetchVoiceModel = tasks.register("fetchVoiceModel") {
    // Plain values only: the task action must not capture the script (configuration cache).
    val packName = project.name
    val catalog = Properties().apply { rootProject.file("voicemodels/models.properties").reader().use(::load) }
    val (archive, bytes, sha256) = catalog.getProperty(packName.removePrefix("voice_model_"))!!.split("|")
    val zipBytes = bytes.toLong()
    val cacheDir = providers.gradleProperty("itera.voiceModels.dir").map { file(it) }
        .getOrElse(gradle.gradleUserHomeDir.resolve("caches/itera-voice-models"))
    val assetsRoot = file("src/main/assets")
    val assetsDir = assetsRoot.resolve(packName)

    description = "Download, verify and unpack $archive into this asset pack"
    inputs.property("sha256", sha256)
    outputs.dir(assetsDir)
    doLast {
        cacheDir.mkdirs()
        val zip = cacheDir.resolve("$archive.zip")
        fun digest() = MessageDigest.getInstance("SHA-256").let { md ->
            zip.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    md.update(buffer, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
        if (!zip.isFile || zip.length() != zipBytes || digest() != sha256) {
            logger.lifecycle("Downloading $archive")
            URI("https://alphacephei.com/vosk/models/$archive.zip").toURL().openStream().use { input ->
                zip.outputStream().use { input.copyTo(it) }
            }
            check(zip.length() == zipBytes && digest() == sha256) {
                "$archive.zip does not match the pinned SHA-256; refusing to bundle it"
            }
        }
        // only this pack's folder: nothing else may sit in the shared asset namespace
        assetsRoot.deleteRecursively()
        ZipInputStream(zip.inputStream().buffered()).use { entries ->
            while (true) {
                val entry = entries.nextEntry ?: break
                // drop the archive's top folder; refuse anything that escapes it
                val relative = entry.name.substringAfter('/', "")
                if (relative.isEmpty() || entry.isDirectory) continue
                check(!relative.split('/').contains("..")) { "Unsafe entry in $archive" }
                val target = assetsDir.resolve(relative)
                target.parentFile.mkdirs()
                target.outputStream().use { entries.copyTo(it) }
            }
        }
    }
}

// Only the bundle path packages asset packs.
tasks.configureEach {
    if (name == "generateAssetPackManifest" || name.startsWith("assetPack")) {
        dependsOn(fetchVoiceModel)
    }
}
