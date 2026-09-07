package com.example.nexus.core.presence

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

/**
 * Local temporal reasoning engine.
 * Computes human-friendly relative temporal expressions and daily phase classifications.
 */
class TimeAwareIntelligence {

    fun getTimeOfDay(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): TimeOfDay {
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    fun isWeekend(calendar: Calendar = Calendar.getInstance()): Boolean {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    fun formatRelativeTime(timestamp: Long, currentTime: Long = System.currentTimeMillis()): String {
        val diffMs = currentTime - timestamp
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHours = diffMin / 60
        val diffDays = diffHours / 24

        return when {
            diffMs < 0 -> {
                val futureSec = -diffMs / 1000
                val futureMin = futureSec / 60
                val futureHours = futureMin / 60
                when {
                    futureMin < 1 -> "in a few seconds"
                    futureMin < 60 -> "in $futureMin min"
                    futureHours < 24 -> "in $futureHours hrs"
                    else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
            }
            diffSec < 60 -> "just now"
            diffMin < 60 -> "$diffMin min ago"
            diffHours < 24 -> "$diffHours hrs ago"
            diffDays == 1L -> "yesterday"
            diffDays < 7 -> "$diffDays days ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
