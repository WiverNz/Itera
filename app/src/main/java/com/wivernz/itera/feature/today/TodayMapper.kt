package com.wivernz.itera.feature.today

import com.wivernz.itera.core.designsystem.component.StepState
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.model.UserPreferences
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

enum class Greeting { MORNING, AFTERNOON, EVENING }

/** Where a hero button or step row leads. The route layer turns these into navigation. */
sealed interface TodayTarget {
    data class Exercise(val activityId: Long, val techniqueId: String) : TodayTarget
    data class Focus(val activityId: Long, val minutes: Int, val techniqueId: String) : TodayTarget
    data class Review(val activityId: Long) : TodayTarget
    data class Combination(val activityId: Long) : TodayTarget
    data class Reflection(val activityId: Long) : TodayTarget
    data class DayComplete(val dayId: Long) : TodayTarget

    /** A practice prompt ("Tap to log when you use it") completes in place. */
    data class LogPractice(val activityId: Long, val name: String) : TodayTarget
}

/** The hero branches, in the prototype's order (D-06) with the review inserted before focus (P-01). */
sealed interface TodayHero {
    data class Exercise(
        val techniqueId: String,
        val skill: Skill,
        val name: String,
        val line: String,
        val minutes: Int,
        val isNew: Boolean,
        val target: TodayTarget
    ) : TodayHero

    data class Combination(val curriculum: Boolean, val minutes: Int, val target: TodayTarget) :
        TodayHero

    data class Review(
        val techniqueId: String,
        val skill: Skill,
        val name: String,
        val topic: String?,
        val subtitle: String,
        val target: TodayTarget
    ) : TodayHero

    data class Focus(val minutes: Int, val enabled: Boolean, val target: TodayTarget) : TodayHero

    data class Reflection(val minutes: Int, val enabled: Boolean, val target: TodayTarget) :
        TodayHero

    /** The day is done. [quiet] once Day complete has been seen: no action is offered. */
    data class DayDone(val programDay: Int, val quiet: Boolean, val target: TodayTarget) : TodayHero
}

sealed interface StepSubtitle {
    data class Planned(val text: String) : StepSubtitle
    data class Note(val text: String) : StepSubtitle
    data object Done : StepSubtitle
    data object Skipped : StepSubtitle
    data class Snoozed(val until: LocalTime) : StepSubtitle
}

data class TodayStep(
    val activityId: Long,
    val title: String,
    val subtitle: StepSubtitle,
    val state: StepState,
    // null for a skipped row, which is drawn in the line colour
    val accent: Skill?,
    val target: TodayTarget?
)

data class TodayUiState(
    val greeting: Greeting = Greeting.MORNING,
    val programDay: Int = 1,
    val carryOver: String? = null,
    val hero: TodayHero? = null,
    val steps: List<TodayStep> = emptyList(),
    // every activity shown, optional ones included (not the day's requiredCount)
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val loading: Boolean = true,
    val error: Boolean = false,
    val loggedPractice: TodayTarget.LogPractice? = null
)

/** Everything Today renders from. */
data class TodayInput(
    val day: TrainingDay?,
    val techniques: Map<String, Technique>,
    val curriculumDays: Int,
    val preferences: UserPreferences,
    val now: LocalTime,
    // reviewItemId -> topic title, for topic-bound reviews
    val reviewTopics: Map<Long, String> = emptyMap(),
    // practice prompts logged in place, still inside their undo window
    val pendingPractice: Set<Long> = emptySet(),
    val failed: Boolean = false,
    val zone: ZoneId = ZoneOffset.UTC,
    val loggedPractice: TodayTarget.LogPractice? = null
)

fun greetingFor(time: LocalTime): Greeting = when {
    time.hour < NOON -> Greeting.MORNING
    time.hour < EVENING_HOUR -> Greeting.AFTERNOON
    else -> Greeting.EVENING
}

/** Pure: every hero branch and row state is table-testable without a view model. */
fun mapToUiState(input: TodayInput): TodayUiState {
    val day = input.day
    val base = TodayUiState(
        greeting = greetingFor(input.now),
        programDay = day?.programDay ?: input.preferences.currentProgramDay,
        carryOver = day?.carryOverIntent?.takeIf { it.isNotBlank() },
        loading = day == null && !input.failed,
        error = input.failed,
        loggedPractice = input.loggedPractice
    )
    if (day == null) return base
    val shown = day.activities
        .filter { it.state != ActivityState.EXPIRED && !it.isCombinationStep }
        .sortedBy { it.orderIndex }
    fun done(a: PlanActivity) = a.state == ActivityState.COMPLETED || a.id in input.pendingPractice
    fun closed(a: PlanActivity) = done(a) || a.state == ActivityState.SKIPPED
    val steps = shown.map { stepFor(it, input, done(it)) }
    return base.copy(
        hero = if (input.failed) null else heroFor(day, shown, input, ::closed),
        steps = steps,
        completedCount = shown.count(::done),
        totalCount = shown.size
    )
}

