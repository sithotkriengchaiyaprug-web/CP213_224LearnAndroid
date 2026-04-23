package com.example.zerotouchbudget.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {

    fun getCurrentDateString(): String {
        return newDateFormat().format(Calendar.getInstance().time)
    }

    fun getDayBounds(dateString: String): Pair<Long, Long> {
        val parsedDate = newDateFormat().parse(dateString)
            ?: throw IllegalArgumentException("Invalid date format: $dateString")
        val calendar = Calendar.getInstance().apply {
            time = parsedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        return startOfDay to endOfDay
    }

    fun getTodayBounds(): Pair<Long, Long> = getDayBounds(getCurrentDateString())

    fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return newDateFormat().format(calendar.time)
    }

    private fun newDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }
}
