package com.wivernz.itera.data.repository
import com.wivernz.itera.data.activityEntity
import com.wivernz.itera.data.dayEntity
import com.wivernz.itera.data.fixtureDate
import com.wivernz.itera.data.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class ProgressQueryTest {
    @Test fun aggregatesCountRealCompletedPracticesOnly() = runTest {
        val db = testDatabase()
        try {
            val d1 = db.trainingDayDao().insert(dayEntity())
            val d2 = db.trainingDayDao().insert(dayEntity(fixtureDate.plusDays(1)))
            val dao = db.planActivityDao()
            dao.insertAll(
                listOf(
                    activityEntity(
                        d1,
                        "COMPLETED"
                    ),
                    activityEntity(
                        d1,
                        "COMPLETED"
                    ),
                    activityEntity(
                        d1,
                        "SKIPPED"
                    ),
                    activityEntity(
                        d2,
                        "COMPLETED"
                    ).copy(source = "COMBINATION"),
                    activityEntity(
                        d2,
                        "COMPLETED"
                    ).copy(copyKey = "activity_focus_generic")
                )
            )
            assertEquals(2, dao.countDistinctPracticeDays("two_minute_rule").first())
            assertEquals(
                3,
                dao.usesSince(
                    "two_minute_rule",
                    fixtureDate.toEpochDay()
                ).first()
            )
            assertEquals(
                1,
                dao.usesSince(
                    "two_minute_rule",
                    fixtureDate.plusDays(1).toEpochDay()
                ).first()
            )
            assertEquals(
                fixtureDate.toEpochDay(),
                dao.firstUse("two_minute_rule").first()
            )
            assertTrue(dao.usedInCombination("two_minute_rule").first())
            assertEquals(0, dao.countDistinctPracticeDays("unknown").first())
            assertNull(dao.firstUse("unknown").first())
        } finally {
            db.close()
        }
    }
}
