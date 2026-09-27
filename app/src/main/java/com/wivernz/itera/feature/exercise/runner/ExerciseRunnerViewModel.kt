package com.wivernz.itera.feature.exercise.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.AnalyticsTechnique
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ScreenRoute
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CompletionRule
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.AbandonActivityUseCase
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.SaveDraftUseCase
import com.wivernz.itera.domain.training.SnoozeActivityUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.domain.voice.VoiceItem
import com.wivernz.itera.domain.voice.VoiceItemMatcher
import com.wivernz.itera.domain.voice.VoiceMatch
import com.wivernz.itera.feature.exercise.template.CompletionGate
import com.wivernz.itera.feature.exercise.template.CompletionRules
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandHost
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceRejection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ExerciseRunnerUiState(
    val loading: Boolean = true,
    // unknown activity id, or its technique is missing from the catalogue
    val missing: Boolean = false,
    val techniqueId: String = "",
    val name: String = "",
    val skill: Skill = Skill.HABITS,
    val why: String = "",
    val task: String = "",
    val programDay: Int = 1,
    val body: ExerciseBody = ExerciseBody.Generic,
    // "Not now - remind me at {time}"; null when no snooze fits before 20:00 or the activity cannot snooze
    val snoozeAt: LocalTime? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val values: Map<String, BlockValue> = emptyMap(),
    // the add-item field of each checklist, and when its first keystroke happened (the item's stopwatch start)
    val pendingItems: Map<String, String> = emptyMap(),
    // live stopwatch seconds per checklist item id
    val elapsed: Map<String, Int> = emptyMap(),
    val gate: CompletionGate = CompletionGate.Ready,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val hasDraft: Boolean get() = values.values.any(CompletionRules::isFilled) ||
        pendingItems.values.any(String::isNotBlank)
}

sealed interface ExerciseRunnerEffect {
    data class OpenBody(val activityId: Long, val techniqueId: String, val body: ExerciseBody) :
        ExerciseRunnerEffect
    data class ShowResult(val activityId: Long, val techniqueId: String) : ExerciseRunnerEffect

    /** Snoozed, or backed out of the run: return to the previous screen. */
    data object Close : ExerciseRunnerEffect
}

/**
 * The runner host (ADR-0007) for the intro step and the template run step; the result step has its own view model.
 * The template body renders [ExerciseBlock]s and keeps [BlockValue]s as the `ActivityResult.Template` draft.
 */
