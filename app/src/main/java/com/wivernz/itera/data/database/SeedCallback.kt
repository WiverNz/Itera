package com.wivernz.itera.data.database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Clock
/** Temporary catalogue coupling required by detailed issue 006; replace in milestone 004. */
class SeedCallback(private val clock: Clock) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = clock.millis()
        techniqueIds.forEach { id ->
            val unlocked = id == "two_minute_rule" || id == "daily_reflection"
            db.execSQL(
                "INSERT INTO technique_state (techniqueId, unlockedAt, unlockedOnProgramDay, introCompletedAt) VALUES (?, ?, ?, NULL)",
                arrayOf<Any?>(
                    id,
                    if (unlocked) now else null,
                    if (unlocked) 1 else null
                )
            )
        }
    }
    companion object {
        val techniqueIds = listOf(
            "two_minute_rule",
            "pomodoro",
            "eisenhower_matrix",
            "feynman",
            "habit_stacking",
            "five_second_rule",
            "daily_reflection",
            "spaced_repetition",
            "deep_work",
            "premortem",
            "two_list_strategy",
            "information_diet",
            "one_percent_improvement",
            "pareto_principle"
        )
    }
}
