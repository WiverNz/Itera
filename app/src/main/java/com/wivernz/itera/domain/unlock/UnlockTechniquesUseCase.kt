package com.wivernz.itera.domain.unlock

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import java.time.Clock
import javax.inject.Inject

/**
 * Unlocks every catalogue technique whose `introDay` has been reached and which is still locked, recording the
 * program day the unlock actually happened. Idempotent and monotone: nothing is ever re-locked here.
 */
class UnlockTechniquesUseCase @Inject constructor(
    private val catalog: TechniqueCatalogRepository,
    private val states: TechniqueStateRepository,
    private val tx: TransactionRunner,
    private val analytics: Analytics,
    private val clock: Clock
) {
    /** Returns the techniques unlocked by this call. */
    suspend operator fun invoke(programDay: Int): List<TechniqueId> {
        val techniques = catalog.catalog()
        val now = clock.instant()
        val unlocked = tx.inTransaction {
            states.ensureRows(techniques.map { it.id })
            techniques.filter {
                (
                    UnlockRules.shouldUnlock(
                        it,
                        programDay
                    ) || (it.introDay == null && !it.retired)
                    ) &&
                    states.unlock(it.id, now, programDay)
            }.map { it.id }
        }
        unlocked.forEach { id ->
            AnalyticsTechnique.of(id.value)?.let {
                analytics.track(Event.TechniqueUnlocked(it, programDay))
            }
        }
        return unlocked
    }
}
