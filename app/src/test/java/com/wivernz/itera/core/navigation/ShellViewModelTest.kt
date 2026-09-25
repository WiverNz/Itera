package com.wivernz.itera.core.navigation

import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        Dispatchers.resetMain()
    }

    private class Preferences(override val preferences: Flow<UserPreferences>) :
        PreferencesRepository {
        override suspend fun update(transform: (UserPreferences) -> UserPreferences) = Unit
        override suspend fun clear() = Unit
    }

    @Test fun startsLoadingThenFollowsPreferencesLive() = runTest(dispatcher) {
        val preferences = MutableStateFlow(UserPreferences())
        val vm = ShellViewModel(Preferences(preferences), SavedStateHandle())
        assertTrue(vm.state.value.loading)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertFalse(vm.state.value.preferences!!.onboardingCompleted)
        preferences.value =
            UserPreferences(onboardingCompleted = true, theme = ThemePreference.DARK)
        runCurrent()
        assertTrue(vm.state.value.preferences!!.onboardingCompleted)
        assertEquals(ThemePreference.DARK, vm.state.value.preferences!!.theme)
    }

    @Test fun preferenceFailureReleasesSplash() = runTest(dispatcher) {
        val vm =
            ShellViewModel(Preferences(flow { throw IllegalStateException() }), SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
    }

    @Test fun pendingLinkSurvivesRecreationAndOnlyMatchingConsumptionClearsIt() {
        val saved = SavedStateHandle()
        val preferences = Preferences(MutableStateFlow(UserPreferences()))
        val first = ShellViewModel(preferences, saved)
        val link = RouteCodec.encode(Review(42))
        first.acceptDeepLink(link)
        first.acceptDeepLink("invalid")
        val recreated = ShellViewModel(preferences, saved)
        assertEquals(link, recreated.pendingDeepLink.value)
        recreated.consumeDeepLink(RouteCodec.encode(Today))
        assertEquals(link, recreated.pendingDeepLink.value)
        recreated.consumeDeepLink(link)
        assertNull(ShellViewModel(preferences, saved).pendingDeepLink.value)
    }
}
