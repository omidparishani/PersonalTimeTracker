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
import com.personal.timetracker.util.AttendanceEditor
import com.personal.timetracker.util.BarChartView
import com.personal.timetracker.util.BarItem
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var statusIcon: TextView
    private lateinit var pulseDot: View
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
    private var pulseAnimator: android.animation.ObjectAnimator? = null
    private var tickerJob: Job? = null

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
            radius = 28f
            cardElevation = if (dark) 2f else 6f
            strokeWidth = 0
            setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        val heroBg = LinearLayout(ctx).apply {
            background = ThemeHelper.gradient(primary, dark, 28f)
        }
        val statusInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 34, 32, 34)
            gravity = Gravity.CENTER
        }
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusIcon = TextView(ctx).apply {
            text = "■"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(ThemeHelper.onColor(primary))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(androidx.core.graphics.ColorUtils.blendARGB(primary, android.graphics.Color.WHITE, 0.22f))
            }
            layoutParams = LinearLayout.LayoutParams(DialogHelper.dp(ctx, 52), DialogHelper.dp(ctx, 52)).apply {
                marginEnd = DialogHelper.dp(ctx, 14)
            }
        }
        val titleCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusTitle = TextView(ctx).apply {
            textSize = 20f
            setTextColor(ThemeHelper.onColor(primary))
            setTypeface(null, Typeface.BOLD)
        }
        pulseDot = View(ctx).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(DialogHelper.dp(ctx, 9), DialogHelper.dp(ctx, 9)).apply {
                marginStart = DialogHelper.dp(ctx, 8)
            }
            visibility = View.GONE
        }
        titleRow.addView(statusTitle)
        titleRow.addView(pulseDot)
        statusSub = TextView(ctx).apply {
            textSize = 13f
            setTextColor(ThemeHelper.onColor(primary))
            alpha = 0.9f
            setPadding(0, 6, 0, 0)
        }
        titleCol.addView(titleRow)
        titleCol.addView(statusSub)
        topRow.addView(statusIcon)
        topRow.addView(titleCol)
        statusInner.addView(topRow)
        heroBg.addView(statusInner)
        statusCard.addView(heroBg)
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
                    statusIcon.text = if (isWorking) "▶" else "■"
                    btnIn.isEnabled = !isWorking
                    btnOut.isEnabled = isWorking
                    btnIn.alpha = if (isWorking) 0.45f else 1f
                    btnOut.alpha = if (!isWorking) 0.45f else 1f

                    val activeRec = list.find { it.status == "active" && it.exitTime == null }
                    if (isWorking && activeRec != null) {
                        pulseDot.visibility = View.VISIBLE
                        if (pulseAnimator == null) {
                            pulseAnimator = android.animation.ObjectAnimator.ofFloat(pulseDot, "alpha", 1f, 0.2f).apply {
                                duration = 750
                                repeatMode = android.animation.ValueAnimator.REVERSE
                                repeatCount = android.animation.ValueAnimator.INFINITE
                                start()
                            }
                        }
                        startTicker(activeRec.entryTime)
                        val end = TimeCalc.suggestedEnd(activeRec.entryTime, settings.minimumWorkMinutes)
                        suggestedText.text = "پایان پیشنهادی کار: $end"
                    } else {
                        pulseDot.visibility = View.GONE
                        pulseAnimator?.cancel(); pulseAnimator = null
                        stopTicker()
                        statusSub.text = TimeUtils.toJalaliShort()
                        suggestedText.text = ""
                    }

                    val worked = list.sumOf { it.duration }
                    val leave = list.sumOf { it.leaveDuration }
                    summaryRow.removeAllViews()
                    summaryRow.addView(statCard("ساعت کار", TimeUtils.formatDuration(worked), primary()))
                    summaryRow.addView(statCard("مرخصی", TimeUtils.formatDuration(leave), primary()))
                    summaryRow.addView(statCard("تردد", "${list.size}", primary()))

                    // chart
                    chartBox.removeAllViews()
                    val settingsMin = settings.minimumWorkMinutes.coerceAtLeast(1)
                    val jiraLogs = try {
                        (requireActivity().application as App).repository.getLogsByDate(TimeUtils.today())
                    } catch (_: Exception) { emptyList() }

                    val barItems = mutableListOf(
                        BarItem("حداقل کار — ${TimeUtils.formatDuration(settingsMin)}", settingsMin, primary()),
                        BarItem("کار انجام‌شده — ${TimeUtils.formatDuration(worked)}", worked, primary()),
                        BarItem("مرخصی — ${TimeUtils.formatDuration(leave)}", leave, 0xFFF9A825.toInt())
                    )
                    if (jiraLogs.isNotEmpty()) {
                        val byTask = jiraLogs.groupBy { it.taskId }
                        byTask.forEach { (tid, logs) ->
                            val sum = logs.sumOf { it.duration }
                            barItems.add(BarItem("لاگ تسک #$tid — ${TimeUtils.formatDuration(sum)}", sum, 0xFF00897B.toInt()))
                        }
                    }
                    val chartCard = MaterialCardView(ctx)
                    ThemeHelper.applyCard(chartCard, dark())
                    val chart = BarChartView(ctx).apply {
                        items = barItems
                        trackColor = ThemeHelper.outline(dark())
                        labelColor = ThemeHelper.textPrimary(dark())
                    }
                    chartCard.setContentPadding(24, 20, 24, 20)
                    chartCard.addView(chart)
                    chartBox.addView(chartCard)

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
                            val box = LinearLayout(ctx).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(24, 18, 24, 18)
                            }
                            val headRow = LinearLayout(ctx).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                            }
                            headRow.addView(TextView(ctx).apply {
                                setTextColor(ThemeHelper.textPrimary(dark()))
                                textSize = 14f
                                setTypeface(null, Typeface.BOLD)
                                text = if (r.exitTime != null) "${r.entryTime}  →  ${r.exitTime}" else r.entryTime
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            })
                            if (r.exitTime == null) {
                                headRow.addView(ThemeHelper.pill(ctx, "فعال", primary(), ThemeHelper.onColor(primary())))
                            }
                            box.addView(headRow)
                            if (r.exitTime != null) {
                                box.addView(TextView(ctx).apply {
                                    setTextColor(ThemeHelper.textSecondary(dark()))
                                    textSize = 12.5f
                                    setPadding(0, 6, 0, 0)
                                    text = buildString {
                                        append(TimeUtils.formatDuration(r.duration))
                                        if (r.leaveDuration > 0) {
                                            append("   ·   مرخصی "); append(TimeUtils.formatDuration(r.leaveDuration))
                                        }
                                    }
                                })
                            }
                            val rActions = LinearLayout(ctx).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(0, 10, 0, 0)
                            }
                            rActions.addView(ThemeHelper.iconButton(ctx, "✎", primary(), dark(), "ویرایش تردد") {
                                AttendanceEditor.open(ctx, r, repo, lifecycleScope, primary(), dark())
                            })
                            rActions.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark(), "حذف تردد") {
                                AttendanceEditor.confirmDelete(ctx, r, repo, lifecycleScope, primary(), dark())
                            })
                            box.addView(rActions)
                            card.addView(box)
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

    private fun startTicker(entryTime: String) {
        tickerJob?.cancel()
        tickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val elapsed = TimeUtils.minutesBetween(entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                statusSub.text = "از ساعت $entryTime  ·  ${TimeUtils.formatDuration(elapsed)}"
                delay(30_000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onDestroyView() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        stopTicker()
        super.onDestroyView()
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
        val dot = View(ctx).apply {
            val density = ctx.resources.displayMetrics.density
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(primary)
            }
            layoutParams = LinearLayout.LayoutParams((8 * density).toInt(), (8 * density).toInt()).apply {
                bottomMargin = (6 * density).toInt()
            }
        }
        box.addView(dot)
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
