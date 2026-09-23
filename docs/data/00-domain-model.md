# Domain model

Pure Kotlin. No Android imports, no Room annotations, no `kotlinx.serialization` annotations on domain types (serialization lives on DTOs in `data`). Everything here is unit-testable on the JVM.

Package: `com.wivernz.itera.domain.model`

## 1. Enumerations

```kotlin
enum class Skill { FOCUS, PLANNING, LEARNING, HABITS, REFLECTION }

/** Which UI body renders the exercise. One-to-one with a composable in feature/exercise. */
enum class ExerciseType {
    TEMPLATE,      // data-driven blocks (see section 4)
    FOCUS_TIMER,   // Pomodoro, Deep Work
    EISENHOWER,
    FEYNMAN,
    PREMORTEM,
    HABIT_STACK,
    REFLECTION,
    REVIEW,
    COMBINATION
}

enum class ActivityState { SCHEDULED, AVAILABLE, IN_PROGRESS, SNOOZED, COMPLETED, SKIPPED, EXPIRED }

/** Why this activity is in today's plan. Drives ordering and copy. */
enum class ActivitySource {
    PROGRAM,          // the day's new technique
    REVIEW,           // a due spaced-repetition review
    PRACTICE_PROMPT,  // "Keep using: X - tap to log when you use it"
    FOCUS_SUGGESTION, // the optional daytime focus block
    REFLECTION,       // the nightly reflection
    COMBINATION,      // a combination-day chain
    MANUAL            // user-initiated from Technique detail or Library
}

enum class DayPart { MORNING, DAYTIME, EVENING }

enum class Difficulty { EASY, OKAY, HARD }          // "How did it feel?"

enum class MasteryLevel { NONE, MET, PRACTICED, APPLIED, INTEGRATED }

enum class SkillLevel { STARTING, BUILDING, STEADY, STRONG }

enum class RecallGrade { FORGOT, PARTIAL, SOLID }   // spaced-repetition self-grade

enum class TrainingDayStatus { PLANNED, IN_PROGRESS, COMPLETE, ABANDONED }

enum class ProgramPace { GENTLE, STANDARD, INTENSE }

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class TimeBudget(val minutes: Int) { SHORT(5), STANDARD(15), LONG(30) }
```

`ActivityState` includes `SNOOZED` per R-12.

## 2. Catalog types (read-only content)

Loaded from the bundled catalog asset; never written at runtime.

```kotlin
data class Technique(
    val id: TechniqueId,                 // value class over String, e.g. "two_minute_rule"
    val name: String,                    // "2-minute rule"
    val shortDescription: String,        // "Clear small tasks before they accumulate."
    val explanation: String,             // "Why it helps" body on Technique detail
    val skill: Skill,                    // one-to-one (R-03)
    val introDay: Int?,                  // 1..13, null for Daily reflection (always available)
    val exerciseType: ExerciseType,
    val estimatedMinutes: Int,
    val reviewEligible: Boolean,         // only Feynman + Spaced repetition in the MVP
    val relatedTechniqueIds: List<TechniqueId>,   // "Works well with"
    val template: ExerciseTemplate?,     // required when exerciseType == TEMPLATE
    val defaults: TechniqueDefaults      // type-specific seed data (focus length, chips, ...)
)

@JvmInline value class TechniqueId(val value: String)

data class TechniqueDefaults(
    val focusMinutes: Int? = null,           // Pomodoro 25, Deep Work 50
    val breakMinutes: Int? = null,           // Pomodoro 5
    val suggestionChips: List<String> = emptyList(),
    val anchorSuggestions: List<String> = emptyList(),  // Habit stacking
    val habitSuggestions: List<String> = emptyList()
)

data class Curriculum(
    val version: Int,
    val days: List<CurriculumDay>
)

data class CurriculumDay(
    val day: Int,                                  // 1-based program day
    val newTechniqueId: TechniqueId?,              // null on a combination day
    val combination: List<CombinationStep>,        // empty unless this is a combination day
    val weeklyLookBack: Boolean                    // Day 7 and every 7th day after
)

data class CombinationStep(
    val techniqueId: TechniqueId,
    val prompt: String,                            // "Which part moves it most?"
    val hint: String                               // "Pick the one step that gives most of the result."
)
```

## 3. Plan and activity

