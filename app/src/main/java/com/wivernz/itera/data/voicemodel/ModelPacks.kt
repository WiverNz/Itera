package com.wivernz.itera.data.voicemodel

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

enum class PackStatus { NOT_INSTALLED, DOWNLOADING, WAITING_FOR_WIFI, COMPLETED, FAILED }

data class PackState(val pack: String, val status: PackStatus, val percent: Int = 0)

/** Play Asset Delivery, behind a seam: the store is tested without Play. */
interface ModelPacks {
    /** [onUpdate] receives every pack state change. */
    fun listen(onUpdate: (PackState) -> Unit)

    /** [onResult] gets whether Play can serve packs to this install, and their current states. */
    fun refresh(
        packs: List<String>,
        onResult: (available: Boolean, states: List<PackState>) -> Unit
    )

    fun fetch(pack: String)

    fun remove(pack: String)

    /** The extracted pack's assets folder, when it is installed. */
    fun assets(pack: String): File?
}

/**
 * The Play Store downloads and extracts on-demand packs; Itera itself needs no network permission. Installs that
 * did not come from Play (sideloaded APKs, debug builds without local testing) report Play as unavailable, and the
 * document-picker import is used instead.
 */
class PlayModelPacks @Inject constructor(@param:ApplicationContext private val context: Context) :
    ModelPacks {
    private val manager by lazy { AssetPackManagerFactory.getInstance(context) }

    override fun listen(onUpdate: (PackState) -> Unit) {
        runCatching { manager.registerListener { onUpdate(it.toPackState()) } }
    }

    override fun refresh(packs: List<String>, onResult: (Boolean, List<PackState>) -> Unit) {
        runCatching {
            manager.getPackStates(packs)
                .addOnSuccessListener { states ->
                    onResult(true, states.packStates().values.map { it.toPackState() })
                }
                .addOnFailureListener { onResult(false, emptyList()) }
        }.onFailure { onResult(false, emptyList()) }
    }

    override fun fetch(pack: String) {
        runCatching { manager.fetch(listOf(pack)) }
    }

    override fun remove(pack: String) {
        runCatching { manager.removePack(pack) }
    }

    override fun assets(pack: String): File? =
        runCatching { manager.getPackLocation(pack)?.assetsPath()?.let(::File) }.getOrNull()

    private fun AssetPackState.toPackState(): PackState {
        val total = totalBytesToDownload()
        val percent = if (total > 0) (bytesDownloaded() * PERCENT / total).toInt() else 0
        val status = when (status()) {
            AssetPackStatus.COMPLETED -> PackStatus.COMPLETED
            AssetPackStatus.PENDING, AssetPackStatus.DOWNLOADING, AssetPackStatus.TRANSFERRING ->
                PackStatus.DOWNLOADING
            AssetPackStatus.WAITING_FOR_WIFI, AssetPackStatus.REQUIRES_USER_CONFIRMATION ->
                PackStatus.WAITING_FOR_WIFI
            AssetPackStatus.FAILED -> PackStatus.FAILED
            else -> PackStatus.NOT_INSTALLED
        }
        return PackState(name(), status, percent)
    }

    private companion object {
        const val PERCENT = 100
    }
}
