package com.wivernz.itera.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.migration.IteraMigrations
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The schema-change guard (docs/testing/03-ci.md section 2): the database version, the committed schema
 * exports and the migration registry must agree. A version bump without its `Migration(n, n + 1)` in
 * [IteraMigrations.ALL] fails here; an entity change without a version bump rewrites the committed
 * `app/schemas` export, which CI rejects with `git diff --exit-code -- app/schemas` after the build.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaMigrationGuardTest {
    // @Database is not retained at runtime; the opened database reports the version Room compiled in.
    private val version = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        IteraDatabase::class.java
    ).build().let { db ->
        try {
            db.openHelper.readableDatabase.version
        } finally {
            db.close()
        }
    }
    private val exports = File("schemas/${IteraDatabase::class.java.name}")
        .listFiles { file -> file.name.endsWith(".json") }.orEmpty()
        .associateBy { it.nameWithoutExtension.toInt() }

    @Test fun everySchemaVersionUpToTheCurrentOneIsExported() {
        assertEquals("exported schema versions", (1..version).toList(), exports.keys.sorted())
        exports.forEach { (number, file) ->
            val declared = Regex(
                "\"version\"\\s*:\\s*(\\d+)"
            ).find(file.readText())?.groupValues?.get(1)?.toInt()
            assertEquals("${file.name} declares its own version", number, declared)
        }
    }

    @Test fun everyVersionStepHasExactlyOneRegisteredMigration() {
        val steps = IteraMigrations.ALL.map { it.startVersion to it.endVersion }
        assertEquals(
            "a schema version was added without its migration in IteraMigrations.ALL",
            (1 until version).map { it to it + 1 },
            steps.sortedBy { it.first }
        )
        assertTrue("no migration skips or repeats a step", steps.distinct().size == steps.size)
    }
}
