package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.model.TechniqueId
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressSummaryTest {
    private val skills = mapOf(
        TechniqueId("pomodoro") to Skill.FOCUS,
        TechniqueId("daily_reflection") to Skill.REFLECTION,
        TechniqueId("two_minute_rule") to Skill.HABITS
    )

    private fun summary(
        today: LocalDate,
        started: LocalDate?,
        vararg done: Pair<LocalDate, String>
    ) = SkillLevels.summarize(
        today = today,
        programStartedOn = started,
        completions = done.map { CompletionFact(it.first, TechniqueId(it.second)) },
        skillOf = skills::get,
        activityCount = done.size,
        focusSecondsAllTime = 3000,
        focusSecondsInWindow = 1500
    )

    @Test fun emptyDatabase() {
        val s = summary(LocalDate.of(2026, 3, 28), null)
        assertEquals(0, s.trainedDays)
        assertEquals(1, s.windowDays)
        assertTrue(s.skills.all { it.level == SkillLevel.STARTING && it.practiceCount == 0 })
        assertEquals(Skill.entries.size, s.skills.size)
        assertEquals(1, s.dayDots.size)
    }

    @Test fun windowCrossesAMonthBoundaryAndCapsAt14() {
        val today = LocalDate.of(2026, 4, 3)
        val s = summary(
            today,
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 20) to "pomodoro", // outside the window
            LocalDate.of(2026, 3, 21) to "pomodoro",
            LocalDate.of(2026, 3, 31) to "pomodoro",
            LocalDate.of(2026, 4, 1) to "daily_reflection",
            LocalDate.of(2026, 4, 1) to "pomodoro"
        )
        assertEquals(14, s.windowDays)
        assertEquals(LocalDate.of(2026, 3, 21), s.windowStart)
        assertEquals(3, s.trainedDays)
        assertEquals(14, s.dayDots.size)
        assertEquals(
            setOf(Skill.REFLECTION, Skill.FOCUS),
            s.dayDots.first {
                it.date ==
                    LocalDate.of(2026, 4, 1)
            }.skills
        )
        val focus = s.skills.first { it.skill == Skill.FOCUS }
        assertEquals(3, focus.practiceCount)
        assertEquals(3, focus.daysPracticed)
        assertEquals(SkillLevel.BUILDING, focus.level)
        assertEquals(50, s.focusMinutesAllTime)
        assertEquals(25, s.focusMinutesInWindow)
    }

    @Test fun windowWithGapsGrowsFromTheStart() {
        val start = LocalDate.of(2026, 3, 26)
        val today = LocalDate.of(2026, 3, 30)
        val s = summary(today, start, start to "two_minute_rule", today to "two_minute_rule")
        assertEquals(5, s.windowDays)
        assertEquals(2, s.trainedDays)
        assertEquals(listOf(true, false, false, false, true), s.dayDots.map { it.trained })
        assertFalse(s.dayDots[1].skills.isNotEmpty())
    }

    @Test fun dstChangeKeepsCalendarDays() {
        // Europe/Berlin springs forward on 2026-03-29; windows are calendar dates, not 24-hour blocks.
        val zone = ZoneId.of("Europe/Berlin")
        val start = LocalDate.of(2026, 3, 28)
        val today = start.atStartOfDay(zone).plusHours(48).toLocalDate()
        assertEquals(LocalDate.of(2026, 3, 30), today)
        val s = summary(today, start, LocalDate.of(2026, 3, 29) to "pomodoro")
        assertEquals(3, s.windowDays)
        assertEquals(listOf(false, true, false), s.dayDots.map { it.trained })
        assertEquals(1, SkillLevels.windowSpan(LocalDate.of(2026, 4, 1), today))
    }
}
