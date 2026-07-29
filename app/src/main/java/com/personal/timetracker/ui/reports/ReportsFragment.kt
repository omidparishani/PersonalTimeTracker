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

    private fun metricCard(title: String, value: String, subtitle: String = ""): MaterialCardView {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark())
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }
        box.addView(TextView(ctx).apply {
            text = title; textSize = 12f
            setTextColor(ThemeHelper.textSecondary(dark()))
        })
        box.addView(TextView(ctx).apply {
            text = value; textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary())
            setPadding(0, 6, 0, 0)
        })
        if (subtitle.isNotEmpty()) {
            box.addView(TextView(ctx).apply {
                text = subtitle; textSize = 11f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }
        card.addView(box)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 }
        return card
    }

    private fun dayCard(day: com.personal.timetracker.data.repository.DayBreakdown): MaterialCardView {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark())
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
        }
        box.addView(TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(day.date)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary())
        })
        box.addView(TextView(ctx).apply {
            textSize = 13f
            setTextColor(ThemeHelper.textPrimary(dark()))
            setPadding(0, 8, 0, 0)
            text = buildString {
                append("کار: "); append(TimeUtils.formatDuration(day.worked))
                append(" | مرخصی: "); append(TimeUtils.formatDuration(day.leave))
                append(" | اضافه‌کار: "); append(TimeUtils.formatDuration(day.overtime))
                append('\n')
                if (day.attendance.isEmpty()) append("تردد: —")
                else day.attendance.forEach { r ->
                    append("تردد: ")
                    append(r.entryTime)
                    if (r.exitTime != null) {
                        append(" → "); append(r.exitTime)
                    } else append(" (فعال)")
                    append('\n')
                }
                if (day.taskLogs.isEmpty()) {
                    append("تسک: —")
                } else {
                    append("تسک‌ها:")
                    append('\n')
                    day.taskLogs.forEach { t ->
                        append("  • ")
                        append(t.taskTitle)
                        if (!t.jira.isNullOrBlank()) {
                            append(" ["); append(t.jira); append("]")
                        }
                        append(" — "); append(TimeUtils.formatDuration(t.duration))
                        if (!t.note.isNullOrBlank()) {
                            append(" ("); append(t.note); append(")")
                        }
                        append('\n')
                    }
                }
            }
        })
        card.addView(box)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
        return card
    }

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
            val projects = repo.projectSummary()
            val jiras = repo.jiraSummary()
            val days = repo.dayBreakdown(start, end)

            content.addView(TextView(requireContext()).apply {
                text = "بازه: ${TimeUtils.toJalaliDisplay(start)} تا ${TimeUtils.toJalaliDisplay(end)}"
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(4, 0, 4, 12)
            })

            content.addView(metricCard("ساعت کار", TimeUtils.formatDuration(r.worked)))
            content.addView(metricCard("مرخصی", TimeUtils.formatDuration(r.leave)))
            content.addView(metricCard(
                "اضافه‌کاری",
                TimeUtils.formatDuration(r.overtime),
                "جمع دقایق بیش از حداقل روزانه"
            ))
            content.addView(metricCard(
                "کسری کار",
                TimeUtils.formatDuration(r.undertime),
                "جمع دقایق کمتر از حداقل روزانه"
            ))
            content.addView(metricCard("لاگ روی تسک‌ها", TimeUtils.formatDuration(r.taskLogMinutes)))
            content.addView(metricCard("تعداد ورود/خروج", "${r.entryCount}"))

            content.addView(ThemeHelper.sectionTitle(requireContext(), "جزئیات روزبه‌روز", dark(), primary()))
            if (days.isEmpty()) {
                content.addView(TextView(requireContext()).apply {
                    text = "در این بازه داده‌ای نیست"
                    setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else {
                days.asReversed().forEach { content.addView(dayCard(it)) }
            }

            content.addView(ThemeHelper.sectionTitle(requireContext(), "پروژه‌ها", dark(), primary()))
            if (projects.isEmpty()) {
                content.addView(TextView(requireContext()).apply {
                    text = "داده‌ای نیست"; setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else projects.forEach {
                content.addView(metricCard(it.projectName, TimeUtils.formatDuration(it.total)))
            }

            content.addView(ThemeHelper.sectionTitle(requireContext(), "Jira", dark(), primary()))
            if (jiras.isEmpty()) {
                content.addView(TextView(requireContext()).apply {
                    text = "داده‌ای نیست"; setTextColor(ThemeHelper.textSecondary(dark()))
                })
            } else jiras.forEach {
                content.addView(metricCard(it.jiraNumber, TimeUtils.formatDuration(it.total)))
            }
        }
    }
}
