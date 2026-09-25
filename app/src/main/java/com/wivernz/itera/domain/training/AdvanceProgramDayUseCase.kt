package com.wivernz.itera.domain.training

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import javax.inject.Inject

/**
 * The only writer of `current_program_day` outside the resets (docs/data/02-datastore-preferences.md section 3).
 * Advances by exactly one for a COMPLETE day whose program day is still current, so replays, re-delivered
 * workers and a crash between the day commit and this write are all harmless. An abandoned day never advances.
 */
class AdvanceProgramDayUseCase @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val preferences: PreferencesRepository,
    private val analytics: Analytics
) {
    suspend operator fun invoke(dayId: Long): Boolean {
        val day = plans.day(dayId) ?: return false
        if (day.status != TrainingDayStatus.COMPLETE || day.completedAt == null) return false
        var advanced = false
        preferences.update { prefs ->
            if (prefs.currentProgramDay == day.programDay) {
                advanced = true
                prefs.copy(currentProgramDay = day.programDay + 1)
            } else {
                advanced = false
                prefs
            }
        }
        if (advanced) analytics.track(Event.ProgramDayAdvanced(day.programDay, day.programDay + 1))
        return advanced
    }

    /** Recovers a completed day whose advance was lost (the preference write happens after the commit). */
    suspend fun reconcile(): Boolean = plans.latestCompletedDay()?.let { invoke(it.id) } ?: false
}
