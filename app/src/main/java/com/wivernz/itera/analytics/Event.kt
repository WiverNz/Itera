package com.wivernz.itera.analytics
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.TrainingDayStatus
/** Closed vocabulary: no event accepts arbitrary user-authored strings. */
sealed interface Event {
    val name: String
    val params: Map<String, Any?>
    data object OnboardingStarted : Event {
        override val name: String = "onboarding_started"
        override val params: Map<String, Any?> get() = emptyMap()
    }
    data class OnboardingStepCompleted(val step: Int) : Event {
        override val name: String = "onboarding_step_completed"
        override val params: Map<String, Any?> get() = mapOf("step" to step)
    }
    data class OnboardingCompleted(
        val focusAreaCount: Int,
        val timeBudget: TimeBudget,
        val morningHour: Int,
        val eveningHour: Int,
        val notificationsGranted: Boolean
    ) : Event {
        override val name: String = "onboarding_completed"
        override val params: Map<String, Any?> get() = mapOf(
            "focusAreaCount" to focusAreaCount,
            "timeBudget" to timeBudget.name,
            "morningHour" to morningHour,
            "eveningHour" to eveningHour,
            "notificationsGranted" to notificationsGranted
        )
    }
    data class PlanGenerated(
        val programDay: Int,
        val activityCount: Int,
        val reviewCount: Int,
        val hasCombination: Boolean,
        val generatorVersion: Int
    ) : Event {
        override val name: String = "plan_generated"
        override val params: Map<String, Any?> get() = mapOf(
            "programDay" to programDay,
            "activityCount" to activityCount,
            "reviewCount" to reviewCount,
            "hasCombination" to hasCombination,
            "generatorVersion" to generatorVersion
        )
    }
    data class DayRolledOver(val previousStatus: TrainingDayStatus, val expiredCount: Int) : Event {

