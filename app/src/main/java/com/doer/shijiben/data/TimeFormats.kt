package com.doer.shijiben.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormats {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val dayKeyFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE

    private val dateTimeDisplay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)

    private val dateOnlyDisplay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

    private val timeOnlyDisplay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    fun dayKeyFromMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().format(dayKeyFormatter)

    fun formatDateTimeMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeDisplay)

    fun formatDateMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(dateOnlyDisplay)

    fun formatTimeMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(timeOnlyDisplay)

    /** Matches Material `DatePicker` UTC millis convention for a calendar date. */
    fun localDateToPickerUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun pickerUtcMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun mergeLocalDateKeepingLocalTime(previousMillis: Long, newDate: LocalDate): Long {
        val prev = Instant.ofEpochMilli(previousMillis).atZone(zone).toLocalDateTime()
        val ldt = LocalDateTime.of(newDate, prev.toLocalTime())
        return ldt.atZone(zone).toInstant().toEpochMilli()
    }

    fun mergeLocalTimeKeepingLocalDate(previousMillis: Long, hour: Int, minute: Int): Long {
        val date = Instant.ofEpochMilli(previousMillis).atZone(zone).toLocalDate()
        val ldt = LocalDateTime.of(date, LocalTime.of(hour, minute))
        return ldt.atZone(zone).toInstant().toEpochMilli()
    }

    fun millisToHourMinute(millis: Long): Pair<Int, Int> {
        val t = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return t.hour to t.minute
    }

    fun millisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
