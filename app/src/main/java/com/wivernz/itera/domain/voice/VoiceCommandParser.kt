package com.wivernz.itera.domain.voice

import java.text.Normalizer
import java.util.Locale

/** A word of the utterance: its comparison form and its span in the original text. */
internal data class VoiceToken(val norm: String, val start: Int, val end: Int)

/**
 * Comparison-only normalisation: Unicode (NFKC), case, outer punctuation and whitespace. Original text is never
 * rewritten; arguments are cut from the original by token spans.
 */
internal object VoiceNormalizer {
    private val WORD = Regex("""\S+""")

    // Hyphens stay: "-5" must not read as 5; number words split on them separately.
    private const val OUTER = ".,!?¿¡;:\"'«»“”„‘’()[]…"

    fun tokens(text: String): List<VoiceToken> = WORD.findAll(text).mapNotNull { match ->
        val raw = match.value
        var start = 0
        var end = raw.length
        while (start < end && raw[start] in OUTER) start++
        while (end > start && raw[end - 1] in OUTER) end--
        if (start == end) {
            null
        } else {
            VoiceToken(
                norm(raw.substring(start, end)),
                match.range.first + start,
                match.range.first + end
            )
        }
    }.toList()

    fun norm(word: String): String =
        Normalizer.normalize(word, Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace('ё', 'е')
}

/**
 * The deterministic parser (docs/architecture/07-voice-input.md): anchored phrase prefixes on token boundaries,
 * longest first. Fixed commands must consume the whole utterance; Add/Complete take the rest as data. No fuzzy
 * matching, synonyms beyond the table, negation handling or chaining.
 */
object VoiceCommandParser {
    // an hour-scale ceiling that keeps overflow and absurd values out; setup validation comes later
    const val MAX_MINUTES = 600

    fun parse(utterance: String, language: VoiceLanguage): VoiceParse {
        val tokens = VoiceNormalizer.tokens(utterance)
        if (tokens.isEmpty()) return VoiceParse.Unsupported
        val vocabulary = VoiceVocabulary.of(language)
        val phrase = vocabulary.phrases
            .sortedByDescending { it.tokens.size }
            .firstOrNull { p ->
                tokens.size >= p.tokens.size &&
                    p.tokens.indices.all { tokens[it].norm == p.tokens[it] }
            }
            ?: return VoiceParse.Unsupported
        val rest = tokens.drop(phrase.tokens.size)
        return when (phrase.argument) {
            VoiceArgument.NONE ->
                if (rest.isEmpty()) {
                    VoiceParse.Recognised(fixed(phrase.kind))
                } else {
                    VoiceParse.InvalidArgument(phrase.kind)
                }
            VoiceArgument.TEXT -> {
                val text = if (rest.isEmpty()) {
                    ""
                } else {
                    utterance.substring(
                        rest.first().start,
                        rest.last().end
                    )
                }
                when {
                    text.isBlank() -> VoiceParse.MissingArgument(phrase.kind)
                    phrase.kind == VoiceCommandKind.ADD_ITEM -> VoiceParse.Recognised(
                        VoiceCommand.AddItem(text)
                    )
                    else -> VoiceParse.Recognised(VoiceCommand.CompleteItem(text))
                }
            }
            VoiceArgument.DURATION ->
                if (rest.isEmpty()) {
                    VoiceParse.Recognised(VoiceCommand.StartFocus(null))
                } else {
                    VoiceDuration.parse(rest.map { it.norm }, vocabulary)
                        ?.let { VoiceParse.Recognised(VoiceCommand.StartFocus(it)) }
                        ?: VoiceParse.InvalidArgument(VoiceCommandKind.START_FOCUS)
                }
        }
    }

    /**
     * Reads the recogniser's alternatives, best first. The top alternative runs only when no other alternative
     * implies a different command or argument; otherwise every distinct command is offered for selection.
     */
    fun interpret(alternatives: List<String>, language: VoiceLanguage): VoiceInterpretation {
        val usable = alternatives.filter { it.isNotBlank() }.take(MAX_ALTERNATIVES)
        if (usable.isEmpty()) return VoiceInterpretation.Single(VoiceParse.Unsupported)
        val parses = usable.map { parse(it, language) }
        val commands = parses.filterIsInstance<VoiceParse.Recognised>().map { it.command }
            .distinctBy(::key)
        val top = parses.first()
        return when {
            commands.size > 1 -> VoiceInterpretation.Competing(commands)
            commands.size == 1 && top !is VoiceParse.Recognised -> VoiceInterpretation.Competing(
                commands
            )
            else -> VoiceInterpretation.Single(top)
        }
    }

    private fun key(command: VoiceCommand): Any = when (command) {
        is VoiceCommand.AddItem -> command.kind to VoiceMatchText.key(command.text)
        is VoiceCommand.CompleteItem -> command.kind to VoiceMatchText.key(command.query)
        else -> command
    }

    private fun fixed(kind: VoiceCommandKind): VoiceCommand = when (kind) {
        VoiceCommandKind.PAUSE_FOCUS -> VoiceCommand.PauseFocus
        VoiceCommandKind.RESUME_FOCUS -> VoiceCommand.ResumeFocus
        VoiceCommandKind.END_FOCUS -> VoiceCommand.EndFocus
        VoiceCommandKind.COMPLETE_CURRENT_EXERCISE -> VoiceCommand.CompleteCurrentExercise
        VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION -> VoiceCommand.ShowCurrentRecommendation
        VoiceCommandKind.START_FOCUS -> VoiceCommand.StartFocus(null)
        VoiceCommandKind.ADD_ITEM, VoiceCommandKind.COMPLETE_ITEM -> error(
            "$kind takes an argument"
        )
    }

    private const val MAX_ALTERNATIVES = 5
}

/**
 * "{preposition} {n} {unit}" after the StartFocus phrase: one positive whole-minute count, as digits or a table
 * number word. Anything else (hours, fractions, signs, several numbers, missing unit) is invalid, never a default.
 */
object VoiceDuration {
    fun parse(utterance: String, language: VoiceLanguage): Int? =
        parse(VoiceNormalizer.tokens(utterance).map { it.norm }, VoiceVocabulary.of(language))

    internal fun parse(words: List<String>, vocabulary: VoiceVocabulary): Int? {
        if (words.size < 3) return null
        if (words.first() != VoiceNormalizer.norm(vocabulary.durationPreposition)) return null
        if (words.last() !in vocabulary.minuteUnits) return null
        val number = words.subList(1, words.size - 1)
        val minutes = if (number.size == 1 && number[0].all { it in '0'..'9' }) {
            number[0].toIntOrNull()
        } else {
            val spoken = number.joinToString(" ").replace('-', ' ').split(' ')
                .filter { it.isNotEmpty() }.joinToString(" ")
            vocabulary.numberWords.entries.firstOrNull {
                VoiceNormalizer.norm(it.key) == spoken
            }?.value
        }
        return minutes?.takeIf { it in 1..VoiceCommandParser.MAX_MINUTES }
    }
}
