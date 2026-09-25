package com.wivernz.itera.domain.progress

import com.wivernz.itera.domain.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/** D-09 bands: `score = practiceCount + activeDays`, Strong >= 20, Steady >= 10, Building >= 4. */
class SkillLevelTest {
    private fun score(total: Int) = SkillLevels.skillLevel(SkillFacts(total, 0))

    @Test fun everyBoundaryFromBothSides() {
        assertEquals(SkillLevel.STARTING, score(0))
        assertEquals(SkillLevel.STARTING, score(1))
        assertEquals(SkillLevel.STARTING, score(3))
        assertEquals(SkillLevel.BUILDING, score(4))
        assertEquals(SkillLevel.BUILDING, score(9))
        assertEquals(SkillLevel.STEADY, score(10))
        assertEquals(SkillLevel.STEADY, score(19))
        assertEquals(SkillLevel.STRONG, score(20))
    }

    @Test fun zeroPracticesIsStarting() {
        assertEquals(SkillLevel.STARTING, SkillLevels.skillLevel(SkillFacts(0, 0)))
    }

    @Test fun spreadingBeatsCramming() {
        // eight practices crammed into one day vs five practices on five days
        assertEquals(
            SkillLevel.BUILDING,
            SkillLevels.skillLevel(SkillFacts(practiceCount = 8, activeDays = 1))
        )
        assertEquals(
            SkillLevel.STEADY,
            SkillLevels.skillLevel(SkillFacts(practiceCount = 5, activeDays = 5))
        )
    }
}