```kotlin
data class TrainingDay(
    val id: Long,
    val programDay: Int,          // advances only on completion (R-06)
    val date: LocalDate,          // the calendar day this plan was generated for
    val status: TrainingDayStatus,
    val carryOverIntent: String?, // last night's "What will you change tomorrow?"
    val activities: List<PlanActivity>,
    val completedAt: Instant?
) {
    val completedCount: Int get() = activities.count { it.isCountedComplete }
    val requiredCount: Int get() = activities.count { it.countsTowardDay }
}

data class PlanActivity(
    val id: Long,
    val trainingDayId: Long,
    val techniqueId: TechniqueId,
    val exerciseType: ExerciseType,
    val source: ActivitySource,
    val orderIndex: Int,
    val dayPart: DayPart,
    val title: String,            // resolved copy, already localised at read time
    val subtitle: String,         // "Now - 5 min", "21:00 - 2 min", "Due today - 5 min - from Day 6"
    val instruction: String,
    val estimatedMinutes: Int,
    val state: ActivityState,
    val optional: Boolean,        // focus suggestions and practice prompts are optional
    val scheduledAt: LocalTime?,  // for snooze and notification targeting
    val snoozedUntil: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val durationSeconds: Int?,
    val difficulty: Difficulty?,
    val note: String?,
    val result: ActivityResult?,
    val reviewItemId: Long?       // set when source == REVIEW
) {
    val countsTowardDay: Boolean get() = !optional
    val isCountedComplete: Boolean get() = state == ActivityState.COMPLETED
}
```

`title`, `subtitle` and `instruction` are **resolved strings**, not raw content. The repository resolves them from string resources plus catalog content when mapping the entity to the domain type, so the UI never formats copy and localisation stays in `res/values`. The entity stores a template key plus arguments (see `01-room-schema.md`).

## 4. Template exercises

`ExerciseType.TEMPLATE` is a data-driven body. It covers 5-second rule, 2-minute rule, Information diet, 1% improvement, 80/20 principle and Two-list strategy without a bespoke screen each (guardrail: "Prefer a data-driven exercise model where practical").

```kotlin
data class ExerciseTemplate(
    val blocks: List<ExerciseBlock>,
    val completionRule: CompletionRule
)

sealed interface ExerciseBlock {
    val key: String

    /** Static guidance paragraph. */
    data class Instruction(override val key: String, val text: String, val emphasis: Boolean) : ExerciseBlock

    /** Free text. Autosaved as a draft. */
    data class TextInput(
        override val key: String,
        val label: String,
        val placeholder: String,
        val minLines: Int,
        val maxLines: Int
    ) : ExerciseBlock

    /** Tick-off list the user fills in themselves. `withStopwatch` powers the 2-minute rule. */
    data class Checklist(
        override val key: String,
        val label: String,
        val minItems: Int,
        val maxItems: Int,
        val withStopwatch: Boolean,
        val addItemLabel: String
    ) : ExerciseBlock

    /** User enters several items, then picks exactly one. Powers 80/20. */
    data class PickOne(
        override val key: String,
        val label: String,
        val itemCount: Int,
        val suggestions: List<String>
    ) : ExerciseBlock

    /** Two named lists. Powers the Two-list strategy. */
    data class TwoLists(
        override val key: String,
        val primaryLabel: String,   // "Top 5"
        val secondaryLabel: String, // "Avoid at all costs"
        val primaryCount: Int,
        val secondaryCount: Int
    ) : ExerciseBlock

    /** Multi-select suggestion chips plus an optional free-text escape hatch. */
    data class ChipSelect(
        override val key: String,
        val label: String,
        val options: List<String>,
        val allowCustom: Boolean,
        val maxSelections: Int
    ) : ExerciseBlock
}

sealed interface CompletionRule {
    /** Always completable - a single "Done" tap. Used by practice prompts. */
    data object Always : CompletionRule
    /** Every listed block key must be non-empty. */
    data class RequireBlocks(val keys: List<String>) : CompletionRule
    /** A Checklist block must have at least N ticked items. */
    data class RequireChecked(val key: String, val count: Int) : CompletionRule
}
```

## 5. Results

One sealed hierarchy. Persisted as JSON with a type discriminator (ADR-0006).

