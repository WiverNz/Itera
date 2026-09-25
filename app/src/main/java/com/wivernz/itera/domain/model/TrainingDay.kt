package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 3. */
data class TrainingDay(
    val id: Long,
    // advances only on completion (R-06)
    val programDay: Int,
    // the calendar day this plan was generated for
    val date: LocalDate,
    val status: TrainingDayStatus,
    // last night's "What will you change tomorrow?"
    val carryOverIntent: String?,
    val activities: List<PlanActivity>,
    val completedAt: Instant?
) {
    init {
        require(programDay >= 1)
    }
    val completedCount: Int get() = activities.count { it.isCountedComplete }
    val requiredCount: Int get() = activities.count { it.countsTowardDay }
}

/** Domain model, docs/data/00-domain-model.md section 3. */
data class PlanActivity(
    val id: Long,
    val trainingDayId: Long,
    val techniqueId: TechniqueId,
    val exerciseType: ExerciseType,
    val source: ActivitySource,
    val orderIndex: Int,
    val dayPart: DayPart,
    // resolved copy, already localised at read time
    val title: String,
    // "Now - 5 min", "21:00 - 2 min", "Due today - 5 min - from Day 6"
    val subtitle: String,
    val instruction: String,
    val estimatedMinutes: Int,
    val state: ActivityState,
    // focus suggestions and practice prompts are optional
    val optional: Boolean,
    // for snooze and notification targeting
    val scheduledAt: LocalTime?,
    val snoozedUntil: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val durationSeconds: Int?,
    val difficulty: Difficulty?,
    val note: String?,
    val result: ActivityResult?,
    // set when source == REVIEW
    val reviewItemId: Long?,
    // reflection only: open with the 7-day look-back header (engine 01 section 8)
    val weeklyLookBack: Boolean = false
) {
    init {
        require(reviewItemId == null || source == ActivitySource.REVIEW)
        require(estimatedMinutes >= 0)
    }
    val countsTowardDay: Boolean get() = !optional
    val isCombinationStep: Boolean
        get() = source == ActivitySource.COMBINATION && exerciseType != ExerciseType.COMBINATION
    val isCountedComplete: Boolean get() = state == ActivityState.COMPLETED
}
