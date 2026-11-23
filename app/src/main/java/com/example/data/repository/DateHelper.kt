package com.example.data.repository

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {

    private fun keyFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun shortDayFormat(): SimpleDateFormat =
        SimpleDateFormat("EEE", Locale.getDefault())

    private fun dayNumberFormat(): SimpleDateFormat =
        SimpleDateFormat("d", Locale.getDefault())

    private fun fullDisplayFormat(): SimpleDateFormat =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    private fun monthDayFormat(): SimpleDateFormat =
        SimpleDateFormat("MMM d", Locale.getDefault())

    private fun timeFormat(): SimpleDateFormat =
        SimpleDateFormat("h:mm a", Locale.getDefault())

    fun todayKey(): String = keyFormat().format(Date())

    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return keyFormat().format(cal.time)
    }

    fun parseKey(dateKey: String): Date? = runCatching {
        keyFormat().parse(dateKey)
    }.getOrNull()

    fun formatShortDay(dateKey: String): String {
        val date = parseKey(dateKey) ?: return dateKey
        return shortDayFormat().format(date)
    }

    fun formatDayNumber(dateKey: String): String {
        val date = parseKey(dateKey) ?: return dateKey
        return dayNumberFormat().format(date)
    }

    fun formatMonthDay(dateKey: String): String {
        val date = parseKey(dateKey) ?: return dateKey
        return monthDayFormat().format(date)
    }

    fun formatJournalHeader(dateKey: String): String {
        val today = todayKey()
        val yesterday = dateKeyDaysAgo(1)
        return when (dateKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> parseKey(dateKey)?.let { fullDisplayFormat().format(it) } ?: dateKey
        }
    }

    fun formatTime(timestampMillis: Long): String {
        return timeFormat().format(Date(timestampMillis))
    }

    fun lastNDates(n: Int): List<String> {
        return (n - 1 downTo 0).map { dateKeyDaysAgo(it) }
    }
}
