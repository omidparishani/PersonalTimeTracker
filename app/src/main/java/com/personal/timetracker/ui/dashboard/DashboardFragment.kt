package com.personal.timetracker.ui.dashboard

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var suggestedText: TextView
    private lateinit var summaryRow: LinearLayout
    private lateinit var listBox: LinearLayout
    private lateinit var chartBox: LinearLayout
    private lateinit var runningText: TextView
    private lateinit var runningCard: MaterialCardView
    private lateinit var btnIn: MaterialButton
    private lateinit var btnOut: MaterialButton
    private lateinit var btnCal: MaterialButton
    private lateinit var rootBg: LinearLayout

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark ?: false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val dark = dark()
        val primary = primary()

        rootBg = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.surface(dark))
            setPadding(20)
        }
        val scroll = android.widget.ScrollView(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 24)
        }
        scroll.addView(root)
        rootBg.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        root.addView(ThemeHelper.pageTitle(ctx, "داشبورد", dark))

        // Status hero card
        val statusCard = MaterialCardView(ctx).apply {
            radius = 24f
            cardElevation = 0f
            setCardBackgroundColor(primary)
        }
        val statusInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 36, 32, 36)
            gravity = Gravity.CENTER
        }
        statusTitle = TextView(ctx).apply {
            textSize = 22f
            setTextColor(ThemeHelper.onColor(primary))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        statusSub = TextView(ctx).apply {
            textSize = 14f
            setTextColor(ThemeHelper.onColor(primary))
            gravity = Gravity.CENTER
            alpha = 0.9f
            setPadding(0, 8, 0, 0)
        }
        statusInner.addView(statusTitle)
        statusInner.addView(statusSub)
        statusCard.addView(statusInner)
        root.addView(statusCard, lp(bottom = 16))

        suggestedText = TextView(ctx).apply {
            textSize = 13f
            setTextColor(ThemeHelper.textSecondary(dark))
            setPadding(8, 0, 8, 12)
        }
        root.addView(suggestedText)

        // Buttons
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnIn = MaterialButton(ctx).apply {
            text = "ورود الان"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 }
        }
        btnOut = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "خروج الان"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        ThemeHelper.applyButton(btnIn, primary, true)
        ThemeHelper.applyButton(btnOut, primary, false)
        row.addView(btnIn); row.addView(btnOut)
        root.addView(row)

        btnCal = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "📅 تاریخچه و تقویم"
            setPadding(0, 8, 0, 0)
        }
        ThemeHelper.applyButton(btnCal, primary, false)
        root.addView(btnCal, lp(top = 10, bottom = 8))

        // Running task
        runningCard = MaterialCardView(ctx)
        ThemeHelper.applyCard(runningCard, dark)
        runningText = TextView(ctx).apply {
            textSize = 14f
            setTextColor(ThemeHelper.textPrimary(dark))
            setPadding(28, 24, 28, 24)
        }
        runningCard.addView(runningText)
        root.addView(ThemeHelper.sectionTitle(ctx, "تسک فعال", dark, primary))
        root.addView(runningCard, lp(bottom = 8))

        // Summary chips row
        root.addView(ThemeHelper.sectionTitle(ctx, "خلاصه امروز", dark, primary))
        summaryRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(summaryRow, lp(bottom = 8))

        root.addView(ThemeHelper.sectionTitle(ctx, "نمودار امروز", dark, primary))
        chartBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(chartBox, lp(bottom = 8))

        root.addView(ThemeHelper.sectionTitle(ctx, "ترددهای امروز", dark, primary))
        listBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox)

        val repo = (requireActivity().application as App).repository
        btnIn.setOnClickListener { lifecycleScope.launch { repo.checkIn() } }
        btnOut.setOnClickListener { lifecycleScope.launch { repo.checkOut() } }
        btnCal.setOnClickListener { (activity as? MainActivity)?.openCalendar() }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeRunningTask().collectLatest { task ->
                if (task == null) {
                    runningText.text = "هیچ تسکی در حال اجرا نیست"
                    runningText.setTextColor(ThemeHelper.textSecondary(dark()))
                } else {
                    runningText.setTextColor(ThemeHelper.textPrimary(dark()))
                    runningText.text = buildString {
                        append("▶  ")
                        append(task.taskTitle)
                        append('\n')
                        append(task.projectName)
                        if (!task.jiraNumber.isNullOrBlank()) {
                            append("  ·  ")
                            append(task.jiraNumber)
                        }
                        append('\n')
                        append("مدت ثبت‌شده: ")
                        append(TimeUtils.formatDuration((task.requiredMinutes - task.remainingMinutes).coerceAtLeast(0)))
                    }
                    runningCard.setCardBackgroundColor(ThemeHelper.container(primary(), dark()))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.observeToday().collectLatest { list ->
                    val settings = repo.getSettings()
                    val isWorking = list.any { it.status == "active" && it.exitTime == null }
                    statusTitle.text = if (isWorking) "در حال کار" else "خارج از کار"
                    statusSub.text = TimeUtils.toJalaliShort()
                    btnIn.isEnabled = !isWorking
                    btnOut.isEnabled = isWorking
                    btnIn.alpha = if (isWorking) 0.45f else 1f
                    btnOut.alpha = if (!isWorking) 0.45f else 1f

                    val activeRec = list.find { it.status == "active" && it.exitTime == null }
                    suggestedText.text = if (activeRec != null) {
                        val end = TimeCalc.suggestedEnd(activeRec.entryTime, settings.minimumWorkMinutes)
                        "پایان پیشنهادی کار: $end   |   ورود: ${activeRec.entryTime}"
                    } else ""

                    val worked = list.sumOf { it.duration }
                    val leave = list.sumOf { it.leaveDuration }
                    summaryRow.removeAllViews()
                    summaryRow.addView(statCard("ساعت کار", TimeUtils.formatDuration(worked), primary()))
                    summaryRow.addView(statCard("مرخصی", TimeUtils.formatDuration(leave), primary()))
                    summaryRow.addView(statCard("تردد", "${list.size}", primary()))

                    
                    // chart bars
                    chartBox.removeAllViews()
                    val settingsMin = settings.minimumWorkMinutes.coerceAtLeast(1)
                    val jiraLogs = try {
                        (requireActivity().application as App).repository.getLogsByDate(TimeUtils.today())
                    } catch (_: Exception) { emptyList() }
                    fun bar(label: String, value: Int, maxV: Int, color: Int) {
                        val row = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, 10) }
                        row.addView(TextView(ctx).apply {
                            text = "$label — ${TimeUtils.formatDuration(value)}"
                            textSize = 12f
                            setTextColor(ThemeHelper.textPrimary(dark()))
                        })
                        val track = android.widget.FrameLayout(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 22)
                            setBackgroundColor(ThemeHelper.outline(dark()))
                        }
                        val fill = View(ctx).apply {
                            setBackgroundColor(color)
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                ((value.toFloat() / maxV) * 1000).toInt().coerceIn(8, 1000),
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        track.addView(fill)
                        row.addView(track)
                        chartBox.addView(row)
                    }
                    val maxBar = maxOf(settingsMin, worked, leave, 1)
                    bar("حداقل کار", settingsMin, maxBar, primary())
                    bar("کار انجام‌شده", worked, maxBar, primary())
                    bar("مرخصی", leave, maxBar, 0xFFF9A825.toInt())
                    if (jiraLogs.isNotEmpty()) {
                        val byTask = jiraLogs.groupBy { it.taskId }
                        byTask.forEach { (tid, logs) ->
                            val sum = logs.sumOf { it.duration }
                            bar("لاگ تسک #$tid", sum, maxBar, 0xFF00897B.toInt())
                        }
                    }

                    listBox.removeAllViews()
                    if (list.isEmpty()) {
                        listBox.addView(TextView(ctx).apply {
                            text = "هنوز ترددی ثبت نشده"
                            setTextColor(ThemeHelper.textSecondary(dark()))
                            setPadding(8, 8, 8, 8)
                        })
                    } else {
                        list.forEach { r ->
                            val card = MaterialCardView(ctx)
                            ThemeHelper.applyCard(card, dark())
                            card.addView(TextView(ctx).apply {
                                setPadding(24, 20, 24, 20)
                                setTextColor(ThemeHelper.textPrimary(dark()))
                                textSize = 14f
                                text = buildString {
                                    if (r.exitTime != null) {
                                        append(r.entryTime); append("  →  "); append(r.exitTime)
                                        append('\n')
                                        append(TimeUtils.formatDuration(r.duration))
                                        if (r.leaveDuration > 0) {
                                            append("  ·  مرخصی "); append(TimeUtils.formatDuration(r.leaveDuration))
                                        }
                                    } else {
                                        append(r.entryTime); append("  ·  فعال")
                                    }
                                }
                            })
                            listBox.addView(card, lp(bottom = 10))
                        }
                    }
                }
            } catch (e: Exception) {
                statusTitle.text = "خطا"
                statusSub.text = e.message
            }
        }
        return rootBg
    }

    private fun statCard(label: String, value: String, primary: Int): View {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark())
        card.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = 8
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 20, 16, 20)
            gravity = Gravity.CENTER
        }
        box.addView(TextView(ctx).apply {
            text = value
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary)
            gravity = Gravity.CENTER
        })
        box.addView(TextView(ctx).apply {
            text = label
            textSize = 11f
            setTextColor(ThemeHelper.textSecondary(dark()))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        })
        card.addView(box)
        return card
    }

    private fun lp(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        topMargin = top
        bottomMargin = bottom
    }
}
