package com.personal.timetracker.util

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * A simple Jalali (Persian) date picker dialog built without any external library.
 * Shows three NumberPickers for year, month (Persian names), and day.
 * Calls [onDateSelected] with the Gregorian date string (yyyy-MM-dd) upon confirmation.
 */
object JalaliDatePickerDialog {

    private val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun show(
        ctx: Context,
        primary: Int,
        dark: Boolean,
        initialGregorianDate: String? = null,
        onDateSelected: (gregorianDateStr: String, jalaliDisplay: String) -> Unit
    ) {
        // Parse initial date or use today
        val today = TimeUtils.toJalali()
        val initJ: IntArray = if (!initialGregorianDate.isNullOrBlank()) {
            try { TimeUtils.toJalali(TimeUtils.parseDate(initialGregorianDate)) }
            catch (_: Exception) { today }
        } else today

        var selectedYear = initJ[0]
        var selectedMonth = initJ[1]
        var selectedDay = initJ[2]

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 8)
            gravity = Gravity.CENTER
        }

        val title = TextView(ctx).apply {
            text = "انتخاب تاریخ شمسی"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ThemeHelper.textPrimary(dark))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        // Year picker
        val yearPicker = NumberPicker(ctx).apply {
            minValue = today[0] - 5
            maxValue = today[0] + 5
            value = selectedYear
            wrapSelectorWheel = false
            displayedValues = (minValue..maxValue).map { TimeUtils.faNum(it) }.toTypedArray()
            setOnValueChangedListener { _, _, newVal ->
                selectedYear = minValue + (newVal - minValue)
                // Fix day if needed
                val maxDay = TimeUtils.jalaliMonthDays(selectedYear, selectedMonth)
                if (selectedDay > maxDay) selectedDay = maxDay
            }
        }

        // Month picker
        val monthPicker = NumberPicker(ctx).apply {
            minValue = 1
            maxValue = 12
            value = selectedMonth
            displayedValues = monthNames
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newVal ->
                selectedMonth = newVal
                val maxDay = TimeUtils.jalaliMonthDays(selectedYear, selectedMonth)
                if (selectedDay > maxDay) selectedDay = maxDay
            }
        }

        // Day picker
        val dayCount = TimeUtils.jalaliMonthDays(selectedYear, selectedMonth)
        val dayPicker = NumberPicker(ctx).apply {
            minValue = 1
            maxValue = dayCount
            value = selectedDay.coerceIn(1, dayCount)
            displayedValues = (1..dayCount).map { TimeUtils.faNum(it) }.toTypedArray()
            wrapSelectorWheel = false
            setOnValueChangedListener { _, _, newVal -> selectedDay = newVal }
        }

        fun updateDayPicker() {
            val max = TimeUtils.jalaliMonthDays(selectedYear, selectedMonth)
            dayPicker.displayedValues = null
            dayPicker.maxValue = max
            dayPicker.displayedValues = (1..max).map { TimeUtils.faNum(it) }.toTypedArray()
            if (dayPicker.value > max) dayPicker.value = max
            selectedDay = dayPicker.value
        }

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            selectedYear = yearPicker.minValue + (newVal - yearPicker.minValue)
            updateDayPicker()
        }
        monthPicker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal
            updateDayPicker()
        }

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = 8; marginEnd = 8
        }
        row.addView(yearPicker, lp)
        row.addView(monthPicker, lp)
        row.addView(dayPicker, lp)
        layout.addView(row)

        // Live preview
        val preview = TextView(ctx).apply {
            textSize = 13f
            setTextColor(ThemeHelper.textSecondary(dark))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        fun refreshPreview() {
            preview.text = "${TimeUtils.faNum(selectedDay)} ${monthNames[selectedMonth - 1]} ${TimeUtils.faNum(selectedYear)}"
        }
        refreshPreview()
        layout.addView(preview)

        // refresh preview when picker changes
        val refresher = View.OnScrollChangeListener { _, _, _, _, _ -> refreshPreview() }
        yearPicker.setOnScrollChangeListener(refresher)
        monthPicker.setOnScrollChangeListener(refresher)
        dayPicker.setOnScrollChangeListener(refresher)

        AlertDialog.Builder(ctx)
            .setView(layout)
            .setPositiveButton("تأیید") { _, _ ->
                selectedDay = dayPicker.value
                selectedMonth = monthPicker.value
                selectedYear = yearPicker.value
                try {
                    val greg = TimeUtils.fromJalali(selectedYear, selectedMonth, selectedDay)
                    val gregStr = TimeUtils.formatDate(greg)
                    val jalDisplay = "${TimeUtils.faNum(selectedDay)} ${monthNames[selectedMonth - 1]} ${TimeUtils.faNum(selectedYear)}"
                    onDateSelected(gregStr, jalDisplay)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(ctx, "تاریخ نامعتبر: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }
}
