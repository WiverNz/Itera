package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 1. */
enum class Skill { FOCUS, PLANNING, LEARNING, HABITS, REFLECTION }

/** Which UI body renders the exercise. One-to-one with a composable in feature/exercise. */

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class ExerciseType {
    TEMPLATE, // data-driven blocks (see section 4)
    FOCUS_TIMER, // Pomodoro, Deep Work
    EISENHOWER,
    FEYNMAN,
    PREMORTEM,
    HABIT_STACK,
    REFLECTION,
    REVIEW,
    COMBINATION
}

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class ActivityState {
    SCHEDULED,
    AVAILABLE,
    IN_PROGRESS,
    SNOOZED,
    COMPLETED,
    SKIPPED,
    EXPIRED
}

/** Why this activity is in today's plan. Drives ordering and copy. */

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class ActivitySource {
    PROGRAM, // the day's new technique
    REVIEW, // a due spaced-repetition review
    PRACTICE_PROMPT, // "Keep using: X - tap to log when you use it"
    FOCUS_SUGGESTION, // the optional daytime focus block
    REFLECTION, // the nightly reflection
    COMBINATION, // a combination-day chain
    MANUAL // user-initiated from Technique detail or Library
}

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class DayPart { MORNING, DAYTIME, EVENING }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class Difficulty { EASY, OKAY, HARD } // "How did it feel?"

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class MasteryLevel { NONE, MET, PRACTICED, APPLIED, INTEGRATED }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class SkillLevel { STARTING, BUILDING, STEADY, STRONG }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class RecallGrade { FORGOT, PARTIAL, SOLID } // spaced-repetition self-grade

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class TrainingDayStatus { PLANNED, IN_PROGRESS, COMPLETE, ABANDONED }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class ProgramPace { GENTLE, STANDARD, INTENSE }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/** Domain model, docs/data/00-domain-model.md section 1. */
enum class TimeBudget(val minutes: Int) { SHORT(5), STANDARD(15), LONG(30) }
