package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.RuntimeChecks
import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.FocusSessionRecord
import com.wivernz.itera.domain.repository.HabitStackRecord
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.review.ScheduleReviewUseCase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** What must happen after the completing transaction commits. */
class CompletionOutcome internal constructor(
    val activityId: Long,
    val dayId: Long?,
    internal val dayCompleted: Boolean,
    internal val habitStackId: Long?,
    internal val alreadyCompleted: Boolean
)

/**
 * docs/engine/00-exercise-state-machine.md section 5. Validates the transition, persists the result and runs
 * all seven post-completion effects inside one transaction; a failure in any of them rolls everything back.
 * Completing an already-completed activity is a no-op success, so replays are harmless.
 */
class CompleteActivityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val states: TechniqueStateRepository,
    private val records: PracticeRecordRepository,
    private val scheduleReview: ScheduleReviewUseCase,
    private val advance: AdvanceProgramDayUseCase,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock,
    private val checks: RuntimeChecks
) {
    suspend operator fun invoke(
        activityId: Long,
        result: ActivityResult,
        difficulty: Difficulty? = null,
        note: String? = null
    ): Result<Unit> {
        val outcome = tx.inTransaction {
            completeInTransaction(activityId, result, difficulty, note)
        }
        return outcome.map { afterCommit(it) }
    }

    /** For callers that complete inside a larger transaction; they must call [afterCommit] once it commits. */
    suspend fun completeInTransaction(
        activityId: Long,
        result: ActivityResult,
        difficulty: Difficulty?,
        note: String?
    ): Result<CompletionOutcome> {
        val activity = plans.activity(activityId) ?: return failure(DomainError.ActivityNotFound)
        if (activity.state == ActivityState.COMPLETED) {
            return Result.success(CompletionOutcome(activityId, null, false, null, true))
        }
        if (resultTypeOf(result) != activity.exerciseType) {
            check(!checks.failFast) {
                "Result ${resultTypeOf(result)} does not match ${activity.exerciseType}"
            }
            return failure(DomainError.IllegalTransition(activity.state, "Complete"))
        }
        val day = plans.day(activity.trainingDayId) ?: return failure(DomainError.ActivityNotFound)
        val now = clock.instant()
        val facts = activity.transitionFacts(day, clock.zone)
        // Completing from AVAILABLE or SNOOZED passes through Start, as the table requires.
        var state = activity.state
        if (state == ActivityState.AVAILABLE || state == ActivityState.SNOOZED) {
            state = ActivityStateMachine.transition(state, ActivityEvent.Start, now, facts)
                .getOrElse { return Result.failure(it) }
        }
        ActivityStateMachine.transition(state, ActivityEvent.Complete(result), now, facts)
            .getOrElse { return Result.failure(it) }

        val duration = (result as? ActivityResult.Focus)?.actualSeconds
            ?: secondsBetween(activity.startedAt ?: now, now)
        plans.complete(activityId, result, difficulty, note, now, duration)
        if (activity.exerciseType == ExerciseType.COMBINATION) {
            settleCombinationSteps(day, result as ActivityResult.Combination)
        }

        // 1. First completion of an unlocked technique -> MasteryLevel.MET. The Day-1 generic focus block is
        //    attached to a still-locked Pomodoro and therefore grants nothing.
        if (activity.exerciseType != ExerciseType.COMBINATION && isUnlocked(activity.techniqueId)) {
            states.markIntroCompleted(activity.techniqueId, now)
        }
        // 2. Review scheduling (review-eligible techniques with a recallable answer).
        scheduleReview(activityId, activity.techniqueId, result, day.date)
        // 3. Focus session.
        if (result is ActivityResult.Focus) {
            records.insertFocusSession(
                FocusSessionRecord(
                    activityId = activityId,
                    techniqueId = activity.techniqueId,
                    taskLabel = result.taskLabel,
                    plannedSeconds = result.plannedSeconds,
                    actualSeconds = result.actualSeconds,
                    extendedSeconds = result.extendedSeconds,
                    completedNaturally = result.completedNaturally,
                    startedAt =
                    activity.startedAt ?: now.minusSeconds(result.actualSeconds.toLong()),
                    endedAt = now
                )
            )
        }
        // 4. Habit stack.
        val habitStackId = (result as? ActivityResult.HabitStack)?.let { stack ->
            val id = records.insertHabitStack(
                HabitStackRecord(
                    activityId,
                    stack.anchor,
                    stack.habit,
                    stack.nudgeEnabled,
                    stack.nudgeTime,
                    now
                )
            )
            // Scheduled either way: the scheduler reads the row and cancels a disabled or replaced nudge.
            id
        }
        // 5. Premortem carry-over.
        if (result is ActivityResult.Premortem && result.mitigationAddedToToday) {
            result.mitigationAction?.takeIf { it.isNotBlank() }?.let { addMitigation(day, it) }
        }
        // The reflection's answers also feed tomorrow's carry-over intent and History.
        if (result is ActivityResult.Reflection) {
            plans.saveReflection(day.id, day.date, result, skipped = false, at = now)
        }
        if (day.status == TrainingDayStatus.PLANNED) {
            plans.updateDayStatus(day.id, TrainingDayStatus.IN_PROGRESS, null)
        }
        // 6. Day completion; the program advances after the commit.
        val dayCompleted = completeDayIfDone(plans, analytics, day.id, now)
        // 7. Analytics, inside the transaction.
        AnalyticsTechnique.of(activity.techniqueId.value)?.let {
            analytics.append(
                Event.ExerciseCompleted(
                    techniqueId = it,
                    exerciseType = activity.exerciseType,
                    source = activity.source,
                    durationSeconds = duration,
                    difficulty = difficulty,
                    hasNote = !note.isNullOrBlank()
                )
            )
        }
        return Result.success(
            CompletionOutcome(activityId, day.id, dayCompleted, habitStackId, false)
        )
    }

    suspend fun afterCommit(outcome: CompletionOutcome) {
        if (outcome.alreadyCompleted) return
        reminders.cancelForActivity(outcome.activityId)
        outcome.habitStackId?.let { reminders.scheduleHabitNudge(it) }
        if (outcome.dayCompleted && outcome.dayId != null) advance(outcome.dayId)
    }

    private suspend fun isUnlocked(id: TechniqueId) = states.all().any {
        it.techniqueId == id &&
            it.unlocked
    }

    /**
     * Steps still open when the chain is saved take their outcome from the chain's step results: a step with
     * a result is COMPLETED (earning INTEGRATED credit), one without is SKIPPED.
     */
    private suspend fun settleCombinationSteps(
        day: TrainingDay,
        result: ActivityResult.Combination
    ) {
        val stepResults = result.stepResults.associateBy { it.techniqueId }
        day.activities.filter {
            it.isCombinationStep && it.state !in TERMINAL_STATES
        }.forEach { step ->
            val done = stepResults[step.techniqueId]
            if (done != null) {
                plans.complete(step.id, null, null, null, done.completedAt, null)
                if (isUnlocked(
                        step.techniqueId
                    )
                ) {
                    states.markIntroCompleted(step.techniqueId, done.completedAt)
                }
            } else {
                plans.skipActivity(step.id)
            }
        }
    }

    /**
     * Effect 5 inserts the mitigation into **today's** plan. A premortem completed just before midnight may
     * belong to yesterday's plan; the action then goes to today's plan when one exists, and otherwise stays
     * on the premortem's own day.
     */
    private suspend fun addMitigation(day: TrainingDay, action: String) {
        val target = plans.dayByDate(LocalDate.now(clock)) ?: day
        plans.insertActivity(
            target.id,
            PlannedActivity(
                techniqueId = TechniqueId("premortem"),
                exerciseType = ExerciseType.TEMPLATE,
                source = ActivitySource.MANUAL,
                dayPart = DayPart.DAYTIME,
                orderIndex = (target.activities.maxOfOrNull(PlanActivity::orderIndex) ?: -1) + 1,
                copyKey = "activity_mitigation",
                copyArgs = linkedMapOf("technique" to "premortem"),
                estimatedMinutes = 0,
                optional = true,
                state = ActivityState.AVAILABLE,
                scheduledAt = null,
                draft = ActivityResult.Template(mapOf("action" to BlockValue.Text(action)))
            )
        )
    }
}
