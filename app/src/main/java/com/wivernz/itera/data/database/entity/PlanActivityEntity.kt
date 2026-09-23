package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** plan_activity; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "plan_activity",
    foreignKeys = [
        ForeignKey(
            entity = TrainingDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["trainingDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReviewItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewItemId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = LearningTopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            value = ["trainingDayId"],
            unique = false
        ),
        Index(
            value = [
                "techniqueId",
                "practiceDate"
            ],
            unique = false
        ),
        Index(
            value = ["state"],
            unique = false
        ),
        Index(
            value = ["reviewItemId"],
            unique = false
        ),
        Index(
            value = ["topicId"],
            unique = false
        )
    ]
)
data class PlanActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainingDayId: Long,
    val techniqueId: String,
    val exerciseType: String,
    val source: String,
    val orderIndex: Int,
    val dayPart: String,
    val copyKey: String,
    val copyArgs: String,
    val estimatedMinutes: Int,
    val optional: Boolean,
    val state: String,
    val scheduledAtMinutes: Int? = null,
    val snoozedUntil: Long? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val durationSeconds: Int? = null,
    val difficulty: String? = null,
    val note: String? = null,
    val resultPayload: String? = null,
    val draftPayload: String? = null,
    val reviewItemId: Long? = null,
    val topicId: Long? = null,
    val practiceDate: Long
)
