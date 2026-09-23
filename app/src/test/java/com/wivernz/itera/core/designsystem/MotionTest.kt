package com.wivernz.itera.core.designsystem
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import com.wivernz.itera.core.designsystem.theme.IteraMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class MotionTest {
    @Test fun timingsAndDefaultEasingMatchPrototype() {
        val mastery = IteraMotion.mastery<Float>(2, false) as TweenSpec
        assertEquals(700, mastery.durationMillis)
        assertEquals(300, mastery.delay)
        assertEquals(FastOutSlowInEasing, mastery.easing)
        assertEquals(800, (IteraMotion.progress<Float>(false) as TweenSpec).durationMillis)
        assertEquals(160, (IteraMotion.skill<Float>(2, false) as TweenSpec).delay)
        assertEquals(450, (IteraMotion.check<Float>(false) as TweenSpec).durationMillis)
        assertEquals(500, (IteraMotion.dayArc<Float>(false) as TweenSpec).durationMillis)
        assertEquals(900, (IteraMotion.focus<Float>(false) as TweenSpec).durationMillis)
        val pop = IteraMotion.pop<Float>(false, true) as SpringSpec
        assertEquals(Spring.DampingRatioMediumBouncy, pop.dampingRatio)
        assertEquals(Spring.StiffnessLow, pop.stiffness)
        assertEquals(1200, IteraMotion.PULSE_MILLIS)
        assertEquals(120, IteraMotion.WELCOME_DELAY)
        assertEquals(250, IteraMotion.REFLECTION_ENTER_MILLIS)
        assertEquals(150, IteraMotion.REFLECTION_EXIT_MILLIS)
    }

    @Test fun reducedMotionHasNoDurationOrDelay() {
        listOf(
            IteraMotion.mastery<Float>(3, true),
            IteraMotion.progress(true),
            IteraMotion.skill(3, true),
            IteraMotion.check(true),
            IteraMotion.dayArc(true),
            IteraMotion.focus(true),
            IteraMotion.pop(true)
        ).forEach {
            assertTrue(it is SnapSpec)
            val animation = TargetBasedAnimation(it, Float.VectorConverter, 0f, 1f)
            assertEquals(0L, animation.durationNanos)
            assertEquals(1f, animation.getValueFromNanos(0))
        }
    }
}
