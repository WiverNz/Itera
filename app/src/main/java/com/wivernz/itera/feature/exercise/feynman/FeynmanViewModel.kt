package com.wivernz.itera.feature.exercise.feynman

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.coach.CoachFeedbackProvider
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.LearningTopicRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.review.ReviewScheduler
import com.wivernz.itera.feature.exercise.runner.ExerciseSession
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.exercise.runner.capped
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeynmanUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    // 0 explain, 1 look back
    val step: Int = 0,
    val topics: List<LearningTopic> = emptyList(),
    val topicId: Long? = null,
    val pickingTopic: Boolean = false,
    val newTopic: String = "",
    val explanation: String = "",
    val hardest: Set<String> = emptySet(),
    val note: String = "",
    // days until this topic is explained again: the first interval, or the existing item's current stage
    val reviewInDays: Int = ReviewScheduler.INTERVALS_DAYS[0],
    // ADR-0016: always false in the MVP; the coach box shows its placeholder line
    val coachAvailable: Boolean = false,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val topic: LearningTopic? get() = topics.firstOrNull { it.id == topicId }
    val words: Int get() = FeynmanText.words(explanation)
    val wordsMissing: Int get() = (FeynmanText.MIN_WORDS - words).coerceAtLeast(0)

    /** No topics at all: step 1 opens with inline creation rather than an empty picker. */
    val creatingTopic: Boolean get() = !loading && (topics.isEmpty() || pickingTopic)
    val hasDraft: Boolean get() = explanation.isNotBlank() || note.isNotBlank() ||
        hardest.isNotEmpty()
}

object FeynmanText {
    const val MIN_WORDS = 30

    /** Words are whitespace-separated tokens holding a letter or digit; punctuation alone is not a word. */
    fun words(text: String): Int =
        text.split(Regex("\\s+")).count { token -> token.any(Char::isLetterOrDigit) }
}

sealed interface FeynmanEffect {
    data class ShowResult(val activityId: Long, val techniqueId: String) : FeynmanEffect
    data object Close : FeynmanEffect
}

@HiltViewModel
class FeynmanViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    deps: ExerciseSessionDeps,
    private val topicsRepository: LearningTopicRepository,
    private val reviews: ReviewRepository,
    coach: CoachFeedbackProvider
) : ViewModel() {
    private val session =
        ExerciseSession(deps, checkNotNull(saved.get<Long>(ARG_ACTIVITY)), viewModelScope)
    private val mutable = MutableStateFlow(
        FeynmanUiState(step = saved.get<Int>(KEY_STEP) ?: 0, coachAvailable = coach.isAvailable)
    )
    val state: StateFlow<FeynmanUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<FeynmanEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var techniqueId = TechniqueId(FEYNMAN)

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val loaded = session.load()
        if (loaded == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        techniqueId = loaded.technique.id
        val draft = loaded.draft as? ActivityResult.Feynman
        mutable.update {
            it.copy(
                techniqueId = loaded.technique.id.value,
                name = loaded.technique.name,
                topicId = draft?.topicId?.takeIf { id -> id > 0 },
                explanation = draft?.explanation.orEmpty(),
                hardest = draft?.hardestParts.orEmpty().toSet(),
                note = draft?.reflectionNote.orEmpty()
            )
        }
        topicsRepository.observeTopics().catch { emit(emptyList()) }.collect { topics ->
            val current = mutable.value.topicId?.takeIf { id -> topics.any { it.id == id } }
            val preferred = current
                ?: runCatching { topicsRepository.nextTopicForReview()?.id }.getOrNull()
                    ?.takeIf { id -> topics.any { it.id == id } }
                ?: topics.firstOrNull()?.id
            mutable.update { it.copy(loading = false, topics = topics, topicId = preferred) }
            refreshInterval()
        }
    }

    fun openPicker() = mutable.update { it.copy(pickingTopic = !it.pickingTopic) }

    fun pickTopic(id: Long) {
        mutable.update { it.copy(topicId = id, pickingTopic = false) }
        saveDraft()
        viewModelScope.launch { refreshInterval() }
    }

    fun setNewTopic(text: String) = mutable.update { it.copy(newTopic = text.take(TOPIC_MAX)) }

    /** Inline topic creation; the new topic is selected. */
    fun addTopic() {
        val title = mutable.value.newTopic.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val id = topicsRepository.add(title)
            mutable.update { it.copy(topicId = id, newTopic = "", pickingTopic = false) }
            saveDraft()
            refreshInterval()
        }
    }

    fun setExplanation(text: String) {
        mutable.update { it.copy(explanation = capped(text)) }
        saveDraft()
    }

    /** "Done explaining": 30 words and a topic. */
    fun toReflect() {
        val s = mutable.value
        if (s.wordsMissing > 0 || s.topic == null) return
        saved[KEY_STEP] = 1
        mutable.update { it.copy(step = 1) }
        session.flush()
    }

    fun backToExplain() {
        saved[KEY_STEP] = 0
        mutable.update { it.copy(step = 0) }
    }

    fun toggleHardest(id: String) {
        mutable.update { s ->
            s.copy(
                hardest = if (id in
                    s.hardest
                ) {
                    s.hardest - id
                } else {
                    s.hardest + id
                }
            )
        }
        saveDraft()
    }

    fun setNote(text: String) {
        mutable.update { it.copy(note = capped(text)) }
        saveDraft()
    }

    /** "Finish": the result schedules (or refreshes) the topic's review in the same transaction. */
    fun finish() {
        val s = mutable.value
        val result = currentResult() ?: return
        if (s.busy || s.wordsMissing > 0 || s.topic == null) return
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            session.complete(result).onSuccess {
                effectChannel.send(FeynmanEffect.ShowResult(session.activityId, s.techniqueId))
            }.onFailure { mutable.update { it.copy(busy = false, failed = true) } }
        }
    }

    fun leave() {
        viewModelScope.launch {
            session.leave(currentResult())
            effectChannel.send(FeynmanEffect.Close)
        }
    }

    fun flushDraft() = session.flush()

    private suspend fun refreshInterval() {
        val topicId = mutable.value.topicId
        val existing = topicId?.let {
            runCatching { reviews.activeFor(techniqueId, it) }.getOrNull()
        }
        mutable.update {
            it.copy(reviewInDays = ReviewScheduler.INTERVALS_DAYS[existing?.stageIndex ?: 0])
        }
    }

    private fun saveDraft() {
        currentResult()?.let(session::draft)
    }

    /** Chips are stored as stable ids and rendered in the current language. */
    private fun currentResult(): ActivityResult.Feynman? {
        val s = mutable.value
        if (s.loading) return null
        return ActivityResult.Feynman(
            topicId = s.topicId ?: 0,
            topicTitle = s.topic?.title.orEmpty(),
            explanation = s.explanation,
            wordCount = s.words,
            hardestParts = FEYNMAN_CHIPS.map { it.first }.filter { it in s.hardest },
            reflectionNote = s.note.ifBlank { null }
        )
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val KEY_STEP = "feynman.step"
        private const val FEYNMAN = "feynman_technique"
        private const val TOPIC_MAX = 200
    }
}
