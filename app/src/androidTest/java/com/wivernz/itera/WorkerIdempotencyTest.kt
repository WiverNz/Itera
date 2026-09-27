package com.wivernz.itera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.wivernz.itera.core.notifications.work.DailyPlanWorker
import com.wivernz.itera.core.notifications.work.EventLogTrimWorker
import com.wivernz.itera.di.StorageTestEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkerIdempotencyTest {
    @Test fun hiltWorkersRunAndDailyPlanIsUnique() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as IteraApplication
        val graph = EntryPointAccessors.fromApplication(context, StorageTestEntryPoint::class.java)
        val previous = graph.preferences().preferences.first()
        try {
            graph.preferences().update { it.copy(onboardingCompleted = true) }
            val first = TestListenableWorkerBuilder<DailyPlanWorker>(
                context
            ).setWorkerFactory(app.workerFactory).build()
            assertEquals(ListenableWorker.Result.success(), first.doWork())
            val second = TestListenableWorkerBuilder<DailyPlanWorker>(
                context
            ).setWorkerFactory(app.workerFactory).build()
            assertEquals(ListenableWorker.Result.success(), second.doWork())
            val day = graph.ensureToday()()
            val count = graph.database().openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM training_day WHERE date = ?",
                arrayOf(day.date.toEpochDay())
            ).use {
                it.moveToFirst()
                it.getInt(0)
            }
            assertEquals(1, count)
            val trim = TestListenableWorkerBuilder<EventLogTrimWorker>(
                context
            ).setWorkerFactory(app.workerFactory).build()
            assertEquals(ListenableWorker.Result.success(), trim.doWork())
            assertTrue(graph.database().eventLogDao().exportSince(0).size <= 2000)
        } finally {
            graph.preferences().update { previous }
            graph.reminders().rescheduleAll()
        }
    }
}
