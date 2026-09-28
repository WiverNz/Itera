package com.wivernz.itera.feature.exercise.runner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.R
import com.wivernz.itera.core.common.ObserveEffects
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.Eyebrow
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.component.IteraCard
import com.wivernz.itera.core.designsystem.component.Pill
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TechniqueToken
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.icon.techniqueIcon
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.feature.exercise.template.TemplateActions
import com.wivernz.itera.feature.exercise.template.TemplateBody
import com.wivernz.itera.feature.voice.LocalVoiceToToday
import com.wivernz.itera.feature.voice.ProvideVoiceCommands
import com.wivernz.itera.feature.voice.VoiceCommandAction
import com.wivernz.itera.feature.voice.rememberExerciseVoice

/** Navigation out of the runner; the host maps these to routes. */
class ExerciseRunnerNavigation(
    val openBody: (ExerciseRunnerEffect.OpenBody) -> Unit,
    val showResult: (ExerciseRunnerEffect.ShowResult) -> Unit,
    val close: () -> Unit
)

@Composable
fun ExerciseIntroRoute(vm: ExerciseRunnerViewModel, navigation: ExerciseRunnerNavigation) {
    val state by vm.state.collectAsStateWithLifecycle()
    val locale = currentLocale()
    LaunchedEffect(locale, state.loading) { vm.refreshLanguage() }
    ObserveRunnerEffects(vm, navigation)
    ExerciseIntroScreen(
        state,
        onClose = navigation.close,
        onPrimary = vm::onPrimary,
        onSnooze = vm::onSnooze
    )
}

@Composable
fun ExerciseRunRoute(vm: ExerciseRunnerViewModel, navigation: ExerciseRunnerNavigation) {
    val state by vm.state.collectAsStateWithLifecycle()
    val locale = currentLocale()
    LaunchedEffect(locale, state.loading) { vm.refreshLanguage() }
    val voice = rememberExerciseVoice(vm, vm::leave)
    val toToday = LocalVoiceToToday.current
    LaunchedEffect(state.loading) { if (!state.loading) vm.enterRun() }
    ObserveRunnerEffects(
        vm,
        ExerciseRunnerNavigation(navigation.openBody, navigation.showResult) {
            voice.onClosed(navigation.close, toToday)
        }
    )
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }
    ProvideVoiceCommands(voice.host) {
        ExerciseRunScreen(state, vm.templateActions(), onFinish = vm::finish, onLeave = vm::leave)
    }
}

@Composable
private fun ObserveRunnerEffects(
    vm: ExerciseRunnerViewModel,
    navigation: ExerciseRunnerNavigation
) {
    ObserveEffects(vm.effects) { effect ->
        when (effect) {
            is ExerciseRunnerEffect.OpenBody -> navigation.openBody(effect)
            is ExerciseRunnerEffect.ShowResult -> navigation.showResult(effect)
            ExerciseRunnerEffect.Close -> navigation.close()
        }
    }
}

fun ExerciseRunnerViewModel.templateActions() = object : TemplateActions {
    override fun onText(key: String, text: String) = setText(key, text)
    override fun onPendingItem(key: String, text: String) = setPendingItem(key, text)
    override fun onAddItem(block: ExerciseBlock.Checklist) = addItem(block.key, block)
    override fun onToggleItem(key: String, itemId: String) = toggleItem(key, itemId)
    override fun onChoiceOption(key: String, index: Int, text: String) =
        setChoiceOption(key, index, text)
    override fun onChoose(key: String, index: Int) = choose(key, index)
    override fun onListItem(key: String, primary: Boolean, index: Int, text: String) =
        setListItem(key, primary, index, text)
    override fun onToggleChip(key: String, option: String, max: Int) = toggleChip(key, option, max)
    override fun onChipCustom(key: String, text: String) = setChipCustom(key, text)
}

