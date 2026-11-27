package com.itera.app.data

import org.junit.Assert.*
import org.junit.Test

class PredictionStateTest {
    @Test fun preparationRequiresPredictionActionAndEvidence() {
        val empty = PredictionState()
        assertEquals(empty, empty.advance("2026-11-26"))
        val prediction = empty.copy(prediction = "I will get stuck").advance("2026-11-26")
        assertEquals(1, prediction.page)
        assertEquals(prediction, prediction.advance("2026-11-26"))
        val ready = prediction.copy(action = "Outline for two minutes", evidence = "Count headings").advance("2026-11-26")
        assertTrue(ready.frozen)
        assertEquals("2026-11-26", ready.preparedDate)
        assertEquals(ready, ready.previous())
    }

    @Test fun everyEvidenceDirectionCanFinishWithoutChangingConfidence() {
        for (comparison in 0..3) {
            val review = PredictionState(page = 3, prediction = "I will get stuck",
                preparedDate = "2026-11-26", outcome = "Two headings",
                confidenceBefore = 3, confidenceAfter = 3)
                .advance("2026-11-27").copy(comparison = comparison)
                .advance("2026-11-27").copy(noConclusion = true)
                .advance("2026-11-27")
            assertTrue(review.finished)
            assertEquals(3, review.confidenceAfter)
            assertEquals("2026-11-26", review.preparedDate)
            assertEquals(review, review.advance("2026-11-28"))
        }
    }

    @Test fun observationsAndComparisonAreRequiredButConfidenceIsOptional() {
        val emptyOutcome = PredictionState(page = 3, preparedDate = "2026-11-26")
        assertFalse(emptyOutcome.canAdvance)
        assertFalse(emptyOutcome.copy(page = 4).canAdvance)
        assertFalse(emptyOutcome.copy(page = 5).canAdvance)
        assertTrue(emptyOutcome.copy(page = 5, lesson = "Try a shorter task").canAdvance)
    }

    @Test fun closingDoesNotInventAnOutcomeOrCompletion() {
        val closed = PredictionState(prediction = "Starting will be hard", closed = true)
        assertTrue(closed.finished)
        assertEquals("", closed.outcome)
        assertEquals(-1, closed.comparison)
        assertEquals(closed, closed.advance("2026-11-27"))
    }
}

