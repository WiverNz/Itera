package com.wivernz.itera.data.database.relation
import androidx.room.Embedded
import androidx.room.Relation
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
data class TrainingDayWithActivities(
    @Embedded val day: TrainingDayEntity,
    @Relation(parentColumn = "id", entityColumn = "trainingDayId") val activities:
    List<PlanActivityEntity>
)
