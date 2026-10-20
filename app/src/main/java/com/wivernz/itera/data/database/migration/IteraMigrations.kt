package com.wivernz.itera.data.database.migration

import androidx.room.migration.Migration

/**
 * Every schema migration, one per version step, so none can be forgotten
 * (docs/data/05-migrations-and-content-versioning.md section 1). Version 1 is the first shipped schema, so the
 * list is empty; `SchemaMigrationGuardTest` fails when a schema version is added without its step here.
 */
object IteraMigrations {
    val ALL: Array<Migration> = emptyArray()
}
