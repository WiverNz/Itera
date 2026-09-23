package com.wivernz.itera.di
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.LocalAnalytics
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds abstract fun analytics(impl: LocalAnalytics): Analytics
}
