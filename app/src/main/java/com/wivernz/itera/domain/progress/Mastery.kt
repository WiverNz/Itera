package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

/**
 * docs/engine/03-mastery-and-progress.md section 2. A classification of current facts; nothing is
 * stored.
 */
object Mastery {
    const val PRACTICED_DAYS = 3
    const val APPLIED_USES = 6
    const val APPLIED_SPAN_DAYS = 14

    fun masteryOf(
        unlocked: Boolean,
        introCompleted: Boolean,
        distinctDays: Int,
        totalUses: Int,
        firstUse: LocalDate?,
        lastUse: LocalDate?,
        usedInCombination: Boolean
    ): MasteryLevel = when {
        !unlocked || !introCompleted -> MasteryLevel.NONE
        usedInCombination -> MasteryLevel.INTEGRATED
        totalUses >= APPLIED_USES && firstUse != null && lastUse != null &&
            DAYS.between(firstUse, lastUse) >= APPLIED_SPAN_DAYS -> MasteryLevel.APPLIED
        distinctDays >= PRACTICED_DAYS -> MasteryLevel.PRACTICED
        else -> MasteryLevel.MET
    }

    /** Section 2.1: the gap to the next level. */
    fun nextLevelHint(level: MasteryLevel, distinctDays: Int, totalUses: Int): LevelHint =
        when (level) {
            MasteryLevel.NONE -> LevelHint.Unavailable
            MasteryLevel.MET ->
                LevelHint.ToPracticed((PRACTICED_DAYS - distinctDays).coerceAtLeast(1))
            MasteryLevel.PRACTICED ->
                LevelHint.ToApplied((APPLIED_USES - totalUses).coerceAtLeast(0))
            MasteryLevel.APPLIED -> LevelHint.ToIntegrated
            MasteryLevel.INTEGRATED -> LevelHint.Integrated
        }
}
