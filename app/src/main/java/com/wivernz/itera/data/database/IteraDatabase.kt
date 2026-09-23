package com.wivernz.itera.data.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.wivernz.itera.data.database.dao.EventLogDao
import com.wivernz.itera.data.database.dao.FocusSessionDao
import com.wivernz.itera.data.database.dao.HabitStackDao
import com.wivernz.itera.data.database.dao.LearningTopicDao
import com.wivernz.itera.data.database.dao.PlanActivityDao
import com.wivernz.itera.data.database.dao.ReflectionDao
import com.wivernz.itera.data.database.dao.ReviewAttemptDao
import com.wivernz.itera.data.database.dao.ReviewItemDao
import com.wivernz.itera.data.database.dao.TechniqueStateDao
import com.wivernz.itera.data.database.dao.TrainingDayDao
import com.wivernz.itera.data.database.entity.EventLogEntity
import com.wivernz.itera.data.database.entity.FocusSessionEntity
import com.wivernz.itera.data.database.entity.HabitStackEntity
import com.wivernz.itera.data.database.entity.LearningTopicEntity
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.ReflectionEntity
import com.wivernz.itera.data.database.entity.ReviewAttemptEntity
import com.wivernz.itera.data.database.entity.ReviewItemEntity
import com.wivernz.itera.data.database.entity.TechniqueStateEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
@Database(
    entities = [
        TrainingDayEntity::class,
        PlanActivityEntity::class,
        FocusSessionEntity::class,
        ReflectionEntity::class,
        ReviewItemEntity::class,
        ReviewAttemptEntity::class,
        LearningTopicEntity::class,
        TechniqueStateEntity::class,
        HabitStackEntity::class,
        EventLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class IteraDatabase : RoomDatabase() {
    abstract fun trainingDayDao(): TrainingDayDao
    abstract fun planActivityDao(): PlanActivityDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun reviewItemDao(): ReviewItemDao
    abstract fun reviewAttemptDao(): ReviewAttemptDao
    abstract fun learningTopicDao(): LearningTopicDao
    abstract fun techniqueStateDao(): TechniqueStateDao
    abstract fun habitStackDao(): HabitStackDao
    abstract fun eventLogDao(): EventLogDao
}
