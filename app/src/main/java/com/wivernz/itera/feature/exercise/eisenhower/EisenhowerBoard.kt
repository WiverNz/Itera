package com.wivernz.itera.feature.exercise.eisenhower

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.Quadrant

/** Why the sorter cannot finish yet. */
enum class EisenhowerGate { READY, SORT_ALL, CHOOSE }

/**
 * The sorter's state (issue 022): tap a task, then tap a square. Pure, shared by the Eisenhower screen and the
 * combination chain's inline step.
 */
data class EisenhowerBoard(
    val items: List<EisenhowerItem> = emptyList(),
    val selectedId: String? = null,
    val chosenId: String? = null
) {
    val inbox: List<EisenhowerItem> get() = items.filter { it.quadrant == Quadrant.UNSORTED }
    val allSorted: Boolean get() = items.isNotEmpty() && inbox.isEmpty()
    val selected: EisenhowerItem? get() = items.firstOrNull { it.id == selectedId }
    val doNow: List<EisenhowerItem> get() = placedIn(Quadrant.DO_NOW)

    fun placedIn(quadrant: Quadrant) = items.filter { it.quadrant == quadrant }

    val gate: EisenhowerGate get() = when {
        !allSorted -> EisenhowerGate.SORT_ALL
        doNow.isNotEmpty() && doNow.none { it.id == chosenId } -> EisenhowerGate.CHOOSE
        else -> EisenhowerGate.READY
    }

    /** Selects an unsorted task, or re-selects a placed one to move it. */
    fun select(id: String): EisenhowerBoard =
        if (items.any { it.id == id }) copy(selectedId = id) else this

    /** Places the selected task, then selects the next unsorted one, so the common case is one tap per task. */
    fun place(quadrant: Quadrant): EisenhowerBoard {
        val id = selectedId ?: return this
        if (quadrant == Quadrant.UNSORTED) return this
        val placed = items.map { if (it.id == id) it.copy(quadrant = quadrant) else it }
        val chosen = chosenId?.takeIf { chosen ->
            placed.any { it.id == chosen && it.quadrant == Quadrant.DO_NOW }
        }
        return copy(
            items = placed,
            selectedId = placed.firstOrNull { it.quadrant == Quadrant.UNSORTED }?.id,
            chosenId = chosen
        )
    }

    /** The one Do-now task carried forward. */
    fun choose(id: String): EisenhowerBoard =
        if (doNow.any { it.id == id }) copy(chosenId = id) else this

    fun result() = ActivityResult.Eisenhower(items, chosenId)

    companion object {
        const val MIN_TASKS = 4
        const val MAX_TASKS = 7

        /** One task per line; blank lines are ignored and at most seven are kept. */
        fun lines(text: String): List<String> =
            text.lines().map(String::trim).filter(String::isNotEmpty).take(MAX_TASKS)

        fun fromEntry(text: String): EisenhowerBoard {
            val items = lines(text).mapIndexed { i, label ->
                EisenhowerItem((i + 1).toString(), label, Quadrant.UNSORTED)
            }
            return EisenhowerBoard(items, items.firstOrNull()?.id, null)
        }

        /** Restores a draft; the first unsorted task is selected again. */
        fun fromResult(result: ActivityResult.Eisenhower): EisenhowerBoard = EisenhowerBoard(
            result.items,
            result.items.firstOrNull { it.quadrant == Quadrant.UNSORTED }?.id,
            result.chosenItemId
        )
    }
}
