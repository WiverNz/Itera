package com.wivernz.itera.core.common
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test
class FakeClockTest {
    @Test fun crossesDstAndSetsLocalDate() {
        val clock = FakeClock(Instant.parse("2026-03-29T00:30:00Z"))
        clock.advance(Duration.ofHours(1))
        assertEquals(LocalTime.of(3, 30), LocalTime.now(clock))
        clock.setDate(LocalDate.of(2026, 3, 30))
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), clock.instant())
        assertEquals(clock.instant(), clock.withZone(ZoneOffset.UTC).instant())
    }
}
