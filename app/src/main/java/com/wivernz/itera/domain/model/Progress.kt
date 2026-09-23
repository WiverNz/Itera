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
    // "Practice it on 3 different days to reach Practiced."
    val nextLevelHint: String
)

/** Domain model, docs/data/00-domain-model.md section 7. */
data class SkillProgress(
    val skill: Skill,
    val level: SkillLevel,
    val practiceCount: Int,
    val daysPracticed: Int,
    // 14
    val windowDays: Int,
    // plural: "11 practices"
    val detail: String
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
    val activityCount: Int
)

/** One bar in the Progress day strip. Rendered as a bar, not a dot - see docs/ux/02-screen-specs-train-progress-you.md. */

/** Domain model, docs/data/00-domain-model.md section 7. */
data class DayDot(val date: LocalDate, val trained: Boolean, val skills: Set<Skill>)
