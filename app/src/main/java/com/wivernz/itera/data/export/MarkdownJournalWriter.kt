package com.wivernz.itera.data.export

import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.PlanActivity
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.TechniqueProgress
import java.io.StringWriter
import java.io.Writer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class JournalDay(val date: LocalDate, val programDay: Int?, val activities: List<PlanActivity>)
data class JournalTechnique(
    val name: String,
    val skill: String,
    val level: String,
    val progress: TechniqueProgress
)

/** Resource-backed labels are supplied by the I/O adapter; this renderer has no Android dependency. */
interface JournalCopy {
    fun text(key: String, vararg args: Any): String
    fun skill(skill: Skill): String
    fun difficulty(value: Difficulty): String
    fun recall(value: RecallGrade): String
}

class MarkdownJournalWriter(
    private val copy: JournalCopy,
    private val skills: Map<TechniqueId, Skill>
) {
    fun render(
        days: List<JournalDay>,
        techniques: List<JournalTechnique>,
        exportedOn: LocalDate,
        locale: Locale
    ): String = StringWriter().also {
        render(it, days, techniques, exportedOn, locale)
    }.toString()

    fun render(
        writer: Writer,
        days: List<JournalDay>,
        techniques: List<JournalTechnique>,
        exportedOn: LocalDate,
        locale: Locale
    ) {
        header(
            writer,
            exportedOn,
            days.mapNotNull {
                it.programDay
            },
            days.sumOf { it.activities.size }
        )
        days.sortedByDescending { it.date }.forEach { day(writer, it, locale) }
        techniques(writer, techniques)
    }

    fun header(writer: Writer, exportedOn: LocalDate, programDays: List<Int>, count: Int) {
        val range = if (programDays.isEmpty()) "-" else "${programDays.min()}-${programDays.max()}"
        writer.write(
            "# ${copy.text(
                "title"
            )}\n\n${copy.text("header", exportedOn.toString(), range, count)}\n\n"
        )
    }

    fun day(writer: Writer, day: JournalDay, locale: Locale) {
        val date = day.date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
        )
        val suffix = if (day.activities.isEmpty()) {
            copy.text("rest")
        } else {
            day.programDay?.let { copy.text("day", it) }
                ?: copy.text("rest")
        }
        writer.write("---\n\n## $date - $suffix\n\n")
        day.activities.forEach { a ->
            writer.write("**${inline(a.title)}**")
            if (a.state ==
                ActivityState.SKIPPED
            ) {
                writer.write(" - ${copy.text("skipped")}\n\n")
                return@forEach
            }
            val result = a.result
            if (result !is ActivityResult.Reflection) {
                skills[a.techniqueId]?.let { writer.write(" - ${copy.skill(it)}") }
                if (result is ActivityResult.Focus) {
                    writer.write(
                        " - ${copy.text(
                            "focus",
                            result.actualSeconds / 60,
                            result.plannedSeconds / 60
                        )}"
                    )
                } else {
                    writer.write(
                        " - ${copy.text(
                            "minutes",
                            (a.durationSeconds?.div(60) ?: a.estimatedMinutes)
                        )}"
                    )
                }
                if (result is ActivityResult.Review) {
                    writer.write(" - ${copy.text("recall", copy.recall(result.grade))}")
                } else {
                    a.difficulty?.let {
                        writer.write(" - ${copy.text("felt", copy.difficulty(it))}")
                    }
                }
            }
            writer.write("\n")
            if (result is ActivityResult.Reflection) {
                listOf(
                    "well" to result.wentWell,
                    "not_well" to result.didNotGoWell,
                    "tomorrow" to result.tomorrowChange
                ).forEach { (label, answer) ->
                    if (!answer.isNullOrBlank()) {
                        writer.write("- ${copy.text(label)}:\n")
                        quote(writer, answer)
                    }
                }
            } else {
                authoredText(result).forEach { quote(writer, it) }
            }
            a.note?.takeIf { it.isNotBlank() }?.let { quote(writer, it) }
            writer.write("\n")
        }
    }

    fun techniques(writer: Writer, techniques: List<JournalTechnique>) {
        writer.write(
            "---\n\n## ${copy.text(
                "techniques"
            )}\n\n| ${copy.text("columns")} |\n| --- | --- | --- | --- | --- | --- | --- |\n"
        )
        techniques.filter { it.progress.unlocked }.forEach { t ->
            val p = t.progress
            writer.write(
                "| ${inline(
                    t.name
                )} | ${t.skill} | ${t.level} | ${p.totalUses} | ${p.distinctPracticeDays} | ${p.firstUsedOn ?: "-"} | ${p.lastUsedOn ?: "-"} |\n"
            )
        }
    }

    private fun quote(writer: Writer, text: String) {
        text.lineSequence().forEach { line -> writer.write("> ${inline(line)}\n") }
    }
    private fun inline(text: String): String = buildString(text.length) {
        text.forEach { character ->
            when (character) {
                '\r' -> Unit
                '\n' -> append(' ')
                else -> {
                    if (character in "\\`*_{}[]()#+.!<>|~-") append('\\')
                    append(character)
                }
            }
        }
    }
}

private fun authoredText(result: ActivityResult?): List<String> = when (result) {
    is ActivityResult.Focus -> listOf(result.taskLabel)
    is ActivityResult.Feynman -> listOfNotNull(
        result.topicTitle,
        result.explanation,
        result.reflectionNote
    )
    is ActivityResult.Premortem -> listOf(result.projectName) + result.reasons.map { it.text } +
        listOfNotNull(result.mitigationAction)
    is ActivityResult.Eisenhower -> result.items.map { it.label }
    is ActivityResult.HabitStack -> listOf(result.anchor, result.habit)
    is ActivityResult.Review -> listOf(result.answer, result.previousAnswer)
    is ActivityResult.Combination -> result.stepResults.map { it.summary }
    is ActivityResult.Template -> result.values.values.flatMap {
        when (it) {
            is BlockValue.Text -> listOf(it.text)
            is BlockValue.Items -> it.items.map { item -> item.label }
            is BlockValue.Choice -> it.options
            is BlockValue.Lists -> it.primary + it.secondary
            is BlockValue.Chips -> listOfNotNull(it.custom)
        }
    }
    else -> emptyList()
}