@HiltViewModel
class ExerciseRunnerViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val start: StartActivityUseCase,
    private val snooze: SnoozeActivityUseCase,
    private val abandon: AbandonActivityUseCase,
    private val complete: CompleteActivityUseCase,
    private val saveDraft: SaveDraftUseCase,
    private val analytics: Analytics,
    private val clock: Clock
) : ViewModel(),
    VoiceCommandHost {
    private val activityId: Long = checkNotNull(saved.get<Long>(ARG_ACTIVITY))
    private val mutable = MutableStateFlow(ExerciseRunnerUiState())
    val state: StateFlow<ExerciseRunnerUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<ExerciseRunnerEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private val autosave =
        DraftAutosave<ActivityResult>(viewModelScope) { saveDraft(activityId, it) }
    private var rule: CompletionRule = CompletionRule.Always
    private var exerciseType: ExerciseType = ExerciseType.TEMPLATE
    private var activityState: ActivityState? = null

    // stopwatch bases: item id -> (seconds already counted, running since)
    private val running = mutableMapOf<String, Pair<Int, Instant>>()
    private val pendingSince = mutableMapOf<String, Instant>()
    private var ticker: Job? = null

    init {
        analytics.track(Event.ScreenViewed(ScreenRoute.EXERCISE))
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val activity = plans.activity(activityId)
        val technique = activity?.let { catalog.technique(it.techniqueId) }
        if (activity == null || technique == null) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        exerciseType = activity.exerciseType
        activityState = activity.state
        rule = technique.template?.completionRule ?: CompletionRule.Always
        val blocks = technique.template?.blocks.orEmpty()
        val draft = (plans.draft(activityId) as? ActivityResult.Template)?.values.orEmpty()
        val values = blocks.associate { it.key to (draft[it.key] ?: emptyValue(it)) }
        values.values.filterIsInstance<BlockValue.Items>().flatMap { it.items }
            .filter { !it.done && it.elapsedSeconds != null }
            .forEach { running[it.id] = (it.elapsedSeconds ?: 0) to clock.instant() }
        // Snooze is an AVAILABLE -> SNOOZED transition only.
        val canSnooze = activity.state == ActivityState.AVAILABLE
        mutable.update {
            it.copy(
                loading = false,
                techniqueId = technique.id.value,
                name = technique.name,
                skill = technique.skill,
                why = technique.explanation,
                // a premortem mitigation carries its action as the task
                task =
                (draft[MITIGATION_KEY] as? BlockValue.Text)?.text?.takeIf { blocks.isEmpty() }
                    ?: activity.instruction,
                programDay = plans.programDayOfActivity(activityId) ?: 1,
                body = bodyFor(activity.exerciseType, technique),
                snoozeAt = if (canSnooze) snoozeTimeFor(LocalTime.now(clock)) else null,
                blocks = blocks,
                values = values,
                gate = CompletionRules.evaluate(rule, blocks, values),
                elapsed = liveElapsed(values)
            )
        }
        AnalyticsTechnique.of(technique.id.value)?.let {
            analytics.track(Event.ExerciseOpened(it, activity.exerciseType, activity.source))
        }
        startTicker()
    }

    // ------------------------------------------------------------------ intro

    /** "Start exercise" or, for a generic technique, "I did it". */
    fun onPrimary() {
        val s = mutable.value
        if (s.loading || s.missing || s.busy) return
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            val result = when (s.body) {
                ExerciseBody.Generic -> complete(activityId, ActivityResult.Template(emptyMap()))
                    .map { ExerciseRunnerEffect.ShowResult(activityId, s.techniqueId) }
                // the timer starts the activity itself, so backing out of its sheet leaves it untouched
                is ExerciseBody.Focus -> Result.success(
                    ExerciseRunnerEffect.OpenBody(activityId, s.techniqueId, s.body)
                )
                else -> ensureStarted()
                    .map { ExerciseRunnerEffect.OpenBody(activityId, s.techniqueId, s.body) }
            }
            mutable.update { it.copy(busy = false, failed = result.isFailure) }
            result.onSuccess { effectChannel.send(it) }
        }
    }

    /** "Not now - remind me at {time}". */
    fun onSnooze() {
        val at = mutable.value.snoozeAt ?: run {
            viewModelScope.launch { effectChannel.send(ExerciseRunnerEffect.Close) }
            return
        }
        viewModelScope.launch {
            val until = LocalDate.now(clock).atTime(at).atZone(clock.zone).toInstant()
            snooze(activityId, until)
            effectChannel.send(ExerciseRunnerEffect.Close)
        }
    }

    private suspend fun ensureStarted(): Result<Unit> {
        val current = plans.activity(activityId)?.state
        return if (current == ActivityState.IN_PROGRESS) {
            Result.success(Unit)
        } else {
            start(activityId).onSuccess { activityState = ActivityState.IN_PROGRESS }
        }
    }

    // ------------------------------------------------------------------ run (template body)

    /** A direct Library run has no intro to start it. Keep this idempotent across recomposition. */
    fun enterRun() {
        if (mutable.value.loading || mutable.value.missing) return
        viewModelScope.launch {
            ensureStarted().onFailure { mutable.update { it.copy(failed = true) } }
        }
    }

    fun setText(key: String, text: String) = edit(key, BlockValue.Text(capped(text)))

    fun setChoiceOption(key: String, index: Int, text: String) {
        val value = mutable.value.values[key] as? BlockValue.Choice ?: return
        val options = value.options.toMutableList().also { it[index] = capped(text) }
        val chosen = if (options[index].isBlank() &&
            value.chosenIndex == index
        ) {
            -1
        } else {
            value.chosenIndex
        }
        edit(key, value.copy(options = options, chosenIndex = chosen))
    }

    fun choose(key: String, index: Int) {
        val value = mutable.value.values[key] as? BlockValue.Choice ?: return
        if (value.options.getOrNull(index).isNullOrBlank()) return
        edit(key, value.copy(chosenIndex = index))
    }

    fun setListItem(key: String, primary: Boolean, index: Int, text: String) {
        val value = mutable.value.values[key] as? BlockValue.Lists ?: return
        val list = (if (primary) value.primary else value.secondary).toMutableList()
            .also { it[index] = capped(text) }
        edit(key, if (primary) value.copy(primary = list) else value.copy(secondary = list))
    }

    fun toggleChip(key: String, option: String, max: Int) {
        val value = mutable.value.values[key] as? BlockValue.Chips ?: return
        val selected = when {
            option in value.selected -> value.selected - option
            value.selected.size >= max -> return
            else -> value.selected + option
        }
        edit(key, value.copy(selected = selected))
    }

    fun setChipCustom(key: String, text: String) {
        val value = mutable.value.values[key] as? BlockValue.Chips ?: return
        edit(key, value.copy(custom = capped(text).ifEmpty { null }))
    }

    /** The checklist's add field. Its first keystroke starts the new item's stopwatch. */
    fun setPendingItem(key: String, text: String) {
        if (text.isNotEmpty() && key !in pendingSince) pendingSince[key] = clock.instant()
        if (text.isEmpty()) pendingSince.remove(key)
        mutable.update { it.copy(pendingItems = it.pendingItems + (key to capped(text))) }
        scheduleDraft()
    }

    fun addItem(key: String, block: ExerciseBlock.Checklist) {
        val label = mutable.value.pendingItems[key]?.trim().orEmpty()
        if (append(key, block, label, pendingSince[key] ?: clock.instant())) {
            pendingSince.remove(key)
            mutable.update { it.copy(pendingItems = it.pendingItems - key) }
        }
    }

    /** The one Add rule, shared by the field and voice; the stopwatch starts at [startedAt]. */
    private fun append(
        key: String,
        block: ExerciseBlock.Checklist,
        label: String,
        startedAt: Instant
    ): Boolean {
        val value = mutable.value.values[key] as? BlockValue.Items ?: return false
        if (label.isEmpty() || value.items.size >= block.maxItems) return false
        val id = ((value.items.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        val item = ChecklistItem(
            id,
            label,
            done = false,
            elapsedSeconds = if (block.withStopwatch) 0 else null
        )
        if (block.withStopwatch) running[id] = 0 to startedAt
        edit(key, value.copy(items = value.items + item))
        return true
    }

    /** Ticking freezes the item's stopwatch; unticking lets it run on from there. */
    fun toggleItem(key: String, itemId: String) {
        val value = mutable.value.values[key] as? BlockValue.Items ?: return
        val now = clock.instant()
        val items = value.items.map { item ->
            if (item.id != itemId) return@map item
            if (!item.done) {
                val frozen = running.remove(item.id)?.let { (base, since) ->
                    base + Duration.between(since, now).seconds.toInt()
                } ?: item.elapsedSeconds
                item.copy(done = true, elapsedSeconds = frozen)
            } else {
                item.elapsedSeconds?.let { running[item.id] = it to now }
                item.copy(done = false)
            }
        }
        edit(key, value.copy(items = items))
    }

    /** "Finish": completes with the template result, then the result step. */
    fun finish() {
        val s = mutable.value
        if (s.busy || s.gate != CompletionGate.Ready) return
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            autosave.cancel()
            complete(activityId, ActivityResult.Template(frozenValues())).onSuccess {
                effectChannel.send(ExerciseRunnerEffect.ShowResult(activityId, s.techniqueId))
            }.onFailure {
                mutable.update { it.copy(busy = false, failed = true) }
            }
        }
    }

    // ------------------------------------------------------------------ voice commands (milestone 012)

    /** The run step's one checklist, if it has exactly one: the only list voice may add to or tick. */
    private fun voiceList(): Pair<ExerciseBlock.Checklist, BlockValue.Items>? {
        val s = mutable.value
        val block =
            s.blocks.filterIsInstance<ExerciseBlock.Checklist>().singleOrNull() ?: return null
        val value = s.values[block.key] as? BlockValue.Items ?: return null
        return block to value
    }

    override val voiceCommands: Set<VoiceCommandKind>
        get() = buildSet {
            if (voiceList() != null) {
                add(VoiceCommandKind.ADD_ITEM)
                add(VoiceCommandKind.COMPLETE_ITEM)
            }
            add(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE)
        }

    override fun planVoice(command: VoiceCommand): VoicePlan {
        val s = mutable.value
        if (s.loading || s.missing) return VoicePlan.Reject(VoiceRejection.NotHere)
        return when (command) {
            is VoiceCommand.AddItem -> {
                val (block, value) = voiceList() ?: return VoicePlan.Reject(VoiceRejection.NoList)
                if (value.items.size >= block.maxItems) {
                    VoicePlan.Reject(VoiceRejection.ListFull)
                } else {
                    VoicePlan.Run(VoiceAction.AddItem(command.text))
                }
            }
            is VoiceCommand.CompleteItem -> {
                val (_, value) = voiceList() ?: return VoicePlan.Reject(VoiceRejection.NoList)
                val open = value.items.mapIndexedNotNull { i, item ->
                    VoiceItem(item.id, item.label, i + 1).takeIf { !item.done }
                }
                when (val match = VoiceItemMatcher.match(command.query, open)) {
                    is VoiceMatch.Unique -> VoicePlan.Run(VoiceAction.CompleteItem(match.item))
                    is VoiceMatch.Choose -> VoicePlan.Choose(match.candidates)
                    VoiceMatch.None -> VoicePlan.Reject(VoiceRejection.NoMatch(command.query))
                }
            }
            VoiceCommand.CompleteCurrentExercise ->
                if (s.gate == CompletionGate.Ready && !s.busy) {
                    VoicePlan.Confirm(VoiceAction.CompleteExercise)
                } else {
                    VoicePlan.Reject(VoiceRejection.ExerciseNotReady)
                }
            else -> VoicePlan.Reject(VoiceRejection.NotHere)
        }
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome = when (action) {
        is VoiceAction.AddItem -> {
            val (block, _) = voiceList() ?: return VoiceOutcome.Rejected(VoiceRejection.Stale)
            val label = capped(action.text.trim())
            if (append(block.key, block, label, clock.instant())) {
                VoiceOutcome.Done(VoiceFeedback.Added(label))
            } else {
                VoiceOutcome.Rejected(VoiceRejection.ListFull)
            }
        }
        is VoiceAction.CompleteItem -> {
            val (block, value) = voiceList() ?: return VoiceOutcome.Rejected(VoiceRejection.Stale)
            val item = value.items.firstOrNull { it.id == action.item.id }
            if (item == null || item.done || item.label != action.item.label) {
                VoiceOutcome.Rejected(VoiceRejection.Stale)
            } else {
                toggleItem(block.key, item.id)
                VoiceOutcome.Done(VoiceFeedback.Completed(item.label))
            }
        }
        VoiceAction.CompleteExercise -> {
            val s = mutable.value
            if (s.gate != CompletionGate.Ready || s.busy) {
                VoiceOutcome.Rejected(VoiceRejection.Stale)
            } else {
                finish()
                VoiceOutcome.Done(VoiceFeedback.Handover)
            }
        }
        else -> VoiceOutcome.Rejected(VoiceRejection.NotHere)
    }

    /** Back from the run step: the draft is kept and the activity becomes available again. */
    fun leave() {
        autosave.flush()
        viewModelScope.launch {
            if (plans.activity(activityId)?.state == ActivityState.IN_PROGRESS) {
                saveDraft(activityId, ActivityResult.Template(frozenValues()))
                abandon(activityId)
            }
            effectChannel.send(ExerciseRunnerEffect.Close)
        }
    }

    /** `ON_STOP`: write the draft now rather than after the debounce. */
    fun flushDraft() {
        if (mutable.value.loading || mutable.value.missing) return
        if (exerciseType != ExerciseType.TEMPLATE || mutable.value.blocks.isEmpty()) return
        autosave.schedule(ActivityResult.Template(frozenValues()))
        autosave.flush()
    }

    private fun edit(key: String, value: BlockValue) {
        mutable.update { s ->
            val values = s.values + (key to value)
            s.copy(
                values = values,
                gate = CompletionRules.evaluate(rule, s.blocks, values),
                elapsed = liveElapsed(values)
            )
        }
        scheduleDraft()
    }

    private fun scheduleDraft() {
        if (mutable.value.loading || mutable.value.busy) return
        autosave.schedule(ActivityResult.Template(frozenValues()))
    }

    /** Values with every running stopwatch read at this instant, as they are stored. */
    private fun frozenValues(): Map<String, BlockValue> {
        val live = liveElapsed(mutable.value.values)
        return mutable.value.values.mapValues { (_, value) ->
            if (value is BlockValue.Items) {
                value.copy(
                    items = value.items.map { item ->
                        live[item.id]?.let { item.copy(elapsedSeconds = it) } ?: item
                    }
                )
            } else {
                value
            }
        }
    }

    private fun liveElapsed(values: Map<String, BlockValue>): Map<String, Int> {
        val now = clock.instant()
        return values.values.filterIsInstance<BlockValue.Items>().flatMap { it.items }
            .mapNotNull { item ->
                val seconds = running[item.id]?.let { (base, since) ->
                    base + Duration.between(since, now).seconds.toInt().coerceAtLeast(0)
                } ?: item.elapsedSeconds
                seconds?.let { item.id to it }
            }.toMap()
    }

    private fun startTicker() {
        if (mutable.value.blocks.none { it is ExerciseBlock.Checklist && it.withStopwatch }) return
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                if (running.isNotEmpty()) {
                    mutable.update { it.copy(elapsed = liveElapsed(it.values)) }
                }
            }
        }
    }

    private fun emptyValue(block: ExerciseBlock): BlockValue = when (block) {
        is ExerciseBlock.Instruction -> BlockValue.Text("")
        is ExerciseBlock.TextInput -> BlockValue.Text("")
        is ExerciseBlock.Checklist -> BlockValue.Items(emptyList())
        is ExerciseBlock.PickOne -> BlockValue.Choice(List(block.itemCount) { "" }, -1)
        is ExerciseBlock.TwoLists -> BlockValue.Lists(
            List(block.primaryCount) { "" },
            List(block.secondaryCount) { "" }
        )
        is ExerciseBlock.ChipSelect -> BlockValue.Chips(emptyList(), null)
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val MITIGATION_KEY = "action"
    }
}

/**
 * The snooze rule: the next whole hour at least two hours out, capped at 20:00. Null when that would not be later
 * than now (from 20:00 on), so the intro offers a plain "Not now".
 */
fun snoozeTimeFor(now: LocalTime): LocalTime? {
    val earliest = now.toSecondOfDay() + SNOOZE_LEAD_SECONDS
    val nextHour = (earliest + SECONDS_PER_HOUR - 1) / SECONDS_PER_HOUR * SECONDS_PER_HOUR
    val capped = minOf(nextHour, SNOOZE_CAP.toSecondOfDay())
    return if (capped > now.toSecondOfDay()) LocalTime.ofSecondOfDay(capped.toLong()) else null
}

private const val SECONDS_PER_HOUR = 3600
private const val SNOOZE_LEAD_SECONDS = 2 * SECONDS_PER_HOUR
private val SNOOZE_CAP: LocalTime = LocalTime.of(20, 0)
