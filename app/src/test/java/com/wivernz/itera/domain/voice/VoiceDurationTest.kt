package com.wivernz.itera.domain.voice

import com.wivernz.itera.domain.voice.VoiceLanguage.DE
import com.wivernz.itera.domain.voice.VoiceLanguage.EN
import com.wivernz.itera.domain.voice.VoiceLanguage.ES
import com.wivernz.itera.domain.voice.VoiceLanguage.RU
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** StartFocus durations: pinned forms only; nothing defaults, rounds or guesses. */
class VoiceDurationTest {
    private fun start(language: VoiceLanguage, utterance: String) =
        VoiceCommandParser.parse(utterance, language)

    private fun minutes(language: VoiceLanguage, utterance: String): Int? = (
        (
            start(
                language,
                utterance
            ) as? VoiceParse.Recognised
            )?.command as? VoiceCommand.StartFocus
        )
        ?.minutes

    private fun invalid(language: VoiceLanguage, utterance: String) = assertEquals(
        utterance,
        VoiceParse.InvalidArgument(VoiceCommandKind.START_FOCUS),
        start(language, utterance)
    )

    @Test fun omittedDurationKeepsTheSuggestion() {
        assertEquals(VoiceParse.Recognised(VoiceCommand.StartFocus(null)), start(EN, "start focus"))
        assertEquals(VoiceParse.Recognised(VoiceCommand.StartFocus(null)), start(RU, "начни фокус"))
    }

    @Test fun digitsInEveryLanguage() {
        assertEquals(15, minutes(EN, "start focus for 15 minutes"))
        assertEquals(50, minutes(RU, "начни фокус на 50 минут"))
        assertEquals(25, minutes(DE, "starte Fokus für 25 Minuten"))
        assertEquals(50, minutes(ES, "inicia enfoque durante 50 minutos"))
    }

    @Test fun everyMinuteUnitVariant() {
        assertEquals(1, minutes(EN, "start focus for 1 minute"))
        assertEquals(1, minutes(RU, "начни фокус на 1 минуту"))
        assertEquals(21, minutes(RU, "начни фокус на 21 минута"))
        assertEquals(3, minutes(RU, "начни фокус на 3 минуты"))
        assertEquals(1, minutes(DE, "starte Fokus für 1 Minute"))
        assertEquals(1, minutes(ES, "inicia enfoque durante 1 minuto"))
    }

    @Test fun spokenSetupChoicesAndSeeds() {
        assertEquals(15, minutes(EN, "start focus for fifteen minutes"))
        assertEquals(25, minutes(EN, "start focus for twenty five minutes"))
        assertEquals(25, minutes(EN, "start focus for twenty-five minutes"))
        assertEquals(30, minutes(EN, "start focus for thirty minutes"))
        assertEquals(50, minutes(EN, "start focus for fifty minutes"))
        assertEquals(15, minutes(RU, "начни фокус на пятнадцать минут"))
        assertEquals(25, minutes(RU, "начни фокус на двадцать пять минут"))
        assertEquals(30, minutes(RU, "начни фокус на тридцать минут"))
        assertEquals(50, minutes(RU, "начни фокус на пятьдесят минут"))
        assertEquals(15, minutes(DE, "starte Fokus für fünfzehn Minuten"))
        assertEquals(25, minutes(DE, "starte Fokus für fünfundzwanzig Minuten"))
        assertEquals(30, minutes(DE, "starte Fokus für dreißig Minuten"))
        assertEquals(50, minutes(DE, "starte Fokus für fünfzig Minuten"))
        assertEquals(15, minutes(ES, "inicia enfoque durante quince minutos"))
        assertEquals(25, minutes(ES, "inicia enfoque durante veinticinco minutos"))
        assertEquals(30, minutes(ES, "inicia enfoque durante treinta minutos"))
        assertEquals(50, minutes(ES, "inicia enfoque durante cincuenta minutos"))
    }

    @Test fun malformedDurationsAreInvalidNotDefaulted() {
        invalid(EN, "start focus for 0 minutes")
        invalid(EN, "start focus for -5 minutes")
        invalid(EN, "start focus for 2.5 minutes")
        invalid(RU, "начни фокус на 2,5 минуты")
        invalid(EN, "start focus for 99999999999 minutes")
        invalid(EN, "start focus for 601 minutes")
        invalid(EN, "start focus for 25")
        invalid(EN, "start focus 25 minutes")
        invalid(EN, "start focus for 1 hour")
        invalid(EN, "start focus for 25 minutes 5 minutes")
        invalid(EN, "start focus for 25 and 50 minutes")
        invalid(EN, "start focus for half an hour minutes")
        invalid(EN, "start focus for twenty minutes")
        invalid(DE, "starte Fokus für 25 Sekunden")
        invalid(ES, "inicia enfoque por 25 minutos")
    }

    @Test fun parsingIsSeparateFromSetupValidation() {
        // a valid number the setup may not offer is still parsed; the setup decides later
        assertEquals(40, minutes(EN, "start focus for 40 minutes"))
        assertEquals(600, VoiceDuration.parse("for 600 minutes", EN))
        assertNull(VoiceDuration.parse("for 25 minutes please", EN))
    }
}
