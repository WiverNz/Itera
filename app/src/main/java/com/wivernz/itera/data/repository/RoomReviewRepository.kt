package com.wivernz.itera.data.repository
import com.wivernz.itera.data.database.dao.ReviewItemDao
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.repository.ReviewStorage
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.map
class RoomReviewRepository @Inject constructor(private val dao: ReviewItemDao) : ReviewStorage {

    override fun observeDue(on: LocalDate) = dao.observeDue(on.toEpochDay())
        .map { rows ->
            rows.map {
                it.toDomain()
            }
        }
    override fun observeUpcoming() = dao.observeUpcoming()
        .map { rows ->
            rows.map {
                it.toDomain()
            }
        }
    override suspend fun item(id: Long) = dao.byId(id)?.toDomain()
}
