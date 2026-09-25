package com.wivernz.itera.data.repository

import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.dao.FocusSessionDao
import com.wivernz.itera.data.database.dao.PlanActivityDao
import com.wivernz.itera.data.database.relation.TechniqueFactsRow
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.progress.CompletionFact
import com.wivernz.itera.domain.repository.PracticeFacts
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueFacts
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Raw progress facts; every aggregate is an indexed query over the activity log (ADR-0013). */
class RoomProgressRepository @Inject constructor(
    private val dao: PlanActivityDao,
    private val focus: FocusSessionDao,
    private val copy: CopyResolver,
    private val codec: ResultPayloadCodec,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : ProgressRepository {
    override fun observeHistory(month: YearMonth) = dao.observeHistory(
        month.atDay(1)
            .toEpochDay(),
        month.atEndOfMonth()
            .toEpochDay()
    )
        .map { rows ->
            rows.map {
                HistoryEntry(
                    LocalDate.ofEpochDay(it.practiceDate),
                    it.toDomain(
                        copy,
                        codec
                    )
                )
            }
        }
    override fun observePracticeFacts(id: TechniqueId, since: LocalDate) = combine(
        dao.countDistinctPracticeDays(id.value),
        dao.usesSince(
            id.value,
            since.toEpochDay()
        ),
        dao.firstUse(id.value),
        dao.usedInCombination(id.value)
    ) {
            days,
            uses,
            first,
            combination
        ->
        PracticeFacts(
            days,
            uses,
            first?.let(LocalDate::ofEpochDay),
            combination
        )
    }
    override fun observeTechniqueFacts(): Flow<List<TechniqueFacts>> =
        dao.observeTechniqueFacts().map { rows -> rows.map { it.toDomain() } }
    override suspend fun techniqueFacts() = withContext(io) {
        dao.techniqueFacts().map { it.toDomain() }
    }
    override fun observeCompletions(from: LocalDate, to: LocalDate) =
        dao.observeCompletions(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.map {
                CompletionFact(LocalDate.ofEpochDay(it.practiceDate), TechniqueId(it.techniqueId))
            }
        }
    override fun observeActivityCount() = dao.observeCompletedCount()
    override fun observeFocusSeconds(since: LocalDate?) =
        since?.let { focus.totalSecondsSince(it.toEpochDay()) } ?: focus.totalSeconds()

    private fun TechniqueFactsRow.toDomain() = TechniqueFacts(
        techniqueId = TechniqueId(techniqueId),
        distinctDays = distinctDays,
        totalUses = totalUses,
        firstUse = firstUse?.let(LocalDate::ofEpochDay),
        lastUse = lastUse?.let(LocalDate::ofEpochDay),
        usedInCombination = usedInCombination
    )
}
