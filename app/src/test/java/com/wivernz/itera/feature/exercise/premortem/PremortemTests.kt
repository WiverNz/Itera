@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.premortem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueDefaults
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.exercise.runner.ExerciseRunnerEffect
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.premortemViewModel
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.runnerViewModel
import java.time.LocalDate
import java.time.LocalTime
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

private fun technique(vararg related: String) = Technique(
    TechniqueId(
        "premortem"
    ),
    "Premortem", "", "", Skill.REFLECTION, 12, ExerciseType.PREMORTEM, 10, false,
    related.map(::TechniqueId), null, TechniqueDefaults()
)
private fun named(id: String, name: String) = technique().copy(id = TechniqueId(id), name = name)

private fun PremortemViewModel.fill(action: String = "") {
    state.await { !it.loading }
    setProject("Mobile beta")
    listOf("Scope kept growing", "Nobody tested it", "Backend dev left").forEach {
        setNewReason(it)
        addReason()
    }
    if (action.isNotEmpty()) setAction(action)
}

@RunWith(RobolectricTestRunner::class)
class PremortemViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun computedDateAcrossAYearBoundary() {
        assertEquals(
            LocalDate.of(2027, 3, 30),
            PremortemRules.failureDate(LocalDate.of(2026, 9, 30))
        )
        assertEquals(
            LocalDate.of(2026, 9, 28),
            PremortemRules.failureDate(LocalDate.of(2026, 3, 28))
        )
    }

    @Test fun ideaLineOnlyNamesUnlockedTechniques() {
        val catalog =
            listOf(
                named("two_list_strategy", "Two-list strategy"),
                named("daily_reflection", "Daily reflection")
            )
        val t = technique("two_list_strategy", "daily_reflection")
        assertNull(PremortemRules.idea(t, emptySet(), catalog))
        assertEquals("Daily reflection", PremortemRules.idea(t, setOf("daily_reflection"), catalog))
        assertNull(PremortemRules.idea(technique(), setOf("daily_reflection"), catalog))
    }

    @Test fun addEditReorderDeleteAndTheLimits() {
        val vm = h.premortemViewModel(h.plant("premortem", ExerciseType.PREMORTEM))
        vm.state.await { !it.loading }
        vm.setProject("Beta")
        listOf("a", "b").forEach {
            vm.setNewReason(it)
            vm.addReason()
        }
        assertEquals(1, vm.state.value.reasonsMissing)
        vm.setNewReason("c")
        vm.addReason()
        assertTrue(vm.state.value.ready)
        vm.move(2, -1)
        assertEquals(listOf("a", "c", "b"), vm.state.value.reasons.map { it.text })
        vm.move(0, -1) // no-op at the top
        vm.cycleLikelihood(1)
        assertEquals(Likelihood.LIKELY, vm.state.value.reasons[1].likelihood)
        vm.setReason(0, "a2")
        vm.move(1, -1)
        assertEquals("c", vm.state.value.reasons.first().text) // risk #1 follows the order
        vm.remove(2)
        assertEquals(2, vm.state.value.reasons.size)
        repeat(6) {
            vm.setNewReason("x$it")
            vm.addReason()
        }
        assertEquals(PremortemRules.MAX_REASONS, vm.state.value.reasons.size)
    }

    @Test fun addToTodayCreatesOneManualActivity() {
        val id = h.plant("premortem", ExerciseType.PREMORTEM)
        val vm = h.premortemViewModel(id)
        vm.fill("Write a not-in-beta list")
        vm.addToToday()
        vm.addToToday()
        assertTrue(vm.effects.awaitFirst() is PremortemEffect.ShowResult)
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Premortem
        assertTrue(result.mitigationAddedToToday)
        assertEquals(3, result.reasons.size)
        val manual = runBlocking { h.ensureToday() }.activities.filter {
            it.source ==
                ActivitySource.MANUAL &&
                it.exerciseType == ExerciseType.TEMPLATE
        }
        assertEquals(1, manual.size)
    }

    @Test fun completingWithoutAnActionIsAllowed() {
        val id = h.plant("premortem", ExerciseType.PREMORTEM)
        val vm = h.premortemViewModel(id)
        vm.fill()
        vm.finishWithoutAdding()
        vm.effects.awaitFirst()
        val result = runBlocking { h.plans.activity(id) }!!.result as ActivityResult.Premortem
        assertNull(result.mitigationAction)
    }

    @Test fun nearMidnightAttachesToTodaysPlan() {
        val id = h.plant("premortem", ExerciseType.PREMORTEM)
        val today = runBlocking { h.ensureToday() }
        val vm = h.premortemViewModel(id)
        vm.fill("Cut scope")
        h.at(LocalTime.of(23, 58))
        vm.addToToday()
        vm.effects.awaitFirst()
        val manual = runBlocking { h.plans.day(today.id) }!!.activities
            .count {
                it.source == ActivitySource.MANUAL && it.exerciseType == ExerciseType.TEMPLATE
            }
        assertEquals(1, manual)
    }
}

