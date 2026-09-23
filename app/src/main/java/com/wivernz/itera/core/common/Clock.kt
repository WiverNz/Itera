package com.wivernz.itera.core.common
import java.time.Clock
/** The only production source of ambient wall-clock time. */
fun systemClock(): Clock = Clock.systemDefaultZone()
