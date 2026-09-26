package com.wivernz.itera.feature.exercise.combination

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TrainingDay

/**
 * Step results carry forward through a combination chain: Eisenhower's chosen task becomes 80/20's subject, and
 * 80/20's chosen step becomes the Deep Work task label. This is what makes the chain one move.
 */
object ChainCarry {
    /** The one thing a finished step hands on. */
    fun subjectOf(result: ActivityResult?): String? = when (result) {
        is ActivityResult.Eisenhower ->
            result.items.firstOrNull { it.id == result.chosenItemId }?.label
        is ActivityResult.Template -> result.values.values.filterIsInstance<BlockValue.Choice>()
            .firstNotNullOfOrNull {
                it.options.getOrNull(it.chosenIndex)?.takeIf(String::isNotBlank)
            }
        is ActivityResult.Focus -> result.taskLabel.takeIf(String::isNotBlank)
        else -> null
    }

    /** The chain's steps in order. */
    fun steps(day: TrainingDay): List<PlanActivity> =
        day.activities.filter { it.isCombinationStep }.sortedBy { it.orderIndex }

    /** What the nearest finished earlier step hands to [step]; null when nothing does. */
    fun carriedInto(step: PlanActivity, day: TrainingDay): String? =
        steps(day).takeWhile { it.id != step.id }.reversed()
            .firstNotNullOfOrNull { subjectOf(it.result) }
}
