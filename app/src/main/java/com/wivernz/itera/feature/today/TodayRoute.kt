package com.wivernz.itera.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.feature.voice.ProvideVoiceCommands

/** [onVoiceFocus] opens a focus entry with a voice-requested length (0: none); it defaults to [onNavigate]. */
@Composable
fun TodayRoute(
    vm: TodayViewModel,
    onVoiceFocus: ((TodayTarget.Focus, Int) -> Unit)? = null,
    onNavigate: (TodayTarget) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onNavigate)
    val voiceFocus by rememberUpdatedState(onVoiceFocus)
    val voice = remember(vm) {
        TodayVoiceHost({ vm.state.value }) { target, minutes ->
            voiceFocus?.invoke(target, minutes) ?: navigate(target)
        }
    }
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { }
    }
    ProvideVoiceCommands(voice) {
        TodayScreen(
            state = state,
            onTarget = { target ->
                if (target is TodayTarget.LogPractice) {
                    vm.logPractice(
                        target
                    )
                } else {
                    onNavigate(target)
                }
            },
            onRetry = vm::retry,
            onUndoPractice = vm::undoPractice,
            onPracticeMessageShown = vm::practiceMessageShown
        )
    }
}
