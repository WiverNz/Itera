package com.wivernz.itera.feature.exercise.runner

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.AbandonActivityUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.SaveDraftUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** What every specialised exercise body needs from the engine. */
class ExerciseSessionDeps @Inject constructor(
    val plans: TrainingPlanRepository,
    val catalog: TechniqueCatalogRepository,
    val start: StartActivityUseCase,
    val abandon: AbandonActivityUseCase,
    val complete: CompleteActivityUseCase,
    val saveDraft: SaveDraftUseCase,
    val analytics: Analytics
)

/**
 * The host behaviour shared by the specialised bodies (ADR-0007): opening starts the activity, drafts autosave on
 * a 2 s debounce and flush on `ON_STOP`, leaving keeps the draft and makes the activity available again, and
 * completing goes through `CompleteActivityUseCase`.
 */
class ExerciseSession(
    private val deps: ExerciseSessionDeps,
    val activityId: Long,
    scope: CoroutineScope
) {
    class Loaded(
        val activity: PlanActivity,
        val technique: Technique,
        val day: TrainingDay?,
        val draft: ActivityResult?
    )

    private val autosave = DraftAutosave<ActivityResult>(scope) { deps.saveDraft(activityId, it) }
    private var completed = false

    init {
        deps.analytics.track(Event.ScreenViewed(ScreenRoute.EXERCISE))
    }

    /** Loads the activity and starts it if it was only available; null for an unknown id. */
    suspend fun load(): Loaded? {
        val activity = deps.plans.activity(activityId) ?: return null
        val technique = deps.catalog.technique(activity.techniqueId) ?: return null
        if (activity.state == ActivityState.AVAILABLE || activity.state == ActivityState.SNOOZED) {
            deps.start(activityId)
        }
        completed = activity.state == ActivityState.COMPLETED
        return Loaded(
            deps.plans.activity(activityId) ?: activity,
            technique,
            deps.plans.day(activity.trainingDayId),
            deps.plans.draft(activityId)
        )
    }

    fun draft(result: ActivityResult) {
        if (!completed) autosave.schedule(result)
    }

    fun flush() {
        if (!completed) autosave.flush()
    }

    /** Back out without a result: the draft stays, the activity is AVAILABLE again. */
    suspend fun leave(latest: ActivityResult?) {
        autosave.cancel()
        if (completed) return
        latest?.let { deps.saveDraft(activityId, it) }
        if (deps.plans.activity(activityId)?.state == ActivityState.IN_PROGRESS) {
            deps.abandon(activityId)
        }
    }

    suspend fun complete(
        result: ActivityResult,
        difficulty: Difficulty? = null,
        note: String? = null
    ): Result<Unit> {
        autosave.cancel()
        return deps.complete(activityId, result, difficulty, note).onSuccess { completed = true }
    }
}
