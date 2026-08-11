package com.personal.timetracker.ui.reports

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.AttendanceEditor
import com.personal.timetracker.util.BarChartView
import com.personal.timetracker.util.BarItem
import com.personal.timetracker.util.ChartHelper
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.DonutChartView
import com.personal.timetracker.util.DonutItem
import com.personal.timetracker.util.TaskLogEditor
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.launch

class ReportsFragment : Fragment() {
    private lateinit var content: LinearLayout
    private var period = 0
    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.surface(dark()))
            setPadding(16)
        }
        root.addView(ThemeHelper.pageTitle(ctx, "گزارش‌ها", dark()))
        val group = MaterialButtonToggleGroup(ctx).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        val b1 = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "روزانه"; id = View.generateViewId()
        }
        val b2 = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "هفتگی"; id = View.generateViewId()
        }
        val b3 = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "ماهانه"; id = View.generateViewId()
        }
        listOf(b1, b2, b3).forEach {
            ThemeHelper.applyButton(it, primary(), false)
            group.addView(it)
        }
        group.check(b1.id)
        group.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            period = when (id) { b2.id -> 1; b3.id -> 2; else -> 0 }
            load()
        }
        root.addView(group)
        content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(0, 16, 0, 0)
            addView(content)
        })
        load()
        return root
    }

    private fun metricCard(title: String, value: String, subtitle: String = "", accent: Int? = null): MaterialCardView {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark())
        val outer = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val bar = View(ctx).apply {
            setBackgroundColor(accent ?: primary())
            layoutParams = LinearLayout.LayoutParams((4 * resources.displayMetrics.density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 24, 20)
        }
        box.addView(TextView(ctx).apply {
            text = title; textSize = 12f
            setTextColor(ThemeHelper.textSecondary(dark()))
        })
        box.addView(TextView(ctx).apply {
            text = value; textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(accent ?: primary())
            setPadding(0, 6, 0, 0)
        })
        if (subtitle.isNotEmpty()) {
            box.addView(TextView(ctx).apply {
                text = subtitle; textSize = 11f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }
        outer.addView(bar)
        outer.addView(box, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(outer)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        return card
    }

    private fun dayCard(day: com.personal.timetracker.data.repository.DayBreakdown, repo: com.personal.timetracker.data.repository.AppRepository): MaterialCardView {
        val ctx = requireContext()
        val primary = primary()
        val dark = dark()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark)
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
        }
        box.addView(TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(day.date)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary)
        })
        box.addView(TextView(ctx).apply {
            textSize = 12.5f
            setTextColor(ThemeHelper.textSecondary(dark))
            setPadding(0, 6, 0, 10)
            text = "کار: ${TimeUtils.formatDuration(day.worked)}   ·   مرخصی: ${TimeUtils.formatDuration(day.leave)}   ·   اضافه‌کار: ${TimeUtils.formatDuration(day.overtime)}"
        })

        if (day.attendance.isNotEmpty()) {
            day.attendance.forEach { r ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }
                row.addView(TextView(ctx).apply {
                    text = if (r.exitTime != null) "تردد: ${r.entryTime} → ${r.exitTime}" else "تردد: ${r.entryTime} (فعال)"
                    textSize = 12.5f
                    setTextColor(ThemeHelper.textPrimary(dark))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش تردد") {
                    AttendanceEditor.open(ctx, r, repo, lifecycleScope, primary, dark) { load() }
                })
                row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف تردد") {
                    AttendanceEditor.confirmDelete(ctx, r, repo, lifecycleScope, primary, dark) { load() }
                })
                box.addView(row)
            }
        } else {
            box.addView(TextView(ctx).apply {
                text = "تردد: —"; textSize = 12.5f; setTextColor(ThemeHelper.textSecondary(dark))
            })
        }

        if (day.taskLogs.isNotEmpty()) {
            box.addView(DialogHelper.sectionLabel(ctx, "تسک‌ها", dark))
            day.taskLogs.forEach { t ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }
                val info = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                info.addView(TextView(ctx).apply {
                    text = buildString {
                        append(t.taskTitle)
                        if (!t.jira.isNullOrBlank()) { append(" ["); append(t.jira); append("]") }
                        append("  ·  "); append(TimeUtils.formatDuration(t.duration))
                    }
                    textSize = 12.5f
                    setTextColor(ThemeHelper.textPrimary(dark))
                })
                if (!t.note.isNullOrBlank()) {
                    info.addView(TextView(ctx).apply {
                        text = t.note; textSize = 11f; setTextColor(ThemeHelper.textSecondary(dark))
                    })
                }
                row.addView(info)
                row.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش لاگ") {
                    lifecycleScope.launch {
                        val task = repo.getTask(t.taskId) ?: return@launch
                        val log = com.personal.timetracker.data.entity.TaskLogEntity(
                            id = t.logId, taskId = t.taskId, date = day.date,
                            startTime = null, endTime = null, duration = t.duration,
                            note = t.note, createdAt = TimeUtils.nowDateTime()
                        )
                        TaskLogEditor.openEdit(ctx, task, log, repo, lifecycleScope, primary, dark) { load() }
                    }
                })
                row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف لاگ") {
                    lifecycleScope.launch {
                        val task = repo.getTask(t.taskId) ?: return@launch
                        val log = com.personal.timetracker.data.entity.TaskLogEntity(
                            id = t.logId, taskId = t.taskId, date = day.date,
                            startTime = null, endTime = null, duration = t.duration,
                            note = t.note, createdAt = TimeUtils.nowDateTime()
                        )
                        TaskLogEditor.confirmDelete(ctx, task, log, repo, lifecycleScope, primary, dark) { load() }
                    }
                })
                box.addView(row)
            }
        }

        card.addView(box)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
        return card
    }

    private val palette = intArrayOf(
        0xFF1565C0.toInt(), 0xFF00897B.toInt(), 0xFFF9A825.toInt(),
        0xFF8E24AA.toInt(), 0xFFE53935.toInt(), 0xFF3949AB.toInt(),
        0xFF43A047.toInt(), 0xFFFB8C00.toInt()
    )

    private fun load() {
        val repo = (requireActivity().application as App).repository
        val end = TimeUtils.today()
        val start = when (period) {
            1 -> TimeUtils.startOfWeek()
            2 -> TimeUtils.startOfMonth()
            else -> end
        }
        content.removeAllViews()
        lifecycleScope.launch {
            val r = repo.report(start, end)
            val projects = repo.projectSummaryRange(start, end)
            val jiras = repo.jiraSummaryRange(start, end)
            val days = repo.dayBreakdown(start, end)
            val ctx = requireContext()

            content.addView(TextView(ctx).apply {
                text = "بازه: ${TimeUtils.toJalaliDisplay(start)} تا ${TimeUtils.toJalaliDisplay(end)}"
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(4, 0, 4, 12)
            })

            // Summary bar chart: worked vs leave vs overtime vs undertime
            val summaryItems = listOf(
                BarItem("ساعت کار — ${TimeUtils.formatDuration(r.worked)}", r.worked, primary()),
                BarItem("مرخصی — ${TimeUtils.formatDuration(r.leave)}", r.leave, 0xFFF9A825.toInt()),
                BarItem("اضافه‌کاری — ${TimeUtils.formatDuration(r.overtime)}", r.overtime, 0xFF43A047.toInt()),
                BarItem("کسری کار — ${TimeUtils.formatDuration(r.undertime)}", r.undertime, 0xFFE53935.toInt())
            )
            if (summaryItems.any { it.value > 0 }) {
                val chartCard = MaterialCardView(ctx)
                ThemeHelper.applyCard(chartCard, dark())
                chartCard.setContentPadding(24, 20, 24, 20)
                chartCard.addView(BarChartView(ctx).apply {
                    items = summaryItems
                    trackColor = ThemeHelper.outline(dark())
                    labelColor = ThemeHelper.textPrimary(dark())
                })
                content.addView(chartCard, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 14 })
            }

            content.addView(metricCard("ساعت کار", TimeUtils.formatDuration(r.worked), accent = primary()))
            content.addView(metricCard("مرخصی", TimeUtils.formatDuration(r.leave), accent = 0xFFF9A825.toInt()))
            content.addView(metricCard(
                "اضافه‌کاری",
                TimeUtils.formatDuration(r.overtime),
                "جمع دقایق بیش از حداقل روزانه",
                accent = 0xFF43A047.toInt()
            ))
            content.addView(metricCard(
                "کسری کار",
                TimeUtils.formatDuration(r.undertime),
                "جمع دقایق کمتر از حداقل روزانه",
                accent = 0xFFE53935.toInt()
            ))
            content.addView(metricCard("لاگ روی تسک‌ها", TimeUtils.formatDuration(r.taskLogMinutes)))
            content.addView(metricCard("تعداد ورود/خروج", "${r.entryCount}"))

            content.addView(ThemeHelper.sectionTitle(ctx, "جزئیات روزبه‌روز", dark(), primary()))
            if (days.isEmpty()) {
                content.addView(TextView(ctx).apply {
                    text = "در این بازه داده‌ای نیست"
                    setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else {
                days.asReversed().forEach { content.addView(dayCard(it, repo)) }
            }

            content.addView(ThemeHelper.sectionTitle(ctx, "پروژه‌ها", dark(), primary()))
            if (projects.isEmpty()) {
                content.addView(TextView(ctx).apply {
                    text = "داده‌ای نیست"; setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else {
                val total = projects.sumOf { it.total }
                val donutItems = projects.mapIndexed { i, p ->
                    DonutItem(p.projectName, p.total, palette[i % palette.size])
                }
                val donutCard = MaterialCardView(ctx)
                ThemeHelper.applyCard(donutCard, dark())
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(20, 20, 20, 20)
                }
                val donut = DonutChartView(ctx).apply {
                    items = donutItems
                    trackColor = ThemeHelper.outline(dark())
                    centerTitle = TimeUtils.formatDuration(total)
                    centerSubtitle = "کل"
                    titleColor = ThemeHelper.textPrimary(dark())
                    subtitleColor = ThemeHelper.textSecondary(dark())
                    layoutParams = LinearLayout.LayoutParams(DialogHelper.dp(ctx, 110), DialogHelper.dp(ctx, 110)).apply {
                        marginEnd = DialogHelper.dp(ctx, 14)
                    }
                }
                val legend = ChartHelper.legend(
                    ctx,
                    donutItems.map { Triple(it.label, TimeUtils.formatDuration(it.value), it.color) },
                    dark()
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(donut)
                row.addView(legend)
                donutCard.addView(row)
                content.addView(donutCard, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 })
            }

            content.addView(ThemeHelper.sectionTitle(ctx, "Jira", dark(), primary()))
            if (jiras.isEmpty()) {
                content.addView(TextView(ctx).apply {
                    text = "داده‌ای نیست"; setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else {
                val jiraItems = jiras.mapIndexed { i, j ->
                    BarItem("${j.jiraNumber} — ${TimeUtils.formatDuration(j.total)}", j.total, palette[i % palette.size])
                }
                val jiraCard = MaterialCardView(ctx)
                ThemeHelper.applyCard(jiraCard, dark())
                jiraCard.setContentPadding(24, 20, 24, 20)
                jiraCard.addView(BarChartView(ctx).apply {
                    items = jiraItems
                    trackColor = ThemeHelper.outline(dark())
                    labelColor = ThemeHelper.textPrimary(dark())
                })
                content.addView(jiraCard)
            }
        }
    }
}
