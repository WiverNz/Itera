package com.wivernz.itera.data.database

/**
 * ADR-0014 table ownership. `program` rows are deleted by "Reset program"; `resetFacts` rows are kept but their
 * unlock facts cleared by both tiers; `eraseOnly` rows are deleted by "Erase everything" alone.
 * `ResetCoverageTest` fails when a table is in no tier or when a tier's table has no delete.
 */
object ResetTiers {
    val program =
        setOf(
            "training_day",
            "plan_activity",
            "focus_session",
            "reflection_entry",
            "review_item",
            "review_attempt"
        )
    val resetFacts = setOf("technique_state")
    val eraseOnly = setOf("learning_topic", "habit_stack", "event_log")
}
