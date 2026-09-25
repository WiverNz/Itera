package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.DayDot
import com.wivernz.itera.domain.model.ProgressSummary
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.model.SkillProgress
import com.wivernz.itera.domain.model.TechniqueId
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

/** Skill facts over the trailing window, docs/engine/03-mastery-and-progress.md section 3. */
data class SkillFacts(
    // completions in the window
    val practiceCount: Int,
    // distinct days in the window with at least one completion of this skill
    val activeDays: Int
)

/** One counted completion (see the mastery counting rule in the progress queries). */
data class CompletionFact(val date: LocalDate, val techniqueId: TechniqueId)

object SkillLevels {
    /**
     * D-09: `score = practiceCount + activeDays`, banded at 20 / 10 / 4. The window is the trailing
     * [WINDOW_DAYS] days, which is also the span of the Progress day strip (docs/engine/03 section 4).
     */
    const val WINDOW_DAYS = 14

    fun skillLevel(facts: SkillFacts): SkillLevel {
        val score = facts.practiceCount + facts.activeDays
        return when {
            score >= 20 -> SkillLevel.STRONG
            score >= 10 -> SkillLevel.STEADY
            score >= 4 -> SkillLevel.BUILDING
            else -> SkillLevel.STARTING
        }
    }

    /** Days since the program started plus one, capped at 14; 1 before the program starts. */
    fun windowSpan(programStartedOn: LocalDate?, today: LocalDate): Int {
        if (programStartedOn == null || programStartedOn.isAfter(today)) return 1
        return (
            DAYS.between(
                programStartedOn,
                today
            ) + 1
            ).coerceAtMost(WINDOW_DAYS.toLong()).toInt()
    }

    fun windowStart(programStartedOn: LocalDate?, today: LocalDate): LocalDate =
        today.minusDays(windowSpan(programStartedOn, today) - 1L)

    fun summarize(
        today: LocalDate,
        programStartedOn: LocalDate?,
        completions: List<CompletionFact>,
        skillOf: (TechniqueId) -> Skill?,
        activityCount: Int,
        focusSecondsAllTime: Long,
        focusSecondsInWindow: Long
    ): ProgressSummary {
        val start = windowStart(programStartedOn, today)
        val span = windowSpan(programStartedOn, today)
        val inWindow = completions.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
        val byDate = inWindow.groupBy { it.date }
        val dots = (0 until span).map { offset ->
            val date = start.plusDays(offset.toLong())
            val skills = byDate[date].orEmpty().mapNotNull { skillOf(it.techniqueId) }.toSet()
            DayDot(date, byDate.containsKey(date), skills)
        }
        val skills = Skill.entries.map { skill ->
            val mine = inWindow.filter { skillOf(it.techniqueId) == skill }
            val facts = SkillFacts(mine.size, mine.map { it.date }.distinct().size)
            SkillProgress(skill, skillLevel(facts), facts.practiceCount, facts.activeDays, span)
        }
        return ProgressSummary(
            trainedDays = byDate.size,
            windowDays = span,
            windowStart = start,
            dayDots = dots,
            skills = skills,
            activityCount = activityCount,
            focusMinutesAllTime = (focusSecondsAllTime / 60).toInt(),
            focusMinutesInWindow = (focusSecondsInWindow / 60).toInt()
        )
    }
}
