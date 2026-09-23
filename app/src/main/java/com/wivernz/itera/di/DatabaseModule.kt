package com.wivernz.itera.di
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.SeedCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context, clock: Clock): IteraDatabase =
        Room.databaseBuilder(
            context,
            IteraDatabase::class.java,
            "itera.db"
        ).addCallback(
            SeedCallback(clock)
        ).setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()

    @Provides fun trainingDayDao(db: IteraDatabase) = db.trainingDayDao()

    @Provides fun planActivityDao(db: IteraDatabase) = db.planActivityDao()

    @Provides fun focusSessionDao(db: IteraDatabase) = db.focusSessionDao()

    @Provides fun reflectionDao(db: IteraDatabase) = db.reflectionDao()

    @Provides fun reviewItemDao(db: IteraDatabase) = db.reviewItemDao()

    @Provides fun reviewAttemptDao(db: IteraDatabase) = db.reviewAttemptDao()

    @Provides fun learningTopicDao(db: IteraDatabase) = db.learningTopicDao()

    @Provides fun techniqueStateDao(db: IteraDatabase) = db.techniqueStateDao()

    @Provides fun habitStackDao(db: IteraDatabase) = db.habitStackDao()

    @Provides fun eventLogDao(db: IteraDatabase) = db.eventLogDao()
}
