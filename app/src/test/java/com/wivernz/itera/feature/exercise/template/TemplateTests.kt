package com.wivernz.itera.feature.exercise.template

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CompletionRule
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.reduceMotion
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val checklist = ExerciseBlock.Checklist(
    "tasks",
    "Find two quick tasks",
    2,
    5,
    true,
    "Add another"
)
private val pick = ExerciseBlock.PickOne("steps", "Which part moves it most?", 3, emptyList())
private val text = ExerciseBlock.TextInput("item", "What to remember?", "One fact", 2, 6)
private val lists = ExerciseBlock.TwoLists("lists", "Top 5", "Avoid", 2, 1)
private val chips = ExerciseBlock.ChipSelect("chips", "Pick", listOf("a", "b"), true, 1)
private fun items(vararg done: Boolean) = BlockValue.Items(
    done.mapIndexed { i, d -> ChecklistItem("$i", "task $i", d, 10) }
)

class CompletionRuleTest {
    @Test fun alwaysIsReady() {
        assertEquals(
            CompletionGate.Ready,
            CompletionRules.evaluate(CompletionRule.Always, emptyList(), emptyMap())
        )
    }

    @Test fun requireBlocksEmptyAndFilled() {
        val rule = CompletionRule.RequireBlocks(listOf("item"))
        assertEquals(
            CompletionGate.FillBlocks,
            CompletionRules.evaluate(rule, listOf(text), mapOf("item" to BlockValue.Text("  ")))
        )
        assertEquals(
            CompletionGate.Ready,
            CompletionRules.evaluate(rule, listOf(text), mapOf("item" to BlockValue.Text("fact")))
        )
    }

    @Test fun requireBlocksPickOneNeedsAChoice() {
        val rule = CompletionRule.RequireBlocks(listOf("steps"))
        val entered = BlockValue.Choice(listOf("a", "b", ""), -1)
        assertEquals(
            CompletionGate.PickOne,
            CompletionRules.evaluate(rule, listOf(pick), mapOf("steps" to entered))
        )
        assertEquals(
            CompletionGate.Ready,
            CompletionRules.evaluate(
                rule,
                listOf(pick),
                mapOf("steps" to entered.copy(chosenIndex = 1))
            )
        )
    }

    @Test fun requireCheckedAtNMinusOneAndN() {
        val rule = CompletionRule.RequireChecked("tasks", 2)
        assertEquals(
            CompletionGate.TickMore(1),
            CompletionRules.evaluate(rule, listOf(checklist), mapOf("tasks" to items(true, false)))
        )
        assertEquals(
            CompletionGate.Ready,
            CompletionRules.evaluate(rule, listOf(checklist), mapOf("tasks" to items(true, true)))
        )
    }
}

class BlockValueSerializationTest {
    @Test fun everyVariantRoundTripsThroughTheDraftCodec() {
        val codec = ResultPayloadCodec(TestLogger())
        val draft = ActivityResult.Template(
            mapOf(
                "text" to BlockValue.Text("hello"),
                "items" to BlockValue.Items(
                    listOf(ChecklistItem("1", "a", true, 95), ChecklistItem("2", "b", false, null))
                ),
                "choice" to BlockValue.Choice(listOf("x", "y"), 1),
                "lists" to BlockValue.Lists(listOf("p"), listOf("s")),
                "chips" to BlockValue.Chips(listOf("a"), "custom"),
                "chipsNoCustom" to BlockValue.Chips(emptyList(), null)
            )
        )
        assertEquals(draft, codec.decode(codec.encode(draft), 1, "TEMPLATE"))
    }
}

