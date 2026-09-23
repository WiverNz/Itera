package com.wivernz.itera.data.focus
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class FocusTimerRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun roundTripEveryRestoreBranchAndStalePriority() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreFactory.create(
            serializer = FocusTimerSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
            scope = scope,
            produceFile = { folder.root.resolve("focus.pb") }
        )
        try {
            val clock = FakeClock()
            val logger = TestLogger()
            val repo = DataStoreFocusTimerRepository(store, clock, logger)
            assertEquals(FocusTimerRestore.None, repo.restore())
            val state = FocusTimerState(
                1,
                TechniqueId("pomodoro"),
                "private",
                1500,
                300,
                clock.instant(),
                clock.instant().plusSeconds(1800),
                null,
                1200
            )
            repo.save(state)
            assertEquals(state, repo.observe().first())
            assertEquals(FocusTimerRestore.Running(state), repo.restore())
            val paused = state.copy(pausedAt = clock.instant())
            repo.save(paused)
            clock.advance(Duration.ofHours(1))
            assertEquals(FocusTimerRestore.Paused(paused), repo.restore())
            repo.save(state)
            assertEquals(FocusTimerRestore.AutoComplete(state), repo.restore())
            assertEquals(state, repo.observe().first())
            clock.advance(Duration.ofHours(23))
            assertTrue(repo.restore() is FocusTimerRestore.AutoComplete)
            clock.advance(Duration.ofMillis(1))
            assertEquals(FocusTimerRestore.Discarded, repo.restore())
            assertNull(repo.observe().first())
            assertEquals(1, logger.warnings.size)
            assertFalse(logger.warnings.single().contains("private"))
            repo.save(state.copy(pausedAt = state.endsAt))
            assertEquals(FocusTimerRestore.Discarded, repo.restore())
            repo.clear()
            assertEquals(FocusTimerRestore.None, repo.restore())
        } finally {
            scope.cancel()
        }
    }

    @Test fun corruptionDiscardsTimer() = runTest {
        val file =
            folder.newFile("broken.pb")
                .apply {
                    writeText("private broken payload")
                }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreFactory.create(
            serializer = FocusTimerSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { null },
            scope = scope,
            produceFile = { file }
        )
        try {
            assertNull(store.data.first())
        } finally {
            scope.cancel()
        }
    }
}
