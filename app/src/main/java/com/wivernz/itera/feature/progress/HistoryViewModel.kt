package com.wivernz.itera.feature.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryDay(val date: LocalDate, val entries: List<HistoryEntry>, val skills: List<Skill>)
data class HistoryUiState(
    val month: YearMonth = YearMonth.of(2000, 1),
    val today: LocalDate = LocalDate.of(2000, 1, 1),
    val days: List<HistoryDay> = emptyList(),
    val techniques: Map<TechniqueId, Technique> = emptyMap(),
    val previousEnabled: Boolean = false,
    val nextEnabled: Boolean = false,
    val selectedDate: LocalDate? = null,
    val loading: Boolean = true,
    val failed: Boolean = false
)
fun calendarCells(month: YearMonth, locale: Locale): List<LocalDate?> {
    val offset =
        (month.atDay(1).dayOfWeek.value - WeekFields.of(locale).firstDayOfWeek.value + 7) % 7
    return List((offset + month.lengthOfMonth() + 6) / 7 * 7) { index ->
        (index - offset + 1).takeIf { it in 1..month.lengthOfMonth() }?.let(month::atDay)
    }
}
fun historyDays(
    month: YearMonth,
    start: LocalDate,
    today: LocalDate,
    entries: List<HistoryEntry>,
    techniques: Map<TechniqueId, Technique>
): List<HistoryDay> {
    val first = maxOf(start, month.atDay(1))
    val last = minOf(today, month.atEndOfMonth())
    if (last < first) return emptyList()
    val grouped = entries.groupBy { it.date }
    return generateSequence(last) { it.minusDays(1).takeIf { d -> d >= first } }.map { date ->
        val rows = grouped[date].orEmpty().sortedByDescending { it.activity.completedAt }
        val skills = rows.filter {
            it.activity.state == ActivityState.COMPLETED
        }.mapNotNull { techniques[it.activity.techniqueId]?.skill }.toSet()
        HistoryDay(date, rows, Skill.entries.filter { it in skills }.take(3))
    }.toList()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    progress: ProgressRepository,
    prefs: PreferencesRepository,
    catalog: TechniqueCatalogRepository,
    analytics: Analytics,
    private val clock: Clock
) : ViewModel() {
    private val refresh = MutableStateFlow(0)
    private val month = saved.getStateFlow(
        "month",
        saved.get<String>("selectedDate")?.let { YearMonth.from(LocalDate.parse(it)).toString() }
            ?: YearMonth.now(clock).toString()
    )
    val state = combine(month, refresh) { m, _ -> YearMonth.parse(m) }.flatMapLatest { visible ->
        combine(progress.observeHistory(visible), prefs.preferences) { entries, preferences ->
            val today = LocalDate.now(clock)
            val start = preferences.programStartedOn ?: today
            val techniques = catalog.catalog().associateBy { it.id }
            HistoryUiState(
                visible,
                today,
                historyDays(visible, start, today, entries, techniques),
                techniques,
                visible > YearMonth.from(start),
                visible < YearMonth.from(today),
                saved.get<String>("selectedDate")?.let(LocalDate::parse),
                loading = false
            )
        }.catch {
            emit(
                HistoryUiState(
                    month = visible,
                    today = LocalDate.now(clock),
                    loading = false,
                    failed = true
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.HISTORY))
    }
    fun move(delta: Long) {
        if ((delta < 0 && state.value.previousEnabled) || (delta > 0 && state.value.nextEnabled)) {
            saved["month"] = state.value.month.plusMonths(delta).toString()
            saved["selectedDate"] = null
        }
    }
    fun refresh() {
        refresh.value++
    }
}
