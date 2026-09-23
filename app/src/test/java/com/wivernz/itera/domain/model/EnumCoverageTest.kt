package com.wivernz.itera.domain.model
import org.junit.Assert.assertEquals
import org.junit.Test
class EnumCoverageTest {
    @Test fun constantsMatchDocument() {
        assertEquals(
            "FOCUS PLANNING LEARNING HABITS REFLECTION".split(" "),
            Skill.entries.map { it.name }
        )
        assertEquals(
            (
                "TEMPLATE FOCUS_TIMER EISENHOWER FEYNMAN PREMORTEM " +
                    "HABIT_STACK REFLECTION REVIEW COMBINATION"
                ).split(
                " "
            ),
            ExerciseType.entries.map { it.name }
        )
        assertEquals(
            "SCHEDULED AVAILABLE IN_PROGRESS SNOOZED COMPLETED SKIPPED EXPIRED".split(" "),
            ActivityState.entries.map { it.name }
        )
        assertEquals(
            "PROGRAM REVIEW PRACTICE_PROMPT FOCUS_SUGGESTION REFLECTION COMBINATION MANUAL".split(
                " "
            ),
            ActivitySource.entries.map { it.name }
        )
        assertEquals(
            "MORNING DAYTIME EVENING".split(" "),
            DayPart.entries.map { it.name }
        )
        assertEquals("EASY OKAY HARD".split(" "), Difficulty.entries.map { it.name })
        assertEquals(
            "NONE MET PRACTICED APPLIED INTEGRATED".split(" "),
            MasteryLevel.entries.map { it.name }
        )
        assertEquals(
            "STARTING BUILDING STEADY STRONG".split(" "),
            SkillLevel.entries.map { it.name }
        )
        assertEquals(
            "FORGOT PARTIAL SOLID".split(" "),
            RecallGrade.entries.map { it.name }
        )
        assertEquals(
            "PLANNED IN_PROGRESS COMPLETE ABANDONED".split(" "),
            TrainingDayStatus.entries.map { it.name }
        )
        assertEquals(
            "GENTLE STANDARD INTENSE".split(" "),
            ProgramPace.entries.map { it.name }
        )
        assertEquals(
            "SYSTEM LIGHT DARK".split(" "),
            ThemePreference.entries.map { it.name }
        )
        assertEquals(
            "SHORT STANDARD LONG".split(" "),
            TimeBudget.entries.map { it.name }
        )
        assertEquals(
            "UNSORTED DO_NOW SCHEDULE DELEGATE DROP".split(" "),
            Quadrant.entries.map { it.name }
        )
        assertEquals(
            "POSSIBLE LIKELY CERTAIN".split(" "),
            Likelihood.entries.map { it.name }
        )
        assertEquals(
            "SCHEDULED DUE COMPLETED_STAGE RETIRED".split(" "),
            ReviewState.entries.map { it.name }
        )
        assertEquals(listOf(5, 15, 30), TimeBudget.entries.map { it.minutes })
    }
}
