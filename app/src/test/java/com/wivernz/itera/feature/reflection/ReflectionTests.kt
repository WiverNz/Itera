@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.reflection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.eventually
import com.wivernz.itera.feature.planActivity
import com.wivernz.itera.feature.reduceMotion
import com.wivernz.itera.feature.reflectionViewModel
import com.wivernz.itera.feature.today.TodayInput
import com.wivernz.itera.feature.today.mapToUiState
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun done(activity: PlanActivity, result: ActivityResult) =
    activity.copy(state = ActivityState.COMPLETED, result = result)

private val twoMinute = done(
    planActivity(1, "two_minute_rule", ActivitySource.PROGRAM, ExerciseType.TEMPLATE),
    ActivityResult.Template(
        mapOf(
            "tasks" to BlockValue.Items(
                listOf(ChecklistItem("1", "a", true, 30), ChecklistItem("2", "b", true, 40))
            )
        )
    )
)
private val fiveSecond = done(
    planActivity(1, "five_second_rule", ActivitySource.PROGRAM, ExerciseType.TEMPLATE),
    ActivityResult.Template(emptyMap())
)
private val focusBlock = done(
    planActivity(
        2,
        "pomodoro",
        ActivitySource.FOCUS_SUGGESTION,
        ExerciseType.FOCUS_TIMER,
        optional = true
    ),
    ActivityResult.Focus("report", 1500, 1500, 0, true)
)
private val names = mapOf(
    "five_second_rule" to "5-second rule",
    "two_minute_rule" to "2-minute rule"
)

@RunWith(RobolectricTestRunner::class)
class ReflectionPrefillTest {
    @get:Rule val compose = createComposeRule()

    private fun render(parts: List<PrefillPart>): String {
        var text = ""
        compose.setContent { text = renderPrefill(parts) }
        compose.waitForIdle()
        return text
    }

    @Test fun oneExercise() {
        val parts = ReflectionPrefill.parts(listOf(fiveSecond), names)
        assertEquals(listOf(PrefillPart.Practised(listOf("5-second rule"))), parts)
        assertEquals("Practised 5-second rule", render(parts))
    }

    @Test fun oneExercisePlusOneFocusSession() {
        val parts = ReflectionPrefill.parts(listOf(fiveSecond, focusBlock), names)
        assertEquals("Practised 5-second rule · Focused for 25 minutes", render(parts))
    }

    @Test fun twoQuickTasksPlusFocus() {
        val parts = ReflectionPrefill.parts(listOf(twoMinute, focusBlock), names)
        assertEquals(listOf(PrefillPart.Tasks(2), PrefillPart.Focus(25)), parts)
        assertEquals("Cleared 2 small tasks · Focused for 25 minutes", render(parts))
    }

    @Test fun emptyDayHasNoPrefill() {
        val open = planActivity(1, "two_minute_rule", ActivitySource.PROGRAM, ExerciseType.TEMPLATE)
        assertEquals(emptyList<PrefillPart>(), ReflectionPrefill.parts(listOf(open), names))
        assertEquals("", render(emptyList()))
    }
}

/** Harness helpers: Day 1 in the evening with its reflection reachable. */
private fun EngineHarness.eveningOfDayOne(
    completeExercise: Boolean,
    completeFocus: Boolean
): TrainingDay {
    val day = runBlocking { ensureToday() }
    at(LocalTime.of(20, 30))
    runBlocking {
        refresh()
        day.activities.forEach { a ->
            val done = (a.source == ActivitySource.PROGRAM && completeExercise) ||
                (a.source == ActivitySource.FOCUS_SUGGESTION && completeFocus)
            if (done) complete(a.id, resultFor(a)).getOrThrow()
        }
    }
    return checkNotNull(runBlocking { plans.day(day.id) })
}

private val TrainingDay.reflection get() = activities.first {
    it.source == ActivitySource.REFLECTION
}

