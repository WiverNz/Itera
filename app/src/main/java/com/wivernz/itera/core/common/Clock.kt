package com.wivernz.itera.core.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Reads the active time zone again after a system time-zone change. */
fun systemClock(): Clock = object : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
    override fun instant(): Instant = Clock.systemUTC().instant()
}
