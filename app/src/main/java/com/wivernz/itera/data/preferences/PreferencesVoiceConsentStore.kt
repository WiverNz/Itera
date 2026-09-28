package com.wivernz.itera.data.preferences

import com.wivernz.itera.core.common.dispatchers.DefaultDispatcher
import com.wivernz.itera.core.voice.VoiceConsentStore
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ADR-0022 recogniser choices, kept in the user preferences so Settings can change or revoke them and "Erase all
 * data" clears them. The recogniser reads them synchronously on a mic tap, so the stored values are mirrored in
 * memory; a change takes effect at once, before the write lands.
 */
@Singleton
class PreferencesVoiceConsentStore @Inject constructor(
    private val preferences: PreferencesRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : VoiceConsentStore {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile private var system = false

    @Volatile private var selected: String? = null

    init {
        preferences.preferences
            .onEach {
                system = it.systemRecognitionAllowed
                selected = it.selectedRecognizer
            }
            .catch {
                system = false
                selected = null
            }
            .launchIn(scope)
    }

    override fun systemGranted(): Boolean = system

    override fun grantSystem() {
        system = true
        write { it.copy(systemRecognitionAllowed = true) }
    }

    override fun selectedProvider(): String? = selected

    override fun selectProvider(id: String) {
        selected = id
        write { it.copy(selectedRecognizer = id) }
    }

    override fun clearProvider() {
        selected = null
        write { it.copy(selectedRecognizer = null) }
    }

    private fun write(transform: (UserPreferences) -> UserPreferences) {
        scope.launch { preferences.update(transform) }
    }
}
