package com.wivernz.itera.core.common.time
import android.content.Context
import android.text.format.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
fun is24Hour(context: Context): Boolean = DateFormat.is24HourFormat(context)
fun formatTime(time: LocalTime, locale: Locale, use24Hour: Boolean): String = time.format(
    DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (use24Hour) "Hm" else "hm"),
        locale
    )
)
fun formatTime(time: LocalTime, locale: Locale, context: Context): String =
    formatTime(time, locale, is24Hour(context))
fun formatDateMedium(date: LocalDate, locale: Locale): String = date.format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
)
fun formatDateFull(date: LocalDate, locale: Locale): String = date.format(
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
)
fun formatMonthYear(month: YearMonth, locale: Locale): String =
    month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar {
        it.titlecase(locale)
    }
fun formatShortDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", locale))
fun weekdayNarrow(day: DayOfWeek, locale: Locale): String =
    day.getDisplayName(TextStyle.NARROW, locale)
fun firstDayOfWeek(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek
