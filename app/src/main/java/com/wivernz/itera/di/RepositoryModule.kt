package com.wivernz.itera.di
import com.wivernz.itera.data.focus.DataStoreFocusTimerRepository
import com.wivernz.itera.data.preferences.DataStorePreferencesRepository
import com.wivernz.itera.data.repository.RoomLearningTopicRepository
import com.wivernz.itera.data.repository.RoomProgressRepository
import com.wivernz.itera.data.repository.RoomReviewRepository
import com.wivernz.itera.data.repository.RoomTrainingPlanRepository
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.LearningTopicRepository
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressStorage
import com.wivernz.itera.domain.repository.ReviewStorage
import com.wivernz.itera.domain.repository.TrainingPlanStorage
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
    abstract fun training(impl: RoomTrainingPlanRepository): TrainingPlanStorage

    @Binds @Singleton
    abstract fun review(impl: RoomReviewRepository): ReviewStorage

    @Binds @Singleton
    abstract fun progress(impl: RoomProgressRepository): ProgressStorage

    @Binds @Singleton
    abstract fun topics(impl: RoomLearningTopicRepository): LearningTopicRepository
}
