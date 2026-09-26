package com.wivernz.itera.data.export

import android.content.Context
import android.content.res.Configuration
import com.wivernz.itera.R
import com.wivernz.itera.analytics.Analytics
import com.wivernz.itera.analytics.Event
import com.wivernz.itera.analytics.ExportRange
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.core.designsystem.component.title
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import com.wivernz.itera.domain.repository.JournalExporter
import com.wivernz.itera.domain.repository.PreferencesRepository
import com.wivernz.itera.domain.repository.ProgressRepository
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.domain.repository.TrainingPlanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

fun exportStart(range: ExportRange, today: LocalDate, started: LocalDate?): LocalDate? =
    started?.let {
        maxOf(
            it,
            when (range) {
                ExportRange.MONTH -> today.minusDays(29)
                ExportRange.YEAR -> today.minusYears(1).plusDays(1)
                ExportRange.ALL -> it
            }
        )
    }

class ExportJournalUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: PreferencesRepository,
    private val progress: ProgressRepository,
    private val plans: TrainingPlanRepository,
    private val catalog: TechniqueCatalogRepository,
    private val mastery: ObserveTechniqueProgressUseCase,
    private val analytics: Analytics,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : JournalExporter {
    override suspend fun export(range: ExportRange, locale: Locale): File = withContext(io) {
        val today = LocalDate.now(clock)
        val start = exportStart(range, today, preferences.preferences.first().programStartedOn)
        val directory = File(context.cacheDir, "export").apply { mkdirs() }
        directory.listFiles()?.filter {
            it.lastModified() < clock.millis() - 86_400_000
        }?.forEach { it.delete() }
        val output = File(directory, "itera-journal-$today.md")
        val localized = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
        )
        val copy = ResourceJournalCopy(localized)
        val techniques = catalog.catalog()
        val facts = mastery().first()
        val writer = MarkdownJournalWriter(copy, techniques.associate { it.id to it.skill })
        // Only compact header facts are retained; activity payloads are read one month at a time.
        val programDays = mutableListOf<Int>()
        var count = 0
        var dayCount = 0
        if (start != null) {
            var month = YearMonth.from(today)
            while (month >= YearMonth.from(start)) {
                val rows = progress.observeHistory(month).first().filter { it.date in start..today }
                count += rows.size
                rows.map { it.date }.distinct().forEach { date ->
                    plans.dayByDate(date)?.programDay?.let {
                        programDays +=
                            it
                    }
                }
                month = month.minusMonths(1)
            }
        }
        try {
            output.bufferedWriter().use { stream ->
                writer.header(stream, today, programDays, count)
                if (start != null) {
                    var month = YearMonth.from(today)
                    while (month >= YearMonth.from(start)) {
                        val rows = progress.observeHistory(month).first().groupBy { it.date }
                        var date = minOf(today, month.atEndOfMonth())
                        val first = maxOf(start, month.atDay(1))
                        while (date >= first) {
                            writer.day(
                                stream,
                                JournalDay(
                                    date,
                                    plans.dayByDate(date)?.programDay,
                                    rows[date].orEmpty().map {
                                        it.activity
                                    }
                                ),
                                locale
                            )
                            dayCount++
                            date = date.minusDays(1)
                        }
                        month = month.minusMonths(1)
                    }
                }
                writer.techniques(
                    stream,
                    (
                        if (start ==
                            null
                        ) {
                            emptyList()
                        } else {
                            facts
                        }
                        ).mapNotNull { fact ->
                        techniques.firstOrNull {
                            it.id ==
                                fact.techniqueId
                        }?.let {
                            JournalTechnique(
                                it.name,
                                copy.skill(it.skill),
                                localized.getString(fact.level.title),
                                fact
                            )
                        }
                    }
                )
            }
            analytics.track(Event.JournalExported(range, dayCount, output.length()))
            output
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }
    override suspend fun cleanup(file: File) {
        withContext(io) {
            if (file.parentFile?.canonicalFile ==
                File(context.cacheDir, "export").canonicalFile
            ) {
                file.delete()
            }
        }
        Unit
    }
}

class ResourceJournalCopy(private val context: Context) : JournalCopy {
    override fun text(key: String, vararg args: Any): String = context.getString(
        when (key) {
            "title" -> R.string.export_title
            "header" -> R.string.export_header
            "rest" -> R.string.export_rest
            "day" -> R.string.export_day
            "skipped" -> R.string.history_skipped
            "focus" -> R.string.export_focus
            "minutes" -> R.string.export_minutes
            "recall" -> R.string.export_recall
            "felt" -> R.string.export_felt
            "well" -> R.string.export_well
            "not_well" -> R.string.export_not_well
            "tomorrow" -> R.string.export_tomorrow
            "techniques" -> R.string.export_techniques
            "columns" -> R.string.export_columns
            else -> error("Unknown journal label")
        },
        *args
    )
    override fun skill(skill: Skill) = context.getString(skill.title)
    override fun difficulty(value: Difficulty) = context.getString(
        when (value) {
            Difficulty.EASY -> R.string.feel_easy
            Difficulty.OKAY -> R.string.feel_okay
            Difficulty.HARD -> R.string.feel_hard
        }
    )
    override fun recall(value: RecallGrade) = context.getString(
        when (value) {
            RecallGrade.FORGOT -> R.string.review_forgot
            RecallGrade.PARTIAL -> R.string.review_partial
            RecallGrade.SOLID -> R.string.review_solid
        }
    )
}
