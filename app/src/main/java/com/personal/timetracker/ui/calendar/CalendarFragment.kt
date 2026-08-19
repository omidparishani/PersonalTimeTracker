package com.personal.timetracker.ui.calendar

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.AttendanceEditor
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.TaskLogEditor
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * تقویم شمسی ماهانه + جزئیات تردد و تسک هر روز
 */
class CalendarFragment : Fragment() {
    private lateinit var monthTitle: TextView
    private lateinit var grid: GridLayout
    private lateinit var detailBox: LinearLayout
    private var jy = 0
    private var jm = 0
    private var selectedJd = 1

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

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
            val att = repo.getByDateOnce(d)
            val logs = repo.getLogsByDate(d)
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
                append("   ·   لاگ تسک: "); append(TimeUtils.formatDuration(logs.sumOf { it.duration }))
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
            if (logs.isEmpty()) {
                logBox.addView(TextView(ctx).apply {
                    text = "موردی نیست"; textSize = 12.5f; setTextColor(ThemeHelper.textSecondary(dark))
                    setPadding(0, 8, 0, 0)
                })
            } else {
                logs.forEach { log ->
                    val t = tasks.find { it.id == log.taskId }
                    val label = buildString {
                        append(t?.taskTitle ?: "تسک #${log.taskId}")
                        if (!t?.jiraNumber.isNullOrBlank()) { append(" ["); append(t?.jiraNumber); append("]") }
                        append("  ·  "); append(TimeUtils.formatDuration(log.duration))
                    }
                    logBox.addView(rowWithActions(
                        ctx, label, primary, dark,
                        onEdit = {
                            if (t != null) TaskLogEditor.openEdit(ctx, t, log, repo, lifecycleScope, primary, dark) { loadDay() }
                        },
                        onDelete = {
                            if (t != null) TaskLogEditor.confirmDelete(ctx, t, log, repo, lifecycleScope, primary, dark) { loadDay() }
                        }
                    ))
                }
            }
            logCard.addView(logBox)
            logCard.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            detailBox.addView(logCard)
        }
    }

    private fun rowWithActions(
        ctx: android.content.Context, label: String, primary: Int, dark: Boolean,
        onEdit: () -> Unit, onDelete: () -> Unit, extra: View? = null
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
        row.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش", onEdit))
        row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف", onDelete))
        return row
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
}