        override val name: String = "day_rolled_over"
        override val params: Map<String, Any?> get() = mapOf(
            "previousStatus" to previousStatus.name,
            "expiredCount" to expiredCount
        )
    }
    data class DayCompleted(
        val programDay: Int,
        val completedCount: Int,
        val skippedCount: Int,
        val durationMinutes: Int
    ) : Event {
        override val name: String = "day_completed"
        override val params: Map<String, Any?> get() = mapOf(
            "programDay" to programDay,
            "completedCount" to completedCount,
            "skippedCount" to skippedCount,
            "durationMinutes" to durationMinutes
        )
    }
    data class ProgramDayAdvanced(val from: Int, val to: Int) : Event {
        override val name: String = "program_day_advanced"
        override val params: Map<String, Any?> get() = mapOf("from" to from, "to" to to)
    }
    data class ExerciseOpened(
        val techniqueId: AnalyticsTechnique,
        val exerciseType: ExerciseType,
        val source: ActivitySource
    ) : Event {
        override val name: String = "exercise_opened"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "exerciseType" to exerciseType.name,
            "source" to source.name
        )
    }
    data class ExerciseStarted(
        val techniqueId: AnalyticsTechnique,
        val exerciseType: ExerciseType
    ) : Event {
        override val name: String = "exercise_started"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "exerciseType" to exerciseType.name
        )
    }
    data class ExerciseCompleted(
        val techniqueId: AnalyticsTechnique,
        val exerciseType: ExerciseType,
        val source: ActivitySource,
        val durationSeconds: Int,
        val difficulty: Difficulty?,
        val hasNote: Boolean
    ) : Event {
        override val name: String = "exercise_completed"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "exerciseType" to exerciseType.name,
            "source" to source.name,
            "durationSeconds" to durationSeconds,
            "difficulty" to difficulty?.name,
            "hasNote" to hasNote
        )
    }
    data class ExerciseSnoozed(val techniqueId: AnalyticsTechnique, val minutesDeferred: Int) :
        Event {

        override val name: String = "exercise_snoozed"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "minutesDeferred" to minutesDeferred
        )
    }
    data class ExerciseSkipped(
        val techniqueId: AnalyticsTechnique,
        val exerciseType: ExerciseType
    ) : Event {
        override val name: String = "exercise_skipped"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "exerciseType" to exerciseType.name
        )
    }
    data class ExerciseAbandoned(val techniqueId: AnalyticsTechnique, val secondsSpent: Int) :
        Event {

        override val name: String = "exercise_abandoned"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "techniqueId" to techniqueId.id,
                "secondsSpent" to secondsSpent
            )
    }
    data class PracticeLogged(val techniqueId: AnalyticsTechnique) : Event {
        override val name: String = "practice_logged"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf("techniqueId" to techniqueId.id)
    }
    data class FocusStarted(val techniqueId: AnalyticsTechnique, val plannedMinutes: Int) : Event {

        override val name: String = "focus_started"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "plannedMinutes" to plannedMinutes
        )
    }
    data class FocusPaused(val remainingSeconds: Int) : Event {
        override val name: String = "focus_paused"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf("remainingSeconds" to remainingSeconds)
    }
    data class FocusExtended(val addedSeconds: Int, val timesExtended: Int) : Event {
        override val name: String = "focus_extended"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "addedSeconds" to addedSeconds,
                "timesExtended" to timesExtended
            )
    }
    data class FocusCompleted(
        val plannedSeconds: Int,
        val actualSeconds: Int,
        val completedNaturally: Boolean
    ) : Event {
        override val name: String = "focus_completed"
        override val params: Map<String, Any?> get() = mapOf(
            "plannedSeconds" to plannedSeconds,
            "actualSeconds" to actualSeconds,
            "completedNaturally" to completedNaturally
        )
    }
    data class FocusRestored(val remainingSeconds: Int, val afterProcessDeath: Boolean) : Event {

        override val name: String = "focus_restored"
        override val params: Map<String, Any?> get() = mapOf(
            "remainingSeconds" to remainingSeconds,
            "afterProcessDeath" to afterProcessDeath
        )
    }
    data class ReviewDue(
        val techniqueId: AnalyticsTechnique,
        val stageIndex: Int,
        val daysOverdue: Int
    ) : Event {
        override val name: String = "review_due"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "stageIndex" to stageIndex,
            "daysOverdue" to daysOverdue
        )
    }
    data class ReviewOpened(val techniqueId: AnalyticsTechnique, val stageIndex: Int) : Event {

        override val name: String = "review_opened"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "techniqueId" to techniqueId.id,
                "stageIndex" to stageIndex
            )
    }
    data class ReviewRevealed(val techniqueId: AnalyticsTechnique, val stageIndex: Int) : Event {

        override val name: String = "review_revealed"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "techniqueId" to techniqueId.id,
                "stageIndex" to stageIndex
            )
    }
    data class ReviewGraded(
        val techniqueId: AnalyticsTechnique,
        val stageBefore: Int,
        val stageAfter: Int,
        val grade: RecallGrade
    ) : Event {
        override val name: String = "review_graded"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "stageBefore" to stageBefore,
            "stageAfter" to stageAfter,
            "grade" to grade.name
        )
    }
    data class TechniqueUnlocked(val techniqueId: AnalyticsTechnique, val programDay: Int) : Event {

        override val name: String = "technique_unlocked"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "techniqueId" to techniqueId.id,
                "programDay" to programDay
            )
    }
    data class MasteryChanged(
        val techniqueId: AnalyticsTechnique,
        val from: MasteryLevel,
        val to: MasteryLevel
    ) : Event {
        override val name: String = "mastery_changed"
        override val params: Map<String, Any?> get() = mapOf(
            "techniqueId" to techniqueId.id,
            "from" to from.name,
            "to" to to.name
        )
    }
    data class SkillLevelChanged(val skill: Skill, val from: SkillLevel, val to: SkillLevel) :
        Event {

        override val name: String = "skill_level_changed"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "skill" to skill.name,
                "from" to from.name,
                "to" to to.name
            )
    }
    data class ScreenViewed(val route: ScreenRoute) : Event {
        override val name: String = "screen_viewed"
        override val params: Map<String, Any?> get() = mapOf("route" to route.className)
    }
    data class SettingChanged(val key: SettingKey, val value: SettingValue) : Event {
        override val name: String = "setting_changed"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "key" to key.name,
                "value" to value.scalar
            )
    }
    data class ResetPerformed(val tier: ResetTier) : Event {
        override val name: String = "reset_performed"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf("tier" to tier.name.lowercase(java.util.Locale.ROOT))
    }
    data class JournalExported(val range: ExportRange, val dayCount: Int, val sizeBytes: Long) :
        Event {

        override val name: String = "journal_exported"
        override val params: Map<String, Any?> get() = mapOf(
            "range" to range.name.lowercase(java.util.Locale.ROOT),
            "dayCount" to dayCount,
            "sizeBytes" to sizeBytes
        )
    }
    data class NotificationScheduled(val type: NotificationType, val delayMinutes: Int) : Event {

        override val name: String = "notification_scheduled"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "type" to type.name,
                "delayMinutes" to delayMinutes
            )
    }
    data class NotificationPosted(val type: NotificationType) : Event {
        override val name: String = "notification_posted"
        override val params: Map<String, Any?> get() = mapOf("type" to type.name)
    }
    data class NotificationSuppressed(val type: NotificationType, val reason: SuppressionReason) :
        Event {

        override val name: String = "notification_suppressed"
        override val params: Map<
            String,
            Any?
            > get() =
            mapOf(
                "type" to type.name,
                "reason" to reason.name
            )
    }
    data class NotificationOpened(val type: NotificationType) : Event {
        override val name: String = "notification_opened"
        override val params: Map<String, Any?> get() = mapOf("type" to type.name)
    }
}
enum class AnalyticsTechnique(val id: String) {
    TWO_MINUTE_RULE("two_minute_rule"),
    POMODORO("pomodoro"),
    EISENHOWER_MATRIX("eisenhower_matrix"),
    FIVE_SECOND_RULE("five_second_rule"),
    HABIT_STACKING("habit_stacking"),
    FEYNMAN("feynman_technique"),
    TWO_LIST_STRATEGY("two_list_strategy"),
    DEEP_WORK("deep_work"),
    PARETO_PRINCIPLE("pareto_principle"),
    SPACED_REPETITION("spaced_repetition"),
    INFORMATION_DIET("information_diet"),
    PREMORTEM("premortem"),
    ONE_PERCENT_IMPROVEMENT("one_percent_improvement"),
    DAILY_REFLECTION("daily_reflection");

