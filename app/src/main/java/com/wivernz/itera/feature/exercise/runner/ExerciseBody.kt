package com.wivernz.itera.feature.exercise.runner

import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Technique

/** Where the interactive part of an exercise lives. The navigation layer maps each one to a route. */
sealed interface ExerciseBody {
    /** Q-04: a template with no blocks goes intro -> result with "I did it". */
    data object Generic : ExerciseBody
    data object Template : ExerciseBody
    data class Focus(val minutes: Int) : ExerciseBody
    data object Eisenhower : ExerciseBody
    data object Feynman : ExerciseBody
    data object Premortem : ExerciseBody
    data object HabitStack : ExerciseBody
    data object Review : ExerciseBody
    data object Combination : ExerciseBody
    data object Reflection : ExerciseBody
}

/** ADR-0007: the one place an exercise type selects its body. */
fun bodyFor(type: ExerciseType, technique: Technique?): ExerciseBody = when (type) {
    ExerciseType.TEMPLATE ->
        if (technique?.template?.blocks.isNullOrEmpty()) {
            ExerciseBody.Generic
        } else {
            ExerciseBody.Template
        }
    ExerciseType.FOCUS_TIMER -> ExerciseBody.Focus(
        technique?.defaults?.focusMinutes ?: technique?.estimatedMinutes ?: DEFAULT_FOCUS_MINUTES
    )
    ExerciseType.EISENHOWER -> ExerciseBody.Eisenhower
    ExerciseType.FEYNMAN -> ExerciseBody.Feynman
    ExerciseType.PREMORTEM -> ExerciseBody.Premortem
    ExerciseType.HABIT_STACK -> ExerciseBody.HabitStack
    ExerciseType.REVIEW -> ExerciseBody.Review
    ExerciseType.COMBINATION -> ExerciseBody.Combination
    ExerciseType.REFLECTION -> ExerciseBody.Reflection
}

const val DEFAULT_FOCUS_MINUTES = 25
