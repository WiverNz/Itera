package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.progress.Mastery
import com.wivernz.itera.domain.repository.PlannedDay
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.review.ReviewScheduler
import com.wivernz.itera.domain.unlock.ContentReconciler
import com.wivernz.itera.domain.unlock.UnlockTechniquesUseCase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * docs/engine/01-training-plan-engine.md section 3. Idempotent per date (the stored plan is returned; the
 * UNIQUE date index is the backstop against a concurrent generator) and deterministic for identical inputs.
 * A stored day is regenerated only when nothing in it was done and an older engine produced it (section 5).
 */
class GenerateDailyPlanUseCase @Inject constructor(
    private val catalog: TechniqueCatalogRepository,
    private val plans: TrainingPlanRepository,
    private val reviews: ReviewRepository,
    private val progress: ProgressRepository,
    private val states: TechniqueStateRepository,
    private val preferences: PreferencesRepository,
    private val unlock: UnlockTechniquesUseCase,
    private val rollOver: RollOverDayUseCase,
    private val advance: AdvanceProgramDayUseCase,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(date: LocalDate): TrainingDay {
        // 1. An existing plan for the date wins.
        plans.dayByDate(date)?.let { existing ->
            if (!isStale(existing)) return existing
            plans.deleteDay(existing.id)
        }
        // 2. Roll over earlier days first; a rolled-over COMPLETE day advances the program.
        rollOver(date)
        advance.reconcile()
        val prefs = preferences.preferences.first()
        if (prefs.programStartedOn == null) preferences.update { it.copy(programStartedOn = date) }
        val programDay = prefs.currentProgramDay
        // 4. Unlock whatever this program day has reached (the day's new technique included).
        unlock(programDay)
        var generated = false
        val dayId = tx.inTransaction {
            plans.dayByDate(date)?.id ?: run {
                val composed = PlanComposer.compose(inputs(programDay, date, prefs))
                generated = true
                plans.insertDay(
                    PlannedDay(
                        programDay = programDay,
                        date = date,
                        carryOverIntent = plans.latestIntent(),
                        generatorVersion = GENERATOR_VERSION,
                        activities = composed.activities
                    ),
                    clock.instant()
                )
            }
        }
        val day = checkNotNull(plans.day(dayId))
        if (generated) {
            day.activities.mapNotNull { it.reviewItemId }.distinct().forEach { id ->
                reviews.item(id)?.let { review ->
                    com.wivernz.itera.analytics.AnalyticsTechnique.of(review.techniqueId.value)?.let { technique ->
                        analytics.track(Event.ReviewDue(technique, review.stageIndex,
                            java.time.temporal.ChronoUnit.DAYS.between(review.dueOn, date).coerceAtLeast(0).toInt()))
                    }
                }
            }
            analytics.track(
                Event.PlanGenerated(
                    programDay = day.programDay,
                    activityCount = day.activities.size,
                    reviewCount = day.activities.count { it.exerciseType == ExerciseType.REVIEW },
                    hasCombination = day.activities.any {
                        it.exerciseType ==
                            ExerciseType.COMBINATION
                    },
                    generatorVersion = GENERATOR_VERSION
                )
            )
            reminders.onPlanGenerated(day.id)
        }
        return day
    }

    private suspend fun isStale(day: TrainingDay): Boolean {
        val untouched = day.activities.none {
            it.state == ActivityState.COMPLETED || it.state == ActivityState.IN_PROGRESS
        }
        val version = plans.generatorVersion(day.id) ?: GENERATOR_VERSION
        return untouched && version < GENERATOR_VERSION
    }

    private suspend fun inputs(
        programDay: Int,
        date: LocalDate,
        prefs: UserPreferences
    ): PlanInputs {
        val techniques = catalog.catalog()
        val stateById = states.all().associateBy { it.techniqueId }
        val facts = progress.techniqueFacts().associateBy { it.techniqueId }
        val mastery = techniques.associate { technique ->
            val state = stateById[technique.id]
            val fact = facts[technique.id]
            technique.id to Mastery.masteryOf(
                unlocked = state?.unlocked == true,
                introCompleted = state?.introCompletedAt != null,
                distinctDays = fact?.distinctDays ?: 0,
                totalUses = fact?.totalUses ?: 0,
                firstUse = fact?.firstUse,
                lastUse = fact?.lastUse,
                usedInCombination = fact?.usedInCombination == true
            )
        }
        val due = ReviewScheduler.dueOn(reviews.active(), date).map {
            DueReview(it, plans.programDayOfActivity(it.sourceActivityId))
        }
        return PlanInputs(
            programDay = programDay,
            curriculum = catalog.curriculum(),
            catalog = techniques,
            unlocked = stateById.values.filter { it.unlocked }.map { it.techniqueId }.toSet(),
            dueReviews = due,
            preferences = prefs,
            mastery = mastery,
            lastUsedOn = facts.values.mapNotNull { f ->
                f.lastUse?.let { f.techniqueId to it }
            }.toMap()
        )
    }
}

/** The idempotent entry point Today calls on start: reconciles content once, then ensures today's plan. */
class EnsureTodayPlanUseCase @Inject constructor(
    private val reconciler: ContentReconciler,
    private val generate: GenerateDailyPlanUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(): TrainingDay {
        reconciler.reconcile()
        return generate(LocalDate.now(clock))
    }
}
