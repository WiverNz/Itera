package com.wivernz.itera.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.focus.FocusRestoreResult
import com.wivernz.itera.domain.focus.FocusSessionController
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Where app start should take the user after the restore table ran. */
sealed interface FocusRestoreTarget {
    data class Session(val activityId: Long, val minutes: Int, val techniqueId: String) :
        FocusRestoreTarget
    data class Result(val activityId: Long, val techniqueId: String) : FocusRestoreTarget
    data class Chain(val parentActivityId: Long) : FocusRestoreTarget
}

/** Runs the focus restore table once per cold start (docs/engine/05-timer-lifecycle.md section 2). */
@HiltViewModel
class FocusRestoreViewModel @Inject constructor(
    private val controller: FocusSessionController,
    private val plans: TrainingPlanRepository
) : ViewModel() {
    private var ran = false

    fun restore(onTarget: (FocusRestoreTarget) -> Unit) {
        if (ran) return
        ran = true
        viewModelScope.launch {
            val target = when (val restored = controller.restore()) {
                FocusRestoreResult.Nothing -> null
                is FocusRestoreResult.Attach -> FocusRestoreTarget.Session(
                    restored.state.activityId,
                    restored.state.plannedSeconds / SECONDS_PER_MINUTE,
                    restored.state.techniqueId.value
                )
                is FocusRestoreResult.Finished -> finishedTarget(
                    restored.end.activityId,
                    restored.end.techniqueId.value
                )
            }
            target?.let(onTarget)
        }
    }

    private suspend fun finishedTarget(activityId: Long, techniqueId: String): FocusRestoreTarget {
        val activity = plans.activity(activityId)
        if (activity?.source != ActivitySource.COMBINATION) {
            return FocusRestoreTarget.Result(activityId, techniqueId)
        }
        val parent = plans.day(activity.trainingDayId)?.activities
            ?.firstOrNull { it.exerciseType == ExerciseType.COMBINATION }
        return parent?.let { FocusRestoreTarget.Chain(it.id) }
            ?: FocusRestoreTarget.Result(activityId, techniqueId)
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
