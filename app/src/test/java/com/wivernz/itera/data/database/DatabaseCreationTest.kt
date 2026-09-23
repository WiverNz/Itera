package com.wivernz.itera.data.database
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.data.dayEntity
import com.wivernz.itera.data.fixtureDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class DatabaseCreationTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun freshSchemaSeedsAndSurvivesReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = folder.newFile("db").absolutePath
        fun open() = Room.databaseBuilder(
            context,
            IteraDatabase::class.java,
            path
        )
            .addCallback(SeedCallback(FakeClock()))
            .build()
        val db = open()
        val id = db.trainingDayDao().insert(dayEntity())
        val seeds = db.techniqueStateDao().observeAll().first()
        assertEquals(14, seeds.size)
        assertEquals(
            setOf(
                "two_minute_rule",
                "daily_reflection"
            ),
            seeds.filter { it.unlockedAt != null }.map { it.techniqueId }.toSet()
        )
        val tables = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'"
            )
                .use { cursor ->
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
                }
        }.filterNot {
            it.startsWith("android_") || it.startsWith("sqlite_") || it == "room_master_table"
        }.toSet()
        assertEquals(
            ResetTiers.program + ResetTiers.resetFacts + ResetTiers.eraseOnly,
            tables
        )
        db.close()
        val reopened = open()
        assertEquals(
            id,
            reopened.trainingDayDao().findByDate(fixtureDate.toEpochDay())?.id
        )
        assertEquals(14, reopened.techniqueStateDao().observeAll().first().size)
        reopened.close()
    }
}