```kotlin
sealed interface ActivityResult {

    data class Template(
        val values: Map<String, BlockValue>   // keyed by ExerciseBlock.key
    ) : ActivityResult

    data class Focus(
        val taskLabel: String,
        val plannedSeconds: Int,
        val actualSeconds: Int,
        val extendedSeconds: Int,
        val completedNaturally: Boolean       // false if ended early
    ) : ActivityResult

    data class Eisenhower(
        val items: List<EisenhowerItem>,
        val chosenItemId: String?             // the "Do now" item carried into the next step
    ) : ActivityResult

    data class Feynman(
        val topicId: Long,
        val topicTitle: String,
        val explanation: String,
        val wordCount: Int,
        val hardestParts: List<String>,       // chips + custom
        val reflectionNote: String?
    ) : ActivityResult

    data class Premortem(
        val projectName: String,
        val reasons: List<PremortemReason>,
        val mitigationAction: String?,
        val mitigationAddedToToday: Boolean
    ) : ActivityResult

    data class HabitStack(
        val anchor: String,                   // "make coffee"
        val habit: String,                    // "read one page"
        val nudgeEnabled: Boolean,
        val nudgeTime: LocalTime?
    ) : ActivityResult

    data class Reflection(
        val wentWell: String?,
        val wentWellChips: List<String>,
        val didNotGoWell: String?,
        val didNotGoWellChips: List<String>,
        val tomorrowChange: String?
    ) : ActivityResult

    data class Review(
        val reviewItemId: Long,
        val answer: String,
        val grade: RecallGrade,
        val previousAnswer: String
    ) : ActivityResult

    data class Combination(
        val stepResults: List<CombinationStepResult>
    ) : ActivityResult
}

sealed interface BlockValue {
    data class Text(val text: String) : BlockValue
    data class Items(val items: List<ChecklistItem>) : BlockValue
    data class Choice(val options: List<String>, val chosenIndex: Int) : BlockValue
    data class Lists(val primary: List<String>, val secondary: List<String>) : BlockValue
    data class Chips(val selected: List<String>, val custom: String?) : BlockValue
}

data class ChecklistItem(val id: String, val label: String, val done: Boolean, val elapsedSeconds: Int?)
data class EisenhowerItem(val id: String, val label: String, val quadrant: Quadrant)
enum class Quadrant { UNSORTED, DO_NOW, SCHEDULE, DELEGATE, DROP }
data class PremortemReason(val text: String, val likelihood: Likelihood)
enum class Likelihood { POSSIBLE, LIKELY, CERTAIN }
data class CombinationStepResult(val techniqueId: TechniqueId, val summary: String, val completedAt: Instant)
```

## 6. Spaced repetition

```kotlin
data class ReviewItem(
    val id: Long,
    val techniqueId: TechniqueId,
    val topicId: Long?,              // Feynman reviews are bound to a learning topic
    val prompt: String,              // "4 days ago you explained Redis persistence."
    val sourceActivityId: Long,      // the activity that created this item
    val sourceAnswer: String,        // hidden until the review is submitted
    val stageIndex: Int,             // 0..4 -> intervals 1, 4, 9, 21, 60
    val dueOn: LocalDate,
    val lastReviewedOn: LocalDate?,
    val state: ReviewState
)

enum class ReviewState { SCHEDULED, DUE, COMPLETED_STAGE, RETIRED }

data class LearningTopic(
    val id: Long,
    val title: String,               // "Explain what Redis persistence is"
    val createdAt: Instant,
    val archived: Boolean
)
```

## 7. Progress (all derived, never stored - ADR-0013)

```kotlin
data class TechniqueProgress(
    val techniqueId: TechniqueId,
    val unlocked: Boolean,
    val unlocksOnDay: Int?,          // shown as "Day 11" on locked library rows
    val level: MasteryLevel,
    val distinctPracticeDays: Int,
    val totalUses: Int,
    val firstUsedOn: LocalDate?,
    val lastUsedOn: LocalDate?,
    val usedInCombination: Boolean,
    val nextLevelHint: String        // "Practice it on 3 different days to reach Practiced."
)

data class SkillProgress(
    val skill: Skill,
    val level: SkillLevel,
    val practiceCount: Int,
    val daysPracticed: Int,
    val windowDays: Int,             // 14
    val detail: String               // plural: "11 practices"
)

data class ProgressSummary(
    val trainedDays: Int,            // in the trailing window
    val windowDays: Int,             // grows to 14, then caps
    val windowStart: LocalDate,
    val dayDots: List<DayDot>,
    val skills: List<SkillProgress>,
    val activityCount: Int           // "41 activities - notes and reflections"
)

/** One bar in the Progress day strip. Rendered as a bar, not a dot - see docs/ux/02-screen-specs-train-progress-you.md. */
data class DayDot(val date: LocalDate, val trained: Boolean, val skills: Set<Skill>)
```

