package com.wivernz.itera.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.LocalAnalytics
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.core.common.RuntimeChecks
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.data.catalog.AssetTechniqueCatalogRepository
import com.wivernz.itera.data.catalog.CatalogAssetSource
import com.wivernz.itera.data.catalog.CatalogCache
import com.wivernz.itera.data.catalog.CatalogStrings
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.fileAssets
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.repository.RoomDataResetRepository
import com.wivernz.itera.data.repository.RoomLearningTopicRepository
import com.wivernz.itera.data.repository.RoomPracticeRecordRepository
import com.wivernz.itera.data.repository.RoomProgressRepository
import com.wivernz.itera.data.repository.RoomReviewRepository
import com.wivernz.itera.data.repository.RoomTechniqueStateRepository
import com.wivernz.itera.data.repository.RoomTrainingPlanRepository
import com.wivernz.itera.data.repository.RoomTransactionRunner
import com.wivernz.itera.data.testDatabase
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CombinationStepResult
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.progress.ObserveProgressUseCase
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.review.ScheduleReviewUseCase
import com.wivernz.itera.domain.review.SubmitReviewUseCase
import com.wivernz.itera.domain.training.AbandonActivityUseCase
import com.wivernz.itera.domain.training.AddManualPracticeUseCase
import com.wivernz.itera.domain.training.AdvanceProgramDayUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import com.wivernz.itera.domain.training.EraseAllDataUseCase
import com.wivernz.itera.domain.training.GenerateDailyPlanUseCase
import com.wivernz.itera.domain.training.RefreshAvailabilityUseCase
import com.wivernz.itera.domain.training.ResetProgramUseCase
import com.wivernz.itera.domain.training.RollOverDayUseCase
import com.wivernz.itera.domain.training.SaveDraftUseCase
import com.wivernz.itera.domain.training.SkipActivityUseCase
import com.wivernz.itera.domain.training.SnoozeActivityUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import com.wivernz.itera.domain.unlock.ContentReconciler
import com.wivernz.itera.domain.unlock.UnlockTechniquesUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Catalogue strings that resolve every key to itself, so JVM tests need no resources. */
class KeyStrings : CatalogStrings {
    override val locale: Locale = Locale.ENGLISH
    override fun text(key: String) = key
    override fun array(key: String) = listOf(key)
}

class MemoryCatalogCache : CatalogCache {
    var entry: Pair<String, String>? = null
    override suspend fun read() = entry
    override suspend fun write(techniques: String, curriculum: String) {
        entry = techniques to curriculum
    }
}

fun testCatalogRepository(
    assets: CatalogAssetSource = fileAssets,
    failFast: Boolean = true,
    cache: CatalogCache = MemoryCatalogCache()
) = AssetTechniqueCatalogRepository(
    assets,
    KeyStrings(),
    cache,
    RuntimeChecks(failFast),
    TestLogger(),
    Dispatchers.Unconfined
)

class FakePreferences(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences = state
    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        state.value = transform(state.value)
    }
    override suspend fun clear() {
        state.value = UserPreferences()
    }
}

class RecordingReminders : ReminderScheduler {
    val calls = mutableListOf<String>()
    override suspend fun rescheduleAll() {
        calls += "rescheduleAll"
    }
    override suspend fun cancelAll() {
        calls += "cancelAll"
    }
    override suspend fun onPlanGenerated(dayId: Long) {
        calls += "plan:$dayId"
    }
    override suspend fun scheduleSnooze(activityId: Long, at: Instant) {
        calls += "snooze:$activityId"
    }
    override suspend fun cancelForActivity(activityId: Long) {
        calls += "cancel:$activityId"
    }
    override suspend fun scheduleHabitNudge(habitStackId: Long) {
        calls += "habit:$habitStackId"
    }
}

class FakeFocusTimer : FocusTimerRepository {
    var cleared = 0
    override fun observe() = MutableStateFlow<FocusTimerState?>(null)
    override suspend fun save(state: FocusTimerState) = Unit
    override suspend fun clear() {
        cleared++
    }
    override suspend fun restore() = FocusTimerRestore.None
}

