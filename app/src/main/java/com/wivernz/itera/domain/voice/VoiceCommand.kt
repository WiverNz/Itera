package com.wivernz.itera.domain.voice

/**
 * The effective Itera UI language, which also selects the recogniser language and the parser table
 * (docs/i18n/00-localization.md section 13). Anything else falls back to English, as the UI does.
 */
enum class VoiceLanguage(val tag: String) {
    EN("en-US"),
    RU("ru-RU"),
    DE("de-DE"),
    ES("es-ES");

    companion object {
        fun of(languageCode: String?): VoiceLanguage = when (languageCode?.lowercase()) {
            "ru" -> RU
            "de" -> DE
            "es" -> ES
            else -> EN
        }
    }
}

/** The eight typed commands (FR-45). */
enum class VoiceCommandKind {
    ADD_ITEM,
    COMPLETE_ITEM,
    START_FOCUS,
    PAUSE_FOCUS,
    RESUME_FOCUS,
    END_FOCUS,
    COMPLETE_CURRENT_EXERCISE,
    SHOW_CURRENT_RECOMMENDATION
}

sealed interface VoiceCommand {
    val kind: VoiceCommandKind

    /** [text] is the untouched argument span of the utterance. */
    data class AddItem(val text: String) : VoiceCommand {
        override val kind get() = VoiceCommandKind.ADD_ITEM
    }

    data class CompleteItem(val query: String) : VoiceCommand {
        override val kind get() = VoiceCommandKind.COMPLETE_ITEM
    }

    /** [minutes] is null when no duration was said; otherwise a parsed, not yet validated, whole minute count. */
    data class StartFocus(val minutes: Int?) : VoiceCommand {
        override val kind get() = VoiceCommandKind.START_FOCUS
    }

    data object PauseFocus : VoiceCommand {
        override val kind get() = VoiceCommandKind.PAUSE_FOCUS
    }

    data object ResumeFocus : VoiceCommand {
        override val kind get() = VoiceCommandKind.RESUME_FOCUS
    }

    data object EndFocus : VoiceCommand {
        override val kind get() = VoiceCommandKind.END_FOCUS
    }

    data object CompleteCurrentExercise : VoiceCommand {
        override val kind get() = VoiceCommandKind.COMPLETE_CURRENT_EXERCISE
    }

    data object ShowCurrentRecommendation : VoiceCommand {
        override val kind get() = VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION
    }
}

/** One final utterance under one language. Only [Recognised] may lead to an action. */
sealed interface VoiceParse {
    data class Recognised(val command: VoiceCommand) : VoiceParse

    /** A command phrase with nothing after it where text is required, e.g. "add task". */
    data class MissingArgument(val kind: VoiceCommandKind) : VoiceParse

    /** A reserved phrase followed by something it does not accept; never falls back to a shorter command. */
    data class InvalidArgument(val kind: VoiceCommandKind) : VoiceParse

    data object Unsupported : VoiceParse
}

/** How the recogniser's alternatives read as commands. */
sealed interface VoiceInterpretation {
    data class Single(val parse: VoiceParse) : VoiceInterpretation

    /** Alternatives imply different commands or arguments: the user picks one, nothing runs silently. */
    data class Competing(val commands: List<VoiceCommand>) : VoiceInterpretation
}
