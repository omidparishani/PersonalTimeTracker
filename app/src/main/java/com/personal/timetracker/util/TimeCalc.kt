package com.personal.timetracker.util

/** Outcome of applying the flexible-hours (شناوری) policy to a check-in time. */
data class FlexOutcome(
    /** Recommended check-out time to complete the required duration for this day. */
    val suggestedEnd: String,
    /** Leave (مرخصی) minutes to record immediately because entry was later than the
     *  allowed flex window (start + flexibleMinutes). Zero if entry was within the window. */
    val entryLeaveMinutes: Int
)

object TimeCalc {

    /**
     * Required work duration (minutes) for a given date, driven by the weekly-hours
     * schedule rather than a flat daily number:
     * - A holiday (official or manual) requires 0 minutes.
     * - Friday is always off.
     * - If Thursday is NOT a working day: Thursday is also off, and the weekly total is
     *   split evenly across Saturday–Wednesday (5 days).
     * - If Thursday IS a working day: Thursday gets its own configured minutes, and the
     *   remaining weekly total is split evenly across Saturday–Wednesday (5 days).
     */
    fun requiredMinutesForDate(
        date: String,
        weeklyRequiredMinutes: Int,
        thursdayWorking: Boolean,
        thursdayMinutes: Int,
        isHoliday: Boolean
    ): Int {
        if (isHoliday) return 0
        val weekday = TimeUtils.weekdayOf(date) // 0=Sat ... 5=Thu, 6=Fri
        if (weekday == 6) return 0 // Friday
        if (weekday == 5) {
            return if (thursdayWorking) thursdayMinutes.coerceAtLeast(0) else 0
        }
        // Saturday..Wednesday
        val poolMinutes = if (thursdayWorking) {
            (weeklyRequiredMinutes - thursdayMinutes).coerceAtLeast(0)
        } else weeklyRequiredMinutes
        return (poolMinutes / 5).coerceAtLeast(0)
    }

    /**
     * Applies the flexible-hours policy:
     * - The person may check in anytime between [startWorkTime] and [startWorkTime] +
     *   [flexibleMinutes] (the "flex window") with no penalty — their required end time
     *   simply shifts by however late (within the window) they arrived.
     * - If they check in *after* the flex window closes, the extra lateness (from the end
     *   of the flex window to the actual entry) is recorded as leave right away, and the
     *   suggested end time is capped at startWorkTime + flexibleMinutes + requiredDuration
     *   — it does not keep shifting further.
     *
     * Example: start=07:00, flex=120min, requiredDuration=555min (9:15), entry=10:10
     *   flexWindowEnd = 09:00 -> entry (10:10) is past it
     *   entryLeaveMinutes = 10:10 - 09:00 = 70 (leave from 09:00 to 10:10)
     *   suggestedEnd = 09:00 + 9:15 = 18:15
     */
    fun applyFlex(
        entryTime: String,
        startWorkTime: String,
        flexibleMinutes: Int,
        requiredDurationMinutes: Int
    ): FlexOutcome {
        val flexWindowEnd = TimeUtils.addMinutes(startWorkTime, flexibleMinutes)

        val entryAbs = TimeUtils.minutesBetween("00:00", entryTime)
        val startAbs = TimeUtils.minutesBetween("00:00", startWorkTime)
        val flexEndAbs = TimeUtils.minutesBetween("00:00", flexWindowEnd)

        // How much of the (allowed) lateness should shift the end time: clamp entry into
        // [start, flexWindowEnd] — arriving early doesn't grant an earlier end, and arriving
        // beyond the flex window doesn't keep shifting the end further.
        val effectiveAbs = entryAbs.coerceIn(startAbs, flexEndAbs)
        val suggestedEnd = TimeUtils.addMinutes(startWorkTime, (effectiveAbs - startAbs) + requiredDurationMinutes)

        val entryLeave = (entryAbs - flexEndAbs).coerceAtLeast(0)
        return FlexOutcome(suggestedEnd, entryLeave)
    }

    /** Backward-compatible simple version (no flex policy) — end = entry + required minutes. */
    fun suggestedEnd(entryTime: String, requiredMinutes: Int): String =
        TimeUtils.addMinutes(entryTime, requiredMinutes)

    fun earlyLeave(entryTime: String, exitTime: String, requiredMinutes: Int): Int {
        val worked = TimeUtils.minutesBetween(entryTime, exitTime)
        return (requiredMinutes - worked).coerceAtLeast(0)
    }

    /** Leave/overtime minutes from comparing an actual exit time against a target
     *  (flex-aware) end time. Returns Pair(leaveMinutes, overtimeMinutes) — only one of the
     *  two will be non-zero. Assumes both times fall on the same day. */
    fun exitOutcome(exitTime: String, targetEnd: String): Pair<Int, Int> {
        val exitAbs = TimeUtils.minutesBetween("00:00", exitTime)
        val targetAbs = TimeUtils.minutesBetween("00:00", targetEnd)
        val diff = exitAbs - targetAbs
        return if (diff < 0) (-diff) to 0 else 0 to diff
    }

    fun midDayLeave(prevExit: String, nextEntry: String): Int =
        TimeUtils.minutesBetween(prevExit, nextEntry).coerceAtLeast(0)
}