    companion object {
        fun of(id: String): AnalyticsTechnique? = entries.firstOrNull { it.id == id }
    }
}
enum class ScreenRoute(val className: String) {
    WELCOME("Welcome"),
    GOALS("Goals"),
    RHYTHM("Rhythm"),
    FIRST_WEEK("FirstWeek"),
    TODAY("Today"),
    EXERCISE("Exercise"),
    FOCUS("FocusSession"),
    REFLECTION("Reflection"),
    DAY_COMPLETE("DayComplete"),
    TRAIN("Train"),
    LIBRARY("Library"),
    TECHNIQUE("TechniqueDetail"),
    PROGRESS("Progress"),
    HISTORY("History"),
    YOU("You"),
    TOPICS("LearningTopics"),
    PRIVACY("Privacy")
}
enum class SettingKey {
    FOCUS_AREAS,
    MORNING_TIME,
    EVENING_TIME,
    TIME_BUDGET,
    PACE,
    THEME,
    NOTIFY_MORNING,
    NOTIFY_FOCUS,
    NOTIFY_REVIEWS,
    NOTIFY_EVENING,
    LANGUAGE
}
sealed interface SettingValue {
    val scalar: Any
    data class Toggle(val enabled: Boolean) : SettingValue {
        override val scalar: Any get() =
            enabled
    }
    data class Hour(val hour: Int) : SettingValue {
        init {
            require(hour in 0..23)
        }
        override val scalar: Any get() =
            hour
    }
    data class Budget(val budget: TimeBudget) : SettingValue {
        override val scalar: Any get() =
            budget.name
    }
    data class Pace(val pace: ProgramPace) : SettingValue {
        override val scalar: Any get() =
            pace.name
    }
    data class Theme(val theme: ThemePreference) : SettingValue {
        override val scalar: Any get() =
            theme.name
    }
    data class Language(val language: LanguageChoice) : SettingValue {
        override val scalar: Any get() =
            language.name
    }
    data class Area(val skill: Skill) : SettingValue {
        override val scalar: Any get() =
            skill.name
    }
}
enum class LanguageChoice { SYSTEM, EN, RU, DE, ES }
enum class ResetTier { PROGRAM, ALL }
enum class ExportRange { MONTH, YEAR, ALL }
enum class NotificationType { MORNING, FOCUS, REVIEW, EVENING, HABIT }
enum class SuppressionReason {
    PERMISSION_DENIED,
    DISABLED,
    FOCUS_RUNNING,
    ALREADY_COMPLETE
}
