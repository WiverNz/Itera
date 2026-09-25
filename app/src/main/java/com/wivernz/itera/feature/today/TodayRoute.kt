package com.wivernz.itera.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodayRoute(vm: TodayViewModel, onNavigate: (TodayTarget) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }
    TodayScreen(
        state = state,
        onTarget = { target ->
            if (target is TodayTarget.LogPractice) vm.logPractice(target) else onNavigate(target)
        },
        onRetry = vm::retry,
        onUndoPractice = vm::undoPractice,
        onPracticeMessageShown = vm::practiceMessageShown
    )
}
