package com.wivernz.itera.feature.exercise.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.review.ReviewScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The result screen's production variants (docs/ux/02-screen-specs-exercise.md section 3). */
sealed interface ResultVariant {
    data object Standard : ResultVariant

    /** "{n} minutes of {planned} planned" and the break hint. */
    data class Focus(val minutes: Int, val plannedMinutes: Int, val breakMinutes: Int?) :
        ResultVariant

    /** The interval ladder in place of the mastery ladder; [nextDays] is null once the item retired. */
    data class Review(val stageIndex: Int, val nextDays: Int?) : ResultVariant
}

data class ExerciseResultUiState(
    val readOnlyActivity: com.wivernz.itera.domain.model.PlanActivity? = null,
    val loading: Boolean = true,
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    val skill: Skill = Skill.HABITS,
    val difficulty: Difficulty? = null,
    val note: String = "",
    val level: MasteryLevel = MasteryLevel.NONE,
    val hint: LevelHint = LevelHint.Unavailable,
    val variant: ResultVariant = ResultVariant.Standard
)

/** "How did it feel?" and the note save on every change, never on "Done", which is navigation only. */
@HiltViewModel
class ExerciseResultViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val reviews: ReviewRepository,
    private val techniques: ObserveTechniqueProgressUseCase
) : ViewModel() {
    private val readOnly = saved.get<Boolean>("readOnly") == true
    private val activityId: Long = checkNotNull(saved.get<Long>(ARG_ACTIVITY))
    private val mutable = MutableStateFlow(ExerciseResultUiState())
    val state: StateFlow<ExerciseResultUiState> = mutable.asStateFlow()
    private val autosave = DraftAutosave<Unit>(viewModelScope, NOTE_DEBOUNCE) { writeFeedback() }

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val activity = plans.activity(activityId)
        val technique = activity?.let { catalog.technique(it.techniqueId) }
        if (activity == null || technique == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        if (readOnly) {
            mutable.update { it.copy(loading = false, readOnlyActivity = activity) }
            return
        }
        val variant = when (val result = activity.result) {
            is ActivityResult.Focus -> ResultVariant.Focus(
                minutes = result.actualSeconds / SECONDS_PER_MINUTE,
                plannedMinutes = result.plannedSeconds / SECONDS_PER_MINUTE,
                breakMinutes = technique.defaults.breakMinutes
            )
            is ActivityResult.Review -> reviews.item(result.reviewItemId)?.let { item ->
                if (item.state == ReviewState.RETIRED) {
                    ResultVariant.Review(ReviewScheduler.INTERVALS_DAYS.lastIndex, null)
                } else {
                    ResultVariant.Review(
                        item.stageIndex,
                        ReviewScheduler.INTERVALS_DAYS[item.stageIndex]
                    )
                }
            } ?: ResultVariant.Standard
            else -> ResultVariant.Standard
        }
        mutable.update {
            it.copy(
                loading = false,
                techniqueId = technique.id.value,
                name = technique.name,
                skill = technique.skill,
                difficulty = activity.difficulty,
                note = activity.note.orEmpty(),
                variant = variant
            )
        }
        techniques().catch { }.collect { all ->
            all.firstOrNull { it.techniqueId == technique.id }?.let { progress ->
                mutable.update { it.copy(level = progress.level, hint = progress.nextLevelHint) }
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        if (mutable.value.loading || readOnly) return
        mutable.update { it.copy(difficulty = difficulty) }
        viewModelScope.launch { writeFeedback() }
    }

    fun setNote(note: String) {
        if (mutable.value.loading || readOnly) return
        mutable.update { it.copy(note = capped(note)) }
        autosave.schedule(Unit)
    }

    /** `ON_STOP` and "Done": nothing typed is lost. */
    fun flush() = autosave.flush()

    private suspend fun writeFeedback() {
        if (readOnly) return
        val s = mutable.value
        plans.updateFeedback(activityId, s.difficulty, s.note.ifBlank { null })
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val SECONDS_PER_MINUTE = 60
        private val NOTE_DEBOUNCE = 400.milliseconds
    }
}