## 8. Preferences

```kotlin
data class UserPreferences(
    val onboardingCompleted: Boolean,
    val focusAreas: Set<Skill>,          // 1-2 selected at onboarding
    val morningTime: LocalTime,          // default 08:30
    val eveningTime: LocalTime,          // default 21:00
    val timeBudget: TimeBudget,          // default STANDARD (15)
    val pace: ProgramPace,               // default STANDARD
    val theme: ThemePreference,          // default SYSTEM
    val notifyMorning: Boolean,
    val notifyFocus: Boolean,
    val notifyReviews: Boolean,
    val notifyEvening: Boolean,
    val aiCoachEnabled: Boolean,         // always false in the MVP
    val programStartedOn: LocalDate?,
    val currentProgramDay: Int,          // 1-based
    val contentVersion: Int
)
```

## 9. Repository interfaces

Declared in `domain/repository`, implemented in `data`. All reads are `Flow`; all writes are `suspend`.

```kotlin
interface TechniqueCatalogRepository {
    suspend fun catalog(): List<Technique>
    suspend fun technique(id: TechniqueId): Technique?
    suspend fun curriculum(): Curriculum
}

interface TrainingPlanRepository {
    fun observeToday(): Flow<TrainingDay?>
    fun observeDay(id: Long): Flow<TrainingDay?>
    suspend fun ensurePlanFor(date: LocalDate): TrainingDay
    suspend fun updateActivityState(activityId: Long, state: ActivityState): Unit
    suspend fun saveDraft(activityId: Long, draft: ActivityResult): Unit
    suspend fun completeActivity(activityId: Long, result: ActivityResult, difficulty: Difficulty?, note: String?): Unit
    suspend fun snoozeActivity(activityId: Long, until: Instant): Unit
    suspend fun skipActivity(activityId: Long): Unit
    suspend fun completeDay(dayId: Long): Unit
    suspend fun addManualPractice(techniqueId: TechniqueId): Long
}

interface ReviewRepository {
    fun observeDue(on: LocalDate): Flow<List<ReviewItem>>
    fun observeUpcoming(): Flow<List<ReviewItem>>
    suspend fun item(id: Long): ReviewItem?
    suspend fun schedule(techniqueId: TechniqueId, topicId: Long?, prompt: String, answer: String, sourceActivityId: Long): Long
    suspend fun submit(reviewItemId: Long, answer: String, grade: RecallGrade): Unit
}

interface ProgressRepository {
    fun observeSummary(): Flow<ProgressSummary>
    fun observeTechniqueProgress(): Flow<List<TechniqueProgress>>
    fun observeTechniqueProgress(id: TechniqueId): Flow<TechniqueProgress>
    fun observeHistory(month: YearMonth): Flow<List<HistoryEntry>>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences): Unit
}

interface LearningTopicRepository {
    fun observeTopics(): Flow<List<LearningTopic>>
    suspend fun add(title: String): Long
    suspend fun rename(id: Long, title: String): Unit
    suspend fun archive(id: Long): Unit
    suspend fun nextTopicForReview(): LearningTopic?
}
```

## 10. Invariants

Enforced by the engine and covered by unit tests (`docs/testing/01-test-matrix.md`).

1. A `TrainingDay` has exactly one `PROGRAM` or `COMBINATION` activity.
2. A `TrainingDay` has exactly one `REFLECTION` activity, always `dayPart = EVENING`.
3. A `TrainingDay` has at most one `FOCUS_SUGGESTION`.
4. A standard day has three activities - program, focus-or-practice, reflection - so the user-visible counter reads "0 / 3", matching the prototype. A day with a due review has four. `requiredCount`, used for day completion, counts only non-optional activities and is a different number.
5. An activity may reference a `reviewItemId` only when `source == REVIEW`.
6. `programDay` is monotonically non-decreasing and increases by at most 1 per completed day.
7. A technique is practisable only when `unlocked`; the engine never emits a locked technique into a plan.
8. `stageIndex` never decreases below 0 and never exceeds 4.
9. `ActivityResult`'s concrete type always matches the activity's `exerciseType`.
