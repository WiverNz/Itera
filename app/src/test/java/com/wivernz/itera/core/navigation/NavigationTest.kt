package com.wivernz.itera.core.navigation
import android.app.Application
import android.provider.Settings
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var actions: NavigationActions
    private lateinit var back: OnBackPressedDispatcher

    @Before fun reduceMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Application>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f
        )
    }

    @Composable private fun Fake(route: AppRoute, navigation: NavigationActions) {
        actions = navigation
        back = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        var count by rememberSaveable { mutableIntStateOf(0) }
        Column(Modifier.testTag(route::class.simpleName.orEmpty())) {
            Text(count.toString(), Modifier.testTag("count"))
            IteraButton("increment", { count++ })
        }
    }
    private fun start(completed: Boolean = true) {
        compose.setContent {
            IteraTheme { AppNavHost(completed, destination = { route, nav -> Fake(route, nav) }) }
        }
    }

    @Test fun firstLaunchStartsWelcome() {
        start(false)
        compose.onNodeWithTag("Welcome").assertExists()
        compose.onNodeWithTag("BottomBar").assertDoesNotExist()
    }

    @Test fun completedOnboardingStartsToday() {
        start()
        compose.onNodeWithTag("Today").assertExists()
    }

    @Test fun tabsRestoreStateRetapIsNoOpAndBackIsBounded() {
        start()
        compose.onNodeWithTag("TabTrain").performClick()
        compose.onNodeWithText("increment").performClick()
        compose.onNodeWithTag("TabTrain").performClick()
        compose.onNodeWithTag("count").assertTextEquals("1")
        repeat(3) {
            compose.onNodeWithTag("TabProgress").performClick()
            compose.onNodeWithTag("TabYou").performClick()
            compose.onNodeWithTag("TabTrain").performClick()
        }
        compose.onNodeWithTag("count").assertTextEquals("1")
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithTag("Today").assertExists()
    }

    @Test fun secondaryAndFlowRoutesHideBarAndReturnToCaller() {
        start()
        compose.onNodeWithTag("TabTrain").performClick()
        listOf(
            Library, History, TechniqueDetail("feynman"), ExerciseIntro(1, "feynman"), TwoMinute(1),
            FocusSession(
                1,
                25
            ),
            Eisenhower(1), Feynman(1), FeynmanFeedback(1), Review(1), Premortem(1),
            HabitStack(1), Combination(1), Reflection(1)
        ).forEach { route ->
            compose.runOnIdle { actions.navigate(route) }
            compose.onNodeWithTag(route::class.simpleName!!).assertExists()
            compose.onNodeWithTag("BottomBar").assertDoesNotExist()
            compose.runOnIdle { back.onBackPressed() }
            compose.onNodeWithTag("Train").assertExists()
        }
    }

    @Test fun finishingOnboardingRemovesAllItsSteps() {
        start(false)
        compose.runOnIdle { actions.navigate(Goals) }
        compose.runOnIdle { actions.navigate(Rhythm) }
        compose.runOnIdle { actions.navigate(FirstWeek) }
        compose.runOnIdle { actions.finishOnboarding() }
        compose.onNodeWithTag("Today").assertExists()
        compose.onNodeWithTag("TabTrain").performClick()
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithTag("Today").assertExists()
        compose.onNodeWithTag("Welcome").assertDoesNotExist()
    }

    @Test fun resultBackClearsExerciseFlow() {
        start()
        compose.runOnIdle { actions.navigate(ExerciseIntro(1, "two_minute_rule")) }
        compose.runOnIdle { actions.navigate(TwoMinute(1)) }
        compose.runOnIdle { actions.showResult(ExerciseResult(1, "two_minute_rule")) }
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithTag("Today").assertExists()
    }

    @Test fun deepLinkIsConsumedOnceAndSurvivesRestoration() {
        val pending = mutableStateOf<String?>(RouteCodec.encode(Review(7)))
        var consumed = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            IteraTheme {
                AppNavHost(true, pending.value, {
                    consumed++
                    pending.value =
                        null
                }, { route, nav -> Fake(route, nav) })
            }
        }
        compose.onNodeWithTag("Review").assertExists()
        compose.runOnIdle { assertEquals(1, consumed) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("Review").assertExists()
        compose.runOnIdle {
            assertEquals(1, consumed)
            back.onBackPressed()
        }
        compose.onNodeWithTag("Today").assertExists()
    }
}