/** Berlin, Saturday 2026-03-28 08:00 local; DST starts the next night. */
val HARNESS_START: Instant = Instant.parse("2026-03-28T07:00:00Z")

/** The whole engine over a real in-memory Room database seeded from the real catalogue. */
class EngineHarness(
    val clock: FakeClock = FakeClock(HARNESS_START),
    initialPreferences: UserPreferences = UserPreferences(onboardingCompleted = true),
    val db: IteraDatabase = testDatabase(),
    failFast: Boolean = true,
    recordsOverride: ((PracticeRecordRepository) -> PracticeRecordRepository)? = null
) {
    private val io = Dispatchers.IO
    private val context = ApplicationProvider.getApplicationContext<Context>()
    val codec = ResultPayloadCodec(TestLogger())
    val copy = CopyResolver(context, clock, TestLogger())
    val checks = RuntimeChecks(failFast)
    val catalog = testCatalogRepository()
    val prefs = FakePreferences(initialPreferences)
    val reminders = RecordingReminders()
    val focusTimer = FakeFocusTimer()
    val plans = RoomTrainingPlanRepository(db, copy, codec, clock, io)
    val states = RoomTechniqueStateRepository(db.techniqueStateDao(), io)
    val reviews = RoomReviewRepository(db.reviewItemDao(), db.reviewAttemptDao(), io)
    val progress =
        RoomProgressRepository(db.planActivityDao(), db.focusSessionDao(), copy, codec, io)
    val topics = RoomLearningTopicRepository(db.learningTopicDao(), clock, io)
    val records: PracticeRecordRepository = RoomPracticeRecordRepository(
        db.focusSessionDao(),
        db.habitStackDao(),
        io
    ).let { recordsOverride?.invoke(it) ?: it }
    val reset = RoomDataResetRepository(db.resetDao())
    val tx = RoomTransactionRunner(db)
    private val local = LocalAnalytics(db.eventLogDao(), clock, io)

    /** Synchronous, so event_log assertions never race the fire-and-forget writer. */
    val analytics = object : Analytics {
        override fun track(event: Event) = runBlocking { local.append(event) }
        override suspend fun append(event: Event) = local.append(event)
    }

    val unlock = UnlockTechniquesUseCase(catalog, states, tx, analytics, clock)
    val reconciler = ContentReconciler(catalog, prefs, unlock)
    val advance = AdvanceProgramDayUseCase(plans, prefs, analytics)
    val rollOver = RollOverDayUseCase(plans, tx, advance, reminders, analytics, clock)
    val generate = GenerateDailyPlanUseCase(
        catalog, plans, reviews, progress, states, prefs, unlock, rollOver, advance, tx, reminders,
        analytics, clock
    )
    val ensureToday = EnsureTodayPlanUseCase(reconciler, generate, clock)
    val scheduleReview = ScheduleReviewUseCase(reviews, catalog, tx, clock)
    val complete = CompleteActivityUseCase(
        plans, states, records, scheduleReview, advance, tx, reminders, analytics, clock, checks
    )
    val submitReview = SubmitReviewUseCase(plans, reviews, complete, tx, analytics, clock)
    val start = StartActivityUseCase(plans, tx, reminders, analytics, clock)
    val snooze = SnoozeActivityUseCase(plans, reminders, analytics, clock)
    val skip = SkipActivityUseCase(plans, tx, advance, reminders, analytics, clock)
    val abandon = AbandonActivityUseCase(plans, analytics, clock)
    val saveDraft = SaveDraftUseCase(plans, checks)
    val addManual = AddManualPracticeUseCase(catalog, states, plans, ensureToday)
    val refresh = RefreshAvailabilityUseCase(plans, tx, clock)
    val resetProgram =
        ResetProgramUseCase(reset, states, prefs, focusTimer, tx, reminders, analytics, clock)
    val eraseAll = EraseAllDataUseCase(reset, states, prefs, focusTimer, tx, reminders, clock)
    val observeProgress = ObserveProgressUseCase(progress, catalog, prefs, clock)
    val observeTechniques = ObserveTechniqueProgressUseCase(progress, states, catalog)

    val today: LocalDate get() = LocalDate.now(clock)

    fun at(time: LocalTime, date: LocalDate = today) {
        clock.setDate(date)
        val midnight = date.atStartOfDay(clock.zone).toInstant()
        clock.advance(
            java.time.Duration.between(midnight, date.atTime(time).atZone(clock.zone).toInstant())
        )
    }

    /** Moves to the next calendar day at 08:00. */
    fun nextMorning() = at(LocalTime.of(8, 0), today.plusDays(1))

    suspend fun programDay() = prefs.state.value.currentProgramDay

    suspend fun mastery(id: String): MasteryLevel =
        observeTechniques().first().first { it.techniqueId.value == id }.level

    /** Completes every open activity of today's plan in the evening, reviews through SubmitReview. */
    suspend fun trainFullDay(): TrainingDay {
        val day = ensureToday()
        at(LocalTime.of(20, 30))
        refresh()
        val ordered = checkNotNull(plans.day(day.id)).activities
            .sortedBy { if (it.exerciseType == ExerciseType.COMBINATION) 1 else 0 }
        for (activity in ordered) {
            val current = checkNotNull(plans.activity(activity.id))
            if (current.state == ActivityState.COMPLETED ||
                current.state == ActivityState.SKIPPED
            ) {
                continue
            }
            if (current.exerciseType == ExerciseType.REVIEW) {
                submitReview(current.id, "answer", RecallGrade.SOLID).getOrThrow()
            } else {
                complete(current.id, resultFor(current)).getOrThrow()
            }
        }
        return checkNotNull(plans.day(day.id))
    }

    suspend fun resultFor(activity: PlanActivity): ActivityResult = when (activity.exerciseType) {
        ExerciseType.TEMPLATE -> ActivityResult.Template(
            mapOf(
                "tasks" to BlockValue.Items(
                    listOf(ChecklistItem("1", "a", true, 30), ChecklistItem("2", "b", true, 40))
                ),
                "steps" to BlockValue.Choice(listOf("a", "b", "c"), 0),
                "item" to BlockValue.Text("a fact to keep")
            )
        )
        ExerciseType.FOCUS_TIMER -> ActivityResult.Focus("task", 1500, 1500, 0, true)
        ExerciseType.EISENHOWER -> ActivityResult.Eisenhower(
            listOf(EisenhowerItem("1", "roadmap", Quadrant.DO_NOW)),
            "1"
        )
        ExerciseType.FEYNMAN -> {
            val topic = topics.add("Redis persistence")
            ActivityResult.Feynman(
                topic,
                "Redis persistence",
                "RDB snapshots and AOF logs",
                5,
                emptyList(),
                null
            )
        }
        ExerciseType.PREMORTEM -> ActivityResult.Premortem(
            "launch",
            listOf(PremortemReason("scope", Likelihood.LIKELY)),
            "cut scope",
            false
        )
        ExerciseType.HABIT_STACK -> ActivityResult.HabitStack(
            "coffee",
            "read a page",
            true,
            LocalTime.of(8, 0)
        )
        ExerciseType.REFLECTION -> ActivityResult.Reflection(
            "focus",
            emptyList(),
            null,
            emptyList(),
            "start early"
        )
        ExerciseType.REVIEW -> ActivityResult.Review(
            activity.reviewItemId ?: 0,
            "answer",
            RecallGrade.SOLID,
            ""
        )
        ExerciseType.COMBINATION -> ActivityResult.Combination(
            checkNotNull(plans.day(activity.trainingDayId)).activities.filter {
                it.isCombinationStep
            }.map {
                CombinationStepResult(it.techniqueId, "done", clock.instant())
            }
        )
    }

    suspend fun eventNames(): List<String> = db.eventLogDao().exportSince(0).map { it.name }

    fun close() = db.close()
}

fun id(value: String) = TechniqueId(value)
