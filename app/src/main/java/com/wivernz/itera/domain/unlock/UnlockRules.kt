package com.wivernz.itera.domain.unlock

import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId

/**
 * docs/engine/04-unlock-rules.md. A technique unlocks when the program reaches its `introDay`; that is the
 * whole rule. No performance gate, by design.
 */
object UnlockRules {
    val SEEDED: TechniqueId = TechniqueId("daily_reflection")

    fun shouldUnlock(technique: Technique, programDay: Int): Boolean {
        val introDay = technique.introDay
        return !technique.retired && introDay != null && programDay >= introDay
    }

    /**
     * The full unlocked set on [programDay]: seeded techniques, everything whose `introDay` is reached, and
     * anything already unlocked (monotone - a technique met before a content change stays met).
     */
    fun unlockedSet(
        catalog: List<Technique>,
        programDay: Int,
        alreadyUnlocked: Set<TechniqueId> = emptySet()
    ): Set<TechniqueId> = catalog.filter {
        it.id in alreadyUnlocked ||
            (!it.retired && it.introDay == null) ||
            shouldUnlock(it, programDay)
    }.map { it.id }.toSet()

    /** The library's "Day {n}" chip; null for the seeded reflection. */
    fun unlocksOnDay(technique: Technique): Int? = technique.introDay
}
