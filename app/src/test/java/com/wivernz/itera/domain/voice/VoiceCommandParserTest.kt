package com.wivernz.itera.domain.voice

import com.wivernz.itera.domain.voice.VoiceCommandKind.ADD_ITEM
import com.wivernz.itera.domain.voice.VoiceCommandKind.COMPLETE_CURRENT_EXERCISE
import com.wivernz.itera.domain.voice.VoiceCommandKind.COMPLETE_ITEM
import com.wivernz.itera.domain.voice.VoiceCommandKind.PAUSE_FOCUS
import com.wivernz.itera.domain.voice.VoiceCommandKind.START_FOCUS
import com.wivernz.itera.domain.voice.VoiceLanguage.DE
import com.wivernz.itera.domain.voice.VoiceLanguage.EN
import com.wivernz.itera.domain.voice.VoiceLanguage.ES
import com.wivernz.itera.domain.voice.VoiceLanguage.RU
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every documented alias in four languages (docs/i18n/00-localization.md section 13). */
class VoiceCommandParserTest {
    private fun ok(language: VoiceLanguage, utterance: String, command: VoiceCommand) =
        assertEquals(
            utterance,
            VoiceParse.Recognised(command),
            VoiceCommandParser.parse(utterance, language)
        )

    private fun parse(language: VoiceLanguage, utterance: String) =
        VoiceCommandParser.parse(utterance, language)

    @Test fun englishAliases() {
        ok(EN, "add task call Anna", VoiceCommand.AddItem("call Anna"))
        ok(EN, "Add item Buy Milk", VoiceCommand.AddItem("Buy Milk"))
        ok(EN, "complete call Anna", VoiceCommand.CompleteItem("call Anna"))
        ok(EN, "start focus", VoiceCommand.StartFocus(null))
        ok(EN, "start focus for 25 minutes", VoiceCommand.StartFocus(25))
        ok(EN, "pause", VoiceCommand.PauseFocus)
        ok(EN, "pause focus", VoiceCommand.PauseFocus)
        ok(EN, "resume", VoiceCommand.ResumeFocus)
        ok(EN, "resume focus", VoiceCommand.ResumeFocus)
        ok(EN, "end focus", VoiceCommand.EndFocus)
        ok(EN, "complete exercise", VoiceCommand.CompleteCurrentExercise)
        ok(EN, "What should I do now?", VoiceCommand.ShowCurrentRecommendation)
    }

    @Test fun russianAliasesAndDocumentedFixtures() {
        ok(RU, "Добавь задачу купить корм", VoiceCommand.AddItem("купить корм"))
        ok(RU, "добавь пункт позвонить маме", VoiceCommand.AddItem("позвонить маме"))
        ok(RU, "Заверши купить корм", VoiceCommand.CompleteItem("купить корм"))
        ok(RU, "Начни фокус", VoiceCommand.StartFocus(null))
        ok(RU, "Начни фокус на 25 минут", VoiceCommand.StartFocus(25))
        ok(RU, "пауза", VoiceCommand.PauseFocus)
        ok(RU, "приостанови фокус", VoiceCommand.PauseFocus)
        ok(RU, "продолжить", VoiceCommand.ResumeFocus)
        ok(RU, "продолжи фокус", VoiceCommand.ResumeFocus)
        ok(RU, "закончи фокус", VoiceCommand.EndFocus)
        ok(RU, "заверши упражнение", VoiceCommand.CompleteCurrentExercise)
        ok(RU, "Что мне делать сейчас?", VoiceCommand.ShowCurrentRecommendation)
    }

    @Test fun germanAliases() {
        ok(DE, "Füge Aufgabe hinzu Anna anrufen", VoiceCommand.AddItem("Anna anrufen"))
        ok(DE, "füge Eintrag hinzu Milch kaufen", VoiceCommand.AddItem("Milch kaufen"))
        ok(DE, "erledige Anna anrufen", VoiceCommand.CompleteItem("Anna anrufen"))
        ok(DE, "starte Fokus", VoiceCommand.StartFocus(null))
        ok(DE, "Starte Fokus für 25 Minuten", VoiceCommand.StartFocus(25))
        ok(DE, "Pause", VoiceCommand.PauseFocus)
        ok(DE, "pausiere Fokus", VoiceCommand.PauseFocus)
        ok(DE, "weiter", VoiceCommand.ResumeFocus)
        ok(DE, "setze Fokus fort", VoiceCommand.ResumeFocus)
        ok(DE, "beende Fokus", VoiceCommand.EndFocus)
        ok(DE, "Schließe Übung ab", VoiceCommand.CompleteCurrentExercise)
        ok(DE, "Was soll ich jetzt tun?", VoiceCommand.ShowCurrentRecommendation)
    }

    @Test fun spanishAliases() {
        ok(ES, "Añade tarea llamar a Ana", VoiceCommand.AddItem("llamar a Ana"))
        ok(ES, "añade elemento comprar leche", VoiceCommand.AddItem("comprar leche"))
        ok(ES, "completa llamar a Ana", VoiceCommand.CompleteItem("llamar a Ana"))
        ok(ES, "inicia enfoque", VoiceCommand.StartFocus(null))
        ok(ES, "Inicia enfoque durante 25 minutos", VoiceCommand.StartFocus(25))
        ok(ES, "pausa", VoiceCommand.PauseFocus)
        ok(ES, "pausa el enfoque", VoiceCommand.PauseFocus)
        ok(ES, "continúa", VoiceCommand.ResumeFocus)
        ok(ES, "reanuda el enfoque", VoiceCommand.ResumeFocus)
        ok(ES, "termina el enfoque", VoiceCommand.EndFocus)
        ok(ES, "completa el ejercicio", VoiceCommand.CompleteCurrentExercise)
        ok(ES, "¿Qué debo hacer ahora?", VoiceCommand.ShowCurrentRecommendation)
    }

