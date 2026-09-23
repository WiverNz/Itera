package com.wivernz.itera.analytics
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.data.database.dao.EventLogDao
import com.wivernz.itera.data.database.entity.EventLogEntity
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.fail
import org.junit.Test
class LocalAnalyticsTest {
    @Test fun writesOffCallerThreadAndSwallowsFailures() = runTest {
        val caller = Thread.currentThread()
        val written = CompletableDeferred<EventLogEntity>()
        val thread = CompletableDeferred<Thread>()
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val dao = object : EventLogDao {
            var fail = true
            override suspend fun insert(row: EventLogEntity): Long {
                thread.complete(Thread.currentThread())
                if (fail) {
                    fail = false
                    throw IllegalStateException("synthetic")
                }
                written.complete(row)
                return 1
            }
            override suspend fun trimTo(limit: Int) = Unit
            override suspend fun exportSince(timestamp: Long): List<EventLogEntity> = emptyList()
        }
        try {
            val clock = FakeClock()
            val analytics = LocalAnalytics(dao, clock, dispatcher)
            analytics.track(Event.OnboardingStarted)
            analytics.track(Event.FocusPaused(30))
            val row =
                withContext(Dispatchers.IO) {
                    withTimeout(5000) {
                        written.await()
                    }
                }
            assertNotSame(caller, thread.await())
            assertEquals("focus_paused", row.name)
            assertEquals("{\"remainingSeconds\":30}", row.params)
            assertEquals(clock.millis(), row.timestamp)
        } finally {
            dispatcher.close()
        }
    }
}
