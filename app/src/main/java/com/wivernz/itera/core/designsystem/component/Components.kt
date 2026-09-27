// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraMotion
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill

// ---------------------------------------------------------------- layout

/** Standard scrolling screen body: 20dp gutters, vertical rhythm, optional sticky bottom action. */
@Composable
fun ScreenColumn(
    gap: Dp = 20.dp,
    scroll: Boolean = true,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxSize().background(Itera.colors.bg).screenInsets()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content
        )
        if (bottom != null) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = bottom
            )
        }
    }
}

/** Status bar, cutout, navigation bar and keyboard insets for a full-screen page. */
@Composable
fun Modifier.screenInsets(): Modifier = this.windowInsetsPadding(WindowInsets.safeDrawing)

@Composable
fun IteraCard(
    color: Color = Itera.colors.surface,
    radius: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(20.dp),
    gap: Dp = 14.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(color).padding(padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Itera.colors.line))
}

// ---------------------------------------------------------------- text bits

@Composable
fun Eyebrow(text: String, color: Color = Itera.colors.ink2, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = Itera.type.eyebrow, color = color)
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(
            CircleShape
        ).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = Itera.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------- buttons

enum class ButtonKind { Primary, Secondary, Ghost }

@Composable
fun IteraButton(
    text: String,
    onClick: () -> Unit,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> c.ink to c.onInk
        ButtonKind.Secondary -> c.surface2 to c.ink
        ButtonKind.Ghost -> Color.Transparent to c.ink2
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon !=
            null
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Text(text, style = Itera.type.label, color = fg, textAlign = TextAlign.Center)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Itera.colors.surface2)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Itera.colors.ink,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Top row of a flow screen: close/back on the left, small title, optional trailing content. */
@Composable
fun TopBar(
    title: String,
    onNavigate: () -> Unit,
    icon: ImageVector = IteraIcons.Close,
    trailing: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val label = stringResource(
        if (icon ==
            IteraIcons.Back
        ) {
            R.string.action_back
        } else {
            R.string.action_close
        }
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(icon, label, onNavigate)
        Text(
            title,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Itera.colors.ink2,
            textAlign = TextAlign.Center
        )
        Box(Modifier.widthIn(min = 44.dp), contentAlignment = Alignment.CenterEnd) { trailing() }
    }
}

// ---------------------------------------------------------------- technique token

@Composable
fun TechniqueToken(
    skill: Skill,
    icon: ImageVector,
    size: Dp = 48.dp,
    radius: Dp = size * 0.32f,
    modifier: Modifier = Modifier
) {
    val sc = skill.colors(Itera.colors.isDark)
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius)).background(sc.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = sc.content, modifier = Modifier.size(size / 2))
    }
}

// ---------------------------------------------------------------- chips & segmented

@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    singleSelect: Boolean = false,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.5.dp, if (selected) c.ink else c.line), CircleShape)
            .background(if (selected) c.ink else Color.Transparent)
            .then(
                if (singleSelect) {
                    Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
                } else {
                    Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = {
                        onClick()
                    })
                }
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) c.onInk else c.ink
        )
    }
}

