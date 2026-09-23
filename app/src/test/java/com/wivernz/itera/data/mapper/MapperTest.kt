package com.wivernz.itera.data.mapper
import com.wivernz.itera.TestLogger
import com.wivernz.itera.data.activityEntity
import com.wivernz.itera.data.database.entity.FocusSessionEntity
import com.wivernz.itera.data.database.entity.HabitStackEntity
import com.wivernz.itera.data.database.entity.LearningTopicEntity
import com.wivernz.itera.data.database.entity.ReflectionEntity
import com.wivernz.itera.data.database.entity.ReviewAttemptEntity
import com.wivernz.itera.data.database.entity.ReviewItemEntity
import com.wivernz.itera.data.database.relation.TrainingDayWithActivities
import com.wivernz.itera.data.dayEntity
import com.wivernz.itera.data.testCopy
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class MapperTest {
    @Test fun aggregatesPreservePersistenceMetadataAndNulls() {
        val codec = ResultPayloadCodec(TestLogger())
        val copy = testCopy()
        val day = dayEntity().copy(id = 1)
        val row =
            activityEntity(1)
                .copy(
                    topicId =
                    9,
                    draftPayload =
                    "draft",
                    practiceDate =
                    day.date
                )
        assertEquals(row, row.toDomain(copy, codec).toEntity(row, codec))
        assertEquals(
            day,
            TrainingDayWithActivities(
                day,
                listOf(row)
            ).toDomain(
                copy,
                codec
            ).toEntity(day)
        )
        val topic = LearningTopicEntity(2, "private", false, 12)
        assertEquals(topic, topic.toDomain().toEntity())
        val review = ReviewItemEntity(
            3,
            "feynman",
            null,
            "prompt",
            1,
            "answer",
            0,
            day.date,
            null,
            "SCHEDULED",
            13
        )
        assertEquals(review, review.toDomain().toEntity(Instant.ofEpochMilli(13)))
        val focus = FocusSessionEntity(1, 1, "pomodoro", "task", 60, 60, 0, true, 0, 60000)
        assertEquals(focus, focus.toDomain().toEntity(focus))
        val reflection =
            ReflectionEntity(
                1,
                1,
                day.date,
                null,
                "[]",
                null,
                "[]",
                null,
                true,
                0
            )
        assertEquals(reflection, reflection.toDomain().toEntity(reflection))
        val habit = HabitStackEntity(1, null, "anchor", "habit", false, null, 0, false)
        assertEquals(habit, habit.toDomain().toEntity(habit))
        val attempt = ReviewAttemptEntity(1, 3, 1, "answer", "SOLID", 0, 1, 0)
        assertEquals(attempt, attempt.toDomain("before").toEntity(attempt))
    }
}
