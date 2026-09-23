package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** review_item; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "review_item",
    foreignKeys = [
        ForeignKey(
            entity = LearningTopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = [
                "dueOn",
                "state"
            ],
            unique = false
        ),
        Index(
            value = ["techniqueId"],
            unique = false
        ),
        Index(
            value = ["topicId"],
            unique = false
        )
    ]
)
data class ReviewItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val techniqueId: String,
    val topicId: Long? = null,
    val prompt: String,
    val sourceActivityId: Long,
    val sourceAnswer: String,
    val stageIndex: Int,
    val dueOn: Long,
    val lastReviewedOn: Long? = null,
    val state: String,
    val createdAt: Long
)