@RunWith(RobolectricTestRunner::class)
class ReflectionViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun prefillFromExerciseOnly() {
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = false)
        val state = h.reflectionViewModel(day.reflection.id).state.await { !it.loading }
        assertEquals(listOf(PrefillPart.Tasks(2)), state.prefill)
    }

    @Test fun prefillFromExercisePlusFocus() {
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = true)
        val state = h.reflectionViewModel(day.reflection.id).state.await { !it.loading }
        assertEquals(listOf(PrefillPart.Tasks(2), PrefillPart.Focus(25)), state.prefill)
    }

    @Test fun prefillFromNothingCompleted() {
        val day = h.eveningOfDayOne(completeExercise = false, completeFocus = false)
        val state = h.reflectionViewModel(day.reflection.id).state.await { !it.loading }
        assertEquals(emptyList<PrefillPart>(), state.prefill)
    }

    @Test fun clearedPrefillIsStoredEmptyWithChipsAndText() {
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = false)
        val vm = h.reflectionViewModel(day.reflection.id)
        vm.state.await { !it.loading }
        vm.applyPrefill("Cleared 2 small tasks")
        assertEquals("Cleared 2 small tasks", vm.state.await { it.prefill == null }.answers[0])
        vm.setAnswer(0, "")
        vm.next()
        vm.toggleChip(1, "started_late") { "Started late" }
        vm.toggleChip(1, "distracted") { "Started late · Got distracted" }
        vm.toggleChip(1, "started_late") { "Got distracted" }
        assertEquals(
            setOf("distracted"),
            vm.state.await {
                it.chips[1] == setOf("distracted")
            }.chips[1]
        )
        vm.setAnswer(1, "Got distracted by chat")
        vm.next()
        vm.setAnswer(2, "Phone in the drawer")
        vm.next()
        val effect = vm.effects.awaitFirst() as ReflectionEffect.Finished
        assertTrue(effect.dayComplete)
        val stored = runBlocking {
            h.plans.activity(day.reflection.id)
        }!!.result as ActivityResult.Reflection
        assertEquals("", stored.wentWell)
        assertEquals(listOf("distracted"), stored.didNotGoWellChips)
        assertEquals("Got distracted by chat", stored.didNotGoWell)
        assertEquals("Phone in the drawer", stored.tomorrowChange)
    }

    @Test fun skipTonightRecordsSkippedAndStillFinishes() {
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = false)
        val vm = h.reflectionViewModel(day.reflection.id)
        vm.state.await { !it.loading }
        vm.skipTonight()
        val effect = vm.effects.awaitFirst() as ReflectionEffect.Finished
        assertTrue(effect.dayComplete)
        assertEquals(
            ActivityState.SKIPPED,
            runBlocking {
                h.plans.activity(day.reflection.id)
            }!!.state
        )
        assertNull(runBlocking { h.plans.latestIntent() })
    }

    @Test fun draftRestoresAllAnswers() {
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = false)
        val vm = h.reflectionViewModel(day.reflection.id)
        vm.state.await { !it.loading }
        vm.applyPrefill("Cleared 2 small tasks")
        vm.next()
        vm.setAnswer(1, "Late start")
        vm.next()
        vm.setAnswer(2, "Start at 8")
        vm.onClose()
        eventually {
            (runBlocking { h.plans.draft(day.reflection.id) } as? ActivityResult.Reflection)
                ?.takeIf { it.tomorrowChange == "Start at 8" }
        }
        val restored = h.reflectionViewModel(day.reflection.id).state.await { !it.loading }
        assertEquals(listOf("Cleared 2 small tasks", "Late start", "Start at 8"), restored.answers)
        assertNull(restored.prefill)
    }

    @Test fun weeklyLookBackOnDaySeven() {
        repeat(6) {
            runBlocking { h.trainFullDay() }
            h.nextMorning()
        }
        val day = runBlocking { h.ensureToday() }
        assertEquals(7, day.programDay)
        assertTrue(day.reflection.weeklyLookBack)
        val state = h.reflectionViewModel(day.reflection.id).state.await { !it.loading }
        val lookBack = checkNotNull(state.lookBack)
        assertEquals(6, lookBack.daysTrained)
        assertTrue(lookBack.techniquesPractised >= 5)
        assertEquals(listOf("start early"), lookBack.changes)
    }
}

