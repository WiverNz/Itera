package com.wivernz.itera.domain.review

import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.id
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.UserPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduleReviewUseCaseTest {
    private val h = EngineHarness()

    @After fun close() = h.close()

    private fun feynman(topic: Long, text: String) =
        ActivityResult.Feynman(topic, "Topic $topic", text, 3, emptyList(), null)

    @Test fun firstCompletionCreatesStage0DueTomorrow() = runTest {
        val topic = h.topics.add("Redis")
        val itemId = h.scheduleReview(
            11,
            id("feynman_technique"),
            feynman(topic, "first"),
            h.today
        )!!
        val item = h.reviews.item(itemId)!!
        assertEquals(0, item.stageIndex)
        assertEquals(h.today.plusDays(1), item.dueOn)
        assertEquals(ReviewState.SCHEDULED, item.state)
        assertEquals("first", h.reviews.revealAnswer(itemId))
    }

    @Test fun sameTopicUpdatesAnswerAndKeepsStage() = runTest {
        val topic = h.topics.add("Redis")
        val first = h.scheduleReview(
            11,
            id("feynman_technique"),
            feynman(topic, "first"),
            h.today
        )!!
        h.reviews.updateSchedule(h.reviews.item(first)!!.copy(stageIndex = 2))
        val second = h.scheduleReview(
            12,
            id("feynman_technique"),
            feynman(topic, "better"),
            h.today.plusDays(3)
        )
        assertEquals(first, second)
        val item = h.reviews.item(first)!!
        assertEquals(2, item.stageIndex)
        assertEquals("better", item.sourceAnswer)
        assertEquals(12, item.sourceActivityId)
    }

    @Test fun differentTopicCreatesASecondItem() = runTest {
        val a = h.scheduleReview(
            11,
            id("feynman_technique"),
            feynman(h.topics.add("A"), "a"),
            h.today
        )
        val b = h.scheduleReview(
            12,
            id("feynman_technique"),
            feynman(h.topics.add("B"), "b"),
            h.today
        )
        assertNotEquals(a, b)
        assertEquals(2, h.reviews.active().size)
    }

    @Test fun nonEligibleOrEmptyCreatesNothing() = runTest {
        assertNull(
            h.scheduleReview(1, id("pomodoro"), ActivityResult.Focus("t", 1, 1, 0, true), h.today)
        )
        assertNull(
            h.scheduleReview(1, id("feynman_technique"), feynman(h.topics.add("A"), " "), h.today)
        )
        val spaced = ActivityResult.Template(mapOf("item" to BlockValue.Text("mitochondria")))
        val itemId = h.scheduleReview(2, id("spaced_repetition"), spaced, h.today)!!
        assertNull(h.reviews.item(itemId)!!.topicId)
        assertEquals(1, h.reviews.active().size)
    }
}

@RunWith(RobolectricTestRunner::class)
class SubmitReviewUseCaseTest {
    private val h =
        EngineHarness(
            initialPreferences = UserPreferences(onboardingCompleted = true, currentProgramDay = 9)
        )

    @After fun close() = h.close()

    @Test fun gradesCompletesAndIgnoresAReplay() = runTest {
        val topic = h.topics.add("Redis persistence")
        val itemId = h.scheduleReview(
            999,
            id("feynman_technique"),
            ActivityResult.Feynman(topic, "Redis persistence", "snapshots", 1, emptyList(), null),
            h.today.minusDays(1)
        )!!
        val day = h.ensureToday()
        val review = day.activities.single { it.exerciseType == ExerciseType.REVIEW }
        assertEquals(itemId, review.reviewItemId)

        val after = h.submitReview(review.id, "from memory", RecallGrade.SOLID).getOrThrow()
        assertEquals(1, after.stageIndex)
        assertEquals(h.today.plusDays(4), after.dueOn)
        val attempts = h.db.reviewAttemptDao()
        assertEquals(1, attempts.countForActivity(review.id))
        val completed = h.plans.activity(review.id)!!
        assertEquals(ActivityState.COMPLETED, completed.state)
        val result = completed.result as ActivityResult.Review
        assertEquals("snapshots", result.previousAnswer)
        assertEquals(RecallGrade.SOLID, result.grade)

        // replayed tap / re-delivered notification
        h.submitReview(review.id, "again", RecallGrade.FORGOT).getOrThrow()
        assertEquals(1, h.reviews.attemptCount(review.id))
        assertEquals(1, h.reviews.item(itemId)!!.stageIndex)
        assertTrue(h.eventNames().count { it == "review_graded" } == 1)
    }

    @Test fun stageBeforeAndAfterAreRecorded() = runTest {
        val topic = h.topics.add("Topic")
        val itemId = h.scheduleReview(
            999,
            id("feynman_technique"),
            ActivityResult.Feynman(topic, "Topic", "x", 1, emptyList(), null),
            h.today.minusDays(1)
        )!!
        h.reviews.updateSchedule(h.reviews.item(itemId)!!.copy(stageIndex = 4))
        val review = h.ensureToday().activities.single { it.exerciseType == ExerciseType.REVIEW }
        val retired = h.submitReview(review.id, "x", RecallGrade.SOLID).getOrThrow()
        assertEquals(ReviewState.RETIRED, retired.state)
        val cursor = h.db.openHelper.readableDatabase.query(
            "SELECT stageBefore, stageAfter FROM review_attempt"
        )
        cursor.use {
            it.moveToFirst()
            assertEquals(4, it.getInt(0))
            assertEquals(5, it.getInt(1))
        }
        assertTrue(h.submitReview(-1, "x", RecallGrade.SOLID).isFailure)
    }
}
