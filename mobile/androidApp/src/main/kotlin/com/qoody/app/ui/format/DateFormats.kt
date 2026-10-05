package com.qoody.app.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** ICU skeletons: the system picks the ordering and punctuation for each locale. */
private object Skeleton {
    const val MONTH_DAY = "MMMd"
    const val WEEKDAY_MONTH_DAY_LONG = "EEEEMMMd"
    const val WEEKDAY_MONTH_DAY_SHORT = "EEEMMMd"
    const val DAY_OF_MONTH = "d"
}

/** Locale-aware date and time text, so no screen hard-codes a pattern. */
@Immutable
class DateFormats(
    private val locale: Locale,
) {
    /** `Oct 24` */
    fun monthDay(date: LocalDate): String = format(date, Skeleton.MONTH_DAY)

    /** `Thursday, Oct 24` */
    fun weekdayMonthDay(date: LocalDate): String = format(date, Skeleton.WEEKDAY_MONTH_DAY_LONG)

    /** `Thu, Oct 24` */
    fun shortWeekdayMonthDay(date: LocalDate): String = format(date, Skeleton.WEEKDAY_MONTH_DAY_SHORT)

    /** `24` */
    fun dayOfMonth(date: LocalDate): String = format(date, Skeleton.DAY_OF_MONTH)

    /** `October` */
    fun monthName(month: Month): String = javaMonth(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)

    /** `Oct` */
    fun monthAbbreviation(month: Month): String = javaMonth(month).getDisplayName(TextStyle.SHORT_STANDALONE, locale)

    /** `10:14 AM` (or `10:14` where the locale uses 24-hour time) */
    fun time(time: LocalTime): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time.toJavaLocalTime())

    private fun format(
        date: LocalDate,
        skeleton: String,
    ): String {
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(pattern, locale).format(date.toJavaLocalDate())
    }

    private fun javaMonth(month: Month): java.time.Month = java.time.Month.of(month.ordinal + 1)
}

@Composable
fun rememberDateFormats(): DateFormats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateFormats(locale) }
}
