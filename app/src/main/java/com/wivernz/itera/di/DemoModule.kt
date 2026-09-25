package com.wivernz.itera.di

import com.wivernz.itera.domain.demo.DemoDataLoader
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The demo loader is optional: only the debug source set binds one (D-11). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DemoModule {
    @BindsOptionalOf
    abstract fun demoDataLoader(): DemoDataLoader
}
