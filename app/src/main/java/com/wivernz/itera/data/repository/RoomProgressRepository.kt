package com.wivernz.itera.data.repository
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.dao.PlanActivityDao
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.model.HistoryEntry
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.PracticeFacts
import com.wivernz.itera.domain.repository.ProgressStorage
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
class RoomProgressRepository @Inject constructor(
    private val dao: PlanActivityDao,
    private val copy: CopyResolver,
    private val codec: ResultPayloadCodec
) : ProgressStorage {
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
}
