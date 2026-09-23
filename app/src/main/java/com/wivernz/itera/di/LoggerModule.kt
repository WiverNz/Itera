package com.wivernz.itera.di
import com.wivernz.itera.BuildConfig
import com.wivernz.itera.core.common.DebugLogger
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.ReleaseLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {
    @Provides @Singleton
    fun logger(): Logger = if (BuildConfig.DEBUG) DebugLogger() else ReleaseLogger()
}
