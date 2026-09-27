@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.analytics.ExportRange
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.LevelHint
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import java.io.File
import java.io.StringWriter
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

fun journalActivity(
    id: Long,
    title: String,
    result: ActivityResult? = null,
    note: String? = null,
    state: ActivityState = ActivityState.COMPLETED
) = PlanActivity(
    id, 1,
    TechniqueId(
        "pomodoro"
    ),
    ExerciseType.TEMPLATE, ActivitySource.MANUAL, id.toInt(), DayPart.MORNING,
    title, "", "", 5, state, false, null, null, null, null, null, Difficulty.OKAY, note, result, null
)

@RunWith(RobolectricTestRunner::class)
class MarkdownJournalWriterTest {
    private val date = LocalDate.of(2026, 9, 22)
    private fun writer() = MarkdownJournalWriter(
        ResourceJournalCopy(ApplicationProvider.getApplicationContext()),
        mapOf(TechniqueId("pomodoro") to Skill.FOCUS)
    )

    @Test fun goldenCoversRestSkippedEarlyFocusReviewAndPartialReflection() {
        val days = listOf(
            JournalDay(date.minusDays(1), null, emptyList()),
            JournalDay(
                date,
                9,
                listOf(
                    journalActivity(1, "Pomodoro", note = "# header\n- list | pipe"),
                    journalActivity(
                        2,
                        "Deep Work",
                        ActivityResult.Focus("one task", 3000, 1200, 0, false)
                    ).copy(difficulty = null),
                    journalActivity(
                        3,
                        "Review",
                        ActivityResult.Review(
                            1,
                            "my answer",
                            RecallGrade.PARTIAL,
                            "previous answer"
                        )
                    ),
                    journalActivity(
                        4,
                        "Evening reflection",
                        ActivityResult.Reflection(
                            "Protected time.",
                            emptyList(),
                            null,
                            emptyList(),
                            null
                        )
                    ),
                    journalActivity(5, "2-minute rule", state = ActivityState.SKIPPED)
                )
            )
        )
        val actual = writer().render(days, emptyList(), date, Locale.US)
        assertEquals(
            File("src/test/resources/export/journal-golden.md").readText().replace("\r\n", "\n"),
            actual
        )
        val stream = StringWriter()
        writer().render(stream, days, emptyList(), date, Locale.US)
        assertEquals(actual, stream.toString())
    }

    @Test fun emptyAndLockedTechniquesDoNotInventPractice() {
        val p =
            TechniqueProgress(
                TechniqueId(
                    "pomodoro"
                ),
                false, 2, MasteryLevel.NONE, 0, 0, null, null, false, LevelHint.Unavailable
            )
        val actual = writer().render(
            emptyList(),
            listOf(JournalTechnique("Pomodoro", "Focus", "Met", p)),
            date,
            Locale.US
        )
        assertTrue(actual.startsWith("# Itera journal"))
        assertFalse(actual.contains("## Tuesday"))
        assertFalse(actual.contains("| Pomodoro |"))
        assertTrue(actual.endsWith("| --- | --- | --- | --- | --- | --- | --- |\n"))
    }

    @Test fun singleActivityUsesSingularAndEscapesHtmlAndMarkdown() {
        val actual = writer().render(
            listOf(
                JournalDay(date, 1, listOf(journalActivity(1, "Pomodoro", note = "<tag> **note**")))
            ),
            emptyList(),
            date,
            Locale.US
        )
        assertTrue(actual.contains("Days 1-1 - 1 activity\n"))
        assertTrue(actual.contains("> \\<tag\\> \\*\\*note\\*\\*"))
    }
}

@RunWith(RobolectricTestRunner::class)
class ExportJournalUseCaseTest {
    @Test fun allRangeBoundariesIncludeToday() {
        val today = LocalDate.of(2026, 9, 26)
        assertEquals(
            today.minusDays(29),
            exportStart(ExportRange.MONTH, today, today.minusYears(3))
        )
        assertEquals(
            today.minusYears(1).plusDays(1),
            exportStart(ExportRange.YEAR, today, today.minusYears(3))
        )
        assertEquals(today.minusYears(3), exportStart(ExportRange.ALL, today, today.minusYears(3)))
        assertEquals(today, exportStart(ExportRange.MONTH, today, today))
        assertNull(exportStart(ExportRange.ALL, today, null))
    }

