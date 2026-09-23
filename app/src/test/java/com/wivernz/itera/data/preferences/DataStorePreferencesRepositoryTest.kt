package com.wivernz.itera.data.preferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.wivernz.itera.TestLogger
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.UserPreferences
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class DataStorePreferencesRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun defaultsRoundTripAndAtomicUpdate() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { folder.root.resolve("user.preferences_pb") }
        )
        try {
            val logger =
                TestLogger()
            val repo =
                DataStorePreferencesRepository(
                    store,
                    logger
                )
            assertEquals(UserPreferences(), repo.preferences.first())
            val changed = UserPreferences(
                true,
                setOf(
                    Skill.FOCUS,
                    Skill.LEARNING
                ),
                LocalTime.of(
                    9,
                    15
                ),
                LocalTime.of(
                    22,
                    10
                ),
                TimeBudget.LONG,
                ProgramPace.INTENSE,
                ThemePreference.DARK,
                false,
                false,
                false,
                false,
                false,
                LocalDate.of(
                    2026,
                    1,
                    1
                ),
                7,
                3,
                LocalDate.of(
                    2026,
                    1,
                    8
                )
            )
            repo.update { changed }
            assertEquals(changed, repo.preferences.first())
            repo.update {
                it.copy(
                    notifyMorning =
                    true
                )
            }
            assertEquals(
                changed.copy(
                    notifyMorning =
                    true
                ),
                repo.preferences.first()
            )
            coroutineScope {
                repeat(5) {
                    launch {
                        repo.update {
                            it.copy(
                                contentVersion =
                                it.contentVersion + 1
                            )
                        }
                    }
                }
            }
            assertEquals(8, repo.preferences.first().contentVersion)
            store.edit {
                it[themeKey] =
                    "PRIVATE_UNKNOWN"
                it[focusAreasKey] =
                    setOf("FUTURE")
            }
            assertEquals(ThemePreference.SYSTEM, repo.preferences.first().theme)
            assertTrue(logger.warnings.isNotEmpty())
            assertTrue(
                logger.warnings.none {
                    "PRIVATE_UNKNOWN" in it
                }
            )
            repo.update {
                UserPreferences()
            }
            assertEquals(
                UserPreferences(),
                repo.preferences.first()
            )
        } finally {
            scope.cancel()
        }
    }

    @Test fun ioFailureYieldsDefaultsAndWarns() = runTest {
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("private") }
            override suspend fun updateData(
                transform: suspend (Preferences) ->
                Preferences
            ): Preferences = error("unused")
        }
        val logger = TestLogger()
        assertEquals(
            UserPreferences(),
            DataStorePreferencesRepository(
                store,
                logger
            ).preferences.first()
        )
        assertEquals(1, logger.warnings.size)
    }

    @Test fun corruptFileRecoversToDefaults() = runTest {
        val file = folder.newFile("corrupt.preferences_pb").apply { writeText("corrupt") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(
            corruptionHandler =
            androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
                emptyPreferences()
            },

            scope = scope,
            produceFile = { file }
        )
        try {
            assertEquals(
                UserPreferences(),
                DataStorePreferencesRepository(
                    store,
                    TestLogger()
                ).preferences.first()
            )
        } finally {
            scope.cancel()
        }
    }
}
