package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val indiaLocale = Locale("en", "IN")

/**
 * Formats a number into Indian Rupees (e.g., ₹250 or ₹1,250.50).
 * Whole numbers omit ".00" so small expenses like ₹50 stay clean and readable.
 */
fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(indiaLocale).apply {
        val hasDecimals = (amount % 1.0) != 0.0
        minimumFractionDigits = if (hasDecimals) 2 else 0
        maximumFractionDigits = 2
    }
    return "₹${formatter.format(amount)}"
}

/**
 * Checks whether a saved timestamp belongs to today's calendar date on the device.
 */
fun isTimestampToday(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val entryCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return entryCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
        entryCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
}

/**
 * Formats an entry's timestamp into a friendly date/time string like "Today, 09:15 AM"
 * or "25 Nov, 09:15 AM".
 */
fun formatEntryTimestamp(
    timestampMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val date = Date(timestampMillis)
    return if (isTimestampToday(timestampMillis, nowMillis)) {
        "Today, ${timeFormat.format(date)}"
    } else {
        dateFormat.format(date)
    }
}
