package com.wivernz.itera.feature.progress

import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.progress.SkillFacts
import com.wivernz.itera.domain.progress.SkillLevels
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.subscribe
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

open class ProgressTestBase {
    @get:Rule val main = MainDispatcherRule()
    protected lateinit var h: EngineHarness

    @Before fun setup() {
        h = EngineHarness()
        runBlocking { h.ensureToday() }
    }

    @After fun close() {
        main.clearViewModels()
        h.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class ProgressViewModelTest : ProgressTestBase() {
    @Test fun firstDayGapsAndWindowCapReadRealFacts() = runBlocking {
        val vm = main.track(ProgressViewModel(h.observeProgress, h.prefs, h.analytics))
        val stop = vm.state.subscribe()
        try {
            val first = vm.state.await { !it.loading }.summary!!
            assertEquals(0, first.trainedDays)
            assertEquals(Skill.entries, first.skills.map { it.skill })
            assertTrue(first.skills.all { it.practiceCount == 0 })
            h.trainFullDay()
            assertEquals(1, vm.state.await { it.summary?.trainedDays == 1 }.summary!!.trainedDays)
            repeat(3) { h.nextMorning() }
            vm.refresh()
            val gaps = vm.state.await { it.summary?.windowDays == 4 }.summary!!
            assertEquals(listOf(true, false, false, false), gaps.dayDots.map { it.trained })
            repeat(15) { h.nextMorning() }
            vm.refresh()
            val capped = vm.state.await { it.summary?.windowDays == 14 }.summary!!
            assertEquals(0, capped.trainedDays)
            assertTrue(capped.activityCount > 0)
        } finally {
            stop()
        }
    }

    @Test fun allBandBoundaries() {
        listOf(
            0 to SkillLevel.STARTING,
            1 to SkillLevel.STARTING,
            3 to SkillLevel.STARTING,
            4 to SkillLevel.BUILDING,
            9 to SkillLevel.BUILDING,
            10 to SkillLevel.STEADY,
            19 to SkillLevel.STEADY,
            20 to SkillLevel.STRONG
        ).forEach { (score, band) ->
            assertEquals(band, SkillLevels.skillLevel(SkillFacts(score, 0)))
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest : ProgressTestBase() {
    @Test fun calendarAlignmentAllWeekdaysAndLeapYear() {
        for (locale in listOf(
            Locale.US,
            Locale.GERMANY,
            Locale.forLanguageTag("ru"),
            Locale.forLanguageTag("es")
        )) {
            for (number in 1..12) {
                val month = YearMonth.of(2024, number)
                val cells = calendarCells(month, locale)
                assertEquals(month.lengthOfMonth(), cells.count { it != null })
                assertEquals(0, cells.size % 7)
                val offset = cells.indexOf(month.atDay(1))
                assertEquals(
                    java.time.temporal.WeekFields.of(locale).firstDayOfWeek.plus(offset.toLong()),
                    month.atDay(1).dayOfWeek
                )
            }
        }
        assertEquals(29, calendarCells(YearMonth.of(2024, 2), Locale.US).count { it != null })
    }

    @Test fun realHistoryIncludesSkippedReflectionRestDaysAndBounds() = runBlocking {
        val first = h.ensureToday()
        val reflection = first.activities.first { it.exerciseType == ExerciseType.REFLECTION }
        h.at(java.time.LocalTime.of(21, 0))
        h.refresh()
        h.skip(reflection.id).getOrThrow()
        val started = h.today
        repeat(4) { h.nextMorning() }
        val vm = main.track(
            HistoryViewModel(
                SavedStateHandle(),
                h.progress,
                h.prefs,
                h.catalog,
                h.analytics,
                h.clock
            )
        )
        val stop = vm.state.subscribe()
        try {
            val s = vm.state.await { !it.loading }
            assertFalse(s.nextEnabled)
            assertTrue(s.previousEnabled)
            vm.move(-1)
            val previous = vm.state.await { it.month == YearMonth.from(started) && !it.loading }
            assertFalse(previous.previousEnabled)
            assertTrue(previous.days.any { it.entries.isEmpty() })
            val skipped = previous.days.flatMap { it.entries }.single()
            assertEquals(ActivityState.SKIPPED, skipped.activity.state)
            assertTrue(previous.days.all { it.skills.isEmpty() })
            assertEquals(
                previous.days.map {
                    it.date
                }.sortedDescending(),
                previous.days.map { it.date }
            )
        } finally {
            stop()
        }
    }

    @Test fun selectedDateStartsInItsMonthAndDotsCapAtThree() = runBlocking {
        h.trainFullDay()
        val date = h.today
        h.clock.setDate(date.plusMonths(2))
        val vm = main.track(
            HistoryViewModel(
                SavedStateHandle(mapOf("selectedDate" to date.toString())),
                h.progress,
                h.prefs,
                h.catalog,
                h.analytics,
                h.clock
            )
        )
        val state = vm.state.await { !it.loading }
        assertEquals(YearMonth.from(date), state.month)
        assertEquals(date, state.selectedDate)
        val base = state.days.flatMap { it.entries }.first()
        val catalog = h.catalog.catalog().associateBy { it.id }
        val entries = catalog.values.groupBy {
            it.skill
        }.values.mapIndexed { index, t ->
            base.copy(
                activity = base.activity.copy(id = index.toLong(), techniqueId = t.first().id)
            )
        }
        val day = historyDays(YearMonth.from(date), date, date, entries, catalog).single()
        assertEquals(Skill.entries.take(3), day.skills)
    }
}

class NoStreakLanguageTest {
    @Test fun scansEveryLocaleAndResourceFile() {
        val forbidden =
            Regex(
                "(?i)\\b(streak|in a row|XP|points|серия дней|очки|Punktestand|racha|puntos)\\b|% complete"
            )
        val files = java.io.File("src/main/res").walkTopDown().filter {
            it.extension == "xml" &&
                it.parentFile?.name?.startsWith("values") == true
        }
        files.forEach { file -> assertFalse(file.path, forbidden.containsMatchIn(file.readText())) }
    }
}
