package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TransactionRunner
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Persists the pure rollover (docs/engine/00-exercise-state-machine.md section 6) for every past day. */
class RollOverDayUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val tx: TransactionRunner,
    private val advance: AdvanceProgramDayUseCase,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val clock: Clock
) {
    suspend operator fun invoke(today: LocalDate): List<RolloverOutcome> {
        val now = clock.instant()
        val outcomes = tx.inTransaction {
            plans.daysBefore(today).filter(DayRollover::needsRollover).map { day ->
                val outcome = DayRollover.rollOver(day)
                plans.expire(outcome.expiredActivityIds)
                plans.updateDayStatus(
                    day.id,
                    outcome.status,
                    if (outcome.status ==
                        TrainingDayStatus.COMPLETE
                    ) {
                        day.completedAt ?: now
                    } else {
                        null
                    }
                )
                outcome
            }
        }
        outcomes.forEach { outcome ->
            outcome.expiredActivityIds.forEach { reminders.cancelForActivity(it) }
            analytics.track(Event.DayRolledOver(outcome.status, outcome.expiredActivityIds.size))
            if (outcome.status == TrainingDayStatus.COMPLETE) advance(outcome.dayId)
        }
        return outcomes
    }
}
