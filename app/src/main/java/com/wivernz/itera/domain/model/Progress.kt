package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 7. */
data class TechniqueProgress(
    val techniqueId: TechniqueId,
    val unlocked: Boolean,
    // shown as "Day 11" on locked library rows
    val unlocksOnDay: Int?,
    val level: MasteryLevel,
    val distinctPracticeDays: Int,
    val totalUses: Int,
    val firstUsedOn: LocalDate?,
    val lastUsedOn: LocalDate?,
    val usedInCombination: Boolean,
    // rendered by the UI: "Practice it on 3 more days to reach Practiced."
    val nextLevelHint: LevelHint
)

/** Next-level hint, docs/engine/03-mastery-and-progress.md section 2.1. Structured; the UI owns the copy. */
sealed interface LevelHint {
    data object Unavailable : LevelHint
    data class ToPracticed(val moreDays: Int) : LevelHint
    data class ToApplied(val moreUses: Int) : LevelHint
    data object ToIntegrated : LevelHint
    data object Integrated : LevelHint
}

/** Domain model, docs/data/00-domain-model.md section 7. */
data class SkillProgress(
    val skill: Skill,
    val level: SkillLevel,
    val practiceCount: Int,
    val daysPracticed: Int,
    // 14
    val windowDays: Int
)

/** Domain model, docs/data/00-domain-model.md section 7. */
data class ProgressSummary(
    // in the trailing window
    val trainedDays: Int,
    // grows to 14, then caps
    val windowDays: Int,
    val windowStart: LocalDate,
    val dayDots: List<DayDot>,
    val skills: List<SkillProgress>,
    // "41 activities - notes and reflections"
    val activityCount: Int,
    // SUM(focus_session.actualSeconds) / 60
    val focusMinutesAllTime: Int = 0,
    val focusMinutesInWindow: Int = 0
)

/** One bar in the Progress day strip. Rendered as a bar, not a dot - see docs/ux/02-screen-specs-train-progress-you.md. */

/** Domain model, docs/data/00-domain-model.md section 7. */
data class DayDot(val date: LocalDate, val trained: Boolean, val skills: Set<Skill>)
