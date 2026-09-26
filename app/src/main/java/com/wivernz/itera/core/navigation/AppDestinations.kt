package com.wivernz.itera.core.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wivernz.itera.feature.daycomplete.DayCompleteRoute
import com.wivernz.itera.feature.exercise.combination.CombinationRoute
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerRoute
import com.wivernz.itera.feature.exercise.feynman.FeynmanRoute
import com.wivernz.itera.feature.exercise.habitstack.HabitStackRoute
import com.wivernz.itera.feature.exercise.premortem.PremortemRoute
import com.wivernz.itera.feature.exercise.review.ReviewRoute
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.exercise.runner.ExerciseIntroRoute
import com.wivernz.itera.feature.exercise.runner.ExerciseResultRoute
import com.wivernz.itera.feature.exercise.runner.ExerciseRunRoute
import com.wivernz.itera.feature.exercise.runner.ExerciseRunnerNavigation
import com.wivernz.itera.feature.focus.FocusNavigation
import com.wivernz.itera.feature.focus.FocusSessionRoute
import com.wivernz.itera.feature.onboarding.FirstWeekRoute
import com.wivernz.itera.feature.onboarding.GoalsRoute
import com.wivernz.itera.feature.onboarding.OnboardingViewModel
import com.wivernz.itera.feature.onboarding.RhythmRoute
import com.wivernz.itera.feature.onboarding.WelcomeRoute
import com.wivernz.itera.feature.reflection.ReflectionRoute
import com.wivernz.itera.feature.today.TodayRoute
import com.wivernz.itera.feature.today.TodayTarget

/** Production destination content. Routes owned by later milestones keep the milestone 003 placeholder. */
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
        is ExerciseIntro -> ExerciseIntroRoute(hiltViewModel(), runnerNavigation(actions))
        is ExerciseRun, is TwoMinute -> ExerciseRunRoute(hiltViewModel(), runnerNavigation(actions))
        is ExerciseResult -> ExerciseResultRoute(hiltViewModel(), onDone = actions.backToToday)
        is FocusSession -> FocusSessionRoute(
            hiltViewModel(),
            FocusNavigation(
                showResult = { id, technique -> actions.showResult(ExerciseResult(id, technique)) },
                returnToChain = actions.back,
                close = actions.back,
                toToday = actions.backToToday
            )
        )
        is Eisenhower -> EisenhowerRoute(hiltViewModel(), showResult(actions), actions.back)
        is Feynman, is FeynmanFeedback ->
            FeynmanRoute(hiltViewModel(), showResult(actions), actions.back)
        is Review -> ReviewRoute(hiltViewModel(), showResult(actions), actions.back)
        is Premortem -> PremortemRoute(hiltViewModel(), showResult(actions), actions.back)
        is HabitStack -> HabitStackRoute(hiltViewModel(), showResult(actions), actions.back)
        is Combination -> CombinationRoute(
            hiltViewModel(),
            openFocus = { id, minutes, technique ->
                actions.navigate(FocusSession(id, minutes, technique))
            },
            openBody = { id, technique, body -> actions.navigate(body.route(id, technique)) },
            onClose = actions.back,
            onDone = actions.backToToday
        )
        else -> PlaceholderRoute(route, actions)
    }
}

private fun showResult(actions: NavigationActions): (Long, String) -> Unit =
    { id, technique -> actions.showResult(ExerciseResult(id, technique)) }

private fun runnerNavigation(actions: NavigationActions) = ExerciseRunnerNavigation(
    openBody = { actions.navigate(it.body.route(it.activityId, it.techniqueId)) },
    showResult = { actions.showResult(ExerciseResult(it.activityId, it.techniqueId)) },
    close = actions.back
)

/** Where each exercise body lives; the selection itself is `bodyFor` (ADR-0007). */
fun ExerciseBody.route(activityId: Long, technique: String): AppRoute = when (this) {
    ExerciseBody.Generic -> ExerciseResult(activityId, technique)
    ExerciseBody.Template -> ExerciseRun(activityId, technique)
    is ExerciseBody.Focus -> FocusSession(activityId, minutes, technique)
    ExerciseBody.Eisenhower -> Eisenhower(activityId)
    ExerciseBody.Feynman -> Feynman(activityId)
    ExerciseBody.Premortem -> Premortem(activityId)
    ExerciseBody.HabitStack -> HabitStack(activityId)
    ExerciseBody.Review -> Review(activityId)
    ExerciseBody.Combination -> Combination(activityId)
    ExerciseBody.Reflection -> Reflection(activityId)
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
