package com.wivernz.itera.data.repository

import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.database.dao.ReviewAttemptDao
import com.wivernz.itera.data.database.dao.ReviewItemDao
import com.wivernz.itera.data.database.entity.ReviewAttemptEntity
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.data.mapper.toEntity
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.ReviewAttemptRecord
import com.wivernz.itera.domain.repository.ReviewRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomReviewRepository @Inject constructor(
    private val dao: ReviewItemDao,
    private val attempts: ReviewAttemptDao,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : ReviewRepository {

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
    override suspend fun item(id: Long) = withContext(io) { dao.byId(id)?.toDomain() }
    override suspend fun active() = withContext(io) { dao.active().map { it.toDomain() } }
    override suspend fun activeFor(techniqueId: TechniqueId, topicId: Long?) =
        withContext(io) { dao.activeFor(techniqueId.value, topicId)?.toDomain() }
    override suspend fun insert(item: ReviewItem, createdAt: Instant) =
        withContext(io) { dao.insert(item.copy(id = 0).toEntity(createdAt)) }
    override suspend fun replaceAnswer(
        id: Long,
        prompt: String,
        answer: String,
        sourceActivityId: Long
    ) = withContext(io) { dao.replaceAnswer(id, prompt, answer, sourceActivityId) }
    override suspend fun updateSchedule(item: ReviewItem) = withContext(io) {
        dao.updateStage(
            item.id,
            item.stageIndex,
            item.dueOn.toEpochDay(),
            item.lastReviewedOn?.toEpochDay(),
            item.state.name
        )
    }
    override suspend fun insertAttempt(attempt: ReviewAttemptRecord) = withContext(io) {
        attempts.insert(
            ReviewAttemptEntity(
                reviewItemId = attempt.reviewItemId,
                activityId = attempt.activityId,
                answer = attempt.answer,
                grade = attempt.grade.name,
                stageBefore = attempt.stageBefore,
                stageAfter = attempt.stageAfter,
                createdAt = attempt.createdAt.toEpochMilli()
            )
        )
    }
    override suspend fun attemptCount(activityId: Long) =
        withContext(io) { attempts.countForActivity(activityId) }
    override suspend fun revealAnswer(id: Long) = withContext(io) { dao.answer(id) }
}
