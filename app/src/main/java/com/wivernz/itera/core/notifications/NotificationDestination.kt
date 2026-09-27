package com.wivernz.itera.core.notifications

import com.wivernz.itera.core.navigation.AppRoute
import com.wivernz.itera.core.navigation.ExerciseIntro
import com.wivernz.itera.core.navigation.ExerciseResult
import com.wivernz.itera.core.navigation.Reflection
import com.wivernz.itera.core.navigation.Review
import com.wivernz.itera.core.navigation.Today
import com.wivernz.itera.core.navigation.Train
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import javax.inject.Inject

class NotificationDestination @Inject constructor(private val plans: TrainingPlanRepository) {
    suspend fun resolve(route: AppRoute): AppRoute {
        val id = when (route) {
            is ExerciseIntro -> route.activityId
            is Reflection -> route.activityId
            is Review -> route.activityId
            else -> return route
        }
        val fallback = if (route is Review) Train else Today
        val activity = plans.activity(id) ?: return fallback
        return when (activity.state) {
            ActivityState.COMPLETED -> ExerciseResult(id, activity.techniqueId.value, readOnly = true)
            ActivityState.SKIPPED, ActivityState.EXPIRED -> fallback
            else -> route
        }
    }
}
