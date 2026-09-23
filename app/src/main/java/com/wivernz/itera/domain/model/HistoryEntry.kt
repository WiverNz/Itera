package com.wivernz.itera.domain.model
import java.time.LocalDate
/** Domain model section 7; minimal storage projection, source-of-truth section 11. */
data class HistoryEntry(val date: LocalDate, val activity: PlanActivity)
