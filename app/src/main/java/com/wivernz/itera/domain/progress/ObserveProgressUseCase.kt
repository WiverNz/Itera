package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.ProgressSummary
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TechniqueStateRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** The Progress summary (docs/engine/03-mastery-and-progress.md section 4), derived on every read. */
class ObserveProgressUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val catalog: TechniqueCatalogRepository,
    private val preferences: PreferencesRepository,
    private val clock: Clock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ProgressSummary> = preferences.preferences
        .map { it.programStartedOn }
        .distinctUntilChanged()
        .flatMapLatest { startedOn ->
            val today = LocalDate.now(clock)
            val start = SkillLevels.windowStart(startedOn, today)
            val skills = catalog.catalog().associate { it.id to it.skill }
            combine(
                progress.observeCompletions(start, today),
                progress.observeActivityCount(),
                progress.observeFocusSeconds(null),
                progress.observeFocusSeconds(start)
            ) { completions, count, focusAll, focusWindow ->
                SkillLevels.summarize(
                    today = today,
                    programStartedOn = startedOn,
                    completions = completions,
                    skillOf = skills::get,
                    activityCount = count,
                    focusSecondsAllTime = focusAll,
                    focusSecondsInWindow = focusWindow
                )
            }
        }
}

/** Mastery for every catalogue technique, for the library and technique detail. */
class ObserveTechniqueProgressUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val states: TechniqueStateRepository,
    private val catalog: TechniqueCatalogRepository
) {
    operator fun invoke(): Flow<List<TechniqueProgress>> =
        combine(states.observeAll(), progress.observeTechniqueFacts()) { stateRows, factRows ->
            val stateById = stateRows.associateBy { it.techniqueId }
            val factById = factRows.associateBy { it.techniqueId }
            catalog.catalog().filter { !it.retired }.map { technique ->
                val state = stateById[technique.id]
                val facts = factById[technique.id]
                val distinctDays = facts?.distinctDays ?: 0
                val totalUses = facts?.totalUses ?: 0
                val level = Mastery.masteryOf(
                    unlocked = state?.unlocked == true,
                    introCompleted = state?.introCompletedAt != null,
                    distinctDays = distinctDays,
                    totalUses = totalUses,
                    firstUse = facts?.firstUse,
                    lastUse = facts?.lastUse,
                    usedInCombination = facts?.usedInCombination == true
                )
                TechniqueProgress(
                    techniqueId = technique.id,
                    unlocked = state?.unlocked == true,
                    unlocksOnDay = technique.introDay,
                    level = level,
                    distinctPracticeDays = distinctDays,
                    totalUses = totalUses,
                    firstUsedOn = facts?.firstUse,
                    lastUsedOn = facts?.lastUse,
                    usedInCombination = facts?.usedInCombination == true,
                    nextLevelHint = Mastery.nextLevelHint(level, distinctDays, totalUses)
                )
            }
        }

    fun of(id: TechniqueId): Flow<TechniqueProgress?> =
        invoke().map { all -> all.firstOrNull { it.techniqueId == id } }
}

/** Completed and skipped activities of one month, for History. */
class ObserveHistoryUseCase @Inject constructor(private val progress: ProgressRepository) {
    operator fun invoke(month: YearMonth): Flow<List<HistoryEntry>> = progress.observeHistory(month)
}
