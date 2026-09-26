@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.domain.model.TechniqueId

@Composable
fun TrainRoute(vm: TrainViewModel, navigate: (TrainEffect) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects, navigate)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onEvent(TrainUiEvent.Refresh) }
    TrainScreen(state, vm::onEvent)
}

@Composable
fun LibraryRoute(vm: LibraryViewModel, onBack: () -> Unit, onTechnique: (TechniqueId) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects, onTechnique)
    LibraryScreen(state, vm::onEvent, onBack)
}

@Composable
fun TechniqueDetailRoute(
    vm: TechniqueDetailViewModel,
    onBack: () -> Unit,
    navigate: (TechniqueDetailEffect) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ObserveEffects(vm.effects, navigate)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onEvent(TechniqueDetailUiEvent.Returned) }
    TechniqueDetailScreen(state, vm::onEvent, onBack)
}
