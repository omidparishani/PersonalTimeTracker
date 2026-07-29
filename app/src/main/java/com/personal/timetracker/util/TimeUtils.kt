package com.personal.timetracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private val weekDays =
        arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")
    private val months = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun today(): String = dateFmt.format(Date())
    fun nowTime(): String = timeFmt.format(Date())
    fun nowDateTime(): String = dateTimeFmt.format(Date())
    fun formatDate(date: Date): String = dateFmt.format(date)
    fun parseDate(s: String): Date = dateFmt.parse(s) ?: Date()

    fun minutesBetween(start: String, end: String): Int {
        val sp = start.split(":").map { it.toIntOrNull() ?: 0 }
        val ep = end.split(":").map { it.toIntOrNull() ?: 0 }
        if (sp.size < 2 || ep.size < 2) return 0
        var mins = (ep[0] * 60 + ep[1]) - (sp[0] * 60 + sp[1])
        if (mins < 0) mins += 24 * 60
        return mins
    }

    fun addMinutes(time: String, minutes: Int): String {
        val p = time.split(":").map { it.toIntOrNull() ?: 0 }
        if (p.size < 2) return time
        val total = p[0] * 60 + p[1] + minutes
        val h = ((total / 60) % 24 + 24) % 24
        val m = ((total % 60) + 60) % 60
        return "%02d:%02d".format(h, m)
    }

    fun formatDuration(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        val h = m / 60
        val min = m % 60
        return when {
            h == 0 -> "$min دقیقه"
            min == 0 -> "$h ساعت"
            else -> "$h ساعت و $min دقیقه"
        }
    }

    /** Gregorian Date -> Jalali triple */
    fun toJalali(date: Date = Date()): IntArray {
        val c = Calendar.getInstance().apply { time = date }
        var gy = c.get(Calendar.YEAR)
        val gm = c.get(Calendar.MONTH) + 1
        val gd = c.get(Calendar.DAY_OF_MONTH)

        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var jy = if (gy <= 1600) 0 else 979
        gy -= if (gy <= 1600) 621 else 1600
        val gy2 = if (gm > 2) gy + 1 else gy
        var days =
            365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + g_d_m[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }

    fun toJalaliShort(date: Date = Date()): String {
        val j = toJalali(date)
        return "${j[2]} ${months[j[1] - 1]} ${j[0]}"
    }

    fun toJalaliDisplay(dateStr: String): String {
        return try {
            val d = parseDate(dateStr)
            val j = toJalali(d)
            val cal = Calendar.getInstance().apply { time = d }
            val wd = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]
            "$wd، ${j[2]} ${months[j[1] - 1]} ${j[0]}"
        } catch (_: Exception) {
            dateStr
        }
    }

    fun startOfWeek(): String {
        val c = Calendar.getInstance()
        val day = c.get(Calendar.DAY_OF_WEEK) // 1=Sun
        // Saturday as first day of week in Iran: Sat=7 in Calendar
        val diff = (day + 1) % 7 // days since Saturday
        c.add(Calendar.DAY_OF_MONTH, -diff)
        return dateFmt.format(c.time)
    }

    fun startOfMonth(): String {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return dateFmt.format(c.time)
    }


    /** Days in Jalali month (1-12) */
    fun jalaliMonthDays(jy: Int, jm: Int): Int {
        return when (jm) {
            in 1..6 -> 31
            in 7..11 -> 30
            else -> if (isJalaliLeap(jy)) 30 else 29
        }
    }

    fun isJalaliLeap(jy: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
            1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        val jp = breaks[0]
        var jump = 0
        var leap = -14
        var jy2 = jy
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy2 < jm) break
            leap += jump / 33 * 8 + (jump % 33) / 4
            // continue
        }
        val n = jy2 - (if (jy2 < breaks[1]) breaks[0] else breaks[breaks.size - 2])
        // simplified leap: cycle 33
        val r = (jy + 2346) % 33
        return r in listOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    /** Convert Jalali y,m,d to Gregorian Date */
    fun fromJalali(jy: Int, jm: Int, jd: Int): java.util.Date {
        // Algorithm inverse of toJalali
        val jy0 = jy - 979
        val days = 365 * jy0 + jy0 / 33 * 8 + (jy0 % 33 + 3) / 4 +
                jd + if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        val gy0 = 1600
        var gDayNo = days + 79
        var gy = gy0 + 400 * (gDayNo / 146097)
        gDayNo %= 146097
        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) gDayNo++ else leap = false
        }
        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461
        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }
        val salA = intArrayOf(
            0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        var gd = gDayNo + 1
        for (i in 1..12) {
            if (gd <= salA[i]) {
                gm = i; break
            }
            gd -= salA[i]
        }
        val cal = java.util.Calendar.getInstance()
        cal.set(gy, gm - 1, gd, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun jalaliMonthName(jm: Int): String = listOf(
        "", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    ).getOrElse(jm) { "" }

    fun weekdayJalali(jy: Int, jm: Int, jd: Int): Int {
        val d = fromJalali(jy, jm, jd)
        val c = java.util.Calendar.getInstance().apply { time = d }
        // Calendar.SUNDAY=1 ... convert to Sat=0
        return (c.get(java.util.Calendar.DAY_OF_WEEK) % 7)
    }
}
