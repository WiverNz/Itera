package com.wivernz.itera.di
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.focus.FocusTimerSerializer
import com.wivernz.itera.domain.model.FocusTimerState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun preferences(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher,
        logger: Logger
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler {
            logger.w("DataStoreModule", "Preferences corrupt; using defaults")
            emptyPreferences()
        },
        scope = CoroutineScope(SupervisorJob() + io),
        produceFile = {
            context.preferencesDataStoreFile("user_prefs")
        }
    )

    @Provides @Singleton
    fun focus(
        @ApplicationContext context: Context,
        @IoDispatcher io: CoroutineDispatcher
    ): DataStore<FocusTimerState?> = DataStoreFactory.create(
        serializer = FocusTimerSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { null },
        scope = CoroutineScope(SupervisorJob() + io),
        produceFile = {
            context.dataStoreFile("focus_timer.pb")
        }
    )
}
