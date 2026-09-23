package com.wivernz.itera.di
import com.wivernz.itera.core.common.dispatchers.DefaultDispatcher
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.core.common.dispatchers.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun computation(): CoroutineDispatcher = Dispatchers.Default

    @Provides @MainDispatcher
    fun main(): CoroutineDispatcher = Dispatchers.Main
}
