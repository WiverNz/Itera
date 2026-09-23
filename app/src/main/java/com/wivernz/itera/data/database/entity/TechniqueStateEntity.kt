package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** technique_state; docs/data/01-room-schema.md section 2. */
@Entity(tableName = "technique_state")
data class TechniqueStateEntity(
    @PrimaryKey val techniqueId: String,
    val unlockedAt: Long? = null,
    val unlockedOnProgramDay: Int? = null,
    val introCompletedAt: Long? = null
)
