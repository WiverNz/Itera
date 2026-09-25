package com.wivernz.itera.data.catalog

import com.wivernz.itera.data.database.SeedTechnique

/** The seed callback's catalogue read: runs synchronously inside Room's onCreate. */
fun seedTechniques(assets: CatalogAssetSource): List<SeedTechnique> = CatalogParser.parse(
    assets.read(CatalogParser.TECHNIQUES_ASSET),
    assets.read(CatalogParser.CURRICULUM_ASSET)
).techniques.techniques.map { SeedTechnique(it.id, it.introDay) }
