package com.wivernz.itera.feature.reflection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.TrainingDayStatus
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.SaveDraftUseCase
import com.wivernz.itera.domain.training.SkipActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Day 7 / 14 / 21 summary above question 1 (docs/engine/01 section 8). Adds no extra tap. */
data class WeeklyLookBack(
    val daysTrained: Int,
    val techniquesPractised: Int,
    val focusMinutes: Int,
    // "What will you change tomorrow?" answers carried into this week's days, oldest first
    val changes: List<String>
)

data class ReflectionUiState(
    val loading: Boolean = true,
    // 0..2, the question currently open
    val step: Int = 0,
    val answers: List<String> = listOf("", "", ""),
    // chip ids per question
    val chips: List<Set<String>> = listOf(emptySet(), emptySet(), emptySet()),
    // null once a draft exists or the pre-fill has been applied
    val prefill: List<PrefillPart>? = null,
    val lookBack: WeeklyLookBack? = null,
    val saving: Boolean = false,
    val failed: Boolean = false
)

sealed interface ReflectionEffect {
    /** Saved or skipped: Day complete when the day is done, otherwise back to Today. */
    data class Finished(val dayId: Long, val dayComplete: Boolean) : ReflectionEffect
}

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val progress: ProgressRepository,
    private val complete: CompleteActivityUseCase,
    private val skip: SkipActivityUseCase,
    private val saveDraft: SaveDraftUseCase,
    analytics: Analytics
) : ViewModel() {
    private val activityId: Long = checkNotNull(saved.get<Long>(ARG_ACTIVITY))
    private val mutable = MutableStateFlow(
        ReflectionUiState(step = saved.get<Int>(KEY_STEP) ?: 0)
    )
    val state: StateFlow<ReflectionUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<ReflectionEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var draftJob: Job? = null

    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.REFLECTION))
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val activity = plans.activity(activityId)
        val day = activity?.let { plans.day(it.trainingDayId) }
        val draft = plans.draft(activityId) as? ActivityResult.Reflection
        val names = runCatching { catalog.catalog().associate { it.id.value to it.name } }
            .getOrDefault(emptyMap())
        val lookBack = if (activity?.weeklyLookBack == true &&
            day != null
        ) {
            lookBack(day.date)
        } else {
            null
        }
        mutable.update { state ->
            if (draft != null) {
                state.copy(
                    loading = false,
                    answers = listOf(
                        draft.wentWell.orEmpty(),
                        draft.didNotGoWell.orEmpty(),
                        draft.tomorrowChange.orEmpty()
                    ),
                    chips = listOf(
                        draft.wentWellChips.toSet(),
                        draft.didNotGoWellChips.toSet(),
                        emptySet()
                    ),
                    lookBack = lookBack
                )
            } else {
                state.copy(
                    loading = false,
                    prefill = ReflectionPrefill.parts(day?.activities.orEmpty(), names),
                    lookBack = lookBack
                )
            }
        }
    }

    private suspend fun lookBack(date: LocalDate): WeeklyLookBack {
        val from = date.minusDays(WEEK_DAYS - 1)
        val completions = progress.observeCompletions(from, date).first()
        val focusSeconds = progress.observeFocusSeconds(from).first()
        val days = plans.daysBefore(date.plusDays(1)).filter { it.date >= from }
        return WeeklyLookBack(
            daysTrained = completions.map { it.date }.distinct().size,
            techniquesPractised = completions.map { it.techniqueId }
                .filter { it.value != REFLECTION_ID }
                .distinct().size,
            focusMinutes = (focusSeconds / SECONDS_PER_MINUTE).toInt(),
            changes = days.mapNotNull { it.carryOverIntent?.takeIf(String::isNotBlank) }.distinct()
        )
    }

    /** The screen renders the pre-fill parts into text once; the user may then edit or clear it. */
    fun applyPrefill(text: String) {
        if (mutable.value.prefill == null) return
        mutable.update {
            it.copy(prefill = null, answers = it.answers.toMutableList().also { a -> a[0] = text })
        }
    }

    fun setAnswer(question: Int, text: String) {
        mutable.update { it.copy(answers = it.answers.replaced(question, text)) }
        scheduleDraft()
    }

    /** Toggling a chip rewrites that answer to the selected chips (prototype); typing then overrides it. */
    fun toggleChip(question: Int, chipId: String, rewrite: (Set<String>) -> String) {
        mutable.update { state ->
            val chips = state.chips[question].let { if (chipId in it) it - chipId else it + chipId }
            state.copy(
                chips = state.chips.replaced(question, chips),
                answers = state.answers.replaced(question, rewrite(chips))
            )
        }
        scheduleDraft()
    }

    fun next() {
        val step = mutable.value.step
        if (step < LAST_STEP) {
            saved[KEY_STEP] = step + 1
            mutable.update { it.copy(step = step + 1) }
            scheduleDraft(immediate = true)
        } else {
            finish()
        }
    }

    fun skipTonight() {
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            draftJob?.cancel()
            val result = skip(activityId)
            settle(result)
        }
    }

    /** Close keeps the draft for later. */
    fun onClose() = scheduleDraft(immediate = true)

    private fun finish() {
        if (mutable.value.saving) return
        mutable.update { it.copy(saving = true, failed = false) }
        viewModelScope.launch {
            draftJob?.cancel()
            settle(complete(activityId, currentResult()))
        }
    }

    private suspend fun settle(result: Result<Unit>) {
        val activity = plans.activity(activityId)
        val day = activity?.let { plans.day(it.trainingDayId) }
        if (result.isSuccess && day != null) {
            effectChannel.send(
                ReflectionEffect.Finished(day.id, day.status == TrainingDayStatus.COMPLETE)
            )
        } else {
            mutable.update { it.copy(saving = false, failed = true) }
        }
    }

    private fun currentResult(): ActivityResult.Reflection {
        val s = mutable.value
        return ActivityResult.Reflection(
            wentWell = s.answers[0],
            wentWellChips = s.chips[0].sorted(),
            didNotGoWell = s.answers[1],
            didNotGoWellChips = s.chips[1].sorted(),
            // verbatim: tomorrow's carry-over banner quotes it exactly
            tomorrowChange = s.answers[2]
        )
    }

    private fun scheduleDraft(immediate: Boolean = false) {
        if (mutable.value.loading || mutable.value.saving) return
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            if (!immediate) delay(DRAFT_DEBOUNCE_MILLIS)
            saveDraft(activityId, currentResult())
        }
    }

    private fun <T> List<T>.replaced(index: Int, value: T) =
        toMutableList().also { it[index] = value }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        const val LAST_STEP = 2
        private const val KEY_STEP = "reflection.step"
        private const val WEEK_DAYS = 7L
        private const val SECONDS_PER_MINUTE = 60
        private const val DRAFT_DEBOUNCE_MILLIS = 400L
        private const val REFLECTION_ID = "daily_reflection"
    }
}
