package com.wivernz.itera.data.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/** learning_topic; docs/data/01-room-schema.md section 2. */
@Entity(
    tableName = "learning_topic",
    indices = [Index(value = ["archived", "createdAt"], unique = false)]
)
data class LearningTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val archived: Boolean,
    val createdAt: Long
)
