package com.wivernz.itera
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.wivernz.itera.di.StorageTestEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class StorageLaunchTest {
    @Test fun hiltLaunchStorageAndOnDemandWorkManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val graph = EntryPointAccessors.fromApplication(context, StorageTestEntryPoint::class.java)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertTrue(it.application is IteraApplication) }
            assertNotNull(graph.clock())
            assertNotNull(graph.logger())
            assertNotNull(graph.io())
            runBlocking {
                assertEquals(14, graph.database().techniqueStateDao().observeAll().first().size)
                assertTrue(graph.preferences().preferences.first().currentProgramDay >= 1)
                graph.focus().observe().first()
            }
            assertNotNull(WorkManager.getInstance(context))
        }
    }
}
