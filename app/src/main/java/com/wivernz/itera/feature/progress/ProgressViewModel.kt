package com.wivernz.itera.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ProgressSummary
import com.wivernz.itera.domain.progress.ObserveProgressUseCase
import com.wivernz.itera.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val summary: ProgressSummary? = null,
    val startedOn: LocalDate? = null,
    val loading: Boolean = true,
    val failed: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    observe: ObserveProgressUseCase,
    preferences: PreferencesRepository,
    analytics: Analytics
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    val state = refresh.flatMapLatest {
        combine(observe(), preferences.preferences) { summary, prefs ->
            ProgressUiState(summary, prefs.programStartedOn, loading = false)
        }.catch { emit(ProgressUiState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.PROGRESS))
    }
    fun refresh() {
        refresh.value++
    }
}
