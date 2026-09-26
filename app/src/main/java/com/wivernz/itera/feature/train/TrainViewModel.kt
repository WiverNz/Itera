@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ReviewRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.review.OpenDueReviewUseCase
import com.wivernz.itera.domain.review.ReviewScheduler
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrainNodeState { PAST, TODAY, FUTURE }
data class TrainNode(
    val day: Int,
    val technique: Technique?,
    val state: TrainNodeState,
    val firstCombination: Boolean = false
)
data class TrainUiState(
    val programDay: Int = 1,
    val week: Int = 1,
    val nodes: List<TrainNode> = emptyList(),
    val review: ReviewItem? = null,
    val moreReviews: Int = 0,
    val unlockedCount: Int = 0,
    val catalogCount: Int = 14,
    val loading: Boolean = true,
    val failed: Boolean = false
)
sealed interface TrainUiEvent {
    data class OpenDay(val day: Int) : TrainUiEvent
    data object Review : TrainUiEvent
    data object Library : TrainUiEvent
    data object Refresh : TrainUiEvent
}
sealed interface TrainEffect {
    data class Exercise(val activity: PlanActivity) : TrainEffect
    data object History : TrainEffect
    data object Library : TrainEffect
}
fun trainNodes(
    day: Int,
    curriculum: Curriculum,
    techniques: List<Technique>,
    plans: List<TrainingDay>
): List<TrainNode> {
    val first = (day - 1) / 7 * 7 + 1
    return (first until first + 7).map { n ->
        val authored = curriculum.days.firstOrNull { it.day == n }
        val stored = plans.lastOrNull { it.programDay == n }?.activities?.firstOrNull {
            it.source ==
                ActivitySource.PROGRAM
        }
        val id =
            authored?.newTechniqueId
                ?: stored?.takeIf { it.exerciseType != ExerciseType.COMBINATION }?.techniqueId
        TrainNode(
            n,
            techniques.firstOrNull { it.id == id },
            when {
                n < day -> TrainNodeState.PAST
                n ==
                    day -> TrainNodeState.TODAY
                else -> TrainNodeState.FUTURE
            },
            n == 14
        )
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrainViewModel @Inject constructor(
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val reviews: ReviewRepository,
    private val prefs: PreferencesRepository,
    private val progress: ObserveTechniqueProgressUseCase,
    private val ensure: EnsureTodayPlanUseCase,
    private val analytics: Analytics,
    private val clock: Clock,
    private val openReview: OpenDueReviewUseCase
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val channel = Channel<TrainEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()
    private var opening = false
    val state: StateFlow<TrainUiState> = refresh.flatMapLatest {
        flow {
            val today = ensure()
            val techniques = catalog.catalog().filter { !it.retired }
            val curriculum = catalog.curriculum()
            val past = plans.daysBefore(LocalDate.now(clock))
            emitAll(
                combine(
                    plans.observeDay(today.id),
                    prefs.preferences,
                    progress(),
                    reviews.observeDue(LocalDate.now(clock))
                ) {
                        plan,
                        preferences,
                        facts,
                        due
                    ->
                    // Keep the completed day's map until the next calendar day's plan exists, as Today does.
                    val day = plan?.programDay ?: preferences.currentProgramDay
                    val ordered = ReviewScheduler.dueOn(due, LocalDate.now(clock))
                    TrainUiState(
                        day,
                        (day - 1) / 7 + 1,
                        trainNodes(day, curriculum, techniques, past + listOfNotNull(plan)),
                        ordered.firstOrNull(),
                        (ordered.size - 1).coerceAtLeast(0),
                        facts.count {
                            it.unlocked
                        },
                        techniques.size,
                        loading = false
                    )
                }
            )
        }.catch { emit(state.value.copy(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainUiState())
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.TRAIN))
    }
    fun onEvent(event: TrainUiEvent) {
        when (event) {
            TrainUiEvent.Refresh -> refresh.value++
            TrainUiEvent.Library -> viewModelScope.launch { channel.send(TrainEffect.Library) }
            is TrainUiEvent.OpenDay -> {
                if (event.day <
                    state.value.programDay
                ) {
                    viewModelScope.launch { channel.send(TrainEffect.History) }
                } else if (event.day == state.value.programDay) {
                    open(false)
                }
            }
            TrainUiEvent.Review -> open(true)
        }
    }
    private fun open(review: Boolean) {
        if (opening) return
        opening = true
        viewModelScope.launch {
            try {
                val today = ensure()
                val activity = if (review) {
                    openReview()
                } else {
                    today.activities.firstOrNull { it.exerciseType == ExerciseType.COMBINATION }
                        ?: today.activities.firstOrNull { it.source == ActivitySource.PROGRAM }
                }
                if (activity != null) channel.send(TrainEffect.Exercise(activity))
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                refresh.value++
            } finally {
                opening = false
            }
        }
    }
}
