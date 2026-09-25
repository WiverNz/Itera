@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.feature.reduceMotion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Progress represents real practice only: no streak, points or percentage-complete language on Today. */
@RunWith(RobolectricTestRunner::class)
class ProgressHonestyTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    private val banned = Regex(
        "streak|in a row|don.t break|\\bXP\\b|\\bpoints?\\b|%\\s*complete|серия|подряд|очк|" +
            "Serie|in Folge|Punkte|racha|seguid|puntos",
        RegexOption.IGNORE_CASE
    )

    @Test fun dailyFlowStringsCarryNoStreakLanguageInAnyLocale() {
        val offenders = listOf("values", "values-ru", "values-de", "values-es").flatMap { dir ->
            File("src/main/res/$dir/daily_flow.xml").readLines()
                .filter { banned.containsMatchIn(it) }
                .map { "$dir: ${it.trim()}" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test fun todayStatesRenderNoStreakOrPercentage() {
        val states = listOf(
            TodayFixtures.day1,
            TodayFixtures.midday,
            TodayFixtures.day2(),
            TodayFixtures.day9,
            TodayFixtures.done,
            TodayFixtures.error
        )
        var index = 0
        var current by mutableStateOf(states[0])
        compose.setContent { IteraTheme { TodayScreen(current, {}, {}) } }
        val offenders = mutableListOf<String>()
        while (index < states.size) {
            current = states[index++]
            compose.waitForIdle()
            texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
                .filter { banned.containsMatchIn(it) || it.contains('%') }
                .forEach { offenders += it }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    private fun texts(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            node.children.flatMap(::texts)
}
