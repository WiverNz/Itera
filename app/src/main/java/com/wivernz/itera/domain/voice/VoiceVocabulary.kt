package com.wivernz.itera.domain.voice

/** What follows a command phrase. */
internal enum class VoiceArgument { NONE, TEXT, DURATION }

internal class VoicePhrase(
    val kind: VoiceCommandKind,
    phrase: String,
    val argument: VoiceArgument
) {
    val tokens: List<String> = VoiceNormalizer.tokens(phrase).map { it.norm }
}

/**
 * The normative command vocabulary (docs/i18n/00-localization.md section 13). Slash-separated entries there are
 * explicit aliases here; nothing else is accepted. Parser data, not UI resources.
 */
internal class VoiceVocabulary(
    val phrases: List<VoicePhrase>,
    // StartFocus with a duration: "{preposition} {n} {unit}"
    val durationPreposition: String,
    val minuteUnits: Set<String>,
    // spelled-out setup choices (15/25/50) and seeded suggestions (25/30/50)
    val numberWords: Map<String, Int>
) {
    companion object {
        private fun p(
            kind: VoiceCommandKind,
            phrase: String,
            argument: VoiceArgument = VoiceArgument.NONE
        ) = VoicePhrase(kind, phrase, argument)

        private val EN = VoiceVocabulary(
            listOf(
                p(VoiceCommandKind.ADD_ITEM, "add task", VoiceArgument.TEXT),
                p(VoiceCommandKind.ADD_ITEM, "add item", VoiceArgument.TEXT),
                p(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE, "complete exercise"),
                p(VoiceCommandKind.COMPLETE_ITEM, "complete", VoiceArgument.TEXT),
                p(VoiceCommandKind.START_FOCUS, "start focus", VoiceArgument.DURATION),
                p(VoiceCommandKind.PAUSE_FOCUS, "pause"),
                p(VoiceCommandKind.PAUSE_FOCUS, "pause focus"),
                p(VoiceCommandKind.RESUME_FOCUS, "resume"),
                p(VoiceCommandKind.RESUME_FOCUS, "resume focus"),
                p(VoiceCommandKind.END_FOCUS, "end focus"),
                p(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION, "what should I do now")
            ),
            durationPreposition = "for",
            minuteUnits = setOf("minute", "minutes"),
            numberWords = mapOf("fifteen" to 15, "twenty five" to 25, "thirty" to 30, "fifty" to 50)
        )

        private val RU = VoiceVocabulary(
            listOf(
                p(VoiceCommandKind.ADD_ITEM, "добавь задачу", VoiceArgument.TEXT),
                p(VoiceCommandKind.ADD_ITEM, "добавь пункт", VoiceArgument.TEXT),
                p(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE, "заверши упражнение"),
                p(VoiceCommandKind.COMPLETE_ITEM, "заверши", VoiceArgument.TEXT),
                p(VoiceCommandKind.START_FOCUS, "начни фокус", VoiceArgument.DURATION),
                p(VoiceCommandKind.PAUSE_FOCUS, "пауза"),
                p(VoiceCommandKind.PAUSE_FOCUS, "приостанови фокус"),
                p(VoiceCommandKind.RESUME_FOCUS, "продолжить"),
                p(VoiceCommandKind.RESUME_FOCUS, "продолжи фокус"),
                p(VoiceCommandKind.END_FOCUS, "закончи фокус"),
                p(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION, "что мне делать сейчас")
            ),
            durationPreposition = "на",
            minuteUnits = setOf("минута", "минуту", "минуты", "минут"),
            numberWords = mapOf(
                "пятнадцать" to 15,
                "двадцать пять" to 25,
                "тридцать" to 30,
                "пятьдесят" to 50
            )
        )

        private val DE = VoiceVocabulary(
            listOf(
                p(VoiceCommandKind.ADD_ITEM, "füge Aufgabe hinzu", VoiceArgument.TEXT),
                p(VoiceCommandKind.ADD_ITEM, "füge Eintrag hinzu", VoiceArgument.TEXT),
                p(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE, "schließe Übung ab"),
                p(VoiceCommandKind.COMPLETE_ITEM, "erledige", VoiceArgument.TEXT),
                p(VoiceCommandKind.START_FOCUS, "starte Fokus", VoiceArgument.DURATION),
                p(VoiceCommandKind.PAUSE_FOCUS, "Pause"),
                p(VoiceCommandKind.PAUSE_FOCUS, "pausiere Fokus"),
                p(VoiceCommandKind.RESUME_FOCUS, "weiter"),
                p(VoiceCommandKind.RESUME_FOCUS, "setze Fokus fort"),
                p(VoiceCommandKind.END_FOCUS, "beende Fokus"),
                p(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION, "was soll ich jetzt tun")
            ),
            durationPreposition = "für",
            minuteUnits = setOf("minute", "minuten"),
            numberWords = mapOf(
                "fünfzehn" to 15,
                "fünfundzwanzig" to 25,
                "dreißig" to 30,
                "fünfzig" to 50
            )
        )

        private val ES = VoiceVocabulary(
            listOf(
                p(VoiceCommandKind.ADD_ITEM, "añade tarea", VoiceArgument.TEXT),
                p(VoiceCommandKind.ADD_ITEM, "añade elemento", VoiceArgument.TEXT),
                p(VoiceCommandKind.COMPLETE_CURRENT_EXERCISE, "completa el ejercicio"),
                p(VoiceCommandKind.COMPLETE_ITEM, "completa", VoiceArgument.TEXT),
                p(VoiceCommandKind.START_FOCUS, "inicia enfoque", VoiceArgument.DURATION),
                p(VoiceCommandKind.PAUSE_FOCUS, "pausa"),
                p(VoiceCommandKind.PAUSE_FOCUS, "pausa el enfoque"),
                p(VoiceCommandKind.RESUME_FOCUS, "continúa"),
                p(VoiceCommandKind.RESUME_FOCUS, "reanuda el enfoque"),
                p(VoiceCommandKind.END_FOCUS, "termina el enfoque"),
                p(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION, "¿qué debo hacer ahora?")
            ),
            durationPreposition = "durante",
            minuteUnits = setOf("minuto", "minutos"),
            numberWords = mapOf(
                "quince" to 15,
                "veinticinco" to 25,
                "treinta" to 30,
                "cincuenta" to 50
            )
        )

        fun of(language: VoiceLanguage): VoiceVocabulary = when (language) {
            VoiceLanguage.EN -> EN
            VoiceLanguage.RU -> RU
            VoiceLanguage.DE -> DE
            VoiceLanguage.ES -> ES
        }
    }
}
