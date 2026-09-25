package com.wivernz.itera.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Clock

/** One catalogue technique as the seed needs it. */
data class SeedTechnique(val id: String, val introDay: Int?)

/**
 * Seeds `technique_state` from the catalogue (docs/data/01-room-schema.md section 7): one locked row per
 * technique, then Daily reflection (no intro day) and the Day-1 technique unlocked on program day 1.
 */
class SeedCallback(private val clock: Clock, private val techniques: () -> List<SeedTechnique>) :
    RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = clock.millis()
        techniques().forEach { technique ->
            val unlocked = technique.introDay == null || technique.introDay <= 1
            db.execSQL(
                "INSERT INTO technique_state (techniqueId, unlockedAt, unlockedOnProgramDay, introCompletedAt) VALUES (?, ?, ?, NULL)",
                arrayOf<Any?>(
                    technique.id,
                    if (unlocked) now else null,
                    if (unlocked) 1 else null
                )
            )
        }
    }
}
