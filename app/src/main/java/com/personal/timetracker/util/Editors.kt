package com.personal.timetracker.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.data.entity.TaskLogEntity
import com.personal.timetracker.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Shared attendance add/edit dialog — same styled form used by the Attendance screen,
 * and reusable from Dashboard / Calendar / Reports so a tap there can edit in place
 * instead of only redirecting.
 */
object AttendanceEditor {
    fun open(
        ctx: Context,
        existing: AttendanceEntity?,
        repo: AppRepository,
        scope: CoroutineScope,
        primary: Int,
        dark: Boolean,
        defaultDate: String? = null,
        onDone: () -> Unit = {}
    ) {
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark))
        val (dateL, dateField) = DialogHelper.inputField(ctx, "تاریخ (yyyy-MM-dd)", existing?.date ?: defaultDate ?: TimeUtils.today(), primary)
        dateField.isFocusable = false
        dateField.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                ctx,
                { _, y, m, d -> dateField.setText("%04d-%02d-%02d".format(y, m + 1, d)) },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        layout.addView(dateL)

        layout.addView(DialogHelper.sectionLabel(ctx, "زمان", dark))
        val timeRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (entryL, entryField) = DialogHelper.inputField(ctx, "ورود HH:mm", existing?.entryTime ?: TimeUtils.nowTime(), primary)
        entryField.isFocusable = false
        entryField.setOnClickListener {
            val parts = entryField.text.toString().split(":")
            TimePickerDialog(
                ctx,
                { _, h, m -> entryField.setText("%02d:%02d".format(h, m)) },
                parts.getOrNull(0)?.toIntOrNull() ?: 9,
                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                true
            ).show()
        }
        val (exitL, exitField) = DialogHelper.inputField(ctx, "خروج (خالی = بدون خروج)", existing?.exitTime ?: "", primary)
        exitField.isFocusable = false
        exitField.setOnClickListener {
            val raw = exitField.text?.toString()?.ifEmpty { "17:00" } ?: "17:00"
            val parts = raw.split(":")
            TimePickerDialog(
                ctx,
                { _, h, m -> exitField.setText("%02d:%02d".format(h, m)) },
                parts.getOrNull(0)?.toIntOrNull() ?: 17,
                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                true
            ).show()
        }
        entryL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        exitL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        timeRow.addView(entryL)
        timeRow.addView(exitL)
        layout.addView(timeRow)

        DialogHelper.show(
            ctx = ctx,
            icon = if (existing == null) "＋" else "✎",
            title = if (existing == null) "ثبت تردد" else "ویرایش تردد",
            primary = primary, dark = dark, body = layout, positiveText = "ذخیره",
            onPositive = {
                val date = dateField.text?.toString()?.trim().orEmpty()
                val entry = entryField.text?.toString()?.trim().orEmpty()
                val exit = exitField.text?.toString()?.trim()?.ifBlank { null }
                if (date.isEmpty() || entry.isEmpty()) {
                    Toast.makeText(ctx, "تاریخ و ساعت ورود الزامی است", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    scope.launch {
                        if (existing == null) {
                            repo.addAttendance(date, entry, exit)
                        } else {
                            repo.updateAttendance(existing.copy(date = date, entryTime = entry, exitTime = exit))
                        }
                        Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                    true
                }
            }
        )
    }

    fun confirmDelete(
        ctx: Context, item: AttendanceEntity, repo: AppRepository, scope: CoroutineScope,
        primary: Int, dark: Boolean, onDone: () -> Unit = {}
    ) {
        DialogHelper.confirm(
            ctx = ctx, title = "حذف تردد", message = "این تردد حذف شود؟",
            primary = primary, dark = dark
        ) {
            scope.launch {
                repo.deleteAttendance(item)
                onDone()
            }
        }
    }
}

/**
 * Shared task-log add/edit dialog — same styled form used by the Tasks screen, and
 * reusable from Dashboard / Calendar / Reports.
 */
