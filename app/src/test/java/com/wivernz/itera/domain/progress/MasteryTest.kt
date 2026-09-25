package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryTest {
    private val start = LocalDate.of(2026, 3, 1)

    private fun level(
        unlocked: Boolean = true,
        intro: Boolean = true,
        days: Int = 1,
        uses: Int = 1,
        span: Long? = 0,
        combination: Boolean = false
    ) = Mastery.masteryOf(
        unlocked,
        intro,
        days,
        uses,
        span?.let {
            start
        },
        span?.let { start.plusDays(it) },
        combination
    )

    @Test fun truthTable() {
        assertEquals(MasteryLevel.NONE, level(unlocked = false, combination = true))
        assertEquals(MasteryLevel.NONE, level(intro = false, days = 5, uses = 9))
        assertEquals(MasteryLevel.MET, level(days = 1, uses = 1))
        assertEquals(MasteryLevel.MET, level(days = 2, uses = 5))
        assertEquals(MasteryLevel.PRACTICED, level(days = 3, uses = 3))
        assertEquals(MasteryLevel.PRACTICED, level(days = 5, uses = 5, span = 20))
        assertEquals(MasteryLevel.APPLIED, level(days = 6, uses = 6, span = 14))
        assertEquals(MasteryLevel.INTEGRATED, level(days = 1, uses = 1, combination = true))
        assertEquals(
            MasteryLevel.INTEGRATED,
            level(days = 9, uses = 9, span = 30, combination = true)
        )
    }

    @Test fun appliedSpanBoundary() {
        assertEquals(MasteryLevel.PRACTICED, level(days = 6, uses = 6, span = 13))
        assertEquals(MasteryLevel.APPLIED, level(days = 6, uses = 6, span = 14))
        assertEquals(MasteryLevel.PRACTICED, level(days = 6, uses = 6, span = null))
    }

    @Test fun lockedAndNeverCompleted() {
        assertEquals(
            MasteryLevel.NONE,
            level(unlocked = false, intro = false, days = 0, uses = 0, span = null)
        )
        assertEquals(
            MasteryLevel.NONE,
            level(unlocked = true, intro = false, days = 0, uses = 0, span = null)
        )
    }

    @Test fun nextLevelHints() {
        assertEquals(LevelHint.Unavailable, Mastery.nextLevelHint(MasteryLevel.NONE, 0, 0))
        assertEquals(LevelHint.ToPracticed(2), Mastery.nextLevelHint(MasteryLevel.MET, 1, 1))
        assertEquals(LevelHint.ToPracticed(1), Mastery.nextLevelHint(MasteryLevel.MET, 2, 4))
        assertEquals(LevelHint.ToApplied(2), Mastery.nextLevelHint(MasteryLevel.PRACTICED, 4, 4))
        assertEquals(LevelHint.ToApplied(0), Mastery.nextLevelHint(MasteryLevel.PRACTICED, 7, 9))
        assertEquals(LevelHint.ToIntegrated, Mastery.nextLevelHint(MasteryLevel.APPLIED, 6, 6))
        assertEquals(LevelHint.Integrated, Mastery.nextLevelHint(MasteryLevel.INTEGRATED, 6, 6))
    }
}
