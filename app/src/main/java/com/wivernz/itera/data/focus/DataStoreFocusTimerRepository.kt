package com.wivernz.itera.data.focus
import androidx.datastore.core.DataStore
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.repository.FocusTimerRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
class DataStoreFocusTimerRepository @Inject constructor(
    private val store: DataStore<FocusTimerState?>,
    private val clock: Clock,
    private val logger: Logger
) : FocusTimerRepository {
    override fun observe(): Flow<FocusTimerState?> = store.data
    override suspend fun save(state: FocusTimerState) {
        store.updateData { state }
    }
    override suspend fun clear() {
        store.updateData { null }
    }
    override suspend fun restore(): FocusTimerRestore {
        val state = store.data.first() ?: return FocusTimerRestore.None
        val now = clock.instant()
        return when {
            Duration.between(state.startedAt, now) > Duration.ofHours(24) -> {
                // Conditional clear protects a newly started timer from a concurrent restore.
                store.updateData { if (it == state) null else it }
                logger.w(TAG, "Discarded stale focus timer")
                FocusTimerRestore.Discarded
            }
            state.remaining(now).isZero -> FocusTimerRestore.AutoComplete(state)
            state.pausedAt != null -> FocusTimerRestore.Paused(state)
            else -> FocusTimerRestore.Running(state)
        }
    }
}
private const val TAG = "FocusTimerRepository"
