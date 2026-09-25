package com.wivernz.itera.core.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wivernz.itera.feature.daycomplete.DayCompleteRoute
import com.wivernz.itera.feature.onboarding.FirstWeekRoute
import com.wivernz.itera.feature.onboarding.GoalsRoute
import com.wivernz.itera.feature.onboarding.OnboardingViewModel
import com.wivernz.itera.feature.onboarding.RhythmRoute
import com.wivernz.itera.feature.onboarding.WelcomeRoute
import com.wivernz.itera.feature.reflection.ReflectionRoute
import com.wivernz.itera.feature.today.TodayRoute
import com.wivernz.itera.feature.today.TodayTarget

/** Production destination content. Routes without a feature yet keep the milestone 003 placeholder. */
@Composable
fun AppDestination(route: AppRoute, actions: NavigationActions) {
    when (route) {
        Welcome -> WelcomeRoute(
            onboardingViewModel(),
            onStart = { actions.navigate(Goals) },
            onFinished = actions.finishOnboarding
        )
        Goals -> GoalsRoute(onboardingViewModel(), actions.back) { actions.navigate(Rhythm) }
        Rhythm -> RhythmRoute(onboardingViewModel(), actions.back) { actions.navigate(FirstWeek) }
        FirstWeek -> FirstWeekRoute(onboardingViewModel(), actions.back, actions.finishOnboarding)
        Today -> TodayRoute(hiltViewModel()) { actions.navigate(it.toRoute()) }
        is Reflection -> ReflectionRoute(
            hiltViewModel(),
            onClose = actions.back,
            onFinished = {
                if (it.dayComplete) {
                    actions.navigate(
                        DayComplete(it.dayId)
                    )
                } else {
                    actions.backToToday()
                }
            }
        )
        is DayComplete -> DayCompleteRoute(hiltViewModel(), onGoodNight = actions.backToToday)
        else -> PlaceholderRoute(route, actions)
    }
}

@Composable
private fun onboardingViewModel(): OnboardingViewModel =
    hiltViewModel(LocalOnboardingOwner.current ?: checkNotNull(LocalViewModelStoreOwner.current))

fun TodayTarget.toRoute(): AppRoute = when (this) {
    is TodayTarget.Exercise -> ExerciseIntro(activityId, techniqueId)
    is TodayTarget.Focus -> FocusSession(activityId, minutes, techniqueId)
    is TodayTarget.Review -> Review(activityId)
    is TodayTarget.Combination -> Combination(activityId)
    is TodayTarget.Reflection -> Reflection(activityId)
    is TodayTarget.DayComplete -> DayComplete(dayId)
    // completed in place by Today; never navigated
    is TodayTarget.LogPractice -> Today
}
