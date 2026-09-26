package com.wivernz.itera.di

import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.domain.focus.FocusServiceLauncher
import com.wivernz.itera.domain.focus.FocusSessionController
import com.wivernz.itera.domain.repository.FocusTimerRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.AbandonActivityUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import com.wivernz.itera.feature.focus.AndroidFocusServiceLauncher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FocusModule {
    @Binds
    abstract fun launcher(impl: AndroidFocusServiceLauncher): FocusServiceLauncher

    companion object {
        @Provides
        @Singleton
        @Suppress("LongParameterList")
        fun controller(
            timers: FocusTimerRepository,
            plans: TrainingPlanRepository,
            start: StartActivityUseCase,
            abandon: AbandonActivityUseCase,
            complete: CompleteActivityUseCase,
            launcher: FocusServiceLauncher,
            analytics: Analytics,
            clock: Clock,
            logger: Logger
        ) = FocusSessionController(
            timers, plans, start, abandon, complete, launcher, analytics, clock, logger
        )
    }
}
