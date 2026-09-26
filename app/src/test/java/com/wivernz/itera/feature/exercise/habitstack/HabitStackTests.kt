@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.exercise.habitstack

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.habitStackViewModel
import com.wivernz.itera.feature.plant
import com.wivernz.itera.feature.reduceMotion
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HabitStackViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val h = EngineHarness()

    @After fun close() {
        main.clearViewModels()
        h.close()
    }

    @Test fun derivedNudgeTimes() {
        val morning = LocalTime.of(8, 30)
        assertEquals(LocalTime.of(7, 45), HabitStackRules.nudgeTimeFor("coffee", morning))
        assertEquals(LocalTime.of(12, 30), HabitStackRules.nudgeTimeFor("lunch", morning))
        assertEquals(morning, HabitStackRules.nudgeTimeFor("laptop", morning))
        assertEquals(morning, HabitStackRules.nudgeTimeFor(null, morning))
    }

    @Test fun slotsChipsAndCustomEntry() {
        val vm = h.habitStackViewModel(h.plant("habit_stacking", ExerciseType.HABIT_STACK))
        assertFalse(vm.state.await { !it.loading }.ready)
        vm.pick(StackSlot.ANCHOR, "coffee", "make coffee")
        assertEquals(LocalTime.of(7, 45), vm.state.value.nudgeTime)
        assertFalse(vm.state.value.ready)
        vm.edit(StackSlot.HABIT)
        vm.setCustom(StackSlot.HABIT, "stretch for a minute")
        assertTrue(vm.state.value.ready)
        assertEquals("stretch for a minute", vm.state.value.habit!!.text)
        vm.setCustom(StackSlot.ANCHOR, "feed the cat")
        assertEquals(h.prefs.state.value.morningTime, vm.state.value.nudgeTime)
        vm.setNudgeTime(LocalTime.of(6, 50))
        vm.pick(StackSlot.ANCHOR, "lunch", "sit down for lunch")
        assertEquals(LocalTime.of(6, 50), vm.state.value.nudgeTime) // an edited time is kept
    }

    @Test fun savePersistsSchedulesAndArchivesThePreviousStack() {
        fun save(anchor: String, nudge: Boolean) {
            val vm = h.habitStackViewModel(h.plant("habit_stacking", ExerciseType.HABIT_STACK))
            vm.state.await { !it.loading }
            vm.pick(StackSlot.ANCHOR, anchor, anchor)
            vm.pick(StackSlot.HABIT, "page", "read one page")
            vm.setNudge(nudge)
            vm.save()
            assertTrue(vm.effects.awaitFirst() is HabitStackEffect.ShowResult)
        }
        save("coffee", nudge = true)
        val first = runBlocking { h.db.habitStackDao().observeActive().first() }.single()
        assertTrue("habit:${first.id}" in h.reminders.calls)
        save("lunch", nudge = false)
        val active = runBlocking { h.db.habitStackDao().observeActive().first() }
        assertEquals(listOf("lunch"), active.map { it.anchor })
        assertFalse(active.single().nudgeEnabled)
        // the disabled nudge is still handed to the scheduler, which cancels it and the archived one
        assertTrue("habit:${active.single().id}" in h.reminders.calls)
        val result = runBlocking {
            h.plans.activity(active.single().activityId!!)
        }!!.result as ActivityResult.HabitStack
        assertEquals("read one page", result.habit)
    }
}

@RunWith(RobolectricTestRunner::class)
class HabitStackScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val actions = HabitStackActions({}, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {})

    @Test fun sentenceReadsWithBlanksAndThePrimaryIsGated() {
        compose.setContent {
            IteraTheme {
                HabitStackScreen(
                    HabitStackUiState(loading = false, name = "Habit stacking"),
                    actions
                )
            }
        }
        compose.onNodeWithTag("HabitSentence").assertTextContains("After I ___,", substring = true)
        compose.onNodeWithTag("HabitSave").assertIsNotEnabled()
        compose.onNodeWithText("make coffee").assertExists()
        compose.onNodeWithText("read one page").assertExists()
    }

    @Test fun filledSlotsAndTheNudgeTime() {
        var edited: StackSlot? = null
        val state = HabitStackUiState(
            loading = false,
            anchor = SlotValue("coffee", "make coffee"),
            habit = SlotValue(null, "stretch"),
            nudgeEnabled = true,
            nudgeTime = LocalTime.of(7, 45)
        )
        compose.setContent {
            IteraTheme {
                HabitStackScreen(
                    state,
                    HabitStackActions({
                        edited = it
                    }, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {})
                )
            }
        }
        compose.onNodeWithTag(
            "HabitSentence"
        ).assertTextContains("After I make coffee,", substring = true)
        compose.onNodeWithTag("HabitSave").assertIsEnabled()
        compose.onNodeWithTag("NudgeTime").assertExists()
        compose.onNodeWithText("Nudge me right after I make coffee").assertExists()
        compose.onNodeWithTag("Custom_ANCHOR").performClick()
        compose.runOnIdle { assertEquals(StackSlot.ANCHOR, edited) }
    }
}
