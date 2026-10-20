package com.wivernz.itera.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wivernz.itera.core.designsystem.component.LanguageSheet
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.feature.reduceMotion
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Matrix row: `LanguageSheet` lists, names and applies each language (selection itself in ComponentTest). */
@RunWith(RobolectricTestRunner::class)
class LanguageSheetTest {
    @get:Rule val compose = createComposeRule()

    private val names = listOf(
        "en" to "English",
        "ru" to "Русский",
        "de" to "Deutsch",
        "es" to "Español"
    )

    @Before fun setUp() = reduceMotion()

    @Test fun listsMatchDeviceThenEachLanguageByItsOwnNameInOrder() {
        compose.setContent { IteraTheme { LanguageSheet("", {}, {}) } }
        val options = compose.onAllNodes(
            SemanticsMatcher("radio option") {
                it.config.getOrNull(SemanticsProperties.Role) ==
                    Role.RadioButton
            },
            useUnmergedTree = false
        ).fetchSemanticsNodes()
        val labels = options.map { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
        }
        // the tag badge, the language's own name, then its name in the current UI language
        assertEquals(
            listOf(
                "Match device Currently English",
                "EN English English",
                "RU Русский Russian",
                "DE Deutsch German",
                "ES Español Spanish"
            ),
            labels
        )
    }

    @Test fun eachOptionAppliesItsTagAndShowsAsSelected() {
        var selected by mutableStateOf("")
        val picked = mutableListOf<String>()
        compose.setContent {
            IteraTheme {
                LanguageSheet(selected, {
                    picked += it
                    selected = it
                }, {})
            }
        }
        compose.onNode(
            hasText("Match device", substring = true),
            useUnmergedTree = false
        ).assertIsSelected()
        names.forEach { (tag, name) ->
            compose.onNodeWithText(name).performClick()
            compose.onNode(hasText(name)).assertIsSelected()
            names.filter { it.first != tag }.forEach { (_, other) ->
                compose.onNode(hasText(other)).assertIsNotSelected()
            }
        }
        compose.onAllNodesWithText("Match device", substring = true)[0].performClick()
        compose.runOnIdle { assertEquals(listOf("en", "ru", "de", "es", ""), picked) }
    }
}
