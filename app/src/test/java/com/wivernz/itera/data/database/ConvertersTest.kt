package com.wivernz.itera.data.database
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.TrainingDayStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class ConvertersTest {
    @Test fun temporalAndEnumRoundTrips() {
        val c = Converters()
        listOf(
            Instant.EPOCH,
            Instant.parse("2026-03-29T01:00:00Z")
        )
            .forEach {
                assertEquals(
                    it,
                    c.instant(c.millis(it))
                )
            }
        listOf(
            LocalDate.ofEpochDay(0),
            LocalDate.of(
                2026,
                3,
                29
            )
        )
            .forEach {
                assertEquals(
                    it,
                    c.date(c.epochDay(it))
                )
            }
        listOf(
            LocalTime.MIDNIGHT,
            LocalTime.of(
                23,
                59
            )
        )
            .forEach {
                assertEquals(
                    it,
                    c.time(c.minutes(it))
                )
            }
        assertNull(c.date(null))
        assertNull(c.epochDay(null))
        assertNull(c.time(null))
        assertNull(c.minutes(null))
        assertNull(c.instant(null))
        assertNull(c.millis(null))
        ActivityState.entries.forEach {
            assertEquals(
                it,
                c.activityState(c.activityStateName(it))
            )
        }
        ActivitySource.entries.forEach {
            assertEquals(
                it,
                c.activitySource(c.activitySourceName(it))
            )
        }
        ExerciseType.entries.forEach {
            assertEquals(
                it,
                c.exerciseType(c.exerciseTypeName(it))
            )
        }
        DayPart.entries.forEach { assertEquals(it, c.dayPart(c.dayPartName(it))) }
        Difficulty.entries.forEach { assertEquals(it, c.difficulty(c.difficultyName(it))) }
        RecallGrade.entries.forEach {
            assertEquals(
                it,
                c.recallGrade(c.recallGradeName(it))
            )
        }
        ReviewState.entries.forEach {
            assertEquals(
                it,
                c.reviewState(c.reviewStateName(it))
            )
        }
        TrainingDayStatus.entries.forEach {
            assertEquals(
                it,
                c.trainingDayStatus(c.trainingDayStatusName(it))
            )
        }
    }
}
