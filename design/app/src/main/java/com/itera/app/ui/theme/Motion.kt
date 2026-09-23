package com.itera.app.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

val LocalReduceMotion = staticCompositionLocalOf { false }

/** Prototype timings. Reduced motion uses snap, including delayed and spring specs. */
object IteraMotion {
    const val PulseMillis = 1200
    const val MasteryMillis = 700
    const val MasteryDelay = 150
    const val ProgressMillis = 800
    const val SkillMillis = 700
    const val SkillDelay = 80
    const val CheckMillis = 450
    const val DayArcMillis = 500
    const val FocusMillis = 900
    const val WelcomeDelay = 120
    const val ReflectionEnterMillis = 250
    const val ReflectionExitMillis = 150
    fun <T> tweenSpec(duration: Int, reduced: Boolean, delay: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = duration, delayMillis = delay)
    fun <T> pop(reduced: Boolean, lowStiffness: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (lowStiffness) Spring.StiffnessLow else Spring.StiffnessMedium)
    fun <T> mastery(index: Int, reduced: Boolean) = tweenSpec<T>(MasteryMillis, reduced, MasteryDelay * index)
    fun <T> progress(reduced: Boolean) = tweenSpec<T>(ProgressMillis, reduced)
    fun <T> skill(index: Int, reduced: Boolean) = tweenSpec<T>(SkillMillis, reduced, SkillDelay * index)
    fun <T> check(reduced: Boolean) = tweenSpec<T>(CheckMillis, reduced)
    fun <T> dayArc(reduced: Boolean) = tweenSpec<T>(DayArcMillis, reduced)
    fun <T> focus(reduced: Boolean) = tweenSpec<T>(FocusMillis, reduced)
}

@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
