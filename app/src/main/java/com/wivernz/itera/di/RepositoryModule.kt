package com.wivernz.itera.di
import com.wivernz.itera.data.focus.DataStoreFocusTimerRepository
import com.wivernz.itera.data.preferences.DataStorePreferencesRepository
import com.wivernz.itera.data.repository.RoomDataResetRepository
import com.wivernz.itera.data.repository.RoomLearningTopicRepository
import com.wivernz.itera.data.repository.RoomPracticeRecordRepository
import com.wivernz.itera.data.repository.RoomProgressRepository
import com.wivernz.itera.data.repository.RoomReviewRepository
import com.wivernz.itera.data.repository.RoomTechniqueStateRepository
import com.wivernz.itera.data.repository.RoomTrainingPlanRepository
import com.wivernz.itera.data.repository.RoomTransactionRunner
import com.wivernz.itera.domain.repository.DataResetRepository
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.LearningTopicRepository
import com.wivernz.itera.domain.repository.PracticeRecordRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.ProgressStorage
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.ReviewStorage
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.repository.TrainingPlanStorage
import com.wivernz.itera.domain.repository.TransactionRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds @Singleton
    abstract fun focus(impl: DataStoreFocusTimerRepository): FocusTimerRepository

    @Binds @Singleton
    abstract fun training(impl: RoomTrainingPlanRepository): TrainingPlanRepository

    @Binds
    abstract fun trainingStorage(impl: TrainingPlanRepository): TrainingPlanStorage

    @Binds @Singleton
    abstract fun review(impl: RoomReviewRepository): ReviewRepository

    @Binds
    abstract fun reviewStorage(impl: ReviewRepository): ReviewStorage

    @Binds @Singleton
    abstract fun progress(impl: RoomProgressRepository): ProgressRepository

    @Binds
    abstract fun progressStorage(impl: ProgressRepository): ProgressStorage

    @Binds @Singleton
    abstract fun techniqueStates(impl: RoomTechniqueStateRepository): TechniqueStateRepository

    @Binds @Singleton
    abstract fun practiceRecords(impl: RoomPracticeRecordRepository): PracticeRecordRepository

    @Binds @Singleton
    abstract fun dataReset(impl: RoomDataResetRepository): DataResetRepository

    @Binds @Singleton
    abstract fun transactions(impl: RoomTransactionRunner): TransactionRunner

    @Binds @Singleton
    abstract fun topics(impl: RoomLearningTopicRepository): LearningTopicRepository
}
