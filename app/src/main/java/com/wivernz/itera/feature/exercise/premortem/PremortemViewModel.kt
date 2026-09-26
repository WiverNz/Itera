package com.wivernz.itera.feature.exercise.premortem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import com.wivernz.itera.feature.exercise.runner.ExerciseSession
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.exercise.runner.capped
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PremortemUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    // "It's {month year}": six months from today, formatted by the screen in the current locale
    val failureDate: LocalDate = LocalDate.MIN,
    val project: String = "",
    // user-ordered: risk #1 is the first row, never re-sorted by likelihood
    val reasons: List<PremortemReason> = emptyList(),
    val focused: Int? = null,
    val newReason: String = "",
    val action: String = "",
    // an unlocked related technique for the idea line, or null to omit it
    val idea: String? = null,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val reasonsMissing: Int get() =
        (PremortemRules.MIN_REASONS - reasons.count { it.text.isNotBlank() }).coerceAtLeast(0)
    val ready: Boolean get() = project.isNotBlank() && reasonsMissing == 0
    val canAdd: Boolean get() = reasons.size < PremortemRules.MAX_REASONS
    val hasDraft: Boolean get() = project.isNotBlank() || reasons.isNotEmpty() ||
        action.isNotBlank()
}

object PremortemRules {
    const val MIN_REASONS = 3
    const val MAX_REASONS = 6
    const val MONTHS_AHEAD = 6L

    fun failureDate(today: LocalDate): LocalDate = today.plusMonths(MONTHS_AHEAD)

    /** The first related technique the user has already unlocked; never a locked one. */
    fun idea(technique: Technique, unlocked: Set<String>, catalog: List<Technique>): String? =
        technique.relatedTechniqueIds.firstOrNull { it.value in unlocked }
            ?.let { id -> catalog.firstOrNull { it.id == id && !it.retired }?.name }

    fun next(likelihood: Likelihood): Likelihood =
        Likelihood.entries[(likelihood.ordinal + 1) % Likelihood.entries.size]
}

sealed interface PremortemEffect {
    data class ShowResult(val activityId: Long, val techniqueId: String) : PremortemEffect
    data object Close : PremortemEffect
}

@HiltViewModel
class PremortemViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val deps: ExerciseSessionDeps,
    private val states: TechniqueStateRepository,
    private val clock: Clock
) : ViewModel() {
    private val session =
        ExerciseSession(deps, checkNotNull(saved.get<Long>(ARG_ACTIVITY)), viewModelScope)
    private val mutable = MutableStateFlow(PremortemUiState())
    val state: StateFlow<PremortemUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<PremortemEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var finishing = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val loaded = session.load()
        if (loaded == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        val draft = loaded.draft as? ActivityResult.Premortem
        val unlocked = runCatching { states.all() }.getOrDefault(emptyList())
            .filter { it.unlocked }.map { it.techniqueId.value }.toSet()
        val catalog = runCatching { deps.catalog.catalog() }.getOrDefault(emptyList())
        mutable.update {
            it.copy(
                loading = false,
                techniqueId = loaded.technique.id.value,
                name = loaded.technique.name,
                failureDate = PremortemRules.failureDate(LocalDate.now(clock)),
                project = draft?.projectName.orEmpty(),
                reasons = draft?.reasons.orEmpty(),
                action = draft?.mitigationAction.orEmpty(),
                idea = PremortemRules.idea(loaded.technique, unlocked, catalog)
            )
        }
    }

    fun setProject(text: String) = edit { it.copy(project = text.take(PROJECT_MAX)) }

    fun setNewReason(text: String) = mutable.update { it.copy(newReason = capped(text)) }

    fun addReason() = edit { s ->
        val text = s.newReason.trim()
        if (text.isEmpty() || !s.canAdd) {
            s
        } else {
            s.copy(reasons = s.reasons + PremortemReason(text, Likelihood.POSSIBLE), newReason = "")
        }
    }

    fun setReason(index: Int, text: String) = edit { s ->
        s.copy(
            reasons = s.reasons.mapIndexed { i, r ->
                if (i ==
                    index
                ) {
                    r.copy(text = capped(text))
                } else {
                    r
                }
            }
        )
    }

    fun cycleLikelihood(index: Int) = edit { s ->
        s.copy(
            reasons = s.reasons.mapIndexed { i, r ->
                if (i == index) r.copy(likelihood = PremortemRules.next(r.likelihood)) else r
            }
        )
    }

    fun focus(index: Int?) = mutable.update {
        it.copy(focused = index?.takeIf { i -> i != it.focused })
    }

    fun move(index: Int, by: Int) = edit { s ->
        val target = index + by
        if (target !in s.reasons.indices) {
            s
        } else {
            val list = s.reasons.toMutableList()
            val moved = list.removeAt(index)
            list.add(target, moved)
            s.copy(reasons = list, focused = target)
        }
    }

    fun remove(index: Int) = edit { s ->
        s.copy(reasons = s.reasons.filterIndexed { i, _ -> i != index }, focused = null)
    }

    fun setAction(text: String) = edit { it.copy(action = capped(text)) }

    /** "Add to today": one MANUAL activity for the action, then the exercise completes. */
    fun addToToday() = finish(addToToday = true)

    /** Completing without an action is allowed. */
    fun finishWithoutAdding() = finish(addToToday = false)

    private fun finish(addToToday: Boolean) {
        val s = mutable.value
        if (finishing || !s.ready || (addToToday && s.action.isBlank())) return
        finishing = true
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            session.complete(result(s, addToToday)).onSuccess {
                effectChannel.send(PremortemEffect.ShowResult(session.activityId, s.techniqueId))
            }.onFailure {
                finishing = false
                mutable.update { it.copy(busy = false, failed = true) }
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            session.leave(result(mutable.value, false))
            effectChannel.send(PremortemEffect.Close)
        }
    }

    fun flushDraft() = session.flush()

    private fun edit(transform: (PremortemUiState) -> PremortemUiState) {
        if (mutable.value.loading) return
        mutable.update(transform)
        session.draft(result(mutable.value, false))
    }

    private fun result(s: PremortemUiState, addToToday: Boolean) = ActivityResult.Premortem(
        projectName = s.project.trim(),
        reasons = s.reasons.filter { it.text.isNotBlank() },
        mitigationAction = s.action.trim().ifEmpty { null },
        mitigationAddedToToday = addToToday && s.action.isNotBlank()
    )

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val PROJECT_MAX = 200
    }
}
