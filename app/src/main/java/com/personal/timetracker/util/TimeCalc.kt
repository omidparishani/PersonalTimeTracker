package com.personal.timetracker.util

/**
 * محاسبه موظفی / مرخصی / اضافه‌کار در سطح یک روز (چند ورود و خروج).
 *
 * مثال کاربر:
 * شروع ۷:۳۰، شناوری ۱۲۰، موظفی ۹:۱۵
 * ۷:۳۰–۱۰:۵۲ ، ۱۱:۲۳–۱۳:۰۰ ، ۱۳:۳۴–۱۶:۴۵
 * → خروج پیشنهادی ۱۶:۴۵
 * → مرخصی فقط فاصله‌ها: ۳۱ + ۳۴ = ۶۵ دقیقه (۱:۰۵)
 * → اضافه‌کار ۰
 */

data class FlexOutcome(
    val suggestedEnd: String,
    val entryLeaveMinutes: Int
)

data class PresenceSegment(
    val entryTime: String,
    val exitTime: String?
)

data class DayTotals(
    val workedMinutes: Int,
    val leaveMinutes: Int,
    val overtimeMinutes: Int,
    val suggestedEnd: String,
    val entryLeaveMinutes: Int,
    val gapLeaveMinutes: Int,
    val earlyLeaveMinutes: Int = 0
)

object TimeCalc {

    fun requiredMinutesForDate(
        date: String,
        weeklyRequiredMinutes: Int,
        thursdayWorking: Boolean,
        thursdayMinutes: Int,
        isHoliday: Boolean
    ): Int {
        if (isHoliday) return 0
        val weekday = TimeUtils.weekdayOf(date) // 0=Sat ... 5=Thu, 6=Fri
        if (weekday == 6) return 0 // جمعه
        if (weekday == 5) {
            return if (thursdayWorking) thursdayMinutes.coerceAtLeast(0) else 0
        }
        val pool = if (thursdayWorking) {
            (weeklyRequiredMinutes - thursdayMinutes).coerceAtLeast(0)
        } else weeklyRequiredMinutes
        return (pool / 5).coerceAtLeast(0)
    }

    /**
     * خروج پیشنهادی و مرخصی تأخیر بر اساس اولین ورود روز.
     * ورود در/قبل از شروع یا داخل پنجره شناوری → خروج = ورود + موظفی
     * ورود بعد از پایان شناوری → مرخصی تأخیر + خروج = شروع + شناوری + موظفی
     */
    fun applyFlex(
        entryTime: String,
        startWorkTime: String,
        flexibleMinutes: Int,
        requiredDurationMinutes: Int
    ): FlexOutcome {
        val flexWindowEnd = TimeUtils.addMinutes(startWorkTime, flexibleMinutes.coerceAtLeast(0))
        val entryAbs = absMin(entryTime)
        val startAbs = absMin(startWorkTime)
        val flexEndAbs = absMin(flexWindowEnd)
        val req = requiredDurationMinutes.coerceAtLeast(0)

        return if (entryAbs <= flexEndAbs) {
            // زود / به‌موقع / داخل شناوری — بدون مرخصی تأخیر
            val base = if (entryAbs < startAbs) startWorkTime else entryTime
            // خروج پیشنهادی از لحظه ورود واقعی (حتی اگر زودتر از شروع آمده)
            FlexOutcome(
                suggestedEnd = TimeUtils.addMinutes(entryTime, req),
                entryLeaveMinutes = 0
            )
        } else {
            // دیرتر از شناوری
            FlexOutcome(
                suggestedEnd = TimeUtils.addMinutes(flexWindowEnd, req),
                entryLeaveMinutes = entryAbs - flexEndAbs
            )
        }
    }

    fun suggestedEnd(entryTime: String, requiredMinutes: Int): String =
        TimeUtils.addMinutes(entryTime, requiredMinutes)

    fun earlyLeave(entryTime: String, exitTime: String, requiredMinutes: Int): Int {
        val worked = TimeUtils.minutesBetween(entryTime, exitTime)
        return (requiredMinutes - worked).coerceAtLeast(0)
    }

    fun exitOutcome(exitTime: String, targetEnd: String): Pair<Int, Int> {
        val diff = absMin(exitTime) - absMin(targetEnd)
        return if (diff < 0) (-diff) to 0 else 0 to diff
    }

    fun midDayLeave(prevExit: String, nextEntry: String): Int =
        TimeUtils.minutesBetween(prevExit, nextEntry).coerceAtLeast(0)

    /**
     * محاسبه کامل یک روز.
     * مرخصی = تأخیر ورود اول + فاصله‌های بین جلسات (فقط قبل از خروج پیشنهادی)
     * اضافه‌کار = حضور بعد از خروج پیشنهادی
     * کارکرد = جمع حضورها
     */
    fun computeDayTotals(
        segments: List<PresenceSegment>,
        startWorkTime: String,
        flexibleMinutes: Int,
        requiredMinutes: Int,
        nowTime: String = TimeUtils.nowTime()
    ): DayTotals {
        if (segments.isEmpty()) {
            return DayTotals(0, 0, 0, startWorkTime, 0, 0)
        }
        val ordered = segments.sortedBy { absMin(it.entryTime) }
        val firstEntry = ordered.first().entryTime
        val flex = applyFlex(firstEntry, startWorkTime, flexibleMinutes, requiredMinutes)
        val suggested = flex.suggestedEnd
        val suggestedAbs = absMin(suggested)

        var worked = 0
        var overtime = 0
        for (seg in ordered) {
            val exit = seg.exitTime ?: nowTime
            val e = absMin(seg.entryTime)
            val x = absMin(exit).coerceAtLeast(e)
            worked += (x - e)
            if (x > suggestedAbs) {
                overtime += (x - maxOf(e, suggestedAbs)).coerceAtLeast(0)
            }
        }

        // فاصله بین خروج قبلی و ورود بعدی — فقط تا قبل از خروج پیشنهادی
        var gapLeave = 0
        for (i in 0 until ordered.size - 1) {
            val prevExit = ordered[i].exitTime ?: nowTime
            val nextEntry = ordered[i + 1].entryTime
            val g0 = absMin(prevExit)
            val g1 = absMin(nextEntry)
            if (g1 <= g0) continue
            if (g0 >= suggestedAbs) continue // بعد از خروج پیشنهادی مرخصی نیست
            gapLeave += (minOf(g1, suggestedAbs) - g0).coerceAtLeast(0)
        }

        val leave = flex.entryLeaveMinutes + gapLeave
        return DayTotals(
            workedMinutes = worked.coerceAtLeast(0),
            leaveMinutes = leave.coerceAtLeast(0),
            overtimeMinutes = overtime.coerceAtLeast(0),
            suggestedEnd = suggested,
            entryLeaveMinutes = flex.entryLeaveMinutes,
            gapLeaveMinutes = gapLeave,
            earlyLeaveMinutes = 0
        )
    }

    private fun absMin(t: String): Int = TimeUtils.minutesBetween("00:00", t)
}
