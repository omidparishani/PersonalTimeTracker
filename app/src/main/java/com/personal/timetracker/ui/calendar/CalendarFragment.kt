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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        val prev = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "‹"
            setOnClickListener {
                jm--; if (jm < 1) { jm = 12; jy-- }
                renderMonth(); loadDay()
            }
        }
        monthTitle = TextView(ctx).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(ThemeHelper.textPrimary(dark()))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val next = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "›"
            setOnClickListener {
                jm++; if (jm > 12) { jm = 1; jy++ }
                renderMonth(); loadDay()
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
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        root.addView(headers)

        grid = GridLayout(ctx).apply {
            columnCount = 7
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(grid)

        detailBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 12, 0, 0) }
        root.addView(android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(detailBox)
        })

        renderMonth()
        loadDay()
        return root
    }

    private fun renderMonth() {
        monthTitle.text = "${TimeUtils.jalaliMonthName(jm)} $jy"
        grid.removeAllViews()
        val daysInMonth = TimeUtils.jalaliMonthDays(jy, jm)
        val startWeekday = TimeUtils.weekdayJalali(jy, jm, 1) // 0=Sat
        val primary = primary()
        val dark = dark()
        val today = TimeUtils.toJalali(Calendar.getInstance().time)

        // empty cells
        for (i in 0 until startWeekday) {
            grid.addView(TextView(requireContext()).apply {
                layoutParams = cellLp()
            })
        }
        for (d in 1..daysInMonth) {
            val cell = TextView(requireContext()).apply {
                text = d.toString()
                gravity = Gravity.CENTER
                textSize = 14f
                setPadding(0, 18, 0, 18)
                layoutParams = cellLp()
                setTextColor(ThemeHelper.textPrimary(dark))
                if (d == selectedJd) {
                    setBackgroundColor(primary)
                    setTextColor(ThemeHelper.onColor(primary))
                    setTypeface(null, Typeface.BOLD)
                } else if (jy == today[0] && jm == today[1] && d == today[2]) {
                    setTextColor(primary)
                    setTypeface(null, Typeface.BOLD)
                }
                setOnClickListener {
                    selectedJd = d
                    renderMonth()
                    loadDay()
                }
            }
            grid.addView(cell)
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
        detailBox.removeAllViews()
        detailBox.addView(TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(d)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary())
            setPadding(4, 4, 4, 12)
        })
        lifecycleScope.launch {
            val att = repo.getByDateOnce(d)
            val logs = repo.getLogsByDate(d)
            val tasks = repo.getTasksByDateOnce(d)

            // Summary card
            val worked = att.sumOf {
                if (it.exitTime != null) it.duration
                else if (it.status == "active") TimeUtils.minutesBetween(it.entryTime, TimeUtils.nowTime())
                else 0
            }
            val leave = att.sumOf { it.leaveDuration }
            val settings = repo.getSettings()
            val ot = (worked - settings.minimumWorkMinutes).coerceAtLeast(0)

            detailBox.addView(card(ctx, "جمع روز", buildString {
                append("کار: "); append(TimeUtils.formatDuration(worked))
                append(" | مرخصی: "); append(TimeUtils.formatDuration(leave))
                append(" | اضافه‌کار: "); append(TimeUtils.formatDuration(ot))
                append('\n')
                append("لاگ تسک: "); append(TimeUtils.formatDuration(logs.sumOf { it.duration }))
            }))

            detailBox.addView(card(ctx, "ترددها", buildString {
                if (att.isEmpty()) append("ترددی نیست")
                else att.forEach { r ->
                    append("• ")
                    if (r.exitTime != null) {
                        append(r.entryTime); append(" → "); append(r.exitTime)
                        append(" ("); append(TimeUtils.formatDuration(r.duration)); append(")")
                    } else {
                        append(r.entryTime); append(" (فعال)")
                    }
                    append('\n')
                }
            }))

            detailBox.addView(card(ctx, "تسک‌ها و لاگ‌ها", buildString {
                if (logs.isEmpty() && tasks.isEmpty()) append("موردی نیست")
                else {
                    val byTask = logs.groupBy { it.taskId }
                    if (byTask.isNotEmpty()) {
                        byTask.forEach { (tid, list) ->
                            val t = tasks.find { it.id == tid }
                            append("• ")
                            append(t?.taskTitle ?: "تسک #$tid")
                            if (!t?.jiraNumber.isNullOrBlank()) {
                                append(" ["); append(t?.jiraNumber); append("]")
                            }
                            append('\n')
                            list.forEach { log ->
                                append("   - "); append(TimeUtils.formatDuration(log.duration))
                                if (!log.note.isNullOrBlank()) {
                                    append(" — "); append(log.note)
                                }
                                append('\n')
                            }
                        }
                    } else {
                        tasks.forEach { t ->
                            append("• "); append(t.taskTitle)
                            if (!t.jiraNumber.isNullOrBlank()) {
                                append(" ["); append(t.jiraNumber); append("]")
                            }
                            append('\n')
                        }
                    }
                }
            }))
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
}
