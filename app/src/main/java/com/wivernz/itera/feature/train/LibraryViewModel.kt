@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryRow(val technique: Technique, val unlocked: Boolean, val level: MasteryLevel)
data class LibraryUiState(
    val rows: List<LibraryRow> = emptyList(),
    val filter: Skill? = null,
    val unlockedCount: Int = 0,
    val failed: Boolean = false
)
sealed interface LibraryUiEvent {
    data class Filter(val skill: Skill?) : LibraryUiEvent
    data class Open(val id: TechniqueId) : LibraryUiEvent
    data object Retry : LibraryUiEvent
}
fun libraryRows(
    catalog: List<Technique>,
    progress: List<TechniqueProgress>,
    filter: Skill?
): List<LibraryRow> {
    val facts = progress.associateBy { it.techniqueId }
    return catalog.filter { !it.retired && (filter == null || it.skill == filter) }
        .map {
            LibraryRow(
                it,
                facts[it.id]?.unlocked == true,
                facts[it.id]?.level ?: MasteryLevel.NONE
            )
        }
        .sortedWith(compareBy({ !it.unlocked }, { it.technique.introDay ?: 0 }))
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    catalog: TechniqueCatalogRepository,
    progress: ObserveTechniqueProgressUseCase,
    analytics: Analytics
) : ViewModel() {
    private val retry = MutableStateFlow(0)
    private val filter = saved.getStateFlow<String?>("filter", null)
    private val channel = Channel<TechniqueId>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    val state = retry.flatMapLatest {
        combine(
            flow {
                emit(catalog.catalog())
            },
            progress().onStart { emit(emptyList()) },
            filter
        ) { techniques, facts, selected ->
            val skill = selected?.let(Skill::valueOf)
            LibraryUiState(
                libraryRows(techniques, facts, skill),
                skill,
                facts.count {
                    it.unlocked
                }
            )
        }.catch { emit(LibraryUiState(failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.LIBRARY))
    }
    fun onEvent(event: LibraryUiEvent) {
        when (event) {
            is LibraryUiEvent.Filter -> saved["filter"] = event.skill?.name
            is LibraryUiEvent.Open -> viewModelScope.launch { channel.send(event.id) }
            LibraryUiEvent.Retry -> retry.value++
        }
    }
}
