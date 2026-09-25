package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ResetTier
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.repository.DataResetRepository
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import com.wivernz.itera.domain.unlock.UnlockRules
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * ADR-0014 tier 1, "Reset program": deletes training days, activities, focus sessions, reflections, reviews
 * and attempts in one transaction, clears every unlock except Daily reflection, and returns to Day 1. Keeps
 * preferences, learning topics and habit stacks. Scheduled work is cancelled first and rescheduled after.
 */
class ResetProgramUseCase @Inject constructor(
    private val reset: DataResetRepository,
    private val states: TechniqueStateRepository,
    private val preferences: PreferencesRepository,
    private val focusTimer: FocusTimerRepository,
    private val tx: TransactionRunner,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke() {
        reminders.cancelAll()
        tx.inTransaction {
            reset.deleteProgramData()
            states.resetProgramFacts(setOf(UnlockRules.SEEDED), clock.instant())
        }
        focusTimer.clear()
        preferences.update {
            it.copy(
                currentProgramDay = 1,
                programStartedOn = LocalDate.now(clock),
                lastSeenDayComplete = null
            )
        }
        reminders.rescheduleAll()
        analytics.track(Event.ResetPerformed(ResetTier.PROGRAM))
    }
}
