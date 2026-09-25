package com.wivernz.itera.domain.onboarding

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import com.wivernz.itera.domain.unlock.UnlockTechniquesUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/** What the three onboarding steps collected. */
data class OnboardingChoices(
    val focusAreas: List<Skill>,
    val morningTime: LocalTime,
    val eveningTime: LocalTime,
    val timeBudget: TimeBudget,
    val notificationsGranted: Boolean
)

/**
 * "Start Day 1" (docs/ux/02-screen-specs-onboarding.md section 4): persist preferences, seed unlocks, generate the
 * plan, schedule reminders. Only the preference write can fail the call; when unlocking or plan generation fails,
 * onboarding still completes and Today generates its own plan.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val preferences: PreferencesRepository,
    private val unlock: UnlockTechniquesUseCase,
    private val ensureToday: EnsureTodayPlanUseCase,
    private val reminders: ReminderScheduler,
    private val analytics: Analytics,
    private val logger: Logger,
    private val clock: Clock
) {
    suspend operator fun invoke(choices: OnboardingChoices) {
        val today = LocalDate.now(clock)
        val granted = choices.notificationsGranted
        preferences.update {
            it.copy(
                onboardingCompleted = true,
                focusAreas = choices.focusAreas.toSet(),
                morningTime = choices.morningTime,
                eveningTime = choices.eveningTime,
                timeBudget = choices.timeBudget,
                notifyMorning = granted && it.notifyMorning,
                notifyFocus = granted && it.notifyFocus,
                notifyReviews = granted && it.notifyReviews,
                notifyEvening = granted && it.notifyEvening,
                programStartedOn = it.programStartedOn ?: today
            )
        }
        attempt("Day 1 plan not generated during onboarding") {
            unlock(preferences.preferences.first().currentProgramDay)
            ensureToday()
        }
        attempt("Reminders not scheduled during onboarding") { reminders.rescheduleAll() }
        analytics.track(
            Event.OnboardingCompleted(
                focusAreaCount = choices.focusAreas.size,
                timeBudget = choices.timeBudget,
                morningHour = choices.morningTime.hour,
                eveningHour = choices.eveningTime.hour,
                notificationsGranted = granted
            )
        )
    }

    private suspend fun attempt(failure: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.w(TAG, failure)
        }
    }
}

private const val TAG = "Onboarding"
