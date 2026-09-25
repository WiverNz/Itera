package com.wivernz.itera.data.database.relation

/** One technique's counted-completion aggregate. Dates are epoch days. */
data class TechniqueFactsRow(
    val techniqueId: String,
    val distinctDays: Int,
    val totalUses: Int,
    val firstUse: Long?,
    val lastUse: Long?,
    val usedInCombination: Boolean
)

data class CompletionRow(val practiceDate: Long, val techniqueId: String)
