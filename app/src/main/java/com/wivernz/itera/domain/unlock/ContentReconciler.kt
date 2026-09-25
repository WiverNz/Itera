package com.wivernz.itera.domain.unlock

import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * docs/data/05-migrations-and-content-versioning.md section 2 (ADR-0015). Runs before the first plan
 * generation of a process: inserts locked rows for new techniques, unlocks any whose `introDay` is already
 * reached, and records the reconciled content version. Idempotent; never re-locks; never touches history.
 */
class ContentReconciler @Inject constructor(
    private val catalog: TechniqueCatalogRepository,
    private val preferences: PreferencesRepository,
    private val unlock: UnlockTechniquesUseCase
) {
    /** True when a reconciliation ran. */
    suspend fun reconcile(): Boolean {
        val version = catalog.curriculum().version
        val prefs = preferences.preferences.first()
        if (version <= prefs.contentVersion) return false
        unlock(prefs.currentProgramDay)
        preferences.update { it.copy(contentVersion = version) }
        return true
    }
}
