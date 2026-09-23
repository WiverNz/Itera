package com.itera.app.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.itera.app.R
import com.itera.app.ui.components.IteraIcons

enum class ThemeMode { System, Light, Dark }

enum class Skill(@StringRes val title: Int, @StringRes val description: Int) {
    Focus(R.string.skill_focus, R.string.skill_focus_desc),
    Planning(R.string.skill_planning, R.string.skill_planning_desc),
    Learning(R.string.skill_learning, R.string.skill_learning_desc),
    Habits(R.string.skill_habits, R.string.skill_habits_desc),
    Reflection(R.string.skill_reflection, R.string.skill_reflection_desc),
}

/** Mastery ladder for one technique. */
enum class Mastery(@StringRes val title: Int) {
    Met(R.string.level_met),
    Practiced(R.string.level_practiced),
    Applied(R.string.level_applied),
    Integrated(R.string.level_integrated),
}

/** Which interactive screen runs the exercise of a technique. */
enum class ExerciseKind { TwoMinute, Focus, Eisenhower, Feynman, Premortem, HabitStack, Pareto, Review, Reflection, Generic }

enum class Technique(
    val skill: Skill,
    @StringRes val title: Int,
    @StringRes val short: Int,
    @StringRes val why: Int,
    @StringRes val task: Int,
    val minutes: Int,
    val kind: ExerciseKind,
) {
    TwoMinute(Skill.Habits, R.string.t_two_name, R.string.t_two_short, R.string.t_two_why, R.string.t_two_task, 5, ExerciseKind.TwoMinute),
    Pomodoro(Skill.Focus, R.string.t_pomodoro_name, R.string.t_pomodoro_short, R.string.t_pomodoro_why, R.string.t_pomodoro_task, 30, ExerciseKind.Focus),
    Eisenhower(Skill.Planning, R.string.t_eisenhower_name, R.string.t_eisenhower_short, R.string.t_eisenhower_why, R.string.t_eisenhower_task, 10, ExerciseKind.Eisenhower),
    FiveSecond(Skill.Focus, R.string.t_five_name, R.string.t_five_short, R.string.t_five_why, R.string.t_five_task, 2, ExerciseKind.Generic),
    HabitStack(Skill.Habits, R.string.t_stack_name, R.string.t_stack_short, R.string.t_stack_why, R.string.t_stack_task, 5, ExerciseKind.HabitStack),
    Feynman(Skill.Learning, R.string.t_feynman_name, R.string.t_feynman_short, R.string.t_feynman_why, R.string.t_feynman_task, 15, ExerciseKind.Feynman),
    TwoList(Skill.Planning, R.string.t_twolist_name, R.string.t_twolist_short, R.string.t_twolist_why, R.string.t_twolist_task, 10, ExerciseKind.Generic),
    DeepWork(Skill.Focus, R.string.t_deep_name, R.string.t_deep_short, R.string.t_deep_why, R.string.t_deep_task, 50, ExerciseKind.Focus),
    Pareto(Skill.Planning, R.string.t_pareto_name, R.string.t_pareto_short, R.string.t_pareto_why, R.string.t_pareto_task, 10, ExerciseKind.Pareto),
    Spaced(Skill.Learning, R.string.t_spaced_name, R.string.t_spaced_short, R.string.t_spaced_why, R.string.t_spaced_task, 5, ExerciseKind.Review),
    InfoDiet(Skill.Focus, R.string.t_diet_name, R.string.t_diet_short, R.string.t_diet_why, R.string.t_diet_task, 5, ExerciseKind.Generic),
    Premortem(Skill.Reflection, R.string.t_premortem_name, R.string.t_premortem_short, R.string.t_premortem_why, R.string.t_premortem_task, 10, ExerciseKind.Premortem),
    OnePercent(Skill.Habits, R.string.t_onepct_name, R.string.t_onepct_short, R.string.t_onepct_why, R.string.t_onepct_task, 5, ExerciseKind.Generic),
    DailyReflection(Skill.Reflection, R.string.t_reflect_name, R.string.t_reflect_short, R.string.t_reflect_why, R.string.t_reflect_task, 2, ExerciseKind.Reflection);

    val icon: ImageVector
        get() = when (this) {
            TwoMinute -> IteraIcons.TwoMinute
            Pomodoro -> IteraIcons.Pomodoro
            Eisenhower -> IteraIcons.Eisenhower
            FiveSecond -> IteraIcons.FiveSecond
            HabitStack -> IteraIcons.HabitStack
            Feynman -> IteraIcons.Feynman
            TwoList -> IteraIcons.TwoList
            DeepWork -> IteraIcons.DeepWork
            Pareto -> IteraIcons.Pareto
            Spaced -> IteraIcons.Spaced
            InfoDiet -> IteraIcons.InfoDiet
            Premortem -> IteraIcons.Premortem
            OnePercent -> IteraIcons.OnePercent
            DailyReflection -> IteraIcons.Reflection
        }

    /** Techniques that pair well with this one. */
    val related: List<Technique>
        get() = when (this) {
            TwoMinute -> listOf(FiveSecond, HabitStack, Eisenhower)
            Pomodoro -> listOf(DeepWork, FiveSecond, Eisenhower)
            Eisenhower -> listOf(Pareto, TwoList, TwoMinute)
            FiveSecond -> listOf(TwoMinute, Pomodoro, HabitStack)
            HabitStack -> listOf(OnePercent, TwoMinute, DailyReflection)
            Feynman -> listOf(Spaced, DeepWork, DailyReflection)
            TwoList -> listOf(Pareto, Eisenhower, Premortem)
            DeepWork -> listOf(Pomodoro, InfoDiet, Pareto)
            Pareto -> listOf(Eisenhower, DeepWork, TwoList)
            Spaced -> listOf(Feynman, HabitStack, DailyReflection)
            InfoDiet -> listOf(DeepWork, TwoList, Pomodoro)
            Premortem -> listOf(TwoList, DailyReflection, Pareto)
            OnePercent -> listOf(HabitStack, DailyReflection, TwoMinute)
            DailyReflection -> listOf(Premortem, OnePercent, Feynman)
        }
}

/**
 * The program: one new technique per training day. Daily reflection runs every evening from Day 1.
 * Program days advance when the user trains, not by calendar.
 */
object Program {
    val days: List<Technique> = listOf(
        Technique.TwoMinute, Technique.Pomodoro, Technique.Eisenhower, Technique.FiveSecond,
        Technique.HabitStack, Technique.Feynman, Technique.TwoList,
        Technique.DeepWork, Technique.Pareto, Technique.Spaced, Technique.InfoDiet,
        Technique.Premortem, Technique.OnePercent,
    )
    const val COMBINATION_DAY = 14
    const val WEEK_LENGTH = 7

    /** Technique introduced on [day], or null on combination days. */
    fun techniqueFor(day: Int): Technique? = days.getOrNull(day - 1)

    fun unlocked(day: Int): Set<Technique> =
        (days.take(day.coerceAtMost(days.size)) + Technique.DailyReflection).toSet()

    fun unlockDay(t: Technique): Int? = days.indexOf(t).takeIf { it >= 0 }?.plus(1)
}

enum class Quadrant { DoNow, Schedule, Delegate, Drop }

data class MatrixTask(val id: Int, @StringRes val label: Int, val quadrant: Quadrant?)

enum class DayStep { Exercise, Focus, Reflection }

enum class Feeling(@StringRes val title: Int) { Easy(R.string.feel_easy), Okay(R.string.feel_okay), Hard(R.string.feel_hard) }
