package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** habit_stack; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "habit_stack",
    foreignKeys = [
        ForeignKey(
            entity = PlanActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["activityId"], unique = false)]
)
data class HabitStackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long? = null,
    val anchor: String,
    val habit: String,
    val nudgeEnabled: Boolean,
    val nudgeTimeMinutes: Int? = null,
    val createdAt: Long,
    val archived: Boolean
)
