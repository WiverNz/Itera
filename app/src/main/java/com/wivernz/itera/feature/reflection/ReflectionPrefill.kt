package com.wivernz.itera.feature.reflection

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity

/** One factual clause of the question-1 pre-fill. Rendered and joined by the screen. */
sealed interface PrefillPart {
    data class Practised(val names: List<String>) : PrefillPart
    data class Tasks(val count: Int) : PrefillPart
    data class Focus(val minutes: Int) : PrefillPart
}

/**
 * Question 1 is pre-filled from what was actually logged today: techniques practised, small tasks cleared and
 * focus minutes. Factual, never congratulatory; an empty day yields no pre-fill at all.
 */
object ReflectionPrefill {
    private const val TWO_MINUTE = "two_minute_rule"
    private const val TASKS_KEY = "tasks"

    fun parts(activities: List<PlanActivity>, names: Map<String, String>): List<PrefillPart> {
        val done = activities.filter {
            it.state == ActivityState.COMPLETED && it.source != ActivitySource.REFLECTION
        }
        val tasks = done.filter { it.techniqueId.value == TWO_MINUTE }.sumOf { activity ->
            val result = activity.result as? ActivityResult.Template
            (result?.values?.get(TASKS_KEY) as? BlockValue.Items)?.items?.count { it.done } ?: 0
        }
        val focusMinutes =
            done.sumOf { ((it.result as? ActivityResult.Focus)?.actualSeconds ?: 0) } / 60
        val practised = done.filter {
            it.exerciseType != ExerciseType.FOCUS_TIMER &&
                it.exerciseType != ExerciseType.COMBINATION &&
                !(it.techniqueId.value == TWO_MINUTE && tasks > 0)
        }.map { names[it.techniqueId.value] ?: it.title }.distinct()
        return buildList {
            if (practised.isNotEmpty()) add(PrefillPart.Practised(practised))
            if (tasks > 0) add(PrefillPart.Tasks(tasks))
            if (focusMinutes > 0) add(PrefillPart.Focus(focusMinutes))
        }
    }

    /** The separator the prototype uses when chips rewrite an answer. */
    const val SEPARATOR = " · "
}
