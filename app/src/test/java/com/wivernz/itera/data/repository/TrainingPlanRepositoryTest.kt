package com.wivernz.itera.data.repository
import app.cash.turbine.test
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.data.activityEntity
import com.wivernz.itera.data.dayEntity
import com.wivernz.itera.data.fixtureDate
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.data.testCopy
import com.wivernz.itera.data.testDatabase
import com.wivernz.itera.domain.model.ActivityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class TrainingPlanRepositoryTest {
    @Test fun observesUpdatesDerivesPracticeDateAndCascades() = runTest {
        val db = testDatabase()
        try {
            val day = db.trainingDayDao().insert(dayEntity())
            val id =
                db.planActivityDao()
                    .insertAll(listOf(activityEntity(day)))
                    .single()
            val repository = RoomTrainingPlanRepository(
                db,
                testCopy(),
                ResultPayloadCodec(TestLogger()),
                FakeClock(),
                Dispatchers.IO
            )
            repository.observeToday().test {
                assertEquals(
                    ActivityState.AVAILABLE,
                    awaitItem()!!.activities.single()
                        .state
                )
                repository.updateActivityState(id, ActivityState.COMPLETED)
                assertEquals(
                    ActivityState.COMPLETED,
                    awaitItem()!!.activities.single()
                        .state
                )
                assertEquals(
                    fixtureDate.toEpochDay(),
                    db.planActivityDao().byId(id)!!.practiceDate
                )
                db.trainingDayDao().delete(day)
                assertNull(awaitItem())
                assertNull(db.planActivityDao().byId(id))
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            db.close()
        }
    }
}
