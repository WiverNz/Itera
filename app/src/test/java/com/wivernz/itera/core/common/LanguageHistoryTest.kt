package com.wivernz.itera.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.data.catalog.AssetTechniqueCatalogRepository
import com.wivernz.itera.data.catalog.ResourceCatalogStrings
import com.wivernz.itera.data.fileAssets
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.MemoryCatalogCache
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanguageHistoryTest {
    @Test fun weekOfHistoryRelocalizesWithoutChangingUserResults(): Unit = runBlocking {
        RuntimeEnvironment.setQualifiers("en")
        val h = EngineHarness()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val catalog = AssetTechniqueCatalogRepository(
            fileAssets,
            ResourceCatalogStrings(context),
            MemoryCatalogCache(),
            RuntimeChecks(true),
            TestLogger(),
            Dispatchers.IO
        )
        try {
            repeat(7) {
                h.trainFullDay()
                if (it < 6) h.nextMorning()
            }
            val english = h.progress.observeHistory(YearMonth.from(h.today)).first()
            val names = catalog.catalog().associate { it.id to it.name }
            for (language in listOf("de", "ru", "es")) {
                RuntimeEnvironment.setQualifiers(language)
                val translated = h.progress.observeHistory(YearMonth.from(h.today)).first()
                assertEquals(english.map { it.activity.id }, translated.map { it.activity.id })
                assertEquals(
                    english.map {
                        it.activity.result
                    },
                    translated.map { it.activity.result }
                )
                assertNotEquals(
                    english.map {
                        it.activity.subtitle
                    },
                    translated.map { it.activity.subtitle }
                )
                assertNotEquals(names, catalog.catalog().associate { it.id to it.name })
                val today = h.plans.dayByDate(h.today)!!
                assertEquals(
                    translated.filter {
                        it.date == h.today
                    }.map { it.activity.title }.toSet(),
                    today.activities.map { it.title }.toSet()
                )
            }
        } finally {
            h.close()
            RuntimeEnvironment.setQualifiers("en")
        }
    }
}
