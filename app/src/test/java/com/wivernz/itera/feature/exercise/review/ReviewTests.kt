@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.review

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.feynmanViewModel
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.reviewViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val FIRST = "RDB snapshots every few minutes and an append-only log"

/** Completes a Feynman on today's plan, then opens tomorrow: its review is on Today. */
private fun EngineHarness.reviewTomorrow(): Long {
    runBlocking { topics.add("Redis persistence") }
    val vm = feynmanViewModel(plant("feynman_technique", ExerciseType.FEYNMAN))
    vm.state.await { it.topic != null }
    vm.setExplanation("$FIRST " + (1..30).joinToString(" ") { "w$it" })
    vm.toReflect()
    vm.finish()
    vm.effects.awaitFirst()
    nextMorning()
    return runBlocking { ensureToday() }.activities.single { it.source == ActivitySource.REVIEW }.id
}

@RunWith(RobolectricTestRunner::class)
class ReviewViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun previousAnswerIsWithheldUntilReveal() {
        val vm = h.reviewViewModel(h.reviewTomorrow())
        val before = vm.state.await { !it.loading }
        assertNull(before.previousAnswer)
        assertTrue(FIRST !in before.toString())
        assertEquals("Redis persistence", before.topic)
        assertEquals(1, before.daysAgo)
        assertEquals(0, before.stageIndex)
        assertEquals(4, before.nextDays)
        vm.grade(RecallGrade.SOLID) // not revealed yet: ignored
        vm.reveal()
        assertTrue(vm.state.await { it.revealed }.previousAnswer!!.startsWith(FIRST))
    }

    @Test fun previewAtEachStageIncludingRetirement() {
        assertEquals(
            listOf(4, 9, 21, 60, null),
            (0..4).map {
                com.wivernz.itera.domain.review.ReviewScheduler.previewNextInterval(it)
            }
        )
    }

    @Test fun doubleTapCreatesOneAttemptAndSolidMovesTheLadder() {
        val id = h.reviewTomorrow()
        val vm = h.reviewViewModel(id)
        vm.state.await { !it.loading }
        vm.setAnswer("")
        vm.reveal()
        vm.state.await { it.revealed }
        vm.grade(RecallGrade.SOLID)
        vm.grade(RecallGrade.SOLID)
        assertTrue(vm.effects.awaitFirst() is ReviewEffect.ShowResult)
        assertEquals(1, runBlocking { h.reviews.attemptCount(id) })
        val item = runBlocking { h.reviews.active() }.single()
        assertEquals(1, item.stageIndex)
        assertEquals(h.today.plusDays(4), item.dueOn)
    }

    @Test fun forgotRestartsTheLadder() {
        val id = h.reviewTomorrow()
        val vm = h.reviewViewModel(id)
        vm.state.await { !it.loading }
        vm.reveal()
        vm.state.await { it.revealed }
        vm.grade(RecallGrade.FORGOT)
        vm.effects.awaitFirst()
        val item = runBlocking { h.reviews.active() }.single()
        assertEquals(0, item.stageIndex)
        assertEquals(h.today.plusDays(1), item.dueOn)
        assertEquals(ReviewState.SCHEDULED, item.state)
    }

    @Test fun closingBeforeAndAfterRevealLeavesTheReviewDue() {
        val id = h.reviewTomorrow()
        val vm = h.reviewViewModel(id)
        vm.state.await { !it.loading }
        vm.reveal()
        vm.state.await { it.revealed }
        vm.leave()
        assertEquals(ReviewEffect.Close, vm.effects.awaitFirst())
        assertEquals(ActivityState.AVAILABLE, runBlocking { h.plans.activity(id) }!!.state)
        assertEquals(0, runBlocking { h.reviews.attemptCount(id) })
    }
}

@RunWith(RobolectricTestRunner::class)
class ReviewScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val base =
        ReviewUiState(
            loading = false,
            topic = "Redis persistence",
            daysAgo = 4,
            stageIndex = 1,
            nextDays = 9
        )

    @Test fun previousAnswerAbsentBeforeCompareAndPresentAfter() {
        var state = base
        compose.setContent { IteraTheme { ReviewScreen(state, {}, {}, {}, {}) } }
        compose.onNodeWithText(FIRST, substring = true).assertDoesNotExist()
        compose.onNodeWithTag("ReviewHidden")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Your previous answer is hidden until you finish.")
                )
            )
        compose.onNodeWithTag("Grade_SOLID").assertDoesNotExist()
        compose.onNodeWithText("Your first explanation was 4 days ago:").assertExists()
        compose.onNodeWithText("If this goes well, the next review is in 9 days.").assertExists()
    }

    @Test fun revealShowsBothAnswersAndThreeGrades() {
        compose.setContent {
            IteraTheme {
                ReviewScreen(base.copy(answer = "today", revealed = true, previousAnswer = FIRST), {
                }, {}, {}, {})
            }
        }
        compose.onNodeWithText(FIRST).assertExists()
        compose.onNodeWithText("today").assertExists()
        RecallGrade.entries.forEach { compose.onNodeWithTag("Grade_${it.name}").assertExists() }
    }

    @Test fun ladderHighlightsTheStage() {
        compose.setContent { IteraTheme { ReviewScreen(base, {}, {}, {}, {}) } }
        compose.onNodeWithTag(
            "ReviewLadder"
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Review stage 2 of 5")
            )
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class ReviewIntegrationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun feynmanReviewAppearsNextDayAndReturnsFourDaysAfterSolid() {
        val first = h.reviewTomorrow()
        val vm = h.reviewViewModel(first)
        vm.state.await { !it.loading }
        vm.setAnswer("Snapshots plus a log")
        vm.reveal()
        vm.state.await { it.revealed }
        vm.grade(RecallGrade.SOLID)
        vm.effects.awaitFirst()
        repeat(3) {
            h.nextMorning()
            assertTrue(
                runBlocking { h.ensureToday() }.activities.none {
                    it.source ==
                        ActivitySource.REVIEW
                }
            )
        }
        h.nextMorning()
        assertTrue(
            runBlocking { h.ensureToday() }.activities.any {
                it.source ==
                    ActivitySource.REVIEW
            }
        )
    }
}
