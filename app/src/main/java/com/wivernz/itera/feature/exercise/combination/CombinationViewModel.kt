package com.wivernz.itera.feature.exercise.combination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.CombinationStepResult
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TrainingDay
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import com.wivernz.itera.domain.training.CompleteActivityUseCase
import com.wivernz.itera.domain.training.SaveDraftUseCase
import com.wivernz.itera.domain.training.StartActivityUseCase
import com.wivernz.itera.domain.voice.VoiceCommand
import com.wivernz.itera.domain.voice.VoiceCommandKind
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerBoard
import com.wivernz.itera.feature.exercise.eisenhower.EisenhowerGate
import com.wivernz.itera.feature.exercise.runner.DraftAutosave
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.exercise.runner.bodyFor
import com.wivernz.itera.feature.exercise.runner.capped
import com.wivernz.itera.feature.voice.VoiceAction
import com.wivernz.itera.feature.voice.VoiceCommandHost
import com.wivernz.itera.feature.voice.VoiceFeedback
import com.wivernz.itera.feature.voice.VoiceOutcome
import com.wivernz.itera.feature.voice.VoicePlan
import com.wivernz.itera.feature.voice.VoiceRejection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChainState { DONE, NOW, NEXT }

/** How a step runs inside the chain. */
sealed interface StepKind {
    data object Eisenhower : StepKind
    data class PickOne(val block: ExerciseBlock.PickOne) : StepKind

    /** Opens the full-screen timer and returns to the chain. */
    data class Focus(val minutes: Int) : StepKind

    /** Shown, never completed here: it is part of the evening reflection. */
    data object Reflection : StepKind

    /** A template with no blocks: "Continue" records it. */
    data object Generic : StepKind

    /** Any other body opens its own screen (generated chains past the curriculum). */
    data class Screen(val body: ExerciseBody) : StepKind
}

/** What a finished step shows in its collapsed card. */
sealed interface StepSummary {
    data class Sorted(val chosen: String?) : StepSummary
    data class Picked(val choice: String) : StepSummary
    data class Focused(val minutes: Int) : StepSummary
    data object Done : StepSummary
}

data class ChainStepUi(
    val activityId: Long?,
    val techniqueId: String,
    val name: String,
    val skill: Skill,
    val prompt: String,
    val hint: String,
    val state: ChainState,
    val kind: StepKind,
    val summary: StepSummary? = null
)

data class CombinationUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val programDay: Int = 1,
    val curriculum: Boolean = true,
    val totalMinutes: Int = 0,
    val steps: List<ChainStepUi> = emptyList(),
    // the carried subject of the current step: Eisenhower's chosen task for 80/20, 80/20's choice for Deep Work
    val subject: String? = null,
    val entryText: String = "",
    val board: EisenhowerBoard = EisenhowerBoard(),
    val choice: BlockValue.Choice? = null,
    val busy: Boolean = false,
    val failed: Boolean = false
) {
    val current: ChainStepUi? get() = steps.firstOrNull { it.state == ChainState.NOW }
    val next: ChainStepUi? get() = current?.let { now -> steps.getOrNull(steps.indexOf(now) + 1) }

    /** Every step except the evening reflection is done. */
    val chainDone: Boolean get() = !loading && steps.none {
        it.state != ChainState.DONE && it.kind != StepKind.Reflection
    }
    val anyDone: Boolean get() = steps.any { it.state == ChainState.DONE }

    val entering: Boolean get() = current?.kind == StepKind.Eisenhower && board.items.isEmpty()
    val canContinue: Boolean get() = when (current?.kind) {
        null -> chainDone
        StepKind.Eisenhower ->
            if (board.items.isEmpty()) {
                EisenhowerBoard.lines(entryText).size >= EisenhowerBoard.MIN_TASKS
            } else {
                board.gate == EisenhowerGate.READY
            }
        is StepKind.PickOne -> choice?.options?.getOrNull(choice.chosenIndex)?.isNotBlank() == true
        is StepKind.Focus, StepKind.Generic, is StepKind.Screen -> true
        StepKind.Reflection -> false
    }
}

