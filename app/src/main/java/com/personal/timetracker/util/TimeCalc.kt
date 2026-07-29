package com.personal.timetracker.util

object TimeCalc {
    fun suggestedEnd(entryTime: String, minWorkMinutes: Int): String =
        TimeUtils.addMinutes(entryTime, minWorkMinutes)

    fun earlyLeave(entryTime: String, exitTime: String, minWorkMinutes: Int): Int {
        val worked = TimeUtils.minutesBetween(entryTime, exitTime)
        return (minWorkMinutes - worked).coerceAtLeast(0)
    }

    fun midDayLeave(prevExit: String, nextEntry: String): Int =
        TimeUtils.minutesBetween(prevExit, nextEntry).coerceAtLeast(0)
}
