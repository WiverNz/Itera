package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** review_attempt; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "review_attempt",
    foreignKeys = [
        ForeignKey(
            entity = ReviewItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["reviewItemId"],
            onDelete = ForeignKey.CASCADE
        ), ForeignKey(
            entity = PlanActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["reviewItemId"],
            unique = false
        ), Index(value = ["activityId"], unique = false)
    ]
)
data class ReviewAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reviewItemId: Long,
    val activityId: Long,
    val answer: String,
    val grade: String,
    val stageBefore: Int,
    val stageAfter: Int,
    val createdAt: Long
)
