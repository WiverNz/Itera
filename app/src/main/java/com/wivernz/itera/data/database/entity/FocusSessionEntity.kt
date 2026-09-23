package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** focus_session; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "focus_session",
    foreignKeys = [
        ForeignKey(
            entity = PlanActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["startedAt"],
            unique = false
        ), Index(value = ["activityId"], unique = false)
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val techniqueId: String,
    val taskLabel: String,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val extendedSeconds: Int,
    val completedNaturally: Boolean,
    val startedAt: Long,
    val endedAt: Long
)
