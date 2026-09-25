package com.wivernz.itera.domain.training

import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.repository.DataResetRepository
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.unlock.UnlockRules
import java.time.Clock
import javax.inject.Inject

/**
 * ADR-0014 tier 2, "Erase everything": also deletes learning topics, habit stacks and the event log and clears
 * both DataStore files, returning the app to first run, onboarding included.
 */
class EraseAllDataUseCase @Inject constructor(
    private val reset: DataResetRepository,
    private val states: TechniqueStateRepository,
    private val preferences: PreferencesRepository,
    private val focusTimer: FocusTimerRepository,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        reminders.cancelAll()
        tx.inTransaction {
            reset.deleteAllData()
            states.resetProgramFacts(setOf(UnlockRules.SEEDED), clock.instant())
        }
        focusTimer.clear()
        preferences.clear()
        reminders.rescheduleAll()
    }
}
