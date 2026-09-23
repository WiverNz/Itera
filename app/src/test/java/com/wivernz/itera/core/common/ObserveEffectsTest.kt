package com.wivernz.itera.core.common
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class ObserveEffectsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun queuedEffectIsDeliveredOnceAfterResume() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }
        val channel = Channel<Int>(Channel.BUFFERED)
        val effects = channel.receiveAsFlow()
        val seen = mutableListOf<Int>()
        owner.registry.currentState = Lifecycle.State.CREATED
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                ObserveEffects(effects) {
                    seen +=
                        it
                }
            }
        }
        compose.runOnIdle {
            channel.trySend(7)
            assertTrue(seen.isEmpty())
            owner.registry.currentState =
                Lifecycle.State.STARTED
        }
        compose.waitUntil { seen.size == 1 }
        compose.runOnIdle {
            owner.registry.currentState = Lifecycle.State.CREATED
            owner.registry.currentState =
                Lifecycle.State.STARTED
        }
        compose.runOnIdle {
            assertEquals(listOf(7), seen)
            owner.registry.currentState =
                Lifecycle.State.DESTROYED
        }
    }
}
