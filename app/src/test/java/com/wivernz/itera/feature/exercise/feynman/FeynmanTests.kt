@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.feynman

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.LearningTopic
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.feynmanViewModel
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun words(n: Int) = (1..n).joinToString(" ") { "word$it" }

@RunWith(RobolectricTestRunner::class)
class FeynmanViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun wordCounting() {
        assertEquals(5, FeynmanText.words("  Redis   keeps\tdata,  in memory - ! "))
        assertEquals(0, FeynmanText.words(" ... - "))
    }

    @Test fun inlineTopicCreationWithAnEmptyList() {
        val vm = h.feynmanViewModel(h.plant("feynman_technique", ExerciseType.FEYNMAN))
        val empty = vm.state.await { !it.loading }
        assertTrue(empty.creatingTopic)
        vm.setNewTopic("Redis persistence")
        vm.addTopic()
        val picked = vm.state.await { it.topic != null }
        assertEquals("Redis persistence", picked.topic!!.title)
        assertEquals(1, picked.reviewInDays)
    }

    @Test fun topicSelectionFromTheList() {
        val a = runBlocking { h.topics.add("A") }
        val b = runBlocking { h.topics.add("B") }
        val vm = h.feynmanViewModel(h.plant("feynman_technique", ExerciseType.FEYNMAN))
        vm.state.await { it.topics.size == 2 }
        vm.pickTopic(b)
        assertEquals(b, vm.state.value.topicId)
        assertTrue(a != b)
    }

    @Test fun thirtyWordGateAndStepTransition() {
        runBlocking { h.topics.add("Redis") }
        val vm = h.feynmanViewModel(h.plant("feynman_technique", ExerciseType.FEYNMAN))
        vm.state.await { it.topic != null }
        vm.setExplanation(words(29))
        vm.toReflect()
        assertEquals(0, vm.state.value.step)
        assertEquals(1, vm.state.value.wordsMissing)
        vm.setExplanation(words(30))
        vm.toReflect()
        assertEquals(1, vm.state.value.step)
        vm.toggleHardest("why")
        vm.setNote("Got stuck on AOF")
        assertEquals(setOf("why"), vm.state.value.hardest)
    }

    @Test fun completionSchedulesAReviewAndReExplainingUpdatesIt() {
        val topic = runBlocking { h.topics.add("Redis") }
        val first = h.plant("feynman_technique", ExerciseType.FEYNMAN)
        val vm = h.feynmanViewModel(first)
        vm.state.await { it.topic != null }
        vm.setExplanation(words(31))
        vm.toReflect()
        vm.toggleHardest("terms")
        vm.finish()
        assertTrue(vm.effects.awaitFirst() is FeynmanEffect.ShowResult)
        val result = runBlocking { h.plans.activity(first) }!!.result as ActivityResult.Feynman
        assertEquals(listOf("terms"), result.hardestParts)
        val item = runBlocking { h.reviews.activeFor(TechniqueId("feynman_technique"), topic) }!!
        assertEquals(0, item.stageIndex)
        assertEquals(h.today.plusDays(1), item.dueOn)
        runBlocking { h.reviews.updateSchedule(item.copy(stageIndex = 2)) }

        val again = h.feynmanViewModel(h.plant("feynman_technique", ExerciseType.FEYNMAN))
        assertEquals(9, again.state.await { it.topic != null && it.reviewInDays != 1 }.reviewInDays)
        again.setExplanation(words(40))
        again.toReflect()
        again.finish()
        again.effects.awaitFirst()
        val active = runBlocking { h.reviews.active() }
        assertEquals(1, active.size)
        assertEquals(2, active.single().stageIndex)
    }
}

@RunWith(RobolectricTestRunner::class)
class FeynmanScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val topic = LearningTopic(1, "Redis persistence", Instant.EPOCH, false)
    private val actions = FeynmanActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test fun explainStepIsGatedWithTheShortfall() {
        compose.setContent {
            IteraTheme {
                FeynmanScreen(
                    FeynmanUiState(
                        loading = false,
                        name = "Feynman",
                        topics = listOf(topic),
                        topicId = 1,
                        explanation = words(12)
                    ),
                    actions
                )
            }
        }
        compose.onNodeWithText("Redis persistence").assertExists()
        compose.onNodeWithTag("FeynmanDone").assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Write 18 more words to continue"
                )
            )
        compose.onNodeWithText("12 words").assertExists()
        compose.onNodeWithTag(
            "FeynmanWordAnnouncement"
        ).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("")))
    }

    @Test fun reflectStepHasTheCoachBoxWithoutFabricatedFeedback() {
        compose.setContent {
            IteraTheme {
                FeynmanScreen(
                    FeynmanUiState(
                        loading = false,
                        step = 1,
                        topics = listOf(topic),
                        topicId = 1,
                        reviewInDays = 1
                    ),
                    actions
                )
            }
        }
        compose.onNodeWithTag("CoachBox").assertExists()
        compose.onNodeWithText("Coming later").assertExists()
        compose.onNodeWithTag("CoachButton").assertIsNotEnabled()
        compose.onNodeWithText("You’ll explain this again in 1 day, from memory.").assertExists()
        listOf(
            "The “saving a game” comparison makes snapshots easy to picture.",
            "What happens to changes made after the last snapshot?",
            "“So why not just always use the log?”",
            "Clear",
            "Gap to check",
            "A beginner might ask"
        ).forEach { compose.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test fun emptyTopicListOffersInlineCreation() {
        compose.setContent {
            IteraTheme { FeynmanScreen(FeynmanUiState(loading = false), actions) }
        }
        compose.onNodeWithText("What are you learning?").assertExists()
        compose.onNodeWithTag("FeynmanNewTopic").assertExists()
    }
}
