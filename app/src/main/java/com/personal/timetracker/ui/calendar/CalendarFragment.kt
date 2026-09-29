package com.personal.timetracker.ui.calendar

/**
 * توضیح فایل: تقویم شمسی؛ تردد روز و Worklogهای کاربر جاری.
 * بسته: com.personal.timetracker.ui.calendar
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.AttendanceEditor
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import com.personal.timetracker.util.WorklogTimeFields
import com.personal.timetracker.util.JalaliDatePickerDialog
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * تقویم شمسی ماهانه + جزئیات تردد و تسک هر روز
 */
class CalendarFragment : Fragment() {
    private val repo get() = (requireActivity().application as App).repository

    private lateinit var monthTitle: TextView
    private lateinit var grid: GridLayout
    private lateinit var detailBox: LinearLayout
    private var jy = 0
    private var jm = 0
    private var selectedJd = 1

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

    /**
     * ساخت و برگرداندن View ریشه این Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val todayJ = TimeUtils.toJalali(Calendar.getInstance().time)
        jy = todayJ[0]; jm = todayJ[1]; selectedJd = todayJ[2]

        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.surface(dark()))
            setPadding(12)
        }
        root.addView(ThemeHelper.pageTitle(ctx, "تقویم شمسی", dark()))

        val nav = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val prev = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "‹"
            setOnClickListener {
                jm--; if (jm < 1) { jm = 12; jy-- }
                loadMonthHolidays(); loadDay()
            }
        }
        monthTitle = TextView(ctx).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(ThemeHelper.textPrimary(dark()))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val next = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "›"
            setOnClickListener {
                jm++; if (jm > 12) { jm = 1; jy++ }
                loadMonthHolidays(); loadDay()
            }
        }
        ThemeHelper.applyButton(prev, primary(), false)
        ThemeHelper.applyButton(next, primary(), false)
        nav.addView(prev); nav.addView(monthTitle); nav.addView(next)
        root.addView(nav)

        // weekday headers (شنبه ... جمعه)
        val headers = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { w ->
            headers.addView(TextView(ctx).apply {
                text = w
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        root.addView(headers)

        grid = GridLayout(ctx).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        root.addView(grid)

        detailBox =
            LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 0) }
        root.addView(android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(detailBox)
        })

        loadMonthHolidays()
        loadDay()
        return root
    }

    // نگه‌داری مناسبت‌ها برای ماه جاری
    private var monthHolidays: Map<String, String> = emptyMap() // date-iso -> title

    private fun renderMonth() {
        monthTitle.text = "${TimeUtils.jalaliMonthName(jm)} ${TimeUtils.faNum(jy)}"
        grid.removeAllViews()
        val daysInMonth = TimeUtils.jalaliMonthDays(jy, jm)
        val startWeekday = TimeUtils.weekdayJalali(jy, jm, 1) // 0=Sat
        val primary = primary()
        val dark = dark()
        val today = TimeUtils.toJalali(Calendar.getInstance().time)

        // رنگ‌ها
        val holidayTextColor = if (dark) 0xFFFF5252.toInt() else 0xFFC62828.toInt()
        val weekendTextColor  = if (dark) 0xFFFF8A65.toInt() else 0xFFE64A19.toInt()
        val todayRingColor    = primary

        // empty cells
        for (i in 0 until startWeekday) {
            grid.addView(TextView(requireContext()).apply { layoutParams = cellLp() })
        }
        for (d in 1..daysInMonth) {
            val dateIso = TimeUtils.formatDate(TimeUtils.fromJalali(jy, jm, d))
            val weekday = TimeUtils.weekdayJalali(jy, jm, d)  // 0=Sat…6=Fri
            val isFriday   = weekday == 6
            val isThursday = weekday == 5
            val isHoliday  = monthHolidays.containsKey(dateIso)
            val holidayTitle = monthHolidays[dateIso]

            val isSelected = d == selectedJd
            val isToday    = jy == today[0] && jm == today[1] && d == today[2]

            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(2, 6, 2, 6)
                layoutParams = cellLp()
                isClickable = true
                isFocusable = true
                setOnClickListener { selectedJd = d; renderMonth(); loadDay() }
            }

            val numTv = TextView(requireContext()).apply {
                text = TimeUtils.faNum(d)
                gravity = Gravity.CENTER
                textSize = 13f
                when {
                    isSelected -> {
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(primary)
                            setSize(
                                (34 * resources.displayMetrics.density).toInt(),
                                (34 * resources.displayMetrics.density).toInt()
                            )
                        }
                        setTextColor(ThemeHelper.onColor(primary))
                        setTypeface(null, Typeface.BOLD)
                    }
                    isToday -> {
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setStroke(
                                (2 * resources.displayMetrics.density).toInt(),
                                todayRingColor
                            )
                            setColor(android.graphics.Color.TRANSPARENT)
                            setSize(
                                (34 * resources.displayMetrics.density).toInt(),
                                (34 * resources.displayMetrics.density).toInt()
                            )
                        }
                        setTextColor(if (isHoliday || isFriday) holidayTextColor
                                     else if (isThursday) weekendTextColor
                                     else primary)
                        setTypeface(null, Typeface.BOLD)
                    }
                    isHoliday || isFriday -> setTextColor(holidayTextColor)
                    isThursday -> setTextColor(weekendTextColor)
                    else -> setTextColor(ThemeHelper.textPrimary(dark))
                }
            }
            cell.addView(numTv)

            // نقطه کوچک برای مناسبت
            if (!holidayTitle.isNullOrBlank() && !isSelected) {
                cell.addView(View(requireContext()).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(holidayTextColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        (5 * resources.displayMetrics.density).toInt(),
                        (5 * resources.displayMetrics.density).toInt()
                    ).apply { topMargin = (2 * resources.displayMetrics.density).toInt() }
                })
            }
            grid.addView(cell)
        }
    }

    /** بارگذاری تعطیلات ماه جاری و رندر مجدد */
    private fun loadMonthHolidays() {
        lifecycleScope.launch {
            val repo = (requireActivity().application as App).repository
            val startIso = TimeUtils.formatDate(TimeUtils.fromJalali(jy, jm, 1))
            val endIso   = TimeUtils.formatDate(TimeUtils.fromJalali(jy, jm, TimeUtils.jalaliMonthDays(jy, jm)))
            val all = repo.getHolidaysOnce()
            monthHolidays = all.filter { it.date >= startIso && it.date <= endIso }
                               .associate { it.date to it.title }
            renderMonth()
        }
    }

