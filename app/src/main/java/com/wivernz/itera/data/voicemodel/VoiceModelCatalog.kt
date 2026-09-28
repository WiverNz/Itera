package com.wivernz.itera.data.voicemodel

import com.wivernz.itera.domain.voice.VoiceLanguage

/** One pinned offline model archive (milestone 013). Mirrors voicemodels/models.properties (VoiceModelCatalogTest). */
data class VoiceModelArchive(
    val language: VoiceLanguage,
    /** Archive name without `.zip`; also the model version shown in Settings. */
    val name: String,
    val zipBytes: Long,
    val sha256: String
) {
    /** The on-demand Play Asset Delivery pack that carries this model under `model/`. */
    val pack: String get() = "voice_model_${language.name.lowercase()}"
}

object VoiceModelCatalog {
    val archives: List<VoiceModelArchive> = listOf(
        VoiceModelArchive(
            VoiceLanguage.EN,
            "vosk-model-small-en-us-0.15",
            41_205_931,
            "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"
        ),
        VoiceModelArchive(
            VoiceLanguage.RU,
            "vosk-model-small-ru-0.22",
            46_236_750,
            "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4"
        ),
        VoiceModelArchive(
            VoiceLanguage.DE,
            "vosk-model-small-de-0.15",
            46_499_967,
            "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"
        ),
        VoiceModelArchive(
            VoiceLanguage.ES,
            "vosk-model-small-es-0.42",
            39_817_833,
            "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f"
        )
    )

    fun of(language: VoiceLanguage): VoiceModelArchive = archives.first { it.language == language }

    fun bySha256(sha256: String): VoiceModelArchive? = archives.firstOrNull { it.sha256 == sha256 }

    fun byPack(pack: String): VoiceModelArchive? = archives.firstOrNull { it.pack == pack }

    /** Files every supported Vosk small model has; a model missing any is damaged or incompatible. */
    val requiredFiles = listOf(
        "am/final.mdl",
        "conf/model.conf",
        "conf/mfcc.conf",
        "graph/HCLr.fst",
        "graph/Gr.fst"
    )
}
