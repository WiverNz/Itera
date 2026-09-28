package com.wivernz.itera.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.domain.demo.DemoDataLoader
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.onboarding.CompleteOnboardingUseCase
import com.wivernz.itera.domain.onboarding.OnboardingChoices
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** D-05: Focus and Learning are pre-selected. */
val DEFAULT_FOCUS_AREAS: List<Skill> = listOf(Skill.FOCUS, Skill.LEARNING)

data class OnboardingUiState(
    // oldest first: a third selection drops index 0
    val focusAreas: List<Skill> = DEFAULT_FOCUS_AREAS,
    val morningTime: LocalTime = LocalTime.of(8, 30),
    val eveningTime: LocalTime = LocalTime.of(21, 0),
    val timeBudget: TimeBudget = TimeBudget.STANDARD,
    // null until the permission result is known
    val notificationsGranted: Boolean? = null,
    val finishing: Boolean = false,
    val demoAvailable: Boolean = false
) {
    val canContinueGoals: Boolean get() = focusAreas.isNotEmpty()
}

/** One First-week row, read from the curriculum asset (never hard-coded). */
data class FirstWeekRow(
    val day: Int,
    val techniqueId: String,
    val name: String,
    val skill: Skill,
    // "On Day 4 you revisit one" (week_sub)
    val withReview: Boolean
)

sealed interface OnboardingEffect {
    data object Finished : OnboardingEffect
}

/** One instance for the whole onboarding flow, scoped to the Welcome back-stack entry. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val complete: CompleteOnboardingUseCase,
    private val preferences: PreferencesRepository,
    catalog: TechniqueCatalogRepository,
    private val demo: Optional<DemoDataLoader>,
    private val analytics: Analytics
) : ViewModel() {
    private val effectChannel = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    val state: StateFlow<OnboardingUiState> = combine(
        saved.getStateFlow(KEY_AREAS, ArrayList(DEFAULT_FOCUS_AREAS.map { it.name })),
        saved.getStateFlow(KEY_MORNING, DEFAULT.morningTime.toSecondOfDay()),
        saved.getStateFlow(KEY_EVENING, DEFAULT.eveningTime.toSecondOfDay()),
        saved.getStateFlow(KEY_BUDGET, DEFAULT.timeBudget.name),
        combine(
            saved.getStateFlow<Boolean?>(KEY_GRANTED, null),
            saved.getStateFlow(KEY_FINISHING, false)
        ) { granted, finishing -> granted to finishing }
    ) { areas, morning, evening, budget, (granted, finishing) ->
        OnboardingUiState(
            focusAreas = areas.map(Skill::valueOf),
            morningTime = LocalTime.ofSecondOfDay(morning.toLong()),
            eveningTime = LocalTime.ofSecondOfDay(evening.toLong()),
            timeBudget = TimeBudget.valueOf(budget),
            notificationsGranted = granted,
            finishing = finishing,
            demoAvailable = demo.isPresent
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, current())

    private fun current(): OnboardingUiState = OnboardingUiState(
        focusAreas =
        saved.get<ArrayList<String>>(KEY_AREAS)?.map(Skill::valueOf) ?: DEFAULT_FOCUS_AREAS,
        morningTime = saved.get<Int>(KEY_MORNING)?.let { LocalTime.ofSecondOfDay(it.toLong()) }
            ?: DEFAULT.morningTime,
        eveningTime = saved.get<Int>(KEY_EVENING)?.let { LocalTime.ofSecondOfDay(it.toLong()) }
            ?: DEFAULT.eveningTime,
        timeBudget = saved.get<String>(KEY_BUDGET)?.let(TimeBudget::valueOf) ?: DEFAULT.timeBudget,
        notificationsGranted = saved.get<Boolean>(KEY_GRANTED),
        finishing = saved.get<Boolean>(KEY_FINISHING) ?: false,
        demoAvailable = demo.isPresent
    )

    val firstWeek: StateFlow<List<FirstWeekRow>> = AppLanguage.locale.mapLatest {
        val techniques = catalog.catalog().associateBy { it.id }
        catalog.curriculum().days.take(FIRST_WEEK_DAYS).mapNotNull { day ->
            val technique = day.newTechniqueId?.let(techniques::get) ?: return@mapNotNull null
            FirstWeekRow(
                day.day,
                technique.id.value,
                technique.name,
                technique.skill,
                day.day == REVIEW_DAY
            )
        }
    }.catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onGetStarted() = analytics.track(Event.OnboardingStarted)

    fun onStepCompleted(step: Int) = analytics.track(Event.OnboardingStepCompleted(step))

    /** Max two; a third selection drops the oldest. Deselecting to zero is allowed and disables Continue. */
    fun toggleFocusArea(skill: Skill) {
        val areas = current().focusAreas.toMutableList()
        if (skill in areas) {
            areas.remove(skill)
        } else {
            areas.add(skill)
            while (areas.size > MAX_AREAS) areas.removeAt(0)
        }
        saved[KEY_AREAS] = ArrayList(areas.map { it.name })
    }

    fun setMorningTime(time: LocalTime) {
        saved[KEY_MORNING] = time.withSecond(0).withNano(0).toSecondOfDay()
    }

    fun setEveningTime(time: LocalTime) {
        saved[KEY_EVENING] = time.withSecond(0).withNano(0).toSecondOfDay()
    }

    fun setTimeBudget(budget: TimeBudget) {
        saved[KEY_BUDGET] = budget.name
    }

    /** Written as soon as it is known; a denial turns every reminder off (docs/ux/08 section 7). */
    fun onNotificationResult(granted: Boolean) {
        saved[KEY_GRANTED] = granted
        if (!granted) {
            viewModelScope.launch {
                preferences.update {
                    it.copy(
                        notifyMorning = false,
                        notifyFocus = false,
                        notifyReviews = false,
                        notifyEvening = false
                    )
                }
            }
        }
    }

    /** "Start Day 1". Navigates even if plan generation fails; Today then generates its own plan. */
    fun startDayOne() {
        if (current().finishing) return
        saved[KEY_FINISHING] = true
        onStepCompleted(STEP_FIRST_WEEK)
        val choices = current().let {
            OnboardingChoices(
                focusAreas = it.focusAreas,
                morningTime = it.morningTime,
                eveningTime = it.eveningTime,
                timeBudget = it.timeBudget,
                notificationsGranted = it.notificationsGranted ?: true
            )
        }
        viewModelScope.launch {
            try {
                complete(choices)
                effectChannel.send(OnboardingEffect.Finished)
            } finally {
                saved[KEY_FINISHING] = false
            }
        }
    }

    fun loadDemo() {
        val loader = demo.orElse(null) ?: return
        if (current().finishing) return
        saved[KEY_FINISHING] = true
        viewModelScope.launch {
            try {
                loader.load()
                effectChannel.send(OnboardingEffect.Finished)
            } finally {
                saved[KEY_FINISHING] = false
            }
        }
    }

    companion object {
        const val MAX_AREAS = 2
        const val FIRST_WEEK_DAYS = 7
        const val REVIEW_DAY = 4
        const val STEP_GOALS = 1
        const val STEP_RHYTHM = 2
        const val STEP_FIRST_WEEK = 3
        private val DEFAULT = OnboardingUiState()
        private const val KEY_AREAS = "onboarding.areas"
        private const val KEY_MORNING = "onboarding.morning"
        private const val KEY_EVENING = "onboarding.evening"
        private const val KEY_BUDGET = "onboarding.budget"
        private const val KEY_GRANTED = "onboarding.notificationsGranted"
        private const val KEY_FINISHING = "onboarding.finishing"
    }
}