/** Prototype `ExerciseIntroScreen` (Exercise.kt): a short "why", then one concrete action. */
@Composable
fun ExerciseIntroScreen(
    state: ExerciseRunnerUiState,
    onClose: () -> Unit,
    onPrimary: () -> Unit,
    onSnooze: () -> Unit
) {
    val c = Itera.colors
    if (state.missing) {
        ScreenColumn(modifier = Modifier.testTag("ExerciseIntro")) {
            TopBar(stringResource(R.string.exercise_label), onClose)
            ErrorState(stringResource(R.string.error_activity_missing))
        }
        return
    }
    val sc = state.skill.colors(c.isDark)
    val generic = state.body == ExerciseBody.Generic
    val context = LocalContext.current
    val snoozeLabel = state.snoozeAt?.let {
        stringResource(R.string.exercise_later_at, formatTime(it, currentLocale(), context))
    } ?: stringResource(R.string.exercise_not_now)
    ScreenColumn(
        gap = 22.dp,
        modifier = Modifier.testTag("ExerciseIntro"),
        bottom = {
            IteraButton(
                stringResource(if (generic) R.string.exercise_did_it else R.string.exercise_start),
                onPrimary,
                icon = if (generic) IteraIcons.Check else IteraIcons.Play,
                enabled = !state.loading && !state.busy,
                modifier = Modifier.testTag("IntroPrimary")
            )
            IteraButton(
                snoozeLabel,
                onSnooze,
                kind = ButtonKind.Ghost,
                height = 48.dp,
                enabled = !state.loading,
                modifier = Modifier.testTag("IntroSnooze")
            )
        }
    ) {
        TopBar(stringResource(R.string.exercise_label), onClose)
        TechniqueToken(state.skill, techniqueIcon(state.techniqueId), 64.dp)
        Eyebrow(
            stringResource(
                R.string.exercise_meta,
                stringResource(state.skill.title),
                state.programDay
            ),
            sc.content
        )
        Text(state.name, style = Itera.type.hero, color = c.ink)
        Text(state.why, style = Itera.type.bodyLarge, color = c.ink2)
        IteraCard(radius = 28.dp, padding = PaddingValues(22.dp)) {
            Eyebrow(stringResource(R.string.exercise_yours))
            Text(
                state.task,
                style = Itera.type.headline.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink
            )
        }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
}

/** The run step for template techniques (prototype `TwoMinuteScreen` for the checklist). */
@Composable
fun ExerciseRunScreen(
    state: ExerciseRunnerUiState,
    actions: TemplateActions,
    onFinish: () -> Unit,
    onLeave: () -> Unit
) {
    val c = Itera.colors
    val sc = state.skill.colors(c.isDark)
    var confirming by rememberSaveable { mutableStateOf(false) }
    val back = remember(state.hasDraft) { { if (state.hasDraft) confirming = true else onLeave() } }
    BackHandler(enabled = !state.loading) { back() }
    val reason = gateReason(state.gate)
    val checklist = state.blocks.filterIsInstance<ExerciseBlock.Checklist>().firstOrNull()
    ScreenColumn(
        gap = 22.dp,
        modifier = Modifier.testTag("ExerciseRun"),
        bottom = {
            IteraButton(
                stringResource(
                    if (state.blocks.isEmpty()) R.string.exercise_did_it else R.string.two_finish
                ),
                onFinish,
                enabled = reason == null && !state.busy && !state.loading,
                modifier = Modifier.testTag("RunPrimary").gated(reason)
            )
        }
    ) {
        TopBar(state.name, back, IteraIcons.Back, trailing = {
            if (checklist != null) {
                val items = (state.values[checklist.key] as? BlockValue.Items)?.items.orEmpty()
                Pill("${items.count { it.done }} / ${items.size}", sc.container, sc.content)
            }
        })
        VoiceCommandAction(Modifier.align(Alignment.End))
        TemplateBody(
            state.blocks,
            state.values,
            state.pendingItems,
            state.elapsed,
            state.skill,
            actions
        )
        if (state.blocks.isEmpty() && !state.loading) {
            Text(state.task, style = Itera.type.headline, color = c.ink)
        }
        if (state.failed) {
            Text(
                stringResource(R.string.error_generic),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
        }
    }
    if (confirming) {
        LeaveExerciseDialog(
            onLeave = {
                confirming = false
                onLeave()
            },
            onStay = { confirming = false }
        )
    }
}
