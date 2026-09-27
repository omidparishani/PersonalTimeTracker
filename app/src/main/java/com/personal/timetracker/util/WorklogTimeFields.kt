package com.personal.timetracker.util

import android.app.TimePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

/**
 * فیلدهای زمان Worklog شبیه MyWork جیرا:
 * - ساعت شروع / پایان با دیالوگ TimePicker
 * - مدت: ساعت + دقیقه
 * تغییر هر طرف، طرف دیگر را خودکار به‌روز می‌کند.
 */
object WorklogTimeFields {

    data class Bundle(
        val root: LinearLayout,
        val startEdit: TextInputEditText,
        val endEdit: TextInputEditText,
        val hoursEdit: TextInputEditText,
        val minutesEdit: TextInputEditText
    ) {
        fun durationMinutes(): Int {
            val h = hoursEdit.text?.toString()?.toIntOrNull() ?: 0
            val m = minutesEdit.text?.toString()?.toIntOrNull() ?: 0
            var dur = h * 60 + m
            if (dur <= 0) {
                val st = normalizeHm(startEdit.text?.toString())
                val en = normalizeHm(endEdit.text?.toString())
                if (st != null && en != null) {
                    dur = TimeUtils.minutesBetween(st, en)
                }
            }
            return dur
        }

        fun startHm(): String =
            normalizeHm(startEdit.text?.toString())
                ?: TimeUtils.addMinutes(TimeUtils.nowTime(), -60).take(5)

        fun endHm(): String =
            normalizeHm(endEdit.text?.toString())
                ?: TimeUtils.nowTime().take(5)
    }

    fun build(
        ctx: Context,
        primary: Int,
        dark: Boolean,
        defaultStart: String? = null,
        defaultEnd: String? = null,
        defaultDurationMinutes: Int = 60
    ): Bundle {
        val now = TimeUtils.nowTime().take(5)
        var start = normalizeHm(defaultStart) ?: TimeUtils.addMinutes(now, -60).take(5)
        var end = normalizeHm(defaultEnd)
        if (end == null) {
            end = if (defaultDurationMinutes > 0) {
                TimeUtils.addMinutes(start, defaultDurationMinutes).take(5)
            } else now
        }
        var dur = TimeUtils.minutesBetween(start, end)
        if (dur <= 0 && defaultDurationMinutes > 0) {
            dur = defaultDurationMinutes
            end = TimeUtils.addMinutes(start, dur).take(5)
        }
        if (dur <= 0) dur = 60

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        root.addView(DialogHelper.sectionLabel(ctx, "ساعت شروع / پایان", dark))
        val timeRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (stL, stE) = DialogHelper.inputField(ctx, "شروع (HH:mm)", start, primary)
        val (enL, enE) = DialogHelper.inputField(ctx, "پایان (HH:mm)", end, primary)
        stL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        enL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        // فقط از طریق TimePicker انتخاب شود
        makeTimePickerField(ctx, stE, is24Hour = true)
        makeTimePickerField(ctx, enE, is24Hour = true)
        timeRow.addView(stL)
        timeRow.addView(enL)
        root.addView(timeRow)

        root.addView(DialogHelper.sectionLabel(ctx, "مدت (ساعت و دقیقه)", dark))
        val durRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hL, hE) = DialogHelper.inputField(ctx, "ساعت", (dur / 60).toString(), primary, number = true)
        val (mL, mE) = DialogHelper.inputField(ctx, "دقیقه", (dur % 60).toString(), primary, number = true)
        hL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        mL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        durRow.addView(hL)
        durRow.addView(mL)
        root.addView(durRow)

        root.addView(TextView(ctx).apply {
            text = "با ضربه روی شروع/پایان، انتخاب‌گر ساعت باز می‌شود. تغییر مدت، پایان را به‌روز می‌کند."
            textSize = 11.5f
            setTextColor(ThemeHelper.textSecondary(dark))
            setPadding(DialogHelper.dp(ctx, 4), DialogHelper.dp(ctx, 4), 0, 0)
        })

        var syncing = false

        fun setTextQuiet(edit: TextInputEditText, value: String) {
            if (edit.text?.toString() == value) return
            syncing = true
            edit.setText(value)
            edit.setSelection(value.length.coerceAtMost(edit.text?.length ?: 0))
            syncing = false
        }

        fun syncDurationFromRange() {
            if (syncing) return
            val st = normalizeHm(stE.text?.toString()) ?: return
            val en = normalizeHm(enE.text?.toString()) ?: return
            var minutes = TimeUtils.minutesBetween(st, en)
            if (minutes < 0) minutes += 24 * 60
            if (minutes <= 0) return
            if (minutes > 24 * 60) minutes = 24 * 60
            syncing = true
            hE.setText((minutes / 60).toString())
            mE.setText((minutes % 60).toString())
            syncing = false
        }

        fun syncEndFromDuration() {
            if (syncing) return
            val st = normalizeHm(stE.text?.toString()) ?: return
            val h = hE.text?.toString()?.toIntOrNull() ?: 0
            val m = mE.text?.toString()?.toIntOrNull() ?: 0
            val minutes = (h * 60 + m).coerceAtLeast(0)
            if (minutes <= 0) return
            setTextQuiet(enE, TimeUtils.addMinutes(st, minutes).take(5))
        }

        val rangeWatcher = simpleWatcher { syncDurationFromRange() }
        val durationWatcher = simpleWatcher { syncEndFromDuration() }
        stE.addTextChangedListener(rangeWatcher)
        enE.addTextChangedListener(rangeWatcher)
        hE.addTextChangedListener(durationWatcher)
        mE.addTextChangedListener(durationWatcher)

        return Bundle(root, stE, enE, hE, mE)
    }

    /** فیلد فقط‌خواندنی که با ضربه TimePicker باز می‌کند */
    private fun makeTimePickerField(ctx: Context, edit: TextInputEditText, is24Hour: Boolean) {
        edit.isFocusable = false
        edit.isClickable = true
        edit.isCursorVisible = false
        edit.setOnClickListener {
            val current = normalizeHm(edit.text?.toString())
            val cal = Calendar.getInstance()
            var hour = cal.get(Calendar.HOUR_OF_DAY)
            var minute = cal.get(Calendar.MINUTE)
            if (current != null) {
                val p = current.split(":")
                hour = p[0].toIntOrNull() ?: hour
                minute = p.getOrNull(1)?.toIntOrNull() ?: minute
            }
            TimePickerDialog(
                ctx,
                { _, h, m ->
                    edit.setText(String.format("%02d:%02d", h, m))
                },
                hour,
                minute,
                is24Hour
            ).show()
        }
    }

    private fun simpleWatcher(onChange: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            onChange()
        }
    }

    fun normalizeHm(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return null
        val parts = t.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].take(2).toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return String.format("%02d:%02d", h, m)
    }
}
