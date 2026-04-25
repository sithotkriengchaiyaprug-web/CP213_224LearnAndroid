package com.example.zerotouchbudget.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    val bangkokZone: ZoneId = ZoneId.of("Asia/Bangkok")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val displayFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    /**
     * Get the current time in milliseconds (UTC)
     */
    fun nowUtcMillis(): Long = System.currentTimeMillis()

    /**
     * Converts a UTC timestamp to LocalDate in Asia/Bangkok
     */
    fun toLocalDate(timestampUtc: Long): LocalDate {
        return Instant.ofEpochMilli(timestampUtc).atZone(bangkokZone).toLocalDate()
    }

    /**
     * Converts a UTC timestamp to ZonedDateTime in Asia/Bangkok
     */
    fun toZonedDateTime(timestampUtc: Long): ZonedDateTime {
        return Instant.ofEpochMilli(timestampUtc).atZone(bangkokZone)
    }

    /**
     * Get bounds (start and end) for "Today" in Asia/Bangkok, returned as UTC milliseconds
     */
    fun getTodayBounds(): Pair<Long, Long> {
        val today = LocalDate.now(bangkokZone)
        val startOfDay = today.atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        return startOfDay to endOfDay
    }

    /**
     * Get bounds (start and end) for the current month in Asia/Bangkok, returned as UTC milliseconds
     */
    fun getCurrentMonthBounds(): Pair<Long, Long> {
        val today = LocalDate.now(bangkokZone)
        val startOfMonth = today.withDayOfMonth(1).atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        val endOfMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        return startOfMonth to endOfMonth
    }

    /**
     * Format a UTC timestamp to a nice day string (e.g., "April 25, 2026") using Asia/Bangkok timezone
     */
    fun formatToDayString(timestampUtc: Long): String {
        return toZonedDateTime(timestampUtc).format(displayFormatter)
    }

    /**
     * Get current month string (e.g., "April 2026")
     */
    /**
     * Get the current date string (e.g., "2026-04-25") using Asia/Bangkok timezone
     */
    fun getCurrentDateString(): String {
        return LocalDate.now(bangkokZone).format(dateFormatter)
    }

    /**
     * Get yesterday's date string (e.g., "2026-04-24") using Asia/Bangkok timezone
     */
    fun getYesterdayDateString(): String {
        return LocalDate.now(bangkokZone).minusDays(1).format(dateFormatter)
    }

    /**
     * Get bounds (start and end) for a specific date string (e.g., "2026-04-25") in Asia/Bangkok, returned as UTC milliseconds
     */
    fun getDayBounds(dateString: String): Pair<Long, Long> {
        val date = LocalDate.parse(dateString, dateFormatter)
        val startOfDay = date.atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(bangkokZone).toInstant().toEpochMilli()
        return startOfDay to endOfDay
    }
}