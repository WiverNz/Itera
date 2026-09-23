package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** training_day; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "training_day",
    indices = [
        Index(
            value = ["date"],
            unique = true
        ), Index(value = ["programDay"], unique = false)
    ]
)
data class TrainingDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDay: Int,
    val date: Long,
    val status: String,
    val carryOverIntent: String? = null,
    val generatorVersion: Int,
    val createdAt: Long,
    val completedAt: Long? = null
)
