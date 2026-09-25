package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 2. */
data class Technique(
    // value class over String, e.g. "two_minute_rule"
    val id: TechniqueId,
    // "2-minute rule"
    val name: String,
    // "Clear small tasks before they accumulate."
    val shortDescription: String,
    // "Why it helps" body on Technique detail
    val explanation: String,
    // one-to-one (R-03)
    val skill: Skill,
    // 1..13, null for Daily reflection (always available)
    val introDay: Int?,
    val exerciseType: ExerciseType,
    val estimatedMinutes: Int,
    // only Feynman + Spaced repetition in the MVP
    val reviewEligible: Boolean,
    // "Works well with"
    val relatedTechniqueIds: List<TechniqueId>,
    // required when exerciseType == TEMPLATE
    val template: ExerciseTemplate?,
    // type-specific seed data (focus length, chips, ...)
    val defaults: TechniqueDefaults,
    // hidden from curriculum and library; history still resolves (ADR-0015)
    val retired: Boolean = false
)

/** Domain model, docs/data/00-domain-model.md section 2. */
@JvmInline value class TechniqueId(val value: String)

/** Domain model, docs/data/00-domain-model.md section 2. */
data class TechniqueDefaults(
    // Pomodoro 25, Deep Work 50
    val focusMinutes: Int? = null,
    // Pomodoro 5
    val breakMinutes: Int? = null,
    val suggestionChips: List<String> = emptyList(),
    // Habit stacking
    val anchorSuggestions: List<String> = emptyList(),
    val habitSuggestions: List<String> = emptyList()
)

/** Domain model, docs/data/00-domain-model.md section 2. */
data class Curriculum(val version: Int, val days: List<CurriculumDay>)

/** Domain model, docs/data/00-domain-model.md section 2. */
data class CurriculumDay(
    // 1-based program day
    val day: Int,
    // null on a combination day
    val newTechniqueId: TechniqueId?,
    // empty unless this is a combination day
    val combination: List<CombinationStep>,
    // Day 7 and every 7th day after
    val weeklyLookBack: Boolean
)

/** Domain model, docs/data/00-domain-model.md section 2. */
data class CombinationStep(
    val techniqueId: TechniqueId,
    // "Which part moves it most?"
    val prompt: String,
    // "Pick the one step that gives most of the result."
    val hint: String
)
