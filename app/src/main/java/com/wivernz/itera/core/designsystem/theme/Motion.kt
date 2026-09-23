package com.wivernz.itera.core.designsystem.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReduceMotion = staticCompositionLocalOf { false }

/** Prototype timings. Reduced motion uses snap, including delayed and spring specs. */
object IteraMotion {
    const val PULSE_MILLIS = 1200
    const val MASTERY_MILLIS = 700
    const val MASTERY_DELAY = 150
    const val PROGRESS_MILLIS = 800
    const val SKILL_MILLIS = 700
    const val SKILL_DELAY = 80
    const val CHECK_MILLIS = 450
    const val DAY_ARC_MILLIS = 500
    const val FOCUS_MILLIS = 900
    const val WELCOME_DELAY = 120
    const val REFLECTION_ENTER_MILLIS = 250
    const val REFLECTION_EXIT_MILLIS = 150
    fun <T> tweenSpec(duration: Int, reduced: Boolean, delay: Int = 0): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis = duration, delayMillis = delay)
    fun <T> pop(reduced: Boolean, lowStiffness: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = if (lowStiffness) Spring.StiffnessLow else Spring.StiffnessMedium
            )
        }
    fun <T> mastery(index: Int, reduced: Boolean) = tweenSpec<T>(
        MASTERY_MILLIS,
        reduced,
        MASTERY_DELAY * index
    )
    fun <T> progress(reduced: Boolean) = tweenSpec<T>(PROGRESS_MILLIS, reduced)
    fun <T> skill(index: Int, reduced: Boolean) = tweenSpec<T>(
        SKILL_MILLIS,
        reduced,
        SKILL_DELAY * index
    )
    fun <T> check(reduced: Boolean) = tweenSpec<T>(CHECK_MILLIS, reduced)
    fun <T> dayArc(reduced: Boolean) = tweenSpec<T>(DAY_ARC_MILLIS, reduced)
    fun <T> focus(reduced: Boolean) = tweenSpec<T>(FOCUS_MILLIS, reduced)
}

@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = read()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