sealed interface CombinationEffect {
    data class OpenFocus(val activityId: Long, val minutes: Int, val techniqueId: String) :
        CombinationEffect
    data class OpenBody(val activityId: Long, val techniqueId: String, val body: ExerciseBody) :
        CombinationEffect
    data object Close : CombinationEffect
    data object Done : CombinationEffect
}

/**
 * The combination chain (issue 027). Chain state lives in the database - one child activity per step with
 * `source = COMBINATION` - so the timer round trip and process death lose nothing. Each step completes its own child
 * immediately; the parent completes with every step's summary once the last step before the reflection is done.
 * Scoped to its navigation entry (`docs/architecture/02-state-management.md` section 7).
 */
@HiltViewModel
class CombinationViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val start: StartActivityUseCase,
    private val complete: CompleteActivityUseCase,
    private val saveDraft: SaveDraftUseCase,
    private val clock: Clock
) : ViewModel(),
    VoiceCommandHost {
    private val parentId: Long = checkNotNull(saved.get<Long>(ARG_ACTIVITY))
    private val mutable = MutableStateFlow(CombinationUiState())
    val state: StateFlow<CombinationUiState> = mutable.asStateFlow()
    private val effectChannel = Channel<CombinationEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()
    private var draftStepId: Long? = null
    private val autosave =
        DraftAutosave<Pair<Long, ActivityResult>>(viewModelScope) { (id, draft) ->
            saveDraft(id, draft)
        }
    private var techniques: Map<TechniqueId, Technique> = emptyMap()
    private var completingParent = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val parent = plans.activity(parentId)
        if (parent == null || parent.exerciseType != ExerciseType.COMBINATION) {
            mutable.update { it.copy(loading = false, missing = true) }
            return
        }
        techniques =
            runCatching { catalog.catalog().associateBy { it.id } }.getOrDefault(emptyMap())
        if (parent.state == ActivityState.AVAILABLE || parent.state == ActivityState.SNOOZED) {
            start(parentId)
        }
        plans.observeDay(parent.trainingDayId).filterNotNull().catch { }.collect { render(it) }
    }

    private suspend fun render(day: TrainingDay) {
        val parent = day.activities.firstOrNull { it.id == parentId } ?: return
        val curriculumDay = runCatching { catalog.curriculum() }.getOrNull()?.days
            ?.firstOrNull { it.day == day.programDay && it.combination.isNotEmpty() }
        val rows = ChainCarry.steps(day)
        val authored = curriculumDay?.combination.orEmpty()
        val firstOpen = rows.firstOrNull { it.state !in TERMINAL }
        val steps = rows.mapIndexed { index, row ->
            val technique = techniques[row.techniqueId]
            val step = authored.getOrNull(index)?.takeIf { it.techniqueId == row.techniqueId }
            ChainStepUi(
                activityId = row.id,
                techniqueId = row.techniqueId.value,
                name = technique?.name ?: row.title,
                skill = technique?.skill ?: Skill.PLANNING,
                prompt = step?.prompt ?: technique?.shortDescription.orEmpty(),
                hint = step?.hint ?: row.instruction,
                state = when {
                    row.state in TERMINAL -> ChainState.DONE
                    row.id == firstOpen?.id -> ChainState.NOW
                    else -> ChainState.NEXT
                },
                kind = kindOf(row, technique),
                summary = row.takeIf { it.state in TERMINAL }?.let(::summaryOf)
            )
        } + reflectionStep(authored.lastOrNull()?.takeIf { it.techniqueId == REFLECTION })
        val current = firstOpen
        val switched = current?.id != draftStepId
        draftStepId = current?.id
        val draft = if (switched) current?.let { plans.draft(it.id) } else null
        mutable.update { s ->
            val kind = steps.firstOrNull { it.state == ChainState.NOW }?.kind
            s.copy(
                loading = false,
                programDay = day.programDay,
                curriculum = curriculumDay != null,
                totalMinutes = parent.estimatedMinutes,
                steps = steps,
                subject = current?.let { ChainCarry.carriedInto(it, day) },
                board = if (switched) {
                    (draft as? ActivityResult.Eisenhower)?.let(EisenhowerBoard::fromResult)
                        ?: EisenhowerBoard()
                } else {
                    s.board
                },
                entryText = if (switched) "" else s.entryText,
                choice = if (switched) {
                    (kind as? StepKind.PickOne)?.let { pick ->
                        (draft as? ActivityResult.Template)?.values?.get(
                            pick.block.key
                        ) as? BlockValue.Choice
                            ?: BlockValue.Choice(List(pick.block.itemCount) { "" }, -1)
                    }
                } else {
                    s.choice
                },
                busy = false
            )
        }
        if (current == null && parent.state !in TERMINAL) completeParent(day)
    }

    private fun kindOf(row: PlanActivity, technique: Technique?): StepKind =
        when (row.exerciseType) {
            ExerciseType.EISENHOWER -> StepKind.Eisenhower
            ExerciseType.FOCUS_TIMER -> StepKind.Focus(row.estimatedMinutes)
            ExerciseType.TEMPLATE -> templateKind(technique)
            else -> StepKind.Screen(bodyFor(row.exerciseType, technique))
        }

    private fun templateKind(technique: Technique?): StepKind {
        val blocks = technique?.template?.blocks.orEmpty()
        return blocks.filterIsInstance<ExerciseBlock.PickOne>().firstOrNull()
            ?.let { StepKind.PickOne(it) }
            ?: if (blocks.isEmpty()) StepKind.Generic else StepKind.Screen(ExerciseBody.Template)
    }

    private fun summaryOf(row: PlanActivity): StepSummary = when (val result = row.result) {
        is ActivityResult.Eisenhower -> StepSummary.Sorted(ChainCarry.subjectOf(result))
        is ActivityResult.Focus -> StepSummary.Focused(result.actualSeconds / SECONDS_PER_MINUTE)
        is ActivityResult.Template -> ChainCarry.subjectOf(result)?.let { StepSummary.Picked(it) }
            ?: StepSummary.Done
        else -> StepSummary.Done
    }

    /** The reflection closes every chain; it is part of the evening reflection and has no row. */
    private fun reflectionStep(authored: com.wivernz.itera.domain.model.CombinationStep?) =
        techniques[REFLECTION].let { technique ->
            listOf(
                ChainStepUi(
                    activityId = null,
                    techniqueId = REFLECTION.value,
                    name = technique?.name.orEmpty(),
                    skill = Skill.REFLECTION,
                    prompt = authored?.prompt ?: technique?.shortDescription.orEmpty(),
                    hint = authored?.hint.orEmpty(),
                    state = ChainState.NEXT,
                    kind = StepKind.Reflection
                )
            )
        }

    // ------------------------------------------------------------------ inline bodies

    fun setEntry(text: String) = mutable.update { it.copy(entryText = capped(text)) }

    fun select(id: String) = board(mutable.value.board.select(id))

    fun place(quadrant: com.wivernz.itera.domain.model.Quadrant) =
        board(mutable.value.board.place(quadrant))

    fun choose(id: String) = board(mutable.value.board.choose(id))

    fun setOption(index: Int, text: String) {
        val choice = mutable.value.choice ?: return
        val options = choice.options.toMutableList().also { it[index] = capped(text) }
        val chosen = if (options[index].isBlank() &&
            choice.chosenIndex == index
        ) {
            -1
        } else {
            choice.chosenIndex
        }
        pick(choice.copy(options = options, chosenIndex = chosen))
    }

    fun chooseOption(index: Int) {
        val choice = mutable.value.choice ?: return
        if (choice.options.getOrNull(index).isNullOrBlank()) return
        pick(choice.copy(chosenIndex = index))
    }

    /** The primary: the current step's action. */
    fun onPrimary() {
        val s = mutable.value
        if (s.busy || !s.canContinue) return
        val step = s.current
        if (step == null) {
            viewModelScope.launch { effectChannel.send(CombinationEffect.Done) }
            return
        }
        val id = step.activityId ?: return
        when (val kind = step.kind) {
            StepKind.Eisenhower -> if (s.entering) {
                board(EisenhowerBoard.fromEntry(s.entryText))
            } else {
                completeStep(id, s.board.result())
            }
            is StepKind.PickOne -> completeStep(
                id,
                ActivityResult.Template(mapOf(kind.block.key to checkNotNull(s.choice)))
            )
            is StepKind.Focus -> viewModelScope.launch {
                autosave.flush()
                effectChannel.send(CombinationEffect.OpenFocus(id, kind.minutes, step.techniqueId))
            }
            StepKind.Generic -> completeStep(id, ActivityResult.Template(emptyMap()))
            is StepKind.Screen -> viewModelScope.launch {
                effectChannel.send(CombinationEffect.OpenBody(id, step.techniqueId, kind.body))
            }
            StepKind.Reflection -> Unit
        }
    }

    /** Closing keeps finished steps and drafts; the chain resumes at the right step. */
    fun leave() {
        autosave.flush()
        viewModelScope.launch { effectChannel.send(CombinationEffect.Close) }
    }

    fun flushDraft() = autosave.flush()

    private fun board(board: EisenhowerBoard) {
        mutable.update { it.copy(board = board) }
        mutable.value.current?.activityId?.let { autosave.schedule(it to board.result()) }
    }

    private fun pick(choice: BlockValue.Choice) {
        mutable.update { it.copy(choice = choice) }
        val step = mutable.value.current ?: return
        val key = (step.kind as? StepKind.PickOne)?.block?.key ?: return
        step.activityId?.let {
            autosave.schedule(it to ActivityResult.Template(mapOf(key to choice)))
        }
    }

    /** A step writes its own child activity at once, so an abandoned chain keeps the work already done. */
    private fun completeStep(id: Long, result: ActivityResult) {
        mutable.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            autosave.cancel()
            complete(id, result).onFailure {
                mutable.update { s -> s.copy(busy = false, failed = true) }
            }
        }
    }

    private suspend fun completeParent(day: TrainingDay) {
        if (completingParent) return
        completingParent = true
        val results = ChainCarry.steps(day).filter { it.state == ActivityState.COMPLETED }.map {
            CombinationStepResult(
                it.techniqueId,
                ChainCarry.subjectOf(it.result).orEmpty(),
                it.completedAt ?: clock.instant()
            )
        }
        complete(parentId, ActivityResult.Combination(results))
    }

    // ------------------------------------------------------------------ voice commands (milestone 012)

    /**
     * Only the Eisenhower step's task entry takes items. The parent completes when its steps do, so
     * CompleteCurrentExercise is never offered here: voice cannot finish a chain early.
     */
    override val voiceCommands: Set<VoiceCommandKind>
        get() = if (mutable.value.entering) setOf(VoiceCommandKind.ADD_ITEM) else emptySet()

    override fun planVoice(command: VoiceCommand): VoicePlan {
        val s = mutable.value
        return when {
            command !is VoiceCommand.AddItem -> VoicePlan.Reject(VoiceRejection.NotHere)
            !s.entering -> VoicePlan.Reject(VoiceRejection.NoList)
            EisenhowerBoard.lines(s.entryText).size >= EisenhowerBoard.MAX_TASKS ->
                VoicePlan.Reject(VoiceRejection.ListFull)
            else -> VoicePlan.Run(VoiceAction.AddItem(command.text))
        }
    }

    override suspend fun executeVoice(action: VoiceAction): VoiceOutcome {
        val s = mutable.value
        if (action !is VoiceAction.AddItem) return VoiceOutcome.Rejected(VoiceRejection.NotHere)
        if (!s.entering || EisenhowerBoard.lines(s.entryText).size >= EisenhowerBoard.MAX_TASKS) {
            return VoiceOutcome.Rejected(VoiceRejection.Stale)
        }
        val task = action.text.trim()
        setEntry(s.entryText.trimEnd().let { if (it.isEmpty()) task else it + "\n" + task })
        return VoiceOutcome.Done(VoiceFeedback.Added(task))
    }

    companion object {
        const val ARG_ACTIVITY = "activityId"
        private const val SECONDS_PER_MINUTE = 60
        private val REFLECTION = TechniqueId("daily_reflection")
        private val TERMINAL =
            setOf(ActivityState.COMPLETED, ActivityState.SKIPPED, ActivityState.EXPIRED)
    }
}
