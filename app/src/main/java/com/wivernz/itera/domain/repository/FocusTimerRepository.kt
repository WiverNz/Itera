package com.wivernz.itera.domain.repository
import com.wivernz.itera.domain.model.FocusTimerRestore
import com.wivernz.itera.domain.model.FocusTimerState
import kotlinx.coroutines.flow.Flow
/** docs/data/02-datastore-preferences.md section 4. */
interface FocusTimerRepository {
    fun observe(): Flow<FocusTimerState?>
    suspend fun save(state: FocusTimerState)
    suspend fun clear()
    suspend fun restore(): FocusTimerRestore
}