    @Test fun argumentsKeepTheirOriginalSpelling() {
        ok(
            EN,
            "  add task   Email the Q3 report to Dr. Kim!  ",
            VoiceCommand.AddItem("Email the Q3 report to Dr. Kim")
        )
        ok(RU, "добавь задачу Купить КОРМ", VoiceCommand.AddItem("Купить КОРМ"))
        // command-looking words inside the argument are data
        ok(
            EN,
            "add task pause the newsletter and end focus",
            VoiceCommand.AddItem("pause the newsletter and end focus")
        )
    }

    @Test fun matchingIsAnchoredOnTokenBoundaries() {
        assertEquals(VoiceParse.Unsupported, parse(EN, "please pause"))
        assertEquals(VoiceParse.Unsupported, parse(EN, "paused"))
        assertEquals(VoiceParse.Unsupported, parse(EN, "additem milk"))
        assertEquals(VoiceParse.Unsupported, parse(EN, "completely done"))
    }

    @Test fun negatedChainedAndUnsupportedInputsNeverRun() {
        assertEquals(VoiceParse.Unsupported, parse(EN, "don't pause"))
        assertEquals(VoiceParse.Unsupported, parse(RU, "не пауза"))
        assertEquals(VoiceParse.InvalidArgument(PAUSE_FOCUS), parse(EN, "pause and end focus"))
        assertEquals(VoiceParse.Unsupported, parse(EN, "delete everything"))
        assertEquals(VoiceParse.Unsupported, parse(EN, ""))
        assertEquals(VoiceParse.Unsupported, parse(EN, "  ?! "))
        // another language's vocabulary is unsupported under this one
        assertEquals(VoiceParse.Unsupported, parse(EN, "начни фокус"))
        assertEquals(VoiceParse.Unsupported, parse(DE, "start focus"))
    }

    @Test fun blankArgumentsAreMissing() {
        assertEquals(VoiceParse.MissingArgument(ADD_ITEM), parse(EN, "add task"))
        assertEquals(VoiceParse.MissingArgument(ADD_ITEM), parse(EN, "add item ..."))
        assertEquals(VoiceParse.MissingArgument(COMPLETE_ITEM), parse(RU, "заверши"))
        assertEquals(VoiceParse.MissingArgument(ADD_ITEM), parse(ES, "añade tarea"))
    }

    @Test fun trailingGarbageOnFixedCommandsFailsWithoutFallingBack() {
        assertEquals(
            VoiceParse.InvalidArgument(COMPLETE_CURRENT_EXERCISE),
            parse(EN, "complete exercise now")
        )
        assertEquals(
            VoiceParse.InvalidArgument(COMPLETE_CURRENT_EXERCISE),
            parse(RU, "заверши упражнение быстро")
        )
        assertEquals(
            VoiceParse.InvalidArgument(COMPLETE_CURRENT_EXERCISE),
            parse(ES, "completa el ejercicio ya")
        )
        assertEquals(VoiceParse.InvalidArgument(START_FOCUS), parse(EN, "start focus now"))
        assertEquals(VoiceParse.InvalidArgument(PAUSE_FOCUS), parse(DE, "Pause bitte"))
        assertEquals(
            VoiceParse.InvalidArgument(VoiceCommandKind.SHOW_CURRENT_RECOMMENDATION),
            parse(EN, "what should I do now please")
        )
    }

    @Test fun competingAlternativesAreOfferedNotExecuted() {
        val competing = VoiceCommandParser.interpret(listOf("pause", "end focus"), EN)
        assertEquals(
            VoiceInterpretation.Competing(listOf(VoiceCommand.PauseFocus, VoiceCommand.EndFocus)),
            competing
        )
        // differing arguments compete too
        assertEquals(
            VoiceInterpretation.Competing(
                listOf(VoiceCommand.AddItem("buy milk"), VoiceCommand.AddItem("buy silk"))
            ),
            VoiceCommandParser.interpret(listOf("add task buy milk", "add task buy silk"), EN)
        )
        // a top alternative that is not a command never lets a lower one run silently
        assertEquals(
            VoiceInterpretation.Competing(listOf(VoiceCommand.PauseFocus)),
            VoiceCommandParser.interpret(listOf("pours", "pause"), EN)
        )
    }

    @Test fun agreeingAlternativesRunTheTopOne() {
        assertEquals(
            VoiceInterpretation.Single(VoiceParse.Recognised(VoiceCommand.AddItem("Buy milk"))),
            VoiceCommandParser.interpret(
                listOf("add task Buy milk", "add task buy milk.", "at task by milk"),
                EN
            )
        )
        assertEquals(
            VoiceInterpretation.Single(VoiceParse.Unsupported),
            VoiceCommandParser.interpret(listOf("hello", "hullo"), EN)
        )
        assertEquals(
            VoiceInterpretation.Single(VoiceParse.Unsupported),
            VoiceCommandParser.interpret(emptyList(), EN)
        )
    }
}