@RunWith(RobolectricTestRunner::class)
class PremortemScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private fun actions(onCycle: (Int) -> Unit = {}, onAdd: () -> Unit = {}) =
        PremortemActions({}, {}, {}, onCycle, {}, { _, _ -> }, {}, {}, onAdd, {}, {})

    private val base = PremortemUiState(
        loading = false,
        name = "Premortem",
        failureDate = LocalDate.of(2027, 3, 28),
        project = "Beta",
        reasons = listOf(
            PremortemReason("a", Likelihood.POSSIBLE),
            PremortemReason("b", Likelihood.LIKELY)
        )
    )

    @Test fun headlineShowsTheComputedDateAndThePrimaryIsGated() {
        compose.setContent { IteraTheme { PremortemScreen(base, actions()) } }
        compose.onNodeWithText("March 2027", substring = true).assertExists()
        compose.onNodeWithTag("PremortemAdd").assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Add 1 more reason to finish"
                )
            )
    }

    @Test fun likelihoodChipsToggle() {
        var cycled = -1
        compose.setContent {
            IteraTheme { PremortemScreen(base, actions(onCycle = { cycled = it })) }
        }
        compose.onNodeWithTag("Likelihood_1").performClick()
        compose.runOnIdle { assertEquals(1, cycled) }
    }

    @Test fun addToTodayFiresOnceOnADoubleTap() {
        var state by mutableStateOf(
            base.copy(
                reasons =
                base.reasons + PremortemReason("c", Likelihood.CERTAIN),
                action = "Cut"
            )
        )
        var adds = 0
        compose.setContent {
            IteraTheme {
                PremortemScreen(
                    state,
                    actions(onAdd = {
                        adds++
                        state =
                            state.copy(busy = true)
                    })
                )
            }
        }
        compose.onNodeWithTag("PremortemAdd").performClick()
        compose.onNodeWithTag("PremortemAdd").performClick()
        compose.runOnIdle { assertEquals(1, adds) }
    }
}

@RunWith(RobolectricTestRunner::class)
class PremortemMitigationTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun mitigationAppearsOnTodayAndCanBeCompleted() {
        val vm = h.premortemViewModel(h.plant("premortem", ExerciseType.PREMORTEM))
        vm.fill("Write a not-in-beta list")
        vm.addToToday()
        vm.effects.awaitFirst()
        val mitigation = runBlocking { h.ensureToday() }.activities.single {
            it.source ==
                ActivitySource.MANUAL &&
                it.exerciseType == ExerciseType.TEMPLATE
        }
        val runner = h.runnerViewModel(mitigation.id)
        val intro = runner.state.await { !it.loading }
        assertEquals("Write a not-in-beta list", intro.task)
        assertEquals(ExerciseBody.Generic, intro.body)
        runner.onPrimary()
        assertTrue(runner.effects.awaitFirst() is ExerciseRunnerEffect.ShowResult)
        assertEquals(
            ActivityState.COMPLETED,
            runBlocking {
                h.plans.activity(mitigation.id)
            }!!.state
        )
    }
}
