package com.wivernz.itera.di

import com.wivernz.itera.domain.coach.CoachFeedbackProvider
import com.wivernz.itera.domain.coach.NoOpCoachFeedbackProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoachModule {
    @Binds
    abstract fun coach(impl: NoOpCoachFeedbackProvider): CoachFeedbackProvider
}
