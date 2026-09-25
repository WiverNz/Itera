package com.wivernz.itera.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.core.common.ObserveEffects

/** Collects the flow's one-shot effects; each onboarding destination that can finish the flow hosts one. */
@Composable
private fun FinishEffects(vm: OnboardingViewModel, onFinished: () -> Unit) {
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            OnboardingEffect.Finished -> onFinished()
        }
    }
}

@Composable
fun WelcomeRoute(vm: OnboardingViewModel, onStart: () -> Unit, onFinished: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    FinishEffects(vm, onFinished)
    WelcomeScreen(
        demoAvailable = state.demoAvailable,
        busy = state.finishing,
        onStart = {
            vm.onGetStarted()
            onStart()
        },
        onDemo = vm::loadDemo
    )
}

@Composable
fun GoalsRoute(vm: OnboardingViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    GoalsScreen(
        focusAreas = state.focusAreas,
        onToggle = vm::toggleFocusArea,
        onBack = onBack,
        onNext = {
            vm.onStepCompleted(OnboardingViewModel.STEP_GOALS)
            onNext()
        }
    )
}

/**
 * Requests POST_NOTIFICATIONS on Continue, once, after the inline rationale; the flow advances whatever the
 * answer (docs/ux/08-notification-ux.md section 7).
 */
@Composable
fun RhythmRoute(vm: OnboardingViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember { mutableStateOf(notificationsAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        granted = notificationsAllowed(context)
        onPauseOrDispose { }
    }
    val advance = {
        vm.onStepCompleted(OnboardingViewModel.STEP_RHYTHM)
        onNext()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        vm.onNotificationResult(ok)
        advance()
    }
    RhythmScreen(
        morningTime = state.morningTime,
        eveningTime = state.eveningTime,
        timeBudget = state.timeBudget,
        showRationale = !granted,
        onMorning = vm::setMorningTime,
        onEvening = vm::setEveningTime,
        onBudget = vm::setTimeBudget,
        onBack = onBack,
        onNext = {
            if (!granted &&
                state.notificationsGranted == null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                vm.onNotificationResult(granted)
                advance()
            }
        }
    )
}

@Composable
fun FirstWeekRoute(vm: OnboardingViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val rows by vm.firstWeek.collectAsStateWithLifecycle()
    FinishEffects(vm, onFinished)
    FirstWeekScreen(rows = rows, busy = state.finishing, onBack = onBack, onStart = vm::startDayOne)
}

private fun notificationsAllowed(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
