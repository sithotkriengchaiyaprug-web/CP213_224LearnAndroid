package com.example.zerotouchbudget.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getCurrentDateString(): String {
        return dateFormat.format(Calendar.getInstance().time)
    }

    fun getDayBounds(dateString: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            time = dateFormat.parse(dateString)!!
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
        return dateFormat.format(calendar.time)
    }
}