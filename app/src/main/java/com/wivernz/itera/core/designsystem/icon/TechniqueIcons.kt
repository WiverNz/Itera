package com.wivernz.itera.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector

/** The prototype's `Technique.icon`, keyed by the frozen catalogue id. */
fun techniqueIcon(techniqueId: String): ImageVector = when (techniqueId) {
    "two_minute_rule" -> IteraIcons.TwoMinute
    "pomodoro" -> IteraIcons.Pomodoro
    "eisenhower_matrix" -> IteraIcons.Eisenhower
    "five_second_rule" -> IteraIcons.FiveSecond
    "habit_stacking" -> IteraIcons.HabitStack
    "feynman_technique" -> IteraIcons.Feynman
    "two_list_strategy" -> IteraIcons.TwoList
    "deep_work" -> IteraIcons.DeepWork
    "pareto_principle" -> IteraIcons.Pareto
    "spaced_repetition" -> IteraIcons.Spaced
    "information_diet" -> IteraIcons.InfoDiet
    "premortem" -> IteraIcons.Premortem
    "one_percent_improvement" -> IteraIcons.OnePercent
    "daily_reflection" -> IteraIcons.Reflection
    else -> IteraIcons.OnePercent
}
