package com.wivernz.itera.feature.exercise.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.review.ReviewScheduler
import com.wivernz.itera.domain.review.SubmitReviewUseCase
import com.wivernz.itera.feature.exercise.runner.ExerciseSession
import com.wivernz.itera.feature.exercise.runner.ExerciseSessionDeps
import com.wivernz.itera.feature.exercise.runner.capped
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    // the learning topic; empty for a Spaced-repetition item
    val topic: String = "",
    val daysAgo: Int = 0,
    val stageIndex: Int = 0,
    // the SOLID outcome; null at the last stage, where the item would retire
    val nextDays: Int? = null,
    val answer: String = "",
    val revealed: Boolean = false,
    // null until "Compare": the first answer is never in the state before the reveal
    val previousAnswer: String? = null,
    val busy: Boolean = false,
    val failed: Boolean = false
)

sealed interface ReviewEffect {
    data class ShowResult(val activityId: Long, val techniqueId: String) : ReviewEffect
    data object Close : ReviewEffect
}

/**
 * Explain it again from memory (issue 026). The first answer stays in the repository until the compare tap; grading
 * goes through `SubmitReviewUseCase`, which is idempotent, and a local guard makes a double tap one attempt.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val deps: ExerciseSessionDeps,
    private val reviews: ReviewRepository,
    private val submit: SubmitReviewUseCase,
    private val clock: Clock
) : ViewModel() {
    private val session =
        ExerciseSession(deps, checkNotNull(saved.get<Long>(ARG_ACTIVITY)), viewModelScope)
    private val mutable = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<ReviewEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var item: ReviewItem? = null
    private var submitting = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val loaded = session.load()
        val reviewItem = loaded?.activity?.reviewItemId?.let { reviews.item(it) }
        if (loaded == null || reviewItem == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        item = reviewItem
        val explainedOn = deps.plans.activity(reviewItem.sourceActivityId)?.completedAt
            ?.atZone(clock.zone)?.toLocalDate()
            ?: reviewItem.lastReviewedOn
            ?: reviewItem.dueOn.minusDays(ReviewScheduler.INTERVALS_DAYS[0].toLong())
        val draft = loaded.draft as? ActivityResult.Review
        mutable.update {
            it.copy(
                loading = false,
                topic = reviewItem.prompt,
                daysAgo = ChronoUnit.DAYS.between(explainedOn, LocalDate.now(clock)).toInt()
                    .coerceAtLeast(1),
                stageIndex = reviewItem.stageIndex,
                nextDays = ReviewScheduler.previewNextInterval(reviewItem.stageIndex),
                answer = draft?.answer.orEmpty()
            )
        }
        AnalyticsTechnique.of(reviewItem.techniqueId.value)?.let {
            deps.analytics.track(Event.ReviewOpened(it, reviewItem.stageIndex))
        }
    }

    fun setAnswer(text: String) {
        if (mutable.value.revealed) return
        mutable.update { it.copy(answer = capped(text)) }
        item?.let { session.draft(draftOf(it)) }
    }

    /** "Compare with my first answer". An empty answer may be compared: "I couldn't recall it" is honest. */
    fun reveal() {
        val reviewItem = item ?: return
        if (mutable.value.revealed) return
        viewModelScope.launch {
            val previous = reviews.revealAnswer(reviewItem.id).orEmpty()
            mutable.update { it.copy(revealed = true, previousAnswer = previous) }
            AnalyticsTechnique.of(reviewItem.techniqueId.value)?.let {
                deps.analytics.track(Event.ReviewRevealed(it, reviewItem.stageIndex))
            }
        }
    }

    fun grade(grade: RecallGrade) {
        val reviewItem = item ?: return
        if (!mutable.value.revealed || submitting) return
        submitting = true
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            submit(session.activityId, mutable.value.answer, grade).onSuccess {
                effectChannel.send(
                    ReviewEffect.ShowResult(session.activityId, reviewItem.techniqueId.value)
                )
            }.onFailure {
                submitting = false
                mutable.update { it.copy(busy = false, failed = true) }
            }
        }
    }

    /** Closing before grading discards the attempt; the review stays due. */
    fun leave() {
        viewModelScope.launch {
            session.leave(item?.takeIf { !mutable.value.revealed }?.let(::draftOf))
            effectChannel.send(ReviewEffect.Close)
        }
    }

    fun flushDraft() = session.flush()

    // The draft never carries the first answer; the grade is a placeholder until one is chosen.
    private fun draftOf(item: ReviewItem) =
        ActivityResult.Review(item.id, mutable.value.answer, RecallGrade.PARTIAL, "")

    companion object {
        const val ARG_ACTIVITY = "activityId"
    }
}
