package com.wivernz.itera.core.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ShellUiState(
    val loading: Boolean = true,
    val preferences: UserPreferences? = null,
    val failed: Boolean = false
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    preferences: PreferencesRepository,
    private val saved: SavedStateHandle
) : ViewModel() {
    val state = preferences.preferences.map { ShellUiState(loading = false, preferences = it) }
        .catch { emit(ShellUiState(loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())
    val pendingDeepLink = saved.getStateFlow<String?>("pendingDeepLink", null)
    fun acceptDeepLink(value: String?) {
        if (value != null && RouteCodec.decode(value) != null) saved["pendingDeepLink"] = value
    }
    fun consumeDeepLink(value: String) {
        if (pendingDeepLink.value == value) saved["pendingDeepLink"] = null
    }
}
