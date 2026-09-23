package com.wivernz.itera.core.common
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
class FakeClock(
    private var now: Instant = Instant.parse("2026-03-28T12:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("Europe/Berlin")
) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = FakeClock(now, zone)
    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
    fun setDate(date: LocalDate) {
        now = date.atStartOfDay(zoneId).toInstant()
    }
}
