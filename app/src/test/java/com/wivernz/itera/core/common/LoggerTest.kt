package com.wivernz.itera.core.common
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
@RunWith(RobolectricTestRunner::class)
class LoggerTest {
    @Test fun releaseDropsVerboseAndDebug() {
        ShadowLog.clear()
        val logger = ReleaseLogger()
        logger.v("test", "v")
        logger.d("test", "d")
        logger.i("test", "i")
        logger.w("test", "w")
        logger.e("test", "e")
        assertEquals(
            listOf(
                "i",
                "w",
                "e"
            ),
            ShadowLog.getLogsForTag("test").map { it.msg }
        )
    }
}
