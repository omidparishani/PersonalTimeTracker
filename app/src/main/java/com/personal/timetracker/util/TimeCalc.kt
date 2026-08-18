package com.personal.timetracker.util

/** Outcome of applying the flexible-hours (شناوری) policy to a check-in time. */
data class FlexOutcome(
    /** Recommended check-out time to complete the required daily duration under this policy. */
    val suggestedEnd: String,
    /** Leave (مرخصی) minutes to record immediately because entry was later than the allowed
     *  flex window (start + flexibleMinutes). Zero if entry was within the flex window. */
    val entryLeaveMinutes: Int
)

object TimeCalc {

    /**
     * Applies the flexible-hours policy:
     * - The person may check in anytime between [startWorkTime] and [startWorkTime] +
     *   [flexibleMinutes] (the "flex window") with no penalty — their required end time
     *   simply shifts by however late (within the window) they arrived.
     * - If they check in *after* the flex window closes, the extra lateness (from the end
     *   of the flex window to the actual entry) is recorded as leave right away, and the
     *   suggested end time is capped at startWorkTime + flexibleMinutes + requiredDuration
     *   (equivalently endWorkTime + flexibleMinutes) — it does not keep shifting further.
     *
     * Example: start=07:00, end=16:15, flex=120min, entry=10:10
     *   flexWindowEnd = 09:00 -> entry (10:10) is past it
     *   entryLeaveMinutes = 10:10 - 09:00 = 70 (leave from 09:00 to 10:10)
     *   suggestedEnd = 16:15 + 2:00 = 18:15
     */
    fun applyFlex(
        entryTime: String,
        startWorkTime: String,
        endWorkTime: String,
        flexibleMinutes: Int
    ): FlexOutcome {
        val requiredDuration = TimeUtils.minutesBetween(startWorkTime, endWorkTime)
        val flexWindowEnd = TimeUtils.addMinutes(startWorkTime, flexibleMinutes)

        val entryAbs = TimeUtils.minutesBetween("00:00", entryTime)
        val startAbs = TimeUtils.minutesBetween("00:00", startWorkTime)
        val flexEndAbs = TimeUtils.minutesBetween("00:00", flexWindowEnd)

        // How much of the (allowed) lateness should shift the end time: clamp entry into
        // [start, flexWindowEnd] — arriving early doesn't grant an earlier end, and arriving
        // beyond the flex window doesn't keep shifting the end further.
        val effectiveAbs = entryAbs.coerceIn(startAbs, flexEndAbs)
        val suggestedEnd = TimeUtils.addMinutes(startWorkTime, (effectiveAbs - startAbs) + requiredDuration)

        val entryLeave = (entryAbs - flexEndAbs).coerceAtLeast(0)
        return FlexOutcome(suggestedEnd, entryLeave)
    }

    /** Backward-compatible simple version (no flex policy) — end = entry + minimum minutes. */
    fun suggestedEnd(entryTime: String, minWorkMinutes: Int): String =
        TimeUtils.addMinutes(entryTime, minWorkMinutes)

    fun earlyLeave(entryTime: String, exitTime: String, minWorkMinutes: Int): Int {
        val worked = TimeUtils.minutesBetween(entryTime, exitTime)
        return (minWorkMinutes - worked).coerceAtLeast(0)
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
