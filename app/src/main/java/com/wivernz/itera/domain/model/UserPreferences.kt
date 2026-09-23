package com.wivernz.itera.domain.model
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
/** Domain model, docs/data/00-domain-model.md section 8. */
data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val focusAreas: Set<Skill> = emptySet(),
    val morningTime: LocalTime = LocalTime.of(8, 30),
    val eveningTime: LocalTime = LocalTime.of(21, 0),
    val timeBudget: TimeBudget = TimeBudget.STANDARD,
    val pace: ProgramPace = ProgramPace.STANDARD,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val notifyMorning: Boolean = true,
    val notifyFocus: Boolean = true,
    val notifyReviews: Boolean = true,
    val notifyEvening: Boolean = true,
    val aiCoachEnabled: Boolean = false,
    val programStartedOn: LocalDate? = null,
    val currentProgramDay: Int = 1,
    val contentVersion: Int = 0,
    val lastSeenDayComplete: LocalDate? = null
) {
    init {
        require(currentProgramDay >= 1)
        require(contentVersion >= 0)
        require(!aiCoachEnabled)
    }
}
