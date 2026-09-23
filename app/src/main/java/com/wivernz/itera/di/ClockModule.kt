package com.wivernz.itera.di
import com.wivernz.itera.core.common.systemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides @Singleton
    fun clock(): Clock = systemClock()
}
