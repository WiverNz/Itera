package com.wivernz.itera.core.common.time
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class LocaleFormattingTest {
    @Test fun allFormattersUseRequestedLocaleAndHourCycle() {
        val date = LocalDate.of(2026, 9, 23)
        listOf("en-US", "ru-RU", "de-DE", "es-ES").forEach { tag ->
            val locale = Locale.forLanguageTag(tag)
            assertEquals(
                date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(locale)
                ),
                formatDateMedium(
                    date,
                    locale
                )
            )
            assertEquals(
                date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        .withLocale(locale)
                ),
                formatDateFull(
                    date,
                    locale
                )
            )
            assertEquals(
                date.format(
                    DateTimeFormatter.ofPattern(
                        "d MMM",
                        locale
                    )
                ),
                formatShortDate(
                    date,
                    locale
                )
            )
            assertEquals(
                DayOfWeek.MONDAY.getDisplayName(
                    TextStyle.NARROW,
                    locale
                ),
                weekdayNarrow(
                    DayOfWeek.MONDAY,
                    locale
                )
            )
            assertEquals(
                WeekFields.of(locale)
                    .firstDayOfWeek,
                firstDayOfWeek(locale)
            )
            assertTrue(formatTime(LocalTime.of(21, 5), locale, true).contains("21"))
            assertFalse(
                formatTime(
                    LocalTime.of(
                        21,
                        5
                    ),
                    locale,
                    false
                )
                    .contains("21")
            )
            assertTrue(
                formatMonthYear(
                    YearMonth.from(date),
                    locale
                )
                    .contains("2026")
            )
        }
        assertEquals(
            "Сентябрь 2026",
            formatMonthYear(
                YearMonth.from(date),
                Locale.forLanguageTag("ru")
            )
        )
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.GERMANY))
    }
}
