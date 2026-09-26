package com.wivernz.itera.feature.exercise.eisenhower

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.feature.exercise.runner.ExerciseSession
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.exercise.runner.capped
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EisenhowerUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    // the entry step: tasks one per line
    val entering: Boolean = true,
    val entryText: String = "",
    val board: EisenhowerBoard = EisenhowerBoard(),
    // "Continue with 80/20" on a combination day, "Finish exercise" otherwise
    val combination: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val entryCount: Int get() = EisenhowerBoard.lines(entryText).size
    val entryMissing: Int get() = (EisenhowerBoard.MIN_TASKS - entryCount).coerceAtLeast(0)
    val hasDraft: Boolean get() = entryText.isNotBlank() || board.items.isNotEmpty()
}

sealed interface EisenhowerEffect {
    data class ShowResult(val activityId: Long, val techniqueId: String) : EisenhowerEffect
    data object Close : EisenhowerEffect
}

@HiltViewModel
class EisenhowerViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    deps: ExerciseSessionDeps
) : ViewModel() {
    private val session =
        ExerciseSession(deps, checkNotNull(saved.get<Long>(ARG_ACTIVITY)), viewModelScope)
    private val mutable = MutableStateFlow(EisenhowerUiState())
    val state: StateFlow<EisenhowerUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<EisenhowerEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val loaded = session.load()
        if (loaded == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        val draft = loaded.draft as? ActivityResult.Eisenhower
        val board = draft?.let(EisenhowerBoard::fromResult) ?: EisenhowerBoard()
        val selected = saved.get<String>(KEY_SELECTED)?.takeIf { id ->
            board.items.any { it.id == id }
        }
        mutable.update {
            it.copy(
                loading = false,
                techniqueId = loaded.technique.id.value,
                name = loaded.technique.name,
                entering = board.items.isEmpty(),
                entryText = saved.get<String>(KEY_ENTRY).orEmpty(),
                board = selected?.let(board::select) ?: board,
                combination = loaded.activity.source == ActivitySource.COMBINATION
            )
        }
    }

    fun setEntry(text: String) {
        val value = capped(text)
        saved[KEY_ENTRY] = value
        mutable.update { it.copy(entryText = value) }
    }

    /** "Sort them": at least four tasks. */
    fun startSorting() {
        val s = mutable.value
        if (s.entryMissing > 0) return
        update(EisenhowerBoard.fromEntry(s.entryText))
        mutable.update { it.copy(entering = false) }
    }

    fun select(id: String) = update(mutable.value.board.select(id))

    fun place(quadrant: Quadrant) = update(mutable.value.board.place(quadrant))

    fun choose(id: String) = update(mutable.value.board.choose(id))

    fun finish() {
        val s = mutable.value
        if (s.busy || s.board.gate != EisenhowerGate.READY) return
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            session.complete(s.board.result()).onSuccess {
                effectChannel.send(EisenhowerEffect.ShowResult(session.activityId, s.techniqueId))
            }.onFailure { mutable.update { it.copy(busy = false, failed = true) } }
        }
    }

    fun leave() {
        viewModelScope.launch {
            session.leave(mutable.value.board.takeIf { it.items.isNotEmpty() }?.result())
            effectChannel.send(EisenhowerEffect.Close)
        }
    }

    fun flushDraft() = session.flush()

    private fun update(board: EisenhowerBoard) {
        saved[KEY_SELECTED] = board.selectedId
        mutable.update { it.copy(board = board) }
        session.draft(board.result())
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val KEY_ENTRY = "eisenhower.entry"
        private const val KEY_SELECTED = "eisenhower.selected"
    }
}
