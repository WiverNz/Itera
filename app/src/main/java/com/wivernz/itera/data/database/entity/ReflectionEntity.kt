package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** reflection_entry; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "reflection_entry",
    foreignKeys = [
        ForeignKey(
            entity = TrainingDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["trainingDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["trainingDayId"],
            unique = true
        ), Index(value = ["date"], unique = false)
    ]
)
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainingDayId: Long,
    val date: Long,
    val wentWell: String? = null,
    val wentWellChips: String,
    val didNotGoWell: String? = null,
    val didNotGoWellChips: String,
    val tomorrowChange: String? = null,
    val skipped: Boolean,
    val createdAt: Long
)
