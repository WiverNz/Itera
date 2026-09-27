package com.wivernz.itera.debug

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.core.common.RuntimeChecks
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.demo.DemoDataLoader
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CombinationStepResult
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.repository.LearningTopicRepository
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.review.ScheduleReviewUseCase
import com.wivernz.itera.domain.review.SubmitReviewUseCase
import com.wivernz.itera.domain.training.AdvanceProgramDayUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.EraseAllDataUseCase
import com.wivernz.itera.domain.training.GenerateDailyPlanUseCase
import com.wivernz.itera.domain.training.RefreshAvailabilityUseCase
import com.wivernz.itera.domain.training.RollOverDayUseCase
import com.wivernz.itera.domain.unlock.UnlockTechniquesUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/** A clock the loader moves through past days, so the real engine writes a real history. */
private class SteppedClock(private val zone: ZoneId, var now: Instant) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = SteppedClock(zone, now)
    override fun instant(): Instant = now
    fun at(date: LocalDate, time: LocalTime) {
        now = date.atTime(time).atZone(zone).toInstant()
    }
}

/**
 * Debug-only (D-11): erases local data, then trains Days 1-8 over the last nine calendar days with one rest day,
 * like the prototype's `loadDemo()`. The engine runs unchanged against a stepped clock; today's Day 9 plan is left
 * for Today to generate. All text written here is synthetic.
 */
class DebugDemoDataLoader @Inject constructor(
    private val erase: EraseAllDataUseCase,
    private val catalog: TechniqueCatalogRepository,
    private val plans: TrainingPlanRepository,
    private val reviews: ReviewRepository,
    private val progress: ProgressRepository,
    private val states: TechniqueStateRepository,
    private val records: PracticeRecordRepository,
    private val topics: LearningTopicRepository,
    private val preferences: PreferencesRepository,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val checks: RuntimeChecks,
    private val system: Clock
) : DemoDataLoader {
    override suspend fun load() {
        erase()
        val today = LocalDate.now(system)
        val clock = SteppedClock(system.zone, system.instant())
        val unlock = UnlockTechniquesUseCase(catalog, states, tx, analytics, clock)
        val advance = AdvanceProgramDayUseCase(plans, preferences, analytics)
        val rollOver = RollOverDayUseCase(plans, tx, advance, reminders, analytics, clock)
        val generate = GenerateDailyPlanUseCase(
            catalog, plans, reviews, progress, states, preferences, unlock, rollOver, advance, tx,
            reminders, analytics, clock
        )
        val complete = CompleteActivityUseCase(
            plans, states, records, ScheduleReviewUseCase(reviews, catalog, tx, clock), advance, tx,
            reminders, analytics, clock, checks,
            com.wivernz.itera.analytics.ProgressionEvents(
                com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase(
                    progress,
                    states,
                    catalog
                ),
                com.wivernz.itera.domain.progress.ObserveProgressUseCase(
                    progress,
                    catalog,
                    preferences,
                    clock
                ),
                analytics
            )
        )
        val submitReview = SubmitReviewUseCase(plans, reviews, complete, tx, analytics, clock)
        val refresh = RefreshAvailabilityUseCase(plans, tx, clock)

        preferences.update {
            it.copy(
                onboardingCompleted = true,
                focusAreas = setOf(Skill.FOCUS, Skill.LEARNING),
                programStartedOn = today.minusDays(DAYS_BACK)
            )
        }
        for (offset in DAYS_BACK downTo 1) {
            if (offset == REST_DAY_OFFSET) continue
            val date = today.minusDays(offset)
            clock.at(date, LocalTime.of(8, 0))
            val day = generate(date)
            clock.at(date, LocalTime.of(20, 30))
            refresh(date)
            val open = checkNotNull(plans.day(day.id)).activities
                .filter { it.state != ActivityState.COMPLETED && it.state != ActivityState.SKIPPED }
                .sortedBy { if (it.exerciseType == ExerciseType.COMBINATION) 1 else 0 }
            for (activity in open) {
                val current = plans.activity(activity.id) ?: continue
                if (current.state == ActivityState.COMPLETED ||
                    current.state == ActivityState.SKIPPED
                ) {
                    continue
                }
                if (current.exerciseType == ExerciseType.REVIEW) {
                    submitReview(current.id, DEMO_TEXT, RecallGrade.SOLID)
                } else {
                    complete(current.id, resultFor(current, clock))
                }
            }
        }
        reminders.rescheduleAll()
    }

    private suspend fun resultFor(activity: PlanActivity, clock: Clock): ActivityResult =
        when (activity.exerciseType) {
            ExerciseType.TEMPLATE -> ActivityResult.Template(
                mapOf(
                    "tasks" to BlockValue.Items(
                        listOf(
                            ChecklistItem("1", "Reply to Anna", true, 70),
                            ChecklistItem("2", "Book the dentist", true, 90)
                        )
                    ),
                    "steps" to BlockValue.Choice(listOf("Outline", "Draft", "Polish"), 0),
                    "item" to BlockValue.Text("RDB snapshots vs AOF logs")
                )
            )
            ExerciseType.FOCUS_TIMER -> ActivityResult.Focus(
                "Write the report",
                1500,
                1500,
                0,
                true
            )
            ExerciseType.EISENHOWER -> ActivityResult.Eisenhower(
                listOf(EisenhowerItem("1", "Q4 roadmap", Quadrant.DO_NOW)),
                "1"
            )
            ExerciseType.FEYNMAN -> {
                val topic = topics.add("Redis persistence")
                ActivityResult.Feynman(
                    topic,
                    "Redis persistence",
                    "Redis keeps data in memory and saves snapshots or an append-only log to disk.",
                    14,
                    emptyList(),
                    null
                )
            }
            ExerciseType.PREMORTEM -> ActivityResult.Premortem(
                "Launch",
                listOf(PremortemReason("Scope grew", Likelihood.LIKELY)),
                "Cut one feature",
                false
            )
            ExerciseType.HABIT_STACK -> ActivityResult.HabitStack(
                "make coffee",
                "read one page",
                false,
                null
            )
            ExerciseType.REFLECTION -> ActivityResult.Reflection(
                "Finished the focus block before lunch",
                emptyList(),
                null,
                listOf("started_late"),
                ""
            )
            ExerciseType.REVIEW -> ActivityResult.Review(
                activity.reviewItemId ?: 0,
                DEMO_TEXT,
                RecallGrade.SOLID,
                ""
            )
            ExerciseType.COMBINATION -> ActivityResult.Combination(
                checkNotNull(plans.day(activity.trainingDayId)).activities
                    .filter { it.isCombinationStep }
                    .map { CombinationStepResult(it.techniqueId, "done", clock.instant()) }
            )
        }

    private companion object {
        const val DAYS_BACK = 9L
        const val REST_DAY_OFFSET = 3L
        const val DEMO_TEXT = "Snapshots and an append-only log"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugDemoModule {
    @Binds
    abstract fun demoDataLoader(impl: DebugDemoDataLoader): DemoDataLoader
}
