package com.wivernz.itera.domain.training

import com.wivernz.itera.core.common.result.DomainError
import com.wivernz.itera.core.common.result.DomainException
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ActivityState.AVAILABLE
import com.wivernz.itera.domain.model.ActivityState.COMPLETED
import com.wivernz.itera.domain.model.ActivityState.EXPIRED
import com.wivernz.itera.domain.model.ActivityState.IN_PROGRESS
import com.wivernz.itera.domain.model.ActivityState.SCHEDULED
import com.wivernz.itera.domain.model.ActivityState.SKIPPED
import com.wivernz.itera.domain.model.ActivityState.SNOOZED
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStateMachineTest {
    private val now = Instant.parse("2026-03-28T10:00:00Z")
    private val facts = TransitionFacts(
        exerciseType = ExerciseType.FOCUS_TIMER,
        source = ActivitySource.FOCUS_SUGGESTION,
        optional = true,
        becomesAvailableAt = now,
        snoozedUntil = now,
        endOfDay = now.plusSeconds(12 * 3600)
    )
    private val focus = ActivityResult.Focus("t", 60, 60, 0, true)

    /** One representative per event; `when` below is exhaustive, so a new event fails to compile. */
    private fun sample(kind: String): ActivityEvent = when (kind) {
        "Reach" -> ActivityEvent.Reach
        "Start" -> ActivityEvent.Start
        "Snooze" -> ActivityEvent.Snooze(now.plusSeconds(3600))
        "Skip" -> ActivityEvent.Skip
        "Complete" -> ActivityEvent.Complete(focus)
        "Abandon" -> ActivityEvent.Abandon
        else -> ActivityEvent.DayRollover
    }

    private fun name(event: ActivityEvent) = when (event) {
        ActivityEvent.Reach -> "Reach"
        ActivityEvent.Start -> "Start"
        is ActivityEvent.Snooze -> "Snooze"
        ActivityEvent.Skip -> "Skip"
        is ActivityEvent.Complete -> "Complete"
        ActivityEvent.Abandon -> "Abandon"
        ActivityEvent.DayRollover -> "DayRollover"
    }

    private val events =
        listOf("Reach", "Start", "Snooze", "Skip", "Complete", "Abandon", "DayRollover")

    private val documented: Map<Pair<ActivityState, String>, ActivityState> = mapOf(
        (SCHEDULED to "Reach") to AVAILABLE,
        (SCHEDULED to "DayRollover") to EXPIRED,
        (AVAILABLE to "Start") to IN_PROGRESS,
        (AVAILABLE to "Snooze") to SNOOZED,
        (AVAILABLE to "Skip") to SKIPPED,
        (AVAILABLE to "DayRollover") to EXPIRED,
        (SNOOZED to "Reach") to AVAILABLE,
        (SNOOZED to "Start") to IN_PROGRESS,
        (SNOOZED to "DayRollover") to EXPIRED,
        (IN_PROGRESS to "Complete") to COMPLETED,
        (IN_PROGRESS to "Abandon") to AVAILABLE,
        (IN_PROGRESS to "Skip") to SKIPPED,
        (IN_PROGRESS to "DayRollover") to EXPIRED
    )

    @Test fun everyStateEventPairIsTheDocumentedTargetOrIllegal() {
        var checked = 0
        ActivityState.entries.forEach { state ->
            events.map(::sample).forEach { event ->
                val result = ActivityStateMachine.transition(state, event, now, facts)
                val expected = documented[state to name(event)]
                if (expected != null) {
                    assertEquals("$state x ${name(event)}", expected, result.getOrThrow())
                } else {
                    val error = (result.exceptionOrNull() as DomainException).error
                    assertEquals(DomainError.IllegalTransition(state, name(event)), error)
                }
                checked++
            }
        }
        assertEquals(ActivityState.entries.size * events.size, checked)
        assertEquals(49, checked)
        assertEquals(13, documented.size)
    }

    @Test fun skipGuardAllowsOnlyOptionalAndReflection() {
        val required = facts.copy(optional = false, source = ActivitySource.PROGRAM)
        listOf(AVAILABLE, IN_PROGRESS).forEach { state ->
            assertTrue(
                ActivityStateMachine.transition(state, ActivityEvent.Skip, now, required).isFailure
            )
            assertTrue(
                ActivityStateMachine.transition(
                    state,
                    ActivityEvent.Skip,
                    now,
                    required.copy(source = ActivitySource.REFLECTION)
                ).isSuccess
            )
        }
    }

    @Test fun snoozeGuardNeedsLaterTodayAndReachWaits() {
        fun snooze(until: Instant) =
            ActivityStateMachine.transition(AVAILABLE, ActivityEvent.Snooze(until), now, facts)
        assertTrue(snooze(now).isFailure)
        assertTrue(snooze(now.plusSeconds(1)).isSuccess)
        assertTrue(snooze(facts.endOfDay).isFailure)
        assertTrue(snooze(facts.endOfDay.minusSeconds(1)).isSuccess)

        val later = facts.copy(
            becomesAvailableAt = now.plusSeconds(1),
            snoozedUntil = now.plusSeconds(1)
        )
        assertTrue(
            ActivityStateMachine.transition(SCHEDULED, ActivityEvent.Reach, now, later).isFailure
        )
        assertTrue(
            ActivityStateMachine.transition(SNOOZED, ActivityEvent.Reach, now, later).isFailure
        )
        val atTime = now.plusSeconds(1)
        assertTrue(
            ActivityStateMachine.transition(SCHEDULED, ActivityEvent.Reach, atTime, later).isSuccess
        )
        assertTrue(
            ActivityStateMachine.transition(SNOOZED, ActivityEvent.Reach, atTime, later).isSuccess
        )
    }

    @Test fun completeRejectsAResultOfTheWrongType() {
        val wrong = ActivityEvent.Complete(ActivityResult.Template(emptyMap()))
        assertTrue(ActivityStateMachine.transition(IN_PROGRESS, wrong, now, facts).isFailure)
    }

    @Test fun availabilityByDayPart() {
        fun daytime(morning: String) = becomesAvailableAt(
            DayPart.DAYTIME,
            UserPreferences(morningTime = LocalTime.parse(morning))
        )
        assertEquals(LocalTime.of(11, 0), daytime("06:00"))
        assertEquals(LocalTime.of(11, 0), daytime("08:30"))
        assertEquals(LocalTime.of(14, 30), daytime("13:00"))
        assertEquals(LocalTime.of(23, 59), daytime("23:30"))
        val prefs = UserPreferences(eveningTime = LocalTime.of(21, 0))
        assertEquals(LocalTime.MIDNIGHT, becomesAvailableAt(DayPart.MORNING, prefs))
        assertEquals(LocalTime.of(20, 0), becomesAvailableAt(DayPart.EVENING, prefs))
        assertEquals(
            LocalTime.MIDNIGHT,
            becomesAvailableAt(DayPart.EVENING, UserPreferences(eveningTime = LocalTime.of(0, 30)))
        )
        assertEquals(LocalTime.of(11, 0), availableFrom(DayPart.DAYTIME, null))
        assertEquals(LocalTime.of(20, 0), availableFrom(DayPart.EVENING, null))
        val zone = ZoneId.of("Europe/Berlin")
        val date = LocalDate.of(2026, 3, 29)
        assertEquals(
            Instant.parse("2026-03-29T09:00:00Z"),
            availableAtInstant(DayPart.DAYTIME, LocalTime.of(11, 0), date, zone)
        )
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), endOfDay(date, zone))
    }

    @Test fun resultTypesMapOneToOne() {
        assertEquals(ExerciseType.entries.size, ExerciseType.entries.map { it }.toSet().size)
        assertEquals(ExerciseType.FOCUS_TIMER, resultTypeOf(focus))
    }
}
