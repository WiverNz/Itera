package com.wivernz.itera

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.core.navigation.You
import com.wivernz.itera.di.StorageTestEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation

    @Test fun welcomeAndSettingsApplyImmediatelyAndSurviveRecreation(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = EntryPointAccessors.fromApplication(
            context,
            StorageTestEntryPoint::class.java
        ).preferences()
        val previous = prefs.preferences.first()
        val language = AppLanguage.current()
        try {
            prefs.update { it.copy(onboardingCompleted = false) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { AppLanguage.set("en") }
                waitText("EN")
                click("EN")
                waitText("Deutsch")
                click("Deutsch")
                instrumentation.waitForIdleSync()
                SystemClock.sleep(500)
                scenario.recreate()
                scenario.onActivity {
                    assertEquals("de", it.resources.configuration.locales[0].language)
                    assertEquals("de", AppLanguage.current())
                }
            }
            prefs.update { it.copy(onboardingCompleted = true) }
            val intent = Intent(context, MainActivity::class.java)
                .putExtra(RouteCodec.EXTRA, RouteCodec.encode(You))
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                var label = ""
                scenario.onActivity { label = it.getString(R.string.language_title) }
                click(label, scroll = true)
                waitText("Español")
                click("Español")
                instrumentation.waitForIdleSync()
                SystemClock.sleep(500)
                scenario.recreate()
                scenario.onActivity {
                    assertEquals("es", it.resources.configuration.locales[0].language)
                    assertEquals("es", AppLanguage.current())
                }
            }
        } finally {
            prefs.update { previous }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppLanguage.set(language)
            }
        }
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = listOf(node) +
            (0 until node.childCount).flatMap {
                node.getChild(it)?.let(::descendants).orEmpty()
            }
        return automation.rootInActiveWindow?.let(::descendants).orEmpty()
    }

    private fun waitText(text: String) {
        awaitNode(text, false)
    }

    private fun awaitNode(text: String, scroll: Boolean): AccessibilityNodeInfo {
        repeat(60) {
            val visible = nodes()
            visible.firstOrNull {
                it.text?.toString() == text && it.isVisibleToUser
            }?.let { return it }
            if (scroll) {
                visible.filter { it.isScrollable }.forEach {
                    it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
            }
            SystemClock.sleep(200)
        }
        throw AssertionError("Accessible text not found: $text")
    }

    private fun click(text: String, scroll: Boolean = false) {
        var node = awaitNode(text, scroll)
        while (!node.isClickable) {
            node =
                node.parent ?: throw AssertionError("No click target: $text")
        }
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }
}