    private fun cellLp() = GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(2, 2, 2, 2)
    }

    private fun selectedIso(): String {
        val date = TimeUtils.fromJalali(jy, jm, selectedJd)
        return TimeUtils.formatDate(date)
    }

    /**
     * بارگذاری داده‌های روز انتخاب‌شده در تقویم.
     */
    private fun loadDay() {
        val d = selectedIso()
        val repo = (requireActivity().application as App).repository
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        detailBox.removeAllViews()
        val selectedIsoDate = selectedIso()
        val holidayTitle = monthHolidays[selectedIsoDate]
        detailBox.addView(TextView(ctx).apply {
            val wd = when (TimeUtils.weekdayJalali(jy, jm, selectedJd)) {
                0 -> "شنبه"; 1 -> "یکشنبه"; 2 -> "دوشنبه"
                3 -> "سه‌شنبه"; 4 -> "چهارشنبه"; 5 -> "پنجشنبه"
                6 -> "جمعه"; else -> ""
            }
            text = buildString {
                append("$wd، ${TimeUtils.faNum(selectedJd)} ${TimeUtils.jalaliMonthName(jm)} ${TimeUtils.faNum(jy)}")
                if (!holidayTitle.isNullOrBlank()) {
                    append("\n"); append(holidayTitle)
                }
            }
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (!holidayTitle.isNullOrBlank()) {
                if (dark) 0xFFFF5252.toInt() else 0xFFC62828.toInt()
            } else primary)
            setPadding(4, 4, 4, 12)
        })
        lifecycleScope.launch {
            // سینک Worklogهای همین روز از سرور (بدون نیاز به باز کردن تک‌تک Issue)
            try {
                repo.syncWorklogsForDate(d)
            } catch (_: Exception) { }
            val att = repo.getByDateOnce(d)
            val logs = repo.getJiraWorklogsForDate(d)
            val tasks = repo.getTasksByDateOnce(d)

            // Summary card
            val worked = att.sumOf {
                if (it.exitTime != null) it.duration
                else if (it.status == "active") TimeUtils.minutesBetween(
                    it.entryTime,
                    TimeUtils.nowTime()
                )
                else 0
            }
            val leave = att.sumOf { it.leaveDuration }
            val ot = att.sumOf { it.overtimeDuration }

            detailBox.addView(card(ctx, "جمع روز", buildString {
                append("کار: "); append(TimeUtils.formatDuration(worked))
                append("   ·   مرخصی: "); append(TimeUtils.formatDuration(leave))
                append("   ·   اضافه‌کار: "); append(TimeUtils.formatDuration(ot))
                append("   ·   لاگ تسک: "); append(TimeUtils.formatDuration(logs.sumOf { it.durationMinutes }))
            }))

            // Attendance card with inline edit/delete
            val attCard = MaterialCardView(ctx)
            ThemeHelper.applyCard(attCard, dark)
            val attBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 18, 24, 18)
            }
            attBox.addView(TextView(ctx).apply {
                text = "ترددها"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(primary)
            })
            if (att.isEmpty()) {
                attBox.addView(TextView(ctx).apply {
                    text = "ترددی نیست"; textSize = 12.5f; setTextColor(ThemeHelper.textSecondary(dark))
                    setPadding(0, 8, 0, 0)
                })
            } else {
                att.forEach { r ->
                    attBox.addView(rowWithActions(
                        ctx,
                        if (r.exitTime != null) "${r.entryTime}  →  ${r.exitTime}" else "${r.entryTime} (فعال)",
                        primary, dark,
                        onEdit = { AttendanceEditor.open(ctx, r, repo, lifecycleScope, primary, dark) { loadDay() } },
                        onDelete = { AttendanceEditor.confirmDelete(ctx, r, repo, lifecycleScope, primary, dark) { loadDay() } },
                        extra = if (r.exitTime != null) com.personal.timetracker.util.ChartHelper.attendanceDonut(
                            ctx, r.duration, r.overtimeDuration, r.leaveDuration, dark, primary, sizeDp = 34
                        ) else null
                    ))
                }
            }
            val addAttBtn = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "＋ ثبت تردد این روز"
                textSize = 11f
                ThemeHelper.applyButton(this, primary, false)
                setOnClickListener {
                    AttendanceEditor.open(
                        ctx, null, repo, lifecycleScope, primary, dark, defaultDate = d
                    ) { loadDay() }
                }
            }
            attBox.addView(addAttBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 })
            attCard.addView(attBox)
            attCard.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            detailBox.addView(attCard)

            // Task logs card with inline edit/delete
            val logCard = MaterialCardView(ctx)
            ThemeHelper.applyCard(logCard, dark)
            val logBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 18, 24, 18)
            }
            logBox.addView(TextView(ctx).apply {
                text = "تسک‌ها و لاگ‌ها"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(primary)
            })
            logBox.addView(MaterialButton(ctx).apply {
                text = "＋ ثبت لاگ روی Issue"
                ThemeHelper.applyButton(this, primary, true)
                setOnClickListener { showAddWorklogSearchDialog(d) }
            })
            if (logs.isEmpty()) {
                logBox.addView(TextView(ctx).apply {
                    text = "موردی نیست"; textSize = 12.5f; setTextColor(ThemeHelper.textSecondary(dark))
                    setPadding(0, 8, 0, 0)
                })
            } else {
                logs.forEach { log ->
                    val label = buildString {
                        append(log.issueKey)
                        val started = log.started?.take(16)?.replace("T", " ")
                        if (!started.isNullOrBlank()) {
                            append("  ·  ")
                            append(started)
                        }
                        append("  ·  ")
                        append(TimeUtils.formatDuration(log.durationMinutes))
                        if (!log.comment.isNullOrBlank()) {
                            append(" — ")
                            append(log.comment.take(40))
                        }
                        when (log.syncStatus) {
                            "pending_add", "pending_update", "pending_delete" -> append(" ⏳")
                        }
                    }
                    logBox.addView(rowWithActions(
                        ctx, label, primary, dark,
                        onEdit = {
                            showEditWorklogDialog(log)
                        },
                        onDelete = {
                            DialogHelper.confirm(
                                ctx = ctx,
                                title = "حذف Worklog",
                                message = "${log.issueKey} — ${TimeUtils.formatDuration(log.durationMinutes)}",
                                primary = primary,
                                dark = dark
                            ) {
                                lifecycleScope.launch {
                                    repo.deleteJiraTaskLog(log.localId)
                                    loadDay()
                                }
                            }
                        },
                        onDetails = {
                            showIssueDetailsDialog(log.issueKey)
                        }
                    ))
                }
            }
            logCard.addView(logBox)
            logCard.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            detailBox.addView(logCard)

            // Issueهای در حال انجام از جیرا
            try {
                val openIssues = repo.jiraServiceOrNull()?.fetchAssigned(openOnly = true)?.getOrNull().orEmpty()
                val inProgress = openIssues.filter {
                    it.statusCategory == "indeterminate" ||
                        it.statusName.contains("progress", true) ||
                        it.statusName.contains("در حال", true)
                }
                if (inProgress.isNotEmpty()) {
                    val actCard = MaterialCardView(ctx)
                    ThemeHelper.applyCard(actCard, dark)
                    val actBox = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(24, 18, 24, 18)
                    }
                    actBox.addView(TextView(ctx).apply {
                        text = "Issue فعال (در حال انجام)"
                        textSize = 13f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(primary)
                    })
                    inProgress.take(5).forEach { iss ->
                        actBox.addView(TextView(ctx).apply {
                            text = "${iss.key} — ${iss.summary}\nوضعیت: ${iss.statusName}"
                            textSize = 12.5f
                            setTextColor(ThemeHelper.textPrimary(dark))
                            setPadding(0, 8, 0, 0)
                        })
                    }
                    actCard.addView(actBox)
                    actCard.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    detailBox.addView(actCard)
                }
            } catch (_: Exception) { }
        }
    }

    private fun rowWithActions(
        ctx: android.content.Context, label: String, primary: Int, dark: Boolean,
        onEdit: () -> Unit, onDelete: () -> Unit, extra: View? = null,
        onDetails: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        row.addView(TextView(ctx).apply {
            text = label
            textSize = 12.5f
            setTextColor(ThemeHelper.textPrimary(dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (extra != null) row.addView(extra)
        if (onDetails != null) {
            row.addView(ThemeHelper.iconButton(ctx, "ℹ", primary, dark, "جزئیات Issue", onDetails))
        }
        row.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش", onEdit))
        row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف", onDelete))
        return row
    }

    /** نمایش خلاصه و توضیحات Issue از جیرا */
    private fun showIssueDetailsDialog(issueKey: String) {
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        lifecycleScope.launch {
            val cached = repo.getJiraIssue(issueKey)
            val service = repo.jiraServiceOrNull()
            val remote = service?.getIssue(issueKey)?.getOrNull()
            val summary = remote?.summary ?: cached?.summary ?: "—"
            val desc = remote?.description ?: cached?.description ?: "توضیحی ثبت نشده"
            val status = remote?.statusName ?: cached?.statusName ?: "—"
            val type = remote?.issueTypeName ?: cached?.issueTypeName ?: "—"
            val assignee = remote?.assigneeName ?: cached?.assigneeName ?: "—"
            val body = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
            }
            fun line(title: String, value: String) {
                body.addView(TextView(ctx).apply {
                    text = title
                    textSize = 12f
                    setTextColor(ThemeHelper.textSecondary(dark))
                    setPadding(0, 10, 0, 2)
                })
                body.addView(TextView(ctx).apply {
                    text = value
                    textSize = 14f
                    setTextColor(ThemeHelper.textPrimary(dark))
                })
            }
            line("کلید", issueKey)
            line("عنوان", summary)
            line("وضعیت", status)
            line("نوع", type)
            line("مسئول", assignee)
            line("توضیحات", desc)
            DialogHelper.show(
                ctx, icon = "ℹ", title = "جزئیات Issue",
                subtitle = issueKey,
                primary = primary, dark = dark, body = body,
                positiveText = "بستن", onPositive = { true }
            )
        }
    }

    private fun card(ctx: android.content.Context, title: String, body: String): MaterialCardView {
        val c = MaterialCardView(ctx)
        ThemeHelper.applyCard(c, dark())
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
        }
        box.addView(TextView(ctx).apply {
            text = title
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary())
        })
        box.addView(TextView(ctx).apply {
            text = body
            textSize = 13f
            setTextColor(ThemeHelper.textPrimary(dark()))
            setPadding(0, 8, 0, 0)
        })
        c.addView(box)
        c.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        return c
    }

    /** ویرایش Worklog از تقویم — همگام با شروع/پایان و ساعت/دقیقه */
    private fun showEditWorklogDialog(wl: com.personal.timetracker.data.entity.JiraWorklogCacheEntity) {
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark))
        val (dateL, dateField) = DialogHelper.inputField(
            ctx, "تاریخ", TimeUtils.toJalaliShort(TimeUtils.parseDate(wl.date)), primary
        )
        dateField.tag = wl.date
        dateField.isFocusable = false
        dateField.setOnClickListener {
            JalaliDatePickerDialog.show(ctx, primary = primary, dark = dark, initialGregorianDate = dateField.tag as? String) { g, j ->
                dateField.tag = g; dateField.setText(j)
            }
        }
        layout.addView(dateL)
        val startedHm = wl.started?.substringAfter("T")?.take(5)
        val times = WorklogTimeFields.build(
            ctx, primary, dark,
            defaultStart = startedHm,
            defaultDurationMinutes = wl.durationMinutes.coerceAtLeast(15)
        )
        layout.addView(times.root)
        val (nL, nE) = DialogHelper.inputField(ctx, "توضیح", wl.comment ?: "", primary, multiline = true)
        layout.addView(nL)
        DialogHelper.show(
            ctx, icon = "✎", title = "ویرایش Worklog", subtitle = wl.issueKey,
            primary = primary, dark = dark, body = layout, positiveText = "ذخیره",
            onPositive = {
                val date = (dateField.tag as? String) ?: wl.date
                val dur = times.durationMinutes()
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت نامعتبر", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                lifecycleScope.launch {
                    repo.updateJiraTaskLog(wl.localId, dur, date, nE.text?.toString())
                    Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                    loadDay()
                }
                true
            }
        )
    }

    /** جستجوی Issue و ثبت لاگ برای روز انتخاب‌شده در تقویم */
    private fun showAddWorklogSearchDialog(date: String) {
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val (qL, qE) = DialogHelper.inputField(ctx, "جستجوی کلید یا عنوان Issue", "", primary)
        layout.addView(qL)
        val listBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (ctx.resources.displayMetrics.density * 220).toInt()
            )
            addView(listBox)
        })
        val dlg = DialogHelper.show(
            ctx, icon = "＋", title = "ثبت لاگ",
            subtitle = TimeUtils.toJalaliDisplay(date),
            primary = primary, dark = dark, body = layout,
            positiveText = "جستجو",
            onPositive = {
                val q = qE.text?.toString()?.trim().orEmpty()
                if (q.isBlank()) {
                    Toast.makeText(ctx, "متن جستجو را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                lifecycleScope.launch {
                    listBox.removeAllViews()
                    val service = repo.jiraServiceOrNull()
                    if (service == null) {
                        Toast.makeText(ctx, "جیرا پیکربندی نشده", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    service.search(q, 20).fold(
                        onSuccess = { page ->
                            if (page.items.isEmpty()) {
                                listBox.addView(TextView(ctx).apply {
                                    text = "موردی یافت نشد"
                                    setTextColor(ThemeHelper.textSecondary(dark))
                                })
                            } else {
                                page.items.forEach { item ->
                                    listBox.addView(MaterialButton(ctx).apply {
                                        text = "${item.key} — ${item.summary.take(50)}"
                                        ThemeHelper.applyButton(this, primary, false)
                                        setOnClickListener {
                                            showQuickAddLog(item.key, item.summary, date)
                                        }
                                    })
                                }
                            }
                        },
                        onFailure = { e ->
                            Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show()
                        }
                    )
                }
                false // باز بماند تا انتخاب
            }
        )
    }

    private fun showQuickAddLog(issueKey: String, summary: String, date: String) {
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val times = WorklogTimeFields.build(ctx, primary, dark, defaultDurationMinutes = 60)
        layout.addView(times.root)
        val (nL, nE) = DialogHelper.inputField(ctx, "توضیح", "", primary, multiline = true)
        layout.addView(nL)
        DialogHelper.show(
            ctx, icon = "⏱", title = "ثبت Worklog",
            subtitle = "$issueKey — $summary",
            primary = primary, dark = dark, body = layout, positiveText = "ثبت",
            onPositive = {
                val dur = times.durationMinutes()
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت نامعتبر", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                val st = times.startHm()
                lifecycleScope.launch {
                    repo.addJiraTaskLog(issueKey, dur, date, nE.text?.toString(), st).fold(
                        onSuccess = {
                            Toast.makeText(ctx, "ثبت شد", Toast.LENGTH_SHORT).show()
                            loadDay()
                        },
                        onFailure = { e -> Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
                    )
                }
                true
            }
        )
    }

}