object TaskLogEditor {
    fun openNew(
        ctx: Context, task: TaskEntity, repo: AppRepository, scope: CoroutineScope,
        primary: Int, dark: Boolean, onDone: () -> Unit = {}
    ) {
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark))
        val jalaliHint = TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(TimeUtils.today())
            textSize = 12f
            setTextColor(primary)
            setPadding(DialogHelper.dp(ctx, 4), DialogHelper.dp(ctx, 4), 0, 0)
        }
        val (dateL, dateField) = DialogHelper.inputField(ctx, "تاریخ لاگ (yyyy-MM-dd)", TimeUtils.today(), primary)
        dateField.isFocusable = false
        dateField.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(ctx, { _, y, m, d ->
                val iso = "%04d-%02d-%02d".format(y, m + 1, d)
                dateField.setText(iso)
                jalaliHint.text = TimeUtils.toJalaliDisplay(iso)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        layout.addView(dateL)
        layout.addView(jalaliHint)

        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hoursL, hours) = DialogHelper.inputField(ctx, "ساعت", "1", primary, number = true)
        val (minsL, mins) = DialogHelper.inputField(ctx, "دقیقه", "0", primary, number = true)
        hoursL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = DialogHelper.dp(ctx, 10) }
        minsL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hoursL)
        row.addView(minsL)
        layout.addView(row)

        layout.addView(DialogHelper.sectionLabel(ctx, "یادداشت (اختیاری)", dark))
        val (noteL, note) = DialogHelper.inputField(ctx, "یادداشت", "", primary, multiline = true)
        layout.addView(noteL)

        DialogHelper.show(
            ctx = ctx, icon = "⏱", title = "ثبت لاگ جدید", subtitle = task.taskTitle,
            primary = primary, dark = dark, body = layout, positiveText = "ثبت",
            onPositive = {
                val date = dateField.text?.toString()?.trim().orEmpty().ifEmpty { TimeUtils.today() }
                val dur = ((hours.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                        ((mins.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    scope.launch {
                        repo.addLog(task, date, dur, note = note.text?.toString())
                        Toast.makeText(ctx, "لاگ ثبت شد", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                    true
                }
            }
        )
    }

    fun openEdit(
        ctx: Context, task: TaskEntity, log: TaskLogEntity, repo: AppRepository, scope: CoroutineScope,
        primary: Int, dark: Boolean, onDone: () -> Unit = {}
    ) {
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark))
        val jalaliHint = TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(log.date)
            textSize = 12f
            setTextColor(primary)
            setPadding(DialogHelper.dp(ctx, 4), DialogHelper.dp(ctx, 4), 0, 0)
        }
        val (dateL, dateField) = DialogHelper.inputField(ctx, "تاریخ لاگ (yyyy-MM-dd)", log.date, primary)
        dateField.isFocusable = false
        dateField.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(ctx, { _, y, m, d ->
                val iso = "%04d-%02d-%02d".format(y, m + 1, d)
                dateField.setText(iso)
                jalaliHint.text = TimeUtils.toJalaliDisplay(iso)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        layout.addView(dateL)
        layout.addView(jalaliHint)

        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hoursL, hours) = DialogHelper.inputField(ctx, "ساعت", (log.duration / 60).toString(), primary, number = true)
        val (minsL, mins) = DialogHelper.inputField(ctx, "دقیقه", (log.duration % 60).toString(), primary, number = true)
        hoursL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = DialogHelper.dp(ctx, 10) }
        minsL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hoursL)
        row.addView(minsL)
        layout.addView(row)

        layout.addView(DialogHelper.sectionLabel(ctx, "یادداشت (اختیاری)", dark))
        val (noteL, note) = DialogHelper.inputField(ctx, "یادداشت", log.note ?: "", primary, multiline = true)
        layout.addView(noteL)

        DialogHelper.show(
            ctx = ctx, icon = "✎", title = "ویرایش لاگ", subtitle = task.taskTitle,
            primary = primary, dark = dark, body = layout, positiveText = "ذخیره",
            onPositive = {
                val date = dateField.text?.toString()?.trim().orEmpty().ifEmpty { TimeUtils.today() }
                val dur = ((hours.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                        ((mins.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    scope.launch {
                        repo.updateLog(log.copy(date = date, duration = dur, note = note.text?.toString()))
                        repo.recalculateTask(task.id)
                        Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                    true
                }
            }
        )
    }

    fun confirmDelete(
        ctx: Context, task: TaskEntity, log: TaskLogEntity, repo: AppRepository, scope: CoroutineScope,
        primary: Int, dark: Boolean, onDone: () -> Unit = {}
    ) {
        DialogHelper.confirm(
            ctx = ctx,
            title = "حذف لاگ",
            message = "${TimeUtils.toJalaliDisplay(log.date)} — ${TimeUtils.formatDuration(log.duration)}",
            primary = primary, dark = dark
        ) {
            scope.launch {
                repo.deleteLog(log)
                repo.recalculateTask(task.id)
                onDone()
            }
        }
    }
}
