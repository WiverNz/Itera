package com.wivernz.itera.feature.you

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.wivernz.itera.analytics.SettingKey
import com.wivernz.itera.analytics.SettingValue
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.progress.ProgressTestBase
import com.wivernz.itera.feature.subscribe
import java.util.Optional
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

open class YouTestBase : ProgressTestBase() {
    protected fun you() = main.track(
        YouViewModel(
            h.prefs,
            h.topics,
            h.reminders,
            h.resetProgram,
            h.eraseAll,
            Optional.empty(),
            h.analytics,
            models
        )
    )

    protected val models = FakeVoiceModelStore()
}

@RunWith(RobolectricTestRunner::class)
class YouViewModelTest : YouTestBase() {
    @Test fun systemRecognitionConsentCanBeGivenAndRevoked() = runBlocking {
        val vm = you()
        val stop = vm.state.subscribe()
        try {
            vm.state.await { !it.loading }
            assertFalse(vm.state.value.preferences.systemRecognitionAllowed)
            vm.onEvent(YouUiEvent.SystemRecognition(true))
            vm.state.await { it.preferences.systemRecognitionAllowed }
            vm.onEvent(YouUiEvent.SystemRecognition(false))
            vm.state.await { !it.preferences.systemRecognitionAllowed }
            assertFalse(h.prefs.preferences.awaitFirst().systemRecognitionAllowed)
        } finally {
            stop()
        }
    }

    @Test fun settingsPersistWithoutRegeneratingTodayAndRescheduleOnlyReminders() = runBlocking {
        val vm = you()
        val stop = vm.state.subscribe()
        try {
            vm.state.await { !it.loading }
            val today = h.ensureToday()
            vm.onEvent(YouUiEvent.Permission(true))
            vm.onEvent(YouUiEvent.Name("Лена"))
            vm.state.await { it.preferences.displayName == "Лена" }
            val changes = listOf(
                YouUiEvent.Change(SettingKey.PACE, SettingValue.Pace(ProgramPace.INTENSE)),
                YouUiEvent.Change(SettingKey.THEME, SettingValue.Theme(ThemePreference.DARK)),
                YouUiEvent.Change(SettingKey.TIME_BUDGET, SettingValue.Budget(TimeBudget.LONG)),
                YouUiEvent.Change(SettingKey.MORNING_TIME, SettingValue.Hour(7), 17),
                YouUiEvent.Change(SettingKey.EVENING_TIME, SettingValue.Hour(22), 43)
            )
            changes.forEach(vm::onEvent)
            val state = vm.state.await { it.preferences.eveningTime.minute == 43 }.preferences
            assertEquals(ProgramPace.INTENSE, state.pace)
            assertEquals(ThemePreference.DARK, state.theme)
            assertEquals(TimeBudget.LONG, state.timeBudget)
            assertEquals(17, state.morningTime.minute)
            assertEquals(today, h.plans.day(today.id))
            listOf(
                SettingKey.NOTIFY_MORNING,
                SettingKey.NOTIFY_FOCUS,
                SettingKey.NOTIFY_REVIEWS,
                SettingKey.NOTIFY_EVENING
            ).forEach {
                vm.onEvent(YouUiEvent.Change(it, SettingValue.Toggle(false)))
            }
            val off = vm.state.await { !it.preferences.notifyEvening }.preferences
            assertFalse(
                off.notifyMorning || off.notifyFocus || off.notifyReviews || off.notifyEvening
            )
            assertTrue(h.reminders.calls.count { it == "rescheduleAll" } >= 6)
            vm.onEvent(YouUiEvent.Permission(false))
            assertFalse(vm.state.await { !it.permission }.permission)
            vm.onEvent(YouUiEvent.Permission(true))
            assertTrue(vm.state.await { it.permission }.permission)
        } finally {
            stop()
        }
    }

    @Test fun focusMaxTwoAndTopicLifecycle() = runBlocking {
        val vm = you()
        val stop = vm.state.subscribe()
        try {
            vm.state.await { !it.loading }
            Skill.entries.forEach {
                vm.onEvent(YouUiEvent.Change(SettingKey.FOCUS_AREAS, SettingValue.Area(it)))
            }
            assertEquals(
                2,
                vm.state.await {
                    it.preferences.focusAreas.size == 2
                }.preferences.focusAreas.size
            )
            vm.onEvent(YouUiEvent.Topic(null, "My topic"))
            val topic = vm.state.await { it.topics.size == 1 }.topics.single()
            vm.onEvent(YouUiEvent.Topic(topic.id, "Моя тема"))
            assertEquals(
                "Моя тема",
                vm.state.await {
                    it.topics.single().title == "Моя тема"
                }.topics.single().title
            )
            vm.onEvent(YouUiEvent.Archive(topic.id))
            assertTrue(vm.state.await { it.topics.isEmpty() }.topics.isEmpty())
        } finally {
            stop()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class ResetIntegrationTest : YouTestBase() {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsEntryPointsDelegateToBothRealResetTiers() = runBlocking {
        h.trainFullDay()
        h.topics.add("Keep this")
        h.prefs.update { it.copy(displayName = "Name", theme = ThemePreference.DARK) }
        val vm = you()
        compose.setContent {
            IteraTheme { YouScreen(YouUiState(loading = false), vm::onEvent, true) }
        }
        compose.onNodeWithText("Reset program").performScrollTo().performClick()
        compose.onAllNodesWithText("Reset program").onLast().performClick()
        assertEquals(YouEffect.ResetDone(false), vm.effects.awaitFirst())
        assertEquals(1, h.prefs.state.value.currentProgramDay)
        assertEquals("Name", h.prefs.state.value.displayName)
        assertEquals(ThemePreference.DARK, h.prefs.state.value.theme)
        assertNotNull(h.topics.nextTopicForReview())
        assertNull(h.plans.dayByDate(h.today))
        compose.onNodeWithText("Erase everything").performScrollTo().performClick()
        compose.onAllNodesWithText("Erase everything").onLast().performClick()
        assertEquals(YouEffect.ResetDone(true), vm.effects.awaitFirst())
        assertEquals(UserPreferences(), h.prefs.state.value)
        assertNull(h.topics.nextTopicForReview())
    }
}