@RunWith(RobolectricTestRunner::class)
class ReflectionScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val chipCalls = mutableListOf<String>()

    @Test fun stepsRevealInOrderAndChipsToggle() {
        var state by mutableStateOf(
            ReflectionUiState(loading = false, answers = listOf("Cleared 2 small tasks", "", ""))
        )
        compose.setContent {
            IteraTheme {
                ReflectionScreen(
                    state,
                    onAnswer = { _, _ -> },
                    onChip = { q, id, rewrite ->
                        chipCalls += id
                        val chips = state.chips[q].let { if (id in it) it - id else it + id }
                        state = state.copy(
                            chips = state.chips.toMutableList().also { it[q] = chips },
                            answers = state.answers.toMutableList().also { it[q] = rewrite(chips) }
                        )
                    },
                    onNext = { state = state.copy(step = state.step + 1) },
                    onSkip = {},
                    onClose = {}
                )
            }
        }
        compose.onNodeWithText("What went well today?").assertExists()
        compose.onNodeWithText("Next").performClick()
        // step 1 collapses to its answer; step 2 opens with its chips
        compose.onNodeWithText("Cleared 2 small tasks").assertExists()
        compose.onNodeWithText("Started late").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag(
            "ReflectionField"
        ).assertTextEquals("Started late", includeEditableText = true)
        assertEquals(listOf("started_late"), chipCalls)
        assertEquals("Started late", state.answers[1])
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Done").assertExists()
    }

    @Test fun skipTonightFires() {
        var skipped = false
        compose.setContent {
            IteraTheme {
                ReflectionScreen(ReflectionUiState(loading = false), { _, _ ->
                }, { _, _, _ -> }, {}, {
                    skipped =
                        true
                }, {})
            }
        }
        compose.onNodeWithText("Skip tonight").performClick()
        assertTrue(skipped)
    }

    @Test fun nightSurfaceIsDarkUnderLightTheme() {
        var dark = false
        compose.setContent {
            IteraTheme(dark = false) {
                NightSurface {
                    dark = Itera.colors.isDark
                    ReflectionScreen(ReflectionUiState(loading = false), { _, _ ->
                    }, { _, _, _ -> }, {}, {}, {})
                }
            }
        }
        compose.waitForIdle()
        assertTrue(dark)
    }

    @Test fun weeklyVariantRendersItsHeader() {
        compose.setContent {
            IteraTheme {
                ReflectionScreen(
                    ReflectionUiState(
                        loading = false,
                        lookBack = WeeklyLookBack(6, 5, 125, listOf("Start at 8"))
                    ),
                    { _, _ -> },
                    { _, _, _ -> },
                    {},
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithTag("WeeklyLookBack").assertExists()
        compose.onNodeWithText("6 days trained").assertExists()
        compose.onNodeWithText("125 minutes of focus").assertExists()
        compose.onNodeWithText("“Start at 8”").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
class ReflectionIntegrationTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun questionThreeBecomesTomorrowsCarryOverVerbatim() {
        val h = EngineHarness()
        val day = h.eveningOfDayOne(completeExercise = true, completeFocus = false)
        val vm = h.reflectionViewModel(day.reflection.id)
        vm.state.await { !it.loading }
        vm.next()
        vm.next()
        val change = "  Leave the phone in another room - before 9 "
        vm.setAnswer(2, change)
        vm.next()
        vm.effects.awaitFirst()

        h.nextMorning()
        val tomorrow = runBlocking { h.ensureToday() }
        assertEquals(2, tomorrow.programDay)
        assertEquals(change, tomorrow.carryOverIntent)
        val today = mapToUiState(
            TodayInput(tomorrow, emptyMap(), 14, h.prefs.state.value, LocalTime.of(8, 0))
        )
        assertEquals(change, today.carryOver)
        assertNotNull(today.hero)
        assertFalse(today.error)
        main.clearViewModels()
        h.close()
    }
}
