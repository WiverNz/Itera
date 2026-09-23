package com.wivernz.itera.data.database
/** ADR-0014. These describe ownership, not reset execution (milestone 004). */
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
