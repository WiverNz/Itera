package com.wivernz.itera.domain.model
import com.wivernz.itera.TestLogger
import com.wivernz.itera.data.activityEntity
import com.wivernz.itera.data.fixtureDate
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.mapper.toDomain
import com.wivernz.itera.data.testCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class DomainInvariantsTest {
    @Test fun countersIncludeOptionalCompletionsButRequiredCountDoesNot() {
        val copy = testCopy()
        val codec = ResultPayloadCodec(TestLogger())
        val base = activityEntity(1).toDomain(copy, codec)
        listOf(
            emptyList(),
            listOf(
                true,
                true
            ),
            listOf(
                false,
                false
            ),
            listOf(
                false,
                true
            )
        ).forEach { flags ->
            val activities =
                flags.map {
                    base.copy(
                        optional =
                        it,
                        state =
                        ActivityState.COMPLETED
                    )
                }
            val day = TrainingDay(
                1,
                1,
                fixtureDate,
                TrainingDayStatus.PLANNED,
                null,
                activities,
                null
            )
            assertEquals(flags.count { !it }, day.requiredCount)
            assertEquals(flags.size, day.completedCount)
            ActivityState.entries.forEach { state ->
                assertEquals(
                    state == ActivityState.COMPLETED,
                    base.copy(state = state).isCountedComplete
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) { base.copy(reviewItemId = 1) }
        assertThrows(IllegalArgumentException::class.java) {
            base.copy(
                estimatedMinutes =
                -1
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(
                currentProgramDay =
                0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            UserPreferences(
                aiCoachEnabled =
                true
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReviewItem(
                1,
                TechniqueId("feynman"),
                null,
                "p",
                1,
                "a",
                5,
                fixtureDate,
                null,
                ReviewState.DUE
            )
        }
    }

    @Test fun catalogTemplatesAndProgressRetainTheirContract() {
        val blocks = listOf(
            ExerciseBlock.Instruction(
                "i",
                "text",
                true
            ),
            ExerciseBlock.TextInput(
                "t",
                "label",
                "placeholder",
                1,
                4
            ),
            ExerciseBlock.Checklist(
                "c",
                "label",
                1,
                3,
                true,
                "add"
            ),
            ExerciseBlock.PickOne(
                "p",
                "label",
                3,
                listOf("one")
            ),
            ExerciseBlock.TwoLists(
                "l",
                "primary",
                "secondary",
                3,
                4
            ),
            ExerciseBlock.ChipSelect(
                "s",
                "label",
                listOf("one"),
                true,
                2
            )
        )
        assertEquals(listOf("i", "t", "c", "p", "l", "s"), blocks.map { it.key })
        val templates = listOf(
            CompletionRule.Always,
            CompletionRule.RequireBlocks(listOf("t")),
            CompletionRule.RequireChecked(
                "c",
                1
            )
        ).map { ExerciseTemplate(blocks, it) }
        val defaults =
            TechniqueDefaults(
                25,
                5,
                listOf("chip"),
                listOf("anchor"),
                listOf("habit")
            )
        val technique = Technique(
            TechniqueId("two_minute_rule"),
            "name",
            "short",
            "why",
            Skill.HABITS,
            1,
            ExerciseType.TEMPLATE,
            5,
            false,
            emptyList(),
            templates.first(),
            defaults
        )
        assertEquals(25, technique.defaults.focusMinutes)
        val step = CombinationStep(technique.id, "prompt", "hint")
        val curriculum = Curriculum(
            1,
            listOf(
                CurriculumDay(
                    1,
                    technique.id,
                    emptyList(),
                    false
                ),
                CurriculumDay(
                    14,
                    null,
                    listOf(step),
                    true
                )
            )
        )
        assertTrue(curriculum.days.last().weeklyLookBack)
        val progress = TechniqueProgress(
            technique.id,
            true,
            1,
            MasteryLevel.MET,
            0,
            0,
            null,
            null,
            false,
            "hint"
        )
        val skill = SkillProgress(Skill.HABITS, SkillLevel.STARTING, 1, 1, 14, "detail")
        val summary = ProgressSummary(
            1,
            14,
            fixtureDate,
            listOf(
                DayDot(
                    fixtureDate,
                    true,
                    setOf(Skill.HABITS)
                )
            ),
            listOf(skill),
            1
        )
        assertEquals(1, summary.skills.single().practiceCount)
        assertFalse(progress.usedInCombination)
    }
}
