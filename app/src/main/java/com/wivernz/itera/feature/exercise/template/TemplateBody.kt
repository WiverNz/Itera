package com.wivernz.itera.feature.exercise.template

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.CheckCircle
import com.wivernz.itera.core.designsystem.component.ChoiceChip
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.feature.exercise.runner.CappedNoteField

/** Everything a template body can change. */
interface TemplateActions {
    fun onText(key: String, text: String)
    fun onPendingItem(key: String, text: String)
    fun onAddItem(block: ExerciseBlock.Checklist)
    fun onToggleItem(key: String, itemId: String)
    fun onChoiceOption(key: String, index: Int, text: String)
    fun onChoose(key: String, index: Int)
    fun onListItem(key: String, primary: Boolean, index: Int, text: String)
    fun onToggleChip(key: String, option: String, max: Int)
    fun onChipCustom(key: String, text: String)
}

/**
 * The data-driven template body (ADR-0007, docs/ux/02-screen-specs-exercise.md section 2). An emphasised instruction
 * followed by an input block reads as that block's subtitle, which is how the prototype's 2-minute screen is laid out:
 * the block label as the title, the instruction under it, then the rows.
 */
@Composable
fun TemplateBody(
    blocks: List<ExerciseBlock>,
    values: Map<String, BlockValue>,
    pending: Map<String, String>,
    elapsed: Map<String, Int>,
    skill: Skill,
    actions: TemplateActions,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            val next = blocks.getOrNull(i + 1)
            if (block is ExerciseBlock.Instruction && block.emphasis && next != null &&
                next !is ExerciseBlock.Instruction
            ) {
                Block(next, block.text, values, pending, elapsed, skill, actions)
                i += 2
            } else {
                Block(block, null, values, pending, elapsed, skill, actions)
                i += 1
            }
        }
    }
}

@Composable
private fun Block(
    block: ExerciseBlock,
    sub: String?,
    values: Map<String, BlockValue>,
    pending: Map<String, String>,
    elapsed: Map<String, Int>,
    skill: Skill,
    actions: TemplateActions
) {
    val c = Itera.colors
    Column(
        Modifier.testTag("Block_${block.key}"),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        if (block !is ExerciseBlock.Instruction) {
            Text(block.label(), style = Itera.type.title, color = c.ink)
        }
        if (sub != null) Text(sub, style = Itera.type.body, color = c.ink2)
        when (block) {
            is ExerciseBlock.Instruction -> Text(
                block.text,
                style = Itera.type.body,
                color = if (block.emphasis) c.ink else c.ink2
            )
            is ExerciseBlock.TextInput -> CappedNoteField(
                (values[block.key] as? BlockValue.Text)?.text.orEmpty(),
                { actions.onText(block.key, it) },
                block.placeholder,
                minLines = block.minLines,
                modifier = Modifier.testTag("Field_${block.key}")
            )
            is ExerciseBlock.Checklist -> Checklist(
                block,
                (values[block.key] as? BlockValue.Items)?.items.orEmpty(),
                pending[block.key].orEmpty(),
                elapsed,
                skill,
                actions
            )
            is ExerciseBlock.PickOne -> PickOneBody(
                block,
                values[block.key] as? BlockValue.Choice,
                { i, text -> actions.onChoiceOption(block.key, i, text) },
                { actions.onChoose(block.key, it) }
            )
            is ExerciseBlock.TwoLists ->
                TwoLists(block, values[block.key] as? BlockValue.Lists, actions)
            is ExerciseBlock.ChipSelect ->
                Chips(block, values[block.key] as? BlockValue.Chips, actions)
        }
    }
}

private fun ExerciseBlock.label(): String = when (this) {
    is ExerciseBlock.Instruction -> text
    is ExerciseBlock.TextInput -> label
    is ExerciseBlock.Checklist -> label
    is ExerciseBlock.PickOne -> label
    is ExerciseBlock.TwoLists -> primaryLabel
    is ExerciseBlock.ChipSelect -> label
}

