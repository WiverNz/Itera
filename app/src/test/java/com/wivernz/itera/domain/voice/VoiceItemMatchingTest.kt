package com.wivernz.itera.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceItemMatchingTest {
    private fun items(vararg labels: String) =
        labels.mapIndexed { i, label -> VoiceItem("${i + 1}", label, i + 1) }

    @Test fun uniqueExactMatchIgnoresCaseAndOuterPunctuation() {
        val list = items("Reply to Anna", "Buy milk")
        assertEquals(VoiceMatch.Unique(list[1]), VoiceItemMatcher.match("buy milk.", list))
        assertEquals(VoiceMatch.Unique(list[0]), VoiceItemMatcher.match("REPLY TO ANNA", list))
    }

    @Test fun exactMatchWinsOverPartialOnes() {
        val list = items("Call", "Call Anna", "Call the bank")
        assertEquals(VoiceMatch.Unique(list[0]), VoiceItemMatcher.match("call", list))
    }

    @Test fun duplicateExactLabelsNeedAChoice() {
        val list = items("Buy milk", "Water plants", "buy milk")
        assertEquals(
            VoiceMatch.Choose(listOf(list[0], list[2])),
            VoiceItemMatcher.match("Buy milk", list)
        )
    }

    @Test fun partialMatchesAreWholeContiguousTokensAndAlwaysNeedAChoice() {
        val list = items("Email the Q3 report", "Report bug", "Call Anna about the report")
        assertEquals(VoiceMatch.Choose(list), VoiceItemMatcher.match("report", list))
        assertEquals(VoiceMatch.Choose(listOf(list[0])), VoiceItemMatcher.match("the Q3", list))
        // not contiguous, not a substring of a token, not fuzzy
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("email report", list))
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("repo", list))
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("reprot", list))
    }

    @Test fun noMatchAndBlankQueries() {
        val list = items("Buy milk")
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("walk the dog", list))
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("  ", list))
        assertEquals(VoiceMatch.None, VoiceItemMatcher.match("milk", emptyList()))
    }

    @Test fun cyrillicNormalisation() {
        val list = items("Купить корм", "Позвонить маме")
        assertEquals(VoiceMatch.Unique(list[0]), VoiceItemMatcher.match("купить корм", list))
        assertEquals(
            VoiceMatch.Unique(VoiceItem("1", "Ещё раз", 1)),
            VoiceItemMatcher.match("еще раз", items("Ещё раз"))
        )
    }

    @Test fun dictatedTextIsInsertedOnceAtTheSelection() {
        assertEquals(VoiceInsertion("Buy milk", 8), VoiceText.insert("", 0, 0, " Buy milk "))
        assertEquals(VoiceInsertion("Buy oat milk", 7), VoiceText.insert("Buy milk", 3, 3, "oat"))
        assertEquals(
            VoiceInsertion("Buy oat milk", 7),
            VoiceText.insert("Buy cow milk", 4, 7, "oat")
        )
        assertEquals(VoiceInsertion("Done.", 5), VoiceText.insert("Done", 4, 4, "."))
        assertEquals(
            VoiceInsertion("Call Anna, then", 9),
            VoiceText.insert("Call, then", 4, 4, "Anna")
        )
    }
}
