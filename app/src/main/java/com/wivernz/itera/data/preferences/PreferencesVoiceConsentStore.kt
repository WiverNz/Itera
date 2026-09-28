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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ADR-0022 recogniser choices, kept in the user preferences so Settings can change or revoke them and "Erase all
 * data" clears them. The recogniser reads them synchronously on a mic tap, so the stored values are mirrored in
 * memory; a change takes effect at once, before the write lands. While a write is in flight, stored emissions are
 * not mirrored, since they may predate it (the initial read, or an earlier write); the mirror is refreshed once the
 * last write has landed.
 */
@Singleton
class PreferencesVoiceConsentStore @Inject constructor(
    private val preferences: PreferencesRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher
) : VoiceConsentStore {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile private var system = false

    @Volatile private var selected: String? = null

    private val lock = Any()

    private var pendingWrites = 0

    init {
        preferences.preferences
            .onEach(::mirror)
            .catch {
                synchronized(lock) {
                    system = false
                    selected = null
                }
            }
            .launchIn(scope)
    }

    override fun systemGranted(): Boolean = system

    override fun grantSystem() {
        write({ system = true }) { it.copy(systemRecognitionAllowed = true) }
    }

    override fun selectedProvider(): String? = selected

    override fun selectProvider(id: String) {
        write({ selected = id }) { it.copy(selectedRecognizer = id) }
    }

    override fun clearProvider() {
        write({ selected = null }) { it.copy(selectedRecognizer = null) }
    }

    private fun mirror(stored: UserPreferences) {
        synchronized(lock) {
            if (pendingWrites == 0) {
                system = stored.systemRecognitionAllowed
                selected = stored.selectedRecognizer
            }
        }
    }

    private fun write(apply: () -> Unit, transform: (UserPreferences) -> UserPreferences) {
        synchronized(lock) {
            pendingWrites++
            apply()
        }
        scope.launch {
            try {
                preferences.update(transform)
            } finally {
                val settled = synchronized(lock) { --pendingWrites == 0 }
                if (settled) runCatching { preferences.preferences.first() }.onSuccess(::mirror)
            }
        }
    }
}
