package com.wivernz.itera.domain.voice

/**
 * The restricted phrase list for an offline recogniser in command mode (milestone 013, ADR-0022). Only where every
 * available command is a fixed phrase: free-text commands (Add/Complete) need free-form recognition, so there is
 * no grammar then. Built from the normative vocabulary, so the parser accepts every phrase it contains.
 */
object VoiceGrammar {
    /** Null when any of [available] takes free text or nothing is available: recognise free-form. */
    fun forCommands(available: Set<VoiceCommandKind>, language: VoiceLanguage): List<String>? {
        val vocabulary = VoiceVocabulary.of(language)
        val phrases = vocabulary.phrases.filter { it.kind in available }
        if (phrases.isEmpty() || phrases.any { it.argument == VoiceArgument.TEXT }) return null
        return phrases.flatMap { phrase ->
            val base = phrase.tokens.joinToString(" ")
            if (phrase.argument == VoiceArgument.DURATION) {
                val preposition = VoiceNormalizer.norm(vocabulary.durationPreposition)
                listOf(base) +
                    vocabulary.numberWords.keys.map(VoiceNormalizer::norm).flatMap { number ->
                        vocabulary.minuteUnits.map { unit ->
                            "$base $preposition $number ${VoiceNormalizer.norm(unit)}"
                        }
                    }
            } else {
                listOf(base)
            }
        }.distinct()
    }
}
