package com.wivernz.itera.di

import com.wivernz.itera.BuildConfig
import com.wivernz.itera.core.common.RuntimeChecks
import com.wivernz.itera.core.notifications.WorkReminderScheduler
import com.wivernz.itera.core.notifications.ReminderScheduler
import com.wivernz.itera.data.catalog.AndroidCatalogAssetSource
import com.wivernz.itera.data.catalog.AssetTechniqueCatalogRepository
import com.wivernz.itera.data.catalog.CatalogAssetSource
import com.wivernz.itera.data.catalog.CatalogCache
import com.wivernz.itera.data.catalog.CatalogStrings
import com.wivernz.itera.data.catalog.DataStoreCatalogCache
import com.wivernz.itera.data.catalog.ResourceCatalogStrings
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds @Singleton
    abstract fun catalog(impl: AssetTechniqueCatalogRepository): TechniqueCatalogRepository

    @Binds abstract fun assets(impl: AndroidCatalogAssetSource): CatalogAssetSource

    @Binds @Singleton
    abstract fun strings(impl: ResourceCatalogStrings): CatalogStrings

    @Binds abstract fun cache(impl: DataStoreCatalogCache): CatalogCache

    /** Milestone 009 replaces this binding with the WorkManager scheduler. */
    @Binds @Singleton
    abstract fun reminders(impl: WorkReminderScheduler): ReminderScheduler

    companion object {
        @Provides
        fun runtimeChecks(): RuntimeChecks = RuntimeChecks(failFast = BuildConfig.DEBUG)
    }
}
