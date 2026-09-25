@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.today

import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.training.EnsureTodayPlanUseCase
import com.wivernz.itera.domain.unlock.ContentReconciler
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.subscribe
import com.wivernz.itera.feature.todayViewModel
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val harnesses = mutableListOf<EngineHarness>()

    private fun harness(prefs: UserPreferences = UserPreferences(onboardingCompleted = true)) =
        EngineHarness(initialPreferences = prefs).also { harnesses += it }

    @After fun close() {
        main.clearViewModels()
        harnesses.forEach { it.close() }
    }

    @Test fun startsLoadingThenShowsDayOne() {
        val h = harness()
        val vm = h.todayViewModel()
        assertTrue(TodayUiState().loading)
        val stop = vm.state.subscribe()
        val state = vm.state.await { it.hero != null }
        assertEquals(1, state.programDay)
        assertTrue(state.hero is TodayHero.Exercise)
        assertEquals(0 to 3, state.completedCount to state.totalCount)
        stop()
    }

    @Test fun ensuresThePlanOnceInInit() {
        val h = harness()
        val vm = h.todayViewModel()
        val stop = vm.state.subscribe()
        vm.state.await { it.hero != null }
        vm.onResume()
        vm.state.await { it.hero != null }
        assertEquals(1, runBlocking { h.eventNames() }.count { it == "plan_generated" })
        stop()
    }

    @Test fun planFailureShowsErrorAndRetryFiresAgain() {
        val h = harness()
        val failing = object : TechniqueCatalogRepository {
            override suspend fun catalog(): List<Technique> = error("unreadable")
            override suspend fun technique(id: TechniqueId): Technique? = error("unreadable")
            override suspend fun curriculum(): Curriculum = error("unreadable")
        }
        val ensure =
            EnsureTodayPlanUseCase(
                ContentReconciler(failing, h.prefs, h.unlock),
                h.generate,
                h.clock
            )
        val vm =
            main.track(
                TodayViewModel(ensure, h.plans, failing, h.reviews, h.prefs, h.refresh, h.complete, h.analytics, h.clock)
            )
        val stop = vm.state.subscribe()
        assertTrue(vm.state.await { it.error }.hero == null)
        vm.retry()
        assertTrue(vm.state.await { it.error }.error)
        stop()
    }

    @Test fun practicePromptCompletesInPlaceAndUndoRestores() {
        val h = harness(UserPreferences(onboardingCompleted = true, timeBudget = TimeBudget.SHORT))
        runBlocking { h.trainFullDay() }
        h.nextMorning()
        h.at(LocalTime.of(12, 0))
        val vm = h.todayViewModel()
        val stop = vm.state.subscribe()
        val prompt = vm.state.await { s -> s.steps.any { it.target is TodayTarget.LogPractice } }
            .steps.first { it.target is TodayTarget.LogPractice }
        val target = prompt.target as TodayTarget.LogPractice

        vm.logPractice(target)
        vm.state.await { it.loggedPractice == target }
        vm.state.await { s ->
            s.steps.first { it.activityId == target.activityId }.state.name ==
                "Done"
        }
        vm.undoPractice(target.activityId)
        vm.state.await { s ->
            s.steps.first { it.activityId == target.activityId }.state.name ==
                "Now"
        }
        assertEquals(
            ActivityState.AVAILABLE,
            runBlocking {
                h.plans.activity(target.activityId)
            }!!.state
        )

        vm.logPractice(target)
        // committed once the undo window has passed
        val stored = eventually {
            runBlocking { h.plans.activity(target.activityId) }?.takeIf {
                it.state ==
                    ActivityState.COMPLETED
            }
        }
        assertEquals(ActivitySource.PRACTICE_PROMPT, stored.source)
        stop()
    }

    @Test fun resumeRecomputesGreeting() {
        val h = harness()
        h.at(LocalTime.of(11, 59))
        val vm = h.todayViewModel()
        val stop = vm.state.subscribe()
        vm.state.await { it.hero != null && it.greeting == Greeting.MORNING }
        h.at(LocalTime.of(18, 0))
        vm.onResume()
        vm.state.await { it.greeting == Greeting.EVENING }
        stop()
    }
}
