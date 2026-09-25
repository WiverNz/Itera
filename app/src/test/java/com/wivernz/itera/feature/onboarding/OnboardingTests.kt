@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.navigation.AppNavHost
import com.wivernz.itera.core.navigation.AppRoute
import com.wivernz.itera.core.navigation.FirstWeek
import com.wivernz.itera.core.navigation.Goals
import com.wivernz.itera.core.navigation.NavigationActions
import com.wivernz.itera.core.navigation.Rhythm
import com.wivernz.itera.core.navigation.Today
import com.wivernz.itera.core.navigation.Welcome
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.onboarding.CompleteOnboardingUseCase
import com.wivernz.itera.domain.onboarding.OnboardingChoices
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.unlock.UnlockTechniquesUseCase
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.onboardingViewModel
import com.wivernz.itera.feature.reduceMotion
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val harness = EngineHarness(initialPreferences = UserPreferences())

    @After fun close() {
        main.clearViewModels()
        harness.close()
    }

    @Test fun initialStatePreselectsFocusAndLearning() {
        val state = harness.onboardingViewModel().state.value
        assertEquals(listOf(Skill.FOCUS, Skill.LEARNING), state.focusAreas)
        assertEquals(LocalTime.of(8, 30), state.morningTime)
        assertEquals(LocalTime.of(21, 0), state.eveningTime)
        assertEquals(TimeBudget.STANDARD, state.timeBudget)
        assertTrue(state.canContinueGoals)
    }

    @Test fun thirdSelectionDropsTheOldestAndDeselectingWorks() {
        val vm = harness.onboardingViewModel()
        vm.toggleFocusArea(Skill.HABITS)
        vm.state.await { it.focusAreas == listOf(Skill.LEARNING, Skill.HABITS) }
        vm.toggleFocusArea(Skill.LEARNING)
        vm.state.await { it.focusAreas == listOf(Skill.HABITS) }
    }

    @Test fun deselectingEverythingDisablesContinue() {
        val vm = harness.onboardingViewModel()
        vm.toggleFocusArea(Skill.FOCUS)
        vm.toggleFocusArea(Skill.LEARNING)
        assertFalse(vm.state.await { it.focusAreas.isEmpty() }.canContinueGoals)
    }

    @Test fun timeAndBudgetUpdates() {
        val vm = harness.onboardingViewModel()
        vm.setMorningTime(LocalTime.of(7, 5))
        vm.setEveningTime(LocalTime.of(22, 40))
        vm.setTimeBudget(TimeBudget.LONG)
        val state = vm.state.await { it.timeBudget == TimeBudget.LONG }
        assertEquals(LocalTime.of(7, 5), state.morningTime)
        assertEquals(LocalTime.of(22, 40), state.eveningTime)
        assertEquals(TimeBudget.LONG, state.timeBudget)
    }

    @Test fun permissionDenialTurnsAllFourReminderFlagsOff() {
        val vm = harness.onboardingViewModel()
        vm.onNotificationResult(false)
        val prefs = eventually { harness.prefs.state.value.takeIf { !it.notifyEvening } }
        assertFalse(
            prefs.notifyMorning || prefs.notifyFocus || prefs.notifyReviews || prefs.notifyEvening
        )
        assertFalse(prefs.onboardingCompleted)
    }

    @Test fun startDayOnePerformsTheStepsInOrder() {
        val vm = harness.onboardingViewModel()
        vm.toggleFocusArea(Skill.PLANNING)
        vm.setTimeBudget(TimeBudget.SHORT)
        vm.onNotificationResult(true)
        vm.startDayOne()
        assertEquals(OnboardingEffect.Finished, vm.effects.awaitFirst())
        val prefs = harness.prefs.state.value
        assertTrue(prefs.onboardingCompleted)
        assertEquals(setOf(Skill.LEARNING, Skill.PLANNING), prefs.focusAreas)
        assertEquals(TimeBudget.SHORT, prefs.timeBudget)
        assertEquals(harness.today, prefs.programStartedOn)
        val unlocked = runBlocking {
            harness.states.all()
        }.filter { it.unlocked }.map { it.techniqueId.value }
        assertEquals(setOf("daily_reflection", "two_minute_rule"), unlocked.toSet())
        val day = runBlocking { harness.plans.dayByDate(harness.today) }
        assertNotNull(day)
        // plan generated (and its reminders) before the full reschedule
        assertEquals(listOf("plan:${day!!.id}", "rescheduleAll"), harness.reminders.calls)
        val events = runBlocking { harness.eventNames() }
        assertTrue(events.indexOf("plan_generated") < events.indexOf("onboarding_completed"))
    }

    @Test fun planGenerationFailureStillCompletesOnboarding() {
        val broken = EngineHarness(initialPreferences = UserPreferences())
        val failingCatalog = object : TechniqueCatalogRepository {
            override suspend fun catalog(): List<Technique> = error("catalogue unreadable")
            override suspend fun technique(id: TechniqueId): Technique? =
                error("catalogue unreadable")
            override suspend fun curriculum(): Curriculum = error("catalogue unreadable")
        }
        val use = CompleteOnboardingUseCase(
            broken.prefs,
            UnlockTechniquesUseCase(
                failingCatalog,
                broken.states,
                broken.tx,
                broken.analytics,
                broken.clock
            ),
            broken.ensureToday,
            broken.reminders,
            broken.analytics,
            TestLogger(),
            broken.clock
        )
        runBlocking {
            use(
                OnboardingChoices(
                    listOf(Skill.FOCUS),
                    LocalTime.of(8, 30),
                    LocalTime.of(21, 0),
                    TimeBudget.STANDARD,
                    true
                )
            )
        }
        assertEquals(null, runBlocking { broken.plans.dayByDate(broken.today) })
        assertEquals(listOf("rescheduleAll"), broken.reminders.calls)
        assertTrue(broken.prefs.state.value.onboardingCompleted)
        broken.close()
    }

    @Test fun stateSurvivesSavedStateRoundTrip() {
        val saved = SavedStateHandle()
        val vm = harness.onboardingViewModel(saved)
        vm.toggleFocusArea(Skill.HABITS)
        vm.setEveningTime(LocalTime.of(20, 15))
        vm.setTimeBudget(TimeBudget.SHORT)
        // process death: a new view model over the restored handle
        val restored = harness.onboardingViewModel(
            SavedStateHandle(
                saved.keys().associateWith {
                    saved.get<Any>(it)
                }
            )
        )
        val state = restored.state.await { it.timeBudget == TimeBudget.SHORT }
        assertEquals(listOf(Skill.LEARNING, Skill.HABITS), state.focusAreas)
        assertEquals(LocalTime.of(20, 15), state.eveningTime)
        assertEquals(TimeBudget.SHORT, state.timeBudget)
    }
}

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val week = (1..7).map {
        FirstWeekRow(it, "two_minute_rule", "Technique $it", Skill.HABITS, it == 4)
    }

    @Test fun progressSegmentsAndCounterRender() {
        compose.setContent { IteraTheme { GoalsScreen(listOf(Skill.FOCUS), {}, {}, {}) } }
        compose.onAllNodesWithTag("OnboardingSegment", useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithText("1 of 3").assertExists()
    }

    @Test fun maxTwoSelectionAndContinueDisabledAtZero() {
        compose.setContent {
            var areas by remember { mutableStateOf(listOf(Skill.FOCUS, Skill.LEARNING)) }
            IteraTheme {
                GoalsScreen(areas, { skill ->
                    areas = if (skill in areas) {
                        areas - skill
                    } else {
                        (areas + skill).takeLast(OnboardingViewModel.MAX_AREAS)
                    }
                }, {}, {})
            }
        }
        compose.onNodeWithTag("GoalFOCUS").assertIsOn()
        compose.onNodeWithTag("GoalHABITS").performScrollTo().performClick()
        compose.onNodeWithTag("GoalFOCUS").assertIsOff()
        compose.onNodeWithTag("GoalHABITS").assertIsOn()
        compose.onNodeWithTag("GoalLEARNING").performClick()
        compose.onNodeWithTag("GoalHABITS").performClick()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test fun rhythmContinueAlwaysEnabledAndRationaleOnlyWhenNotGranted() {
        var rationale by mutableStateOf(true)
        compose.setContent {
            IteraTheme {
                RhythmScreen(
                    LocalTime.of(8, 30), LocalTime.of(21, 0), TimeBudget.STANDARD, rationale,
                    {}, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithText("Continue").assertIsEnabled()
        compose.onNodeWithText("15 min").assertExists()
        compose.onNodeWithTag("NotificationRationale").assertExists()
        rationale = false
        compose.onNodeWithTag("NotificationRationale").assertDoesNotExist()
    }

    @Test fun firstWeekRendersSevenCurriculumRows() {
        compose.setContent { IteraTheme { FirstWeekScreen(week, false, {}, {}) } }
        compose.onAllNodesWithTag("WeekRow").assertCountEquals(7)
        compose.onNodeWithText("Technique 4 + review").assertExists()
        compose.onNodeWithText("Today").assertExists()
    }

    @Composable
    private fun Flow(harness: EngineHarness, onRoute: (AppRoute) -> Unit) {
        val vm = remember { harness.onboardingViewModel() }
        val rhythm by vm.state.collectAsState()
        AppNavHost(false, destination = { route, actions: NavigationActions ->
            onRoute(route)
            when (route) {
                Welcome -> WelcomeRoute(vm, { actions.navigate(Goals) }, actions.finishOnboarding)
                Goals -> GoalsRoute(vm, actions.back) { actions.navigate(Rhythm) }
                Rhythm -> RhythmScreen(
                    rhythm.morningTime,
                    rhythm.eveningTime,
                    rhythm.timeBudget,
                    false,
                    {},
                    {},
                    {},
                    actions.back
                ) { actions.navigate(FirstWeek) }
                // Start Day 1's engine transaction is covered by FirstRunIntegrationTest.
                FirstWeek -> FirstWeekScreen(week, false, actions.back, actions.finishOnboarding)
                else -> androidx.compose.material3.Text(route::class.simpleName.orEmpty())
            }
        })
    }

    @Test fun completingTheFlowLandsOnTodayAndBackPreservesSelections() {
        val harness = EngineHarness(initialPreferences = UserPreferences())
        var current: AppRoute = Welcome
        compose.setContent { IteraTheme { Flow(harness) { current = it } } }
        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithTag("GoalHABITS").performScrollTo().performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.waitUntil { current == Rhythm }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil { current == Goals }
        compose.onNodeWithTag("GoalHABITS").assertIsOn()
        compose.onNodeWithTag("GoalFOCUS").assertIsOff()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.waitUntil { current == FirstWeek }
        compose.onNodeWithText("Start Day 1").performClick()
        compose.waitUntil { current == Today }
        compose.onNodeWithTag("BottomBar").assertExists()
        // the onboarding entries are gone: back cannot return to them
        compose.onNodeWithTag("Welcome").assertDoesNotExist()
        harness.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class FirstRunIntegrationTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun cleanInstallOnboardsIntoDayOne() {
        val harness = EngineHarness(initialPreferences = UserPreferences())
        val vm = harness.onboardingViewModel()
        vm.state.await { it.focusAreas.isNotEmpty() }
        vm.onGetStarted()
        vm.onStepCompleted(OnboardingViewModel.STEP_GOALS)
        vm.setMorningTime(LocalTime.of(7, 45))
        vm.onNotificationResult(true)
        vm.onStepCompleted(OnboardingViewModel.STEP_RHYTHM)
        vm.startDayOne()
        vm.effects.awaitFirst()

        val prefs = harness.prefs.state.value
        assertTrue(prefs.onboardingCompleted)
        assertEquals(LocalTime.of(7, 45), prefs.morningTime)
        assertEquals(1, prefs.currentProgramDay)
        val states = runBlocking { harness.states.all() }
        assertEquals(
            setOf("daily_reflection", "two_minute_rule"),
            states.filter { it.unlocked }.map { it.techniqueId.value }.toSet()
        )
        val day = checkNotNull(runBlocking { harness.plans.dayByDate(harness.today) })
        assertEquals(1, day.programDay)
        assertEquals(3, day.activities.size)
        assertEquals(
            listOf(
                ActivitySource.PROGRAM,
                ActivitySource.FOCUS_SUGGESTION,
                ActivitySource.REFLECTION
            ),
            day.activities.sortedBy { it.orderIndex }.map { it.source }
        )
        assertEquals(
            "two_minute_rule",
            day.activities.first {
                it.source == ActivitySource.PROGRAM
            }.techniqueId.value
        )
        val events = runBlocking { harness.eventNames() }
        assertTrue(
            events.containsAll(
                listOf("onboarding_started", "onboarding_step_completed", "onboarding_completed")
            )
        )
        main.clearViewModels()
        harness.close()
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(n: Int) {
    assertEquals(n, fetchSemanticsNodes().size)
}
