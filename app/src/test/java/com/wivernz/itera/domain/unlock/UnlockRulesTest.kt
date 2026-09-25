package com.wivernz.itera.domain.unlock

import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.testCatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockRulesTest {
    private val order = listOf(
        "two_minute_rule",
        "pomodoro",
        "eisenhower_matrix",
        "five_second_rule",
        "habit_stacking",
        "feynman_technique",
        "two_list_strategy",
        "deep_work",
        "pareto_principle",
        "spaced_repetition",
        "information_diet",
        "premortem",
        "one_percent_improvement"
    )

    private suspend fun catalog(): List<Technique> = testCatalogRepository().catalog()

    @Test fun unlockedSetForDays1To20MatchesTheTable() = runTest {
        val catalog = catalog()
        for (day in 1..20) {
            val expected = (
                order.take(
                    day.coerceAtMost(13)
                ) + "daily_reflection"
                ).map(::TechniqueId).toSet()
            assertEquals("day $day", expected, UnlockRules.unlockedSet(catalog, day))
            assertTrue(TechniqueId("daily_reflection") in UnlockRules.unlockedSet(catalog, day))
        }
    }

    @Test fun unlockIsMonotoneAcrossAbandonedDaysAndContentShifts() = runTest {
        val catalog = catalog()
        // An abandoned day leaves programDay unchanged; nothing already unlocked is dropped.
        val met = UnlockRules.unlockedSet(catalog, 5)
        assertEquals(met, UnlockRules.unlockedSet(catalog, 5, met))
        // introDay moved later: a met technique stays unlocked.
        val moved = catalog.map {
            if (it.id.value ==
                "habit_stacking"
            ) {
                it.copy(introDay = 12)
            } else {
                it
            }
        }
        assertTrue(TechniqueId("habit_stacking") in UnlockRules.unlockedSet(moved, 5, met))
        assertFalse(TechniqueId("habit_stacking") in UnlockRules.unlockedSet(moved, 5))
        // introDay moved earlier: it unlocks at the next evaluation.
        val earlier = catalog.map { if (it.id.value == "premortem") it.copy(introDay = 3) else it }
        assertTrue(TechniqueId("premortem") in UnlockRules.unlockedSet(earlier, 5, met))
        // retired techniques never unlock
        val retired = catalog.map { if (it.id.value == "pomodoro") it.copy(retired = true) else it }
        assertFalse(UnlockRules.shouldUnlock(retired.first { it.id.value == "pomodoro" }, 20))
    }

    @Test fun unlocksOnDay() = runTest {
        val catalog = catalog().associateBy { it.id.value }
        assertEquals(11, UnlockRules.unlocksOnDay(catalog.getValue("information_diet")))
        assertNull(UnlockRules.unlocksOnDay(catalog.getValue("daily_reflection")))
    }
}
