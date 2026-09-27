package com.wivernz.itera.domain.voice

/** An incomplete row of the active checklist; [position] is 1-based and tells duplicates apart. */
data class VoiceItem(val id: String, val label: String, val position: Int)

sealed interface VoiceMatch {
    /** One exact label: may complete without a choice. */
    data class Unique(val item: VoiceItem) : VoiceMatch

    /** Duplicate exact labels or any partial match: the user picks one and confirms. */
    data class Choose(val candidates: List<VoiceItem>) : VoiceMatch

    data object None : VoiceMatch
}

internal object VoiceMatchText {
    fun tokens(text: String): List<String> = VoiceNormalizer.tokens(text).map { it.norm }

    fun key(text: String): String = tokens(text).joinToString(" ")
}

/**
 * CompleteItem matching inside the one active list (docs/ux/10-voice-input.md): exact normalised equality first,
 * then contiguous whole-token phrases. Never fuzzy and never the first row silently.
 */
object VoiceItemMatcher {
    fun match(query: String, items: List<VoiceItem>): VoiceMatch {
        val q = VoiceMatchText.tokens(query)
        if (q.isEmpty()) return VoiceMatch.None
        val labelled = items.map { it to VoiceMatchText.tokens(it.label) }
        val exact = labelled.filter { (_, label) -> label == q }.map { it.first }
        if (exact.size == 1) return VoiceMatch.Unique(exact.single())
        if (exact.size > 1) return VoiceMatch.Choose(exact)
        val partial = labelled.filter { (_, label) -> contains(label, q) }.map { it.first }
        return if (partial.isEmpty()) VoiceMatch.None else VoiceMatch.Choose(partial)
    }

    private fun contains(label: List<String>, query: List<String>): Boolean =
        query.size <= label.size &&
            (0..label.size - query.size).any { start ->
                query.indices.all {
                    label[start + it] ==
                        query[it]
                }
            }
}

/** Where dictated text lands. */
data class VoiceInsertion(val text: String, val caret: Int)

/** Final dictation replaces only the captured selection and keeps the text around it. */
object VoiceText {
    private const val NO_SPACE_BEFORE = ".,!?;:)…"

    fun insert(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        transcript: String
    ): VoiceInsertion {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(start, text.length)
        val spoken = transcript.trim()
        val before = text.substring(0, start)
        val after = text.substring(end)
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace() &&
            spoken.firstOrNull()?.let { it in NO_SPACE_BEFORE } != true
        ) {
            " "
        } else {
            ""
        }
        val trail = if (after.isNotEmpty() && !after.first().isWhitespace() &&
            after.first() !in NO_SPACE_BEFORE
        ) {
            " "
        } else {
            ""
        }
        val inserted = lead + spoken + trail
        return VoiceInsertion(
            before + inserted + after,
            before.length + lead.length + spoken.length
        )
    }
}
