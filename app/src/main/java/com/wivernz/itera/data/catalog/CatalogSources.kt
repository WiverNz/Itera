package com.wivernz.itera.data.catalog

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads a bundled asset as UTF-8 text. */
fun interface CatalogAssetSource {
    fun read(path: String): String
}

class AndroidCatalogAssetSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CatalogAssetSource {
    override fun read(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/**
 * Resolves catalogue-derived resource keys. Keys are data (docs/data/04 section 4), so they are looked up by
 * name; ids are cached per process.
 */
interface CatalogStrings {
    val locale: Locale
    fun text(key: String): String?
    fun array(key: String): List<String>?
}

class ResourceCatalogStrings @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CatalogStrings {
    private val ids = ConcurrentHashMap<String, Int>()

    override val locale: Locale get() = context.resources.configuration.locales[0]

    override fun text(key: String): String? = id(key, "string")?.let(context::getString)

    override fun array(key: String): List<String>? =
        id(key, "array")?.let { context.resources.getStringArray(it).toList() }

    @SuppressLint("DiscouragedApi")
    private fun id(key: String, type: String): Int? = ids.getOrPut("$type/$key") {
        context.resources.getIdentifier(key, type, context.packageName)
    }.takeIf { it != 0 }
}

/**
 * The last successfully parsed catalogue (ADR-0005 release fallback): one Preferences entry holding both raw
 * files. Written after every successful parse, read only when the bundled asset fails.
 */
interface CatalogCache {
    suspend fun read(): Pair<String, String>?
    suspend fun write(techniques: String, curriculum: String)
}

class DataStoreCatalogCache @Inject constructor(private val store: DataStore<Preferences>) :
    CatalogCache {
    @Serializable
    private data class Entry(val techniques: String, val curriculum: String)

    override suspend fun read(): Pair<String, String>? = store.data.first()[KEY]?.let {
        runCatching { Json.decodeFromString(Entry.serializer(), it) }.getOrNull()
    }?.let { it.techniques to it.curriculum }

    override suspend fun write(techniques: String, curriculum: String) {
        val value = Json.encodeToString(Entry.serializer(), Entry(techniques, curriculum))
        store.edit { if (it[KEY] != value) it[KEY] = value }
    }

    private companion object {
        val KEY = stringPreferencesKey("catalog_cache")
    }
}
