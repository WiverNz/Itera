package com.wivernz.itera.data.voicemodel

import java.io.File

/**
 * The one place that decides where a Vosk model root is and whether an installation is structurally intact
 * (milestone 013). Import, Play packs, validation and recognition all resolve the root through [resolveRoot], so the
 * engine opens exactly the directory validation loaded.
 */
object VoiceModelLayout {
    /** Why an installation cannot be used; null when the structure is intact. */
    fun missing(root: File): String? = VoiceModelCatalog.requiredFiles.firstOrNull {
        !File(root, it).isFile
    }?.let { "missing $it" }

    /**
     * The Vosk model root under [dir]: [dir] itself when it holds the model, or its single wrapper folder (an
     * archive's top-level `vosk-model-...` directory). Only one level, and only when that folder is the sole directory
     * at the top: anything else is malformed and resolves to null rather than being flattened.
     */
    fun resolveRoot(dir: File): File? {
        if (!dir.isDirectory) return null
        if (missing(dir) == null) return dir
        val folders = dir.listFiles()?.filter { it.isDirectory }.orEmpty()
        return folders.singleOrNull()?.takeIf { missing(it) == null }
    }

    /**
     * A cheap identity of the installed files: count and total size of everything under [root]. Recorded when an
     * installation validates, and compared before each use, so a truncated or partly deleted model is noticed without
     * re-hashing or re-loading it.
     */
    fun fingerprint(root: File): String {
        val files = root.walkTopDown().filter { it.isFile }.toList()
        return "${files.size}:${files.sumOf { it.length() }}"
    }
}
