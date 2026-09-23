package com.wivernz.itera.data.repository
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.database.dao.LearningTopicDao
import com.wivernz.itera.data.database.entity.LearningTopicEntity
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.domain.repository.LearningTopicRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
class RoomLearningTopicRepository @Inject constructor(
    private val dao: LearningTopicDao,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : LearningTopicRepository {
    override fun observeTopics() = dao.observeActive()
        .map { rows ->
            rows.map {
                it.toDomain()
            }
        }
    override suspend fun add(title: String) = withContext(io) {
        dao.insert(
            LearningTopicEntity(
                title = title,
                archived = false,
                createdAt = clock.millis()
            )
        )
    }
    override suspend fun rename(id: Long, title: String) = withContext(io) {
        dao.rename(
            id,
            title
        )
    }
    override suspend fun archive(id: Long) = withContext(io) { dao.archive(id) }
    override suspend fun nextTopicForReview() = dao.leastRecentlyReviewed()?.toDomain()
}