/** The 2-minute rule's rows (prototype `TwoMinuteScreen`), each with its own stopwatch. */
@Composable
private fun Checklist(
    block: ExerciseBlock.Checklist,
    items: List<com.wivernz.itera.domain.model.ChecklistItem>,
    pending: String,
    elapsed: Map<String, Int>,
    skill: Skill,
    actions: TemplateActions
) {
    val c = Itera.colors
    val sc = skill.colors(c.isDark)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.surface)
                    .toggleable(value = item.done, role = Role.Checkbox) {
                        actions.onToggleItem(block.key, item.id)
                    }
                    .testTag("Item_${item.id}")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CheckCircle(item.done, sc.content)
                Text(
                    item.label,
                    style = Itera.type.body.copy(
                        fontWeight = FontWeight.Medium,
                        fontFamily = Itera.type.userText.fontFamily,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                    ),
                    color = if (item.done) c.ink2 else c.ink,
                    modifier = Modifier.weight(1f)
                )
                if (block.withStopwatch) {
                    val seconds = elapsed[item.id] ?: item.elapsedSeconds ?: 0
                    Text(
                        formatStopwatch(seconds),
                        style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = if (seconds > CompletionRules.STOPWATCH_THRESHOLD_SECONDS) {
                            c.accent
                        } else {
                            c.ink2
                        }
                    )
                }
            }
        }
        if (items.size < block.maxItems) {
            CappedNoteField(
                pending,
                { actions.onPendingItem(block.key, it) },
                block.addItemLabel,
                minLines = 1,
                modifier = Modifier.testTag("AddItem_${block.key}"),
                onDone = { actions.onAddItem(block) }
            )
        }
    }
}

fun formatStopwatch(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** 80/20: several entries, then one chosen (the combination step's radio group). Also the chain's step 2 body. */
@Composable
fun PickOneBody(
    block: ExerciseBlock.PickOne,
    value: BlockValue.Choice?,
    onOption: (Int, String) -> Unit,
    onChoose: (Int) -> Unit
) {
    val options = value?.options ?: List(block.itemCount) { "" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, text ->
            CappedNoteField(
                text,
                { onOption(i, it) },
                block.suggestions.getOrNull(i)
                    ?: stringResource(R.string.template_option_hint, i + 1),
                minLines = 1,
                modifier = Modifier.testTag("Option_${block.key}_$i")
            )
        }
        if (options.any(String::isNotBlank)) {
            Text(
                stringResource(R.string.template_pick_label),
                style = Itera.type.label,
                color = Itera.colors.ink,
                modifier = Modifier.padding(top = 6.dp)
            )
            RadioGroup(options, value?.chosenIndex ?: -1, onChoose)
        }
    }
}

/** The prototype's combination radio group: rows on `surface` inside a `surface2` container. */
@Composable
fun RadioGroup(options: List<String>, chosen: Int, onChoose: (Int) -> Unit) {
    val c = Itera.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface2).padding(6.dp)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { i, label ->
            if (label.isBlank()) return@forEachIndexed
            val on = i == chosen
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(2.dp, if (on) c.ink else Color.Transparent, RoundedCornerShape(14.dp))
                    .selectable(on, role = Role.RadioButton) { onChoose(i) }
                    .testTag("Choice_$i")
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .border(if (on) 6.dp else 2.dp, if (on) c.ink else c.ink3, CircleShape)
                )
                Text(
                    label,
                    style = Itera.type.bodySmall.copy(
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = Itera.type.userText.fontFamily
                    ),
                    color = c.ink
                )
            }
        }
    }
}

@Composable
private fun TwoLists(
    block: ExerciseBlock.TwoLists,
    value: BlockValue.Lists?,
    actions: TemplateActions
) {
    val primary = value?.primary ?: List(block.primaryCount) { "" }
    val secondary = value?.secondary ?: List(block.secondaryCount) { "" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        primary.forEachIndexed { i, text ->
            CappedNoteField(
                text,
                { actions.onListItem(block.key, true, i, it) },
                stringResource(R.string.template_item_hint, i + 1),
                minLines = 1,
                modifier = Modifier.testTag("Primary_${block.key}_$i")
            )
        }
        Text(
            block.secondaryLabel,
            style = Itera.type.title,
            color = Itera.colors.ink,
            modifier = Modifier.padding(top = 12.dp)
        )
        secondary.forEachIndexed { i, text ->
            CappedNoteField(
                text,
                { actions.onListItem(block.key, false, i, it) },
                stringResource(R.string.template_item_hint, i + 1),
                minLines = 1,
                modifier = Modifier.testTag("Secondary_${block.key}_$i")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(
    block: ExerciseBlock.ChipSelect,
    value: BlockValue.Chips?,
    actions: TemplateActions
) {
    val selected = value?.selected.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            block.options.forEach { option ->
                ChoiceChip(option, option in selected, onClick = {
                    actions.onToggleChip(block.key, option, block.maxSelections)
                })
            }
        }
        if (block.allowCustom) {
            CappedNoteField(
                value?.custom.orEmpty(),
                { actions.onChipCustom(block.key, it) },
                stringResource(R.string.template_custom_hint),
                minLines = 1,
                modifier = Modifier.testTag("Custom_${block.key}")
            )
        }
    }
}
