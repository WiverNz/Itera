package com.wivernz.itera.domain.unlock

import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.id
import com.wivernz.itera.domain.model.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UnlockTechniquesUseCaseTest {
    private val h = EngineHarness()

    @After fun close() = h.close()

    private suspend fun resetToReflectionOnly() =
        h.states.resetProgramFacts(setOf(id("daily_reflection")), h.clock.instant())

    @Test fun firstRunAtDay5WritesFiveRowsSecondRunNothing() = runTest {
        resetToReflectionOnly()
        val first = h.unlock(5)
        assertEquals(
            listOf(
                "two_minute_rule",
                "pomodoro",
                "eisenhower_matrix",
                "five_second_rule",
                "habit_stacking"
            ).map(::id).toSet(),
            first.toSet()
        )
        assertEquals(emptyList<Any>(), h.unlock(5))
        assertEquals(6, h.states.all().count { it.unlocked })
        assertTrue(h.eventNames().count { it == "technique_unlocked" } == 5)
    }

    @Test fun unlockedOnProgramDayRecordsTheActualDay() = runTest {
        resetToReflectionOnly()
        h.unlock(1)
        h.unlock(5)
        val rows = h.states.all().associateBy { it.techniqueId.value }
        assertEquals(1, rows.getValue("two_minute_rule").unlockedOnProgramDay)
        assertEquals(5, rows.getValue("pomodoro").unlockedOnProgramDay)
        assertEquals(5, rows.getValue("habit_stacking").unlockedOnProgramDay)
        assertNull(rows.getValue("feynman_technique").unlockedAt)
    }

    @Test fun neverRelocks() = runTest {
        h.unlock(9)
        h.unlock(3)
        assertTrue(h.states.all().single { it.techniqueId.value == "pareto_principle" }.unlocked)
    }
}

@RunWith(RobolectricTestRunner::class)
class ContentReconcilerTest {
    private val h = EngineHarness(initialPreferences = UserPreferences(currentProgramDay = 3))

    @After fun close() = h.close()

    @Test fun reconcilesOnceAndRecordsTheVersion() = runTest {
        assertTrue(h.reconciler.reconcile())
        assertEquals(1, h.prefs.state.value.contentVersion)
        val rows = h.states.all().associateBy { it.techniqueId.value }
        assertEquals(3, rows.getValue("eisenhower_matrix").unlockedOnProgramDay)
        assertNull(rows.getValue("five_second_rule").unlockedAt)
        val before = h.states.all()
        assertFalse(h.reconciler.reconcile())
        assertEquals(before, h.states.all())
    }

    @Test fun newTechniqueIsInsertedLockedAndMetTechniquesStayMet() = runTest {
        h.db.openHelper.writableDatabase.execSQL(
            "DELETE FROM technique_state WHERE techniqueId = 'information_diet'"
        )
        // a technique met before its intro day moved later
        h.states.unlock(id("premortem"), h.clock.instant(), 2)
        h.reconciler.reconcile()
        val rows = h.states.all().associateBy { it.techniqueId.value }
        assertNotNull(rows["information_diet"])
        assertNull(rows.getValue("information_diet").unlockedAt)
        assertTrue(rows.getValue("premortem").unlocked)
        assertEquals(14, rows.size)
    }
}