private fun heroFor(
    day: TrainingDay,
    shown: List<PlanActivity>,
    input: TodayInput,
    closed: (PlanActivity) -> Boolean
): TodayHero {
    val doneHero = TodayHero.DayDone(
        day.programDay,
        quiet = input.preferences.lastSeenDayComplete == day.date,
        target = TodayTarget.DayComplete(day.id)
    )
    if (day.status == TrainingDayStatus.COMPLETE) return doneHero
    val primary = shown.firstOrNull {
        it.source == ActivitySource.PROGRAM || it.source == ActivitySource.COMBINATION
    }
    val review = shown.firstOrNull { it.source == ActivitySource.REVIEW && !closed(it) }
    val focus = shown.firstOrNull { it.source == ActivitySource.FOCUS_SUGGESTION }
    val reflection = shown.firstOrNull { it.source == ActivitySource.REFLECTION }
    return when {
        primary != null && !closed(primary) -> primaryHero(day, primary, input)
        review != null -> {
            val technique = input.techniques[review.techniqueId.value]
            TodayHero.Review(
                techniqueId = review.techniqueId.value,
                skill = technique?.skill ?: Skill.LEARNING,
                name = technique?.name ?: review.title,
                topic = review.reviewItemId?.let(input.reviewTopics::get),
                subtitle = review.subtitle,
                target = TodayTarget.Review(review.id)
            )
        }
        focus != null && !closed(focus) && input.now.hour < EVENING_HOUR -> TodayHero.Focus(
            minutes = focus.estimatedMinutes,
            enabled = focus.actionable,
            target = targetFor(focus)
        )
        reflection != null && !closed(reflection) -> TodayHero.Reflection(
            minutes = reflection.estimatedMinutes,
            enabled = reflection.actionable,
            target = targetFor(reflection)
        )
        else -> doneHero
    }
}

private fun primaryHero(day: TrainingDay, primary: PlanActivity, input: TodayInput): TodayHero {
    if (primary.exerciseType == ExerciseType.COMBINATION) {
        return TodayHero.Combination(
            curriculum = day.programDay <= input.curriculumDays,
            minutes = primary.estimatedMinutes,
            target = targetFor(primary)
        )
    }
    val technique = input.techniques[primary.techniqueId.value]
    return TodayHero.Exercise(
        techniqueId = primary.techniqueId.value,
        skill = technique?.skill ?: Skill.HABITS,
        name = technique?.name ?: primary.title,
        line = technique?.shortDescription.orEmpty(),
        minutes = primary.estimatedMinutes,
        isNew = primary.source == ActivitySource.PROGRAM && technique?.introDay == day.programDay,
        target = targetFor(primary)
    )
}

private fun stepFor(activity: PlanActivity, input: TodayInput, done: Boolean): TodayStep {
    val skipped = !done && activity.state == ActivityState.SKIPPED
    val state = when {
        done || skipped -> StepState.Done
        activity.state == ActivityState.AVAILABLE || activity.state == ActivityState.IN_PROGRESS ->
            StepState.Now
        else -> StepState.Next
    }
    val subtitle = when {
        done -> activity.note?.takeIf { it.isNotBlank() }?.let(StepSubtitle::Note)
            ?: StepSubtitle.Done
        skipped -> StepSubtitle.Skipped
        activity.state == ActivityState.SNOOZED && activity.snoozedUntil != null ->
            StepSubtitle.Snoozed(activity.snoozedUntil.atZone(input.zone).toLocalTime())
        else -> StepSubtitle.Planned(activity.subtitle)
    }
    return TodayStep(
        activityId = activity.id,
        title = activity.title,
        subtitle = subtitle,
        state = state,
        accent = if (skipped) null else accentFor(activity, input),
        target = targetFor(activity).takeIf { !done && !skipped && activity.actionable }
    )
}

private fun accentFor(activity: PlanActivity, input: TodayInput): Skill = when (activity.source) {
    ActivitySource.FOCUS_SUGGESTION -> Skill.FOCUS
    ActivitySource.REFLECTION -> Skill.REFLECTION
    else -> input.techniques[activity.techniqueId.value]?.skill ?: Skill.HABITS
}

/** SCHEDULED items are not reachable yet (docs/engine/00 section 4); SNOOZED ones can be started early. */
private val PlanActivity.actionable: Boolean
    get() = state == ActivityState.AVAILABLE ||
        state == ActivityState.IN_PROGRESS ||
        state == ActivityState.SNOOZED

fun targetFor(activity: PlanActivity): TodayTarget = when {
    activity.source == ActivitySource.REFLECTION -> TodayTarget.Reflection(activity.id)
    activity.source == ActivitySource.REVIEW -> TodayTarget.Review(activity.id)
    activity.exerciseType == ExerciseType.COMBINATION -> TodayTarget.Combination(activity.id)
    activity.source == ActivitySource.PRACTICE_PROMPT ->
        TodayTarget.LogPractice(activity.id, activity.title)
    activity.source == ActivitySource.FOCUS_SUGGESTION -> TodayTarget.Focus(
        activity.id,
        activity.estimatedMinutes,
        activity.techniqueId.value
    )
    else -> TodayTarget.Exercise(activity.id, activity.techniqueId.value)
}

private const val NOON = 12
private const val EVENING_HOUR = 18