@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val container = modifier.fillMaxWidth().clip(
        RoundedCornerShape(16.dp)
    ).background(c.surface2).padding(4.dp)
    if (LocalDensity.current.fontScale > 1.6f) {
        Column(container, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (value, label) ->
                Segment(label, value == selected, { onSelect(value) }, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(container, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (value, label) ->
                Segment(label, value == selected, { onSelect(value) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.surface else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) c.ink else c.ink2,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------- steps of the day

enum class StepState(val label: Int) {
    Done(
        R.string.step_done
    ),
    Now(R.string.step_now),
    Next(R.string.step_next)
}

@Composable
fun StepRow(
    state: StepState,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val stateLabel = stringResource(state.label)
    Row(
        modifier.semantics(mergeDescendants = true) { stateDescription = stateLabel }
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick !=
                    null
                ) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StepDot(state, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = Itera.type.body.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (state ==
                        StepState.Done
                    ) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    }
                ),
                color = if (state == StepState.Done) c.ink2 else c.ink
            )
            Text(subtitle, style = Itera.type.bodySmall, color = c.ink2)
        }
        if (onClick !=
            null
        ) {
            Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun StepDot(state: StepState, accent: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val c = Itera.colors
    val reduced = LocalReduceMotion.current
    when (state) {
        StepState.Done -> Box(
            modifier.size(size).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center
        ) {
            if (reduced) {
                Icon(
                    IteraIcons.Check,
                    null,
                    tint = c.surface,
                    modifier = Modifier.size(size * 0.57f)
                )
            } else {
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                AnimatedVisibility(shown, enter = scaleIn(IteraMotion.pop(false)) + fadeIn()) {
                    Icon(
                        IteraIcons.Check,
                        null,
                        tint = c.surface,
                        modifier = Modifier.size(size * 0.57f)
                    )
                }
            }
        }
        StepState.Now -> {
            val pulse = if (reduced) {
                1f
            } else {
                val transition = rememberInfiniteTransition(label = "pulse")
                val alpha by transition.animateFloat(
                    0.35f,
                    1f,
                    infiniteRepeatable(tween(IteraMotion.PULSE_MILLIS), RepeatMode.Reverse),
                    label = "pulseAlpha"
                )
                alpha
            }
            Box(
                modifier.size(size).border(2.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(10.dp).graphicsLayer {
                        alpha = pulse
                    }.clip(CircleShape).background(accent)
                )
            }
        }
        StepState.Next -> Box(modifier.size(size).border(2.dp, c.line, CircleShape))
    }
}

// ---------------------------------------------------------------- mastery ladder

@Composable
fun MasteryLadder(
    level: MasteryLevel,
    color: Color,
    animate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val reached = level.ordinal
    val description = stringResource(R.string.mastery_a11y, stringResource(level.title), reached)
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription =
                description
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MasteryLevel.entries.drop(1).forEachIndexed { i, m ->
            val target = if (i < reached) 1f else 0f
            val reduced = LocalReduceMotion.current
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(animate) { started = true }
            val fill by animateFloatAsState(
                if (!animate || started || reduced) target else 0f,
                animationSpec = IteraMotion.mastery(i, reduced || !animate),
                label = "ladder$i"
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.surface2)
                        .drawBehind {
                            drawRect(color, size = size.copy(width = size.width * fill))
                        }
                )
                Text(
                    stringResource(m.title),
                    style = Itera.type.caption.copy(
                        fontWeight = if (i <
                            reached
                        ) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    ),
                    color = if (i < reached) c.ink else c.ink2
                )
            }
        }
    }
}

/** Thin animated progress bar. */
@Composable
fun ProgressBar(fraction: Float, color: Color, height: Dp = 6.dp, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        animationSpec = IteraMotion.progress(LocalReduceMotion.current),
        label = "bar"
    )
    Box(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(Itera.colors.surface2)
            .drawBehind { drawRect(color, size = size.copy(width = size.width * f)) }
    )
}

// ---------------------------------------------------------------- text field

@Composable
fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 2,
    textStyle: TextStyle = Itera.type.userText,
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    val c = Itera.colors
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        textStyle = textStyle.copy(color = c.ink, fontFamily = Itera.type.userText.fontFamily),
        keyboardOptions = noteKeyboard,
        keyboardActions = noteActions(onDone),
        minLines = minLines,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.ink),
        decorationBox = { inner ->
            NoteDecoration(value.isEmpty(), placeholder, textStyle, bordered, null, inner)
        }
    )
}

/**
 * The same field over a [TextFieldValue], so a caller can read the caret and selection, with an optional
 * [trailing] action (the voice dictation mic) in the field's end padding.
 */
@Composable
fun NoteField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    minLines: Int = 2,
    textStyle: TextStyle = Itera.type.userText,
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = Itera.colors
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        textStyle = textStyle.copy(color = c.ink, fontFamily = Itera.type.userText.fontFamily),
        keyboardOptions = noteKeyboard,
        keyboardActions = noteActions(onDone),
        minLines = minLines,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.ink),
        decorationBox = { inner ->
            NoteDecoration(value.text.isEmpty(), placeholder, textStyle, bordered, trailing, inner)
        }
    )
}

private val noteKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    imeAction = ImeAction.Done
)

private fun noteActions(onDone: (() -> Unit)?) = if (onDone != null) {
    androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() })
} else {
    androidx.compose.foundation.text.KeyboardActions.Default
}

@Composable
private fun NoteDecoration(
    empty: Boolean,
    placeholder: String,
    textStyle: TextStyle,
    bordered: Boolean,
    trailing: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit
) {
    val c = Itera.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .then(
                if (bordered) {
                    Modifier.border(
                        2.dp,
                        c.ink,
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(start = 16.dp, end = if (trailing == null) 16.dp else 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (empty) Text(placeholder, style = textStyle, color = c.ink2)
            inner()
        }
        if (trailing != null) Box(Modifier.padding(vertical = 2.dp)) { trailing() }
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text,
            style = Itera.type.label,
            color = Itera.colors.ink,
            modifier = Modifier.weight(1f)
        )
        if (trailing !=
            null
        ) {
            Text(
                trailing,
                style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Itera.colors.ink2
            )
        }
    }
}

@Composable
fun RowScope.Grow() = androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
