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
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.PlannedActivity
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Looks up an activity with its day and validates [event] against the state machine. */
internal suspend fun TrainingPlanRepository.guarded(
    activityId: Long,
    event: ActivityEvent,
    clock: Clock
): Result<Pair<PlanActivity, TrainingDay>> {
    val activity = activity(activityId) ?: return failure(DomainError.ActivityNotFound)
    val day = day(activity.trainingDayId) ?: return failure(DomainError.ActivityNotFound)
    return ActivityStateMachine.transition(
        activity.state,
        event,
        clock.instant(),
        activity.transitionFacts(day, clock.zone)
    ).map { activity to day }
}

private fun analyticsId(id: TechniqueId) = AnalyticsTechnique.of(id.value)

/** AVAILABLE/SNOOZED -> IN_PROGRESS: stamps `startedAt`, clears a snooze and moves the day to IN_PROGRESS. */
class StartActivityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(activityId: Long): Result<Unit> {
        val guarded = tx.inTransaction {
            plans.guarded(activityId, ActivityEvent.Start, clock).onSuccess { (activity, day) ->
                plans.start(activity.id, clock.instant())
                if (day.status == TrainingDayStatus.PLANNED) {
                    plans.updateDayStatus(day.id, TrainingDayStatus.IN_PROGRESS, null)
                }
            }
        }
        return guarded.map { (activity, _) ->
            if (activity.state == ActivityState.SNOOZED) reminders.cancelForActivity(activity.id)
            analyticsId(activity.techniqueId)?.let {
                analytics.track(Event.ExerciseStarted(it, activity.exerciseType))
            }
        }
    }
}

/** "Not now - remind me later": the snooze must end later today. */
class SnoozeActivityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(activityId: Long, until: Instant): Result<Unit> =
        plans.guarded(activityId, ActivityEvent.Snooze(until), clock).map { (activity, _) ->
            plans.snoozeActivity(activity.id, until)
            reminders.scheduleSnooze(activity.id, until)
            analyticsId(activity.techniqueId)?.let {
                val minutes = Duration.between(clock.instant(), until).toMinutes().toInt()
                analytics.track(Event.ExerciseSnoozed(it, minutes))
            }
        }
}

/** Optional activities and the reflection ("Skip tonight"). Skipping can finish the day. */
class SkipActivityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val tx: TransactionRunner,
    private val advance: AdvanceProgramDayUseCase,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(activityId: Long): Result<Unit> {
        var dayCompleted = false
        val guarded = tx.inTransaction {
            plans.guarded(activityId, ActivityEvent.Skip, clock).onSuccess { (activity, day) ->
                val now = clock.instant()
                plans.skipActivity(activity.id)
                if (activity.source == ActivitySource.REFLECTION) {
                    plans.saveReflection(day.id, day.date, null, skipped = true, at = now)
                }
                dayCompleted = completeDayIfDone(plans, analytics, day.id, now)
            }
        }
        return guarded.map { (activity, day) ->
            reminders.cancelForActivity(activity.id)
            analyticsId(activity.techniqueId)?.let {
                analytics.track(Event.ExerciseSkipped(it, activity.exerciseType))
            }
            if (dayCompleted) advance(day.id)
        }
    }
}

/** IN_PROGRESS -> AVAILABLE: backs out without a result, keeping the draft. */
class AbandonActivityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(activityId: Long): Result<Unit> =
        plans.guarded(activityId, ActivityEvent.Abandon, clock).map { (activity, _) ->
            plans.abandon(activity.id)
            analyticsId(activity.techniqueId)?.let {
                analytics.track(
                    Event.ExerciseAbandoned(it, secondsBetween(activity.startedAt, clock.instant()))
                )
            }
        }
}

/** Autosaves a draft on an open activity. The draft's type must match the activity's exercise type. */
class SaveDraftUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val checks: RuntimeChecks
) {
    suspend operator fun invoke(activityId: Long, draft: ActivityResult): Result<Unit> {
        val activity = plans.activity(activityId) ?: return failure(DomainError.ActivityNotFound)
        if (activity.state in TERMINAL_STATES) {
            return failure(DomainError.IllegalTransition(activity.state, "SaveDraft"))
        }
        if (resultTypeOf(draft) != activity.exerciseType) {
            check(!checks.failFast) {
                "Draft ${resultTypeOf(draft)} does not match ${activity.exerciseType}"
            }
            return failure(DomainError.IllegalTransition(activity.state, "SaveDraft"))
        }
        plans.saveDraft(activityId, draft)
        return Result.success(Unit)
    }
}

/**
 * Practice from Technique detail or the library: an optional MANUAL activity in today's plan, ready to start.
 * Only unlocked techniques can be practised.
 */
class AddManualPracticeUseCase @Inject constructor(
    private val catalog: TechniqueCatalogRepository,
    private val states: TechniqueStateRepository,
    private val plans: TrainingPlanRepository,
    private val ensureToday: EnsureTodayPlanUseCase
) {
    suspend operator fun invoke(techniqueId: TechniqueId): Result<Long> {
        val technique = catalog.technique(techniqueId)
            ?.takeIf { !it.retired }
            ?: return failure(DomainError.ContentMissing(techniqueId.value))
        if (states.all().none { it.techniqueId == techniqueId && it.unlocked }) {
            return failure(DomainError.ContentMissing(techniqueId.value))
        }
        val today = ensureToday()
        val id = plans.insertActivity(
            today.id,
            PlannedActivity(
                techniqueId = technique.id,
                exerciseType = technique.exerciseType,
                source = ActivitySource.MANUAL,
                dayPart = DayPart.DAYTIME,
                orderIndex = (today.activities.maxOfOrNull(PlanActivity::orderIndex) ?: -1) + 1,
                copyKey = "activity_program",
                copyArgs = linkedMapOf(
                    "technique" to technique.id.value,
                    "minutes" to technique.estimatedMinutes
                ),
                estimatedMinutes = technique.estimatedMinutes,
                optional = true,
                state = ActivityState.AVAILABLE,
                scheduledAt = null
            )
        )
        return Result.success(id)
    }
}

/**
 * Applies the lazy `Reach` transitions for a day's plan: SCHEDULED items whose day part has arrived and SNOOZED
 * items whose snooze has passed become AVAILABLE. Today calls it on every emission and on resume.
 */
class RefreshAvailabilityUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val tx: TransactionRunner,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate = LocalDate.now(clock)): List<Long> =
        tx.inTransaction {
            val day = plans.dayByDate(date)
            val now = clock.instant()
            day?.activities.orEmpty().filter {
                (it.state == ActivityState.SCHEDULED || it.state == ActivityState.SNOOZED) &&
                    ActivityStateMachine.transition(
                        it.state,
                        ActivityEvent.Reach,
                        now,
                        it.transitionFacts(checkNotNull(day), clock.zone)
                    ).isSuccess
            }.map {
                plans.makeAvailable(it.id)
                it.id
            }
        }
}