    @Test fun realRepositoryExportCacheShareAndCleanup() = runBlocking {
        val h = EngineHarness()
        try {
            h.trainFullDay()
            val exporter = exporter(h)
            val file = exporter.export(ExportRange.ALL, Locale.US)
            assertEquals("itera-journal-${h.today}.md", file.name)
            assertEquals("export", file.parentFile!!.name)
            val content = file.readText()
            assertTrue(content.contains("## Techniques"))
            assertTrue(content.contains("Day 1"))
            assertTrue(content.contains("> start early"))
            val context = ApplicationProvider.getApplicationContext<Context>()
            val intent = com.wivernz.itera.feature.you.journalShareIntent(
                android.net.Uri.parse(
                    "content://${context.packageName}.fileprovider/journal/${file.name}"
                )
            )
            assertEquals(android.content.Intent.ACTION_SEND, intent.action)
            assertEquals("text/markdown", intent.type)
            assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(
                "${context.packageName}.fileprovider",
                intent.clipData!!.getItemAt(0).uri.authority
            )
            exporter.cleanup(file)
            assertFalse(file.exists())
        } finally {
            h.close()
        }
    }
}
fun exporter(h: EngineHarness) =
    ExportJournalUseCase(ApplicationProvider.getApplicationContext(), h.prefs, h.progress, h.db.trainingDayDao(), h.catalog, h.observeTechniques, h.analytics, h.clock, Dispatchers.IO)

@RunWith(RobolectricTestRunner::class)
class ExportPerformanceTest {
    @Test fun twoYearsStreamUnderThreeSecondsAndFiftyMegabytes() = runBlocking {
        val h = EngineHarness()
        try {
            val first = h.today.minusDays(729)
            h.prefs.update { it.copy(programStartedOn = first) }
            h.unlock(14)
            h.tx.inTransaction {
                repeat(730) { index ->
                    val date = first.plusDays(index.toLong()).toEpochDay()
                    val id = h.db.trainingDayDao().insert(
                        TrainingDayEntity(
                            programDay = index + 1,
                            date = date,
                            status = "COMPLETE",
                            generatorVersion = 1,
                            createdAt = 0
                        )
                    )
                    repeat(3) { n ->
                        h.db.planActivityDao().insertAll(
                            listOf(
                                PlanActivityEntity(
                                    trainingDayId = id, techniqueId = "two_minute_rule", exerciseType = "TEMPLATE", source = "MANUAL", orderIndex = n, dayPart = "MORNING", copyKey = "activity_program", copyArgs = "{}", estimatedMinutes = 2, optional = false, state = "COMPLETED",
                                    note = "Synthetic journal note ".repeat(
                                        20
                                    ),
                                    practiceDate = date
                                )
                            )
                        )
                    }
                }
            }
            val useCase = exporter(h)
            // Warm catalogue/resource loading; the budget measures the export, not JVM startup.
            h.catalog.catalog()
            h.observeTechniques().first()
            val runtime = Runtime.getRuntime()
            System.gc()
            val before = runtime.totalMemory() - runtime.freeMemory()
            val start = System.nanoTime()
            val file = useCase.export(ExportRange.ALL, Locale.US)
            val elapsed = (System.nanoTime() - start) / 1_000_000
            val growth = runtime.totalMemory() - runtime.freeMemory() - before
            assertTrue("export took $elapsed ms", elapsed < 3_000)
            assertTrue("heap grew $growth bytes", growth < 50L * 1024 * 1024)
            assertEquals(730, Regex("(?m)^## .* - Day").findAll(file.readText()).count())
            useCase.cleanup(file)
        } finally {
            h.close()
        }
    }
}
