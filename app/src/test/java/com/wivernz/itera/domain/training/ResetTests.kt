package com.wivernz.itera.domain.training

import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.ResetTiers
import com.wivernz.itera.data.repository.RoomDataResetRepository
import com.wivernz.itera.data.testDatabase
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private suspend fun IteraDatabase.tables(): Set<String> = withContext(Dispatchers.IO) {
    openHelper.readableDatabase.query(
        "SELECT name FROM sqlite_master WHERE type = 'table'"
    ).use { c ->
        buildSet { while (c.moveToNext()) add(c.getString(0)) }
    }.filterNot {
        it.startsWith("android_") || it.startsWith("sqlite_") || it == "room_master_table"
    }.toSet()
}

private suspend fun IteraDatabase.count(table: String): Int = withContext(Dispatchers.IO) {
    openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }
}

/** Trains Days 5-7: habit stack, focus, Feynman topic and review, reflections and the event log all have rows. */
private suspend fun populated(): EngineHarness {
    val h =
        EngineHarness(
            initialPreferences = UserPreferences(onboardingCompleted = true, currentProgramDay = 5)
        )
    h.trainFullDay()
    h.nextMorning()
    h.trainFullDay()
    // Day 7: the Feynman review falls due and is graded
    h.nextMorning()
    h.trainFullDay()
    h.focusTimer.cleared = 0
    h.reminders.calls.clear()
    return h
}

@RunWith(RobolectricTestRunner::class)
class ResetCoverageTest {
    @Test fun everyTableIsAssignedToExactlyOneTier() = runTest {
        val db = testDatabase()
        try {
            val tiers = listOf(ResetTiers.program, ResetTiers.resetFacts, ResetTiers.eraseOnly)
            assertEquals(db.tables(), tiers.flatten().toSet())
            assertEquals(tiers.sumOf { it.size }, tiers.flatten().toSet().size)
            // every deleted table has a delete, and nothing else is deleted
            val deletes = RoomDataResetRepository(db.resetDao()).deletes.map { it.first }
            assertEquals(ResetTiers.program + ResetTiers.eraseOnly, deletes.toSet())
            assertEquals(deletes.size, deletes.toSet().size)
        } finally {
            db.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ResetProgramUseCaseTest {
    @Test fun deletesTier1KeepsTopicsHabitsPreferencesAndReturnsToDay1() = runTest {
        val h = populated()
        try {
            ResetTiers.program.forEach { assertTrue(it, h.db.count(it) > 0) }
            val eraseOnly = ResetTiers.eraseOnly.associateWith { h.db.count(it) }
            eraseOnly.forEach { (table, n) -> assertTrue(table, n > 0) }
            h.prefs.update { it.copy(eveningTime = java.time.LocalTime.of(22, 0)) }

            h.resetProgram()

            ResetTiers.program.forEach { assertEquals(it, 0, h.db.count(it)) }
            assertEquals(eraseOnly.getValue("learning_topic"), h.db.count("learning_topic"))
            assertEquals(eraseOnly.getValue("habit_stack"), h.db.count("habit_stack"))
            assertTrue(h.db.count("event_log") >= eraseOnly.getValue("event_log"))
            val states = h.states.all()
            assertEquals(14, states.size)
            assertEquals(
                listOf("daily_reflection"),
                states.filter {
                    it.unlocked
                }.map { it.techniqueId.value }
            )
            assertTrue(states.all { it.introCompletedAt == null })
            val prefs = h.prefs.state.value
            assertEquals(1, prefs.currentProgramDay)
            assertEquals(h.today, prefs.programStartedOn)
            assertEquals(java.time.LocalTime.of(22, 0), prefs.eveningTime)
            assertTrue(prefs.onboardingCompleted)
            assertEquals("cancelAll", h.reminders.calls.first())
            assertEquals("rescheduleAll", h.reminders.calls.last())
            assertEquals(1, h.focusTimer.cleared)
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class EraseAllDataUseCaseTest {
    @Test fun returnsToFirstRun() = runTest {
        val h = populated()
        try {
            h.eraseAll()
            (ResetTiers.program + ResetTiers.eraseOnly).forEach {
                assertEquals(it, 0, h.db.count(it))
            }
            assertEquals(
                listOf("daily_reflection"),
                h.states.all().filter {
                    it.unlocked
                }.map { it.techniqueId.value }
            )
            val prefs = h.prefs.state.value
            assertEquals(UserPreferences(), prefs)
            assertFalse(prefs.onboardingCompleted)
            assertNull(prefs.programStartedOn)
            assertEquals(listOf("cancelAll", "rescheduleAll"), h.reminders.calls)
            assertEquals(1, h.focusTimer.cleared)
        } finally {
            h.close()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ResetIntegrationTest {
    @Test fun bothTiersEndToEnd() = runTest {
        val h = populated()
        try {
            h.nextMorning()
            h.resetProgram()
            val day1 = h.ensureToday()
            assertEquals(1, day1.programDay)
            assertEquals("two_minute_rule", day1.activities.first().techniqueId.value)
            h.trainFullDay()
            assertEquals(2, h.programDay())

            h.nextMorning()
            h.eraseAll()
            assertFalse(h.prefs.state.value.onboardingCompleted)
            val fresh = h.ensureToday()
            assertEquals(1, fresh.programDay)
            assertEquals(1, h.prefs.state.value.contentVersion)
            assertEquals(3, fresh.activities.size)
        } finally {
            h.close()
        }
    }
}