@RunWith(RobolectricTestRunner::class)
class TemplateBodyTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setup() = reduceMotion()

    /** A tiny host that applies the actions the way the runner does, so each block is exercised end to end. */
    private fun host(
        blocks: List<ExerciseBlock>,
        rule: CompletionRule,
        start: Map<String, BlockValue>
    ): () -> CompletionGate {
        var values by mutableStateOf(start)
        var pending by mutableStateOf(emptyMap<String, String>())
        val actions = object : TemplateActions {
            override fun onText(key: String, text: String) {
                values = values + (key to BlockValue.Text(text))
            }
            override fun onPendingItem(key: String, text: String) {
                pending = pending + (key to text)
            }
            override fun onAddItem(block: ExerciseBlock.Checklist) {
                val old = (values[block.key] as? BlockValue.Items)?.items.orEmpty()
                val label = pending[block.key].orEmpty()
                values =
                    values +
                    (
                        block.key to
                            BlockValue.Items(old + ChecklistItem("${old.size}", label, false, 0))
                        )
                pending = pending - block.key
            }
            override fun onToggleItem(key: String, itemId: String) {
                val old = (values[key] as BlockValue.Items).items
                values =
                    values +
                    (
                        key to
                            BlockValue.Items(
                                old.map {
                                    if (it.id ==
                                        itemId
                                    ) {
                                        it.copy(done = !it.done)
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
            }
            override fun onChoiceOption(key: String, index: Int, text: String) {
                val c = values[key] as BlockValue.Choice
                values =
                    values +
                    (
                        key to
                            c.copy(options = c.options.toMutableList().also { it[index] = text })
                        )
            }
            override fun onChoose(key: String, index: Int) {
                values =
                    values + (key to (values[key] as BlockValue.Choice).copy(chosenIndex = index))
            }
            override fun onListItem(key: String, primary: Boolean, index: Int, text: String) {
                val l = values[key] as BlockValue.Lists
                values = values + (
                    key to if (primary) {
                        l.copy(primary = l.primary.toMutableList().also { it[index] = text })
                    } else {
                        l.copy(secondary = l.secondary.toMutableList().also { it[index] = text })
                    }
                    )
            }
            override fun onToggleChip(key: String, option: String, max: Int) {
                val c = values[key] as BlockValue.Chips
                values =
                    values +
                    (
                        key to
                            c.copy(
                                selected = if (option in
                                    c.selected
                                ) {
                                    c.selected - option
                                } else {
                                    c.selected + option
                                }
                            )
                        )
            }
            override fun onChipCustom(key: String, text: String) {
                values = values + (key to (values[key] as BlockValue.Chips).copy(custom = text))
            }
        }
        compose.setContent {
            IteraTheme { TemplateBody(blocks, values, pending, emptyMap(), Skill.HABITS, actions) }
        }
        return { CompletionRules.evaluate(rule, blocks, values) }
    }

    @Test fun instructionRendersAsTheNextBlocksSubtitle() {
        val how = ExerciseBlock.Instruction("how", "Write them down, do them.", true)
        host(
            listOf(how, checklist),
            CompletionRule.Always,
            mapOf("tasks" to BlockValue.Items(emptyList()))
        )
        compose.onNodeWithText("Find two quick tasks").assertExists()
        compose.onNodeWithText("Write them down, do them.").assertExists()
    }

    @Test fun textInputAcceptsInputAndFillsTheRule() {
        val gate =
            host(
                listOf(text),
                CompletionRule.RequireBlocks(listOf("item")),
                mapOf("item" to BlockValue.Text(""))
            )
        assertEquals(CompletionGate.FillBlocks, gate())
        compose.onNodeWithTag("Field_item").performTextInput("A fact")
        compose.runOnIdle { assertEquals(CompletionGate.Ready, gate()) }
    }

    @Test fun checklistAddsAndTicksItems() {
        val gate =
            host(
                listOf(checklist),
                CompletionRule.RequireChecked("tasks", 2),
                mapOf("tasks" to BlockValue.Items(emptyList()))
            )
        repeat(2) {
            compose.onNodeWithTag("AddItem_tasks").performTextInput("Task $it")
            compose.onNodeWithTag("AddItem_tasks").performImeAction()
        }
        compose.onNodeWithTag("Item_0").performClick()
        compose.runOnIdle { assertEquals(CompletionGate.TickMore(1), gate()) }
        compose.onNodeWithTag("Item_1").performClick().assertIsOn()
        compose.runOnIdle { assertEquals(CompletionGate.Ready, gate()) }
    }

    @Test fun stopwatchShowsElapsedTime() {
        compose.setContent {
            IteraTheme {
                TemplateBody(
                    listOf(checklist),
                    mapOf(
                        "tasks" to
                            BlockValue.Items(listOf(ChecklistItem("1", "Pay bill", false, 0)))
                    ),
                    emptyMap(),
                    mapOf("1" to 125),
                    Skill.HABITS,
                    NoActions
                )
            }
        }
        compose.onNodeWithText("2:05").assertExists()
    }

    @Test fun pickOneEntersThenChooses() {
        val gate =
            host(
                listOf(pick),
                CompletionRule.RequireBlocks(listOf("steps")),
                mapOf("steps" to BlockValue.Choice(listOf("", "", ""), -1))
            )
        compose.onNodeWithTag("Option_steps_0").performTextInput("Define the bets")
        compose.onNodeWithTag("Option_steps_1").performTextInput("Collect requests")
        compose.runOnIdle { assertEquals(CompletionGate.PickOne, gate()) }
        compose.onNodeWithTag("Choice_1").performClick()
        compose.runOnIdle { assertEquals(CompletionGate.Ready, gate()) }
    }

    @Test fun twoListsFillBothLists() {
        val gate =
            host(
                listOf(lists),
                CompletionRule.RequireBlocks(listOf("lists")),
                mapOf("lists" to BlockValue.Lists(listOf("", ""), listOf("")))
            )
        compose.onNodeWithTag("Primary_lists_0").performTextInput("Ship beta")
        compose.runOnIdle { assertEquals(CompletionGate.FillBlocks, gate()) }
        compose.onNodeWithTag("Secondary_lists_0").performTextInput("Redesign")
        compose.runOnIdle { assertEquals(CompletionGate.Ready, gate()) }
    }

    @Test fun chipSelectTogglesAndAcceptsCustom() {
        val gate =
            host(
                listOf(chips),
                CompletionRule.RequireBlocks(listOf("chips")),
                mapOf("chips" to BlockValue.Chips(emptyList(), null))
            )
        compose.onNodeWithText("a").performClick()
        compose.runOnIdle { assertEquals(CompletionGate.Ready, gate()) }
        compose.onNodeWithTag("Custom_chips").performTextInput("mine")
        compose.onNodeWithTag("Custom_chips").assertTextContains("mine")
    }
}

private object NoActions : TemplateActions {
    override fun onText(key: String, text: String) = Unit
    override fun onPendingItem(key: String, text: String) = Unit
    override fun onAddItem(block: ExerciseBlock.Checklist) = Unit
    override fun onToggleItem(key: String, itemId: String) = Unit
    override fun onChoiceOption(key: String, index: Int, text: String) = Unit
    override fun onChoose(key: String, index: Int) = Unit
    override fun onListItem(key: String, primary: Boolean, index: Int, text: String) = Unit
    override fun onToggleChip(key: String, option: String, max: Int) = Unit
    override fun onChipCustom(key: String, text: String) = Unit
}
