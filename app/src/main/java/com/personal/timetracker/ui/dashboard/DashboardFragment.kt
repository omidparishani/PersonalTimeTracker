package com.personal.timetracker.ui.dashboard

/**
 * داشبورد مطابق پروتوتایپ Figma — خلاصه روزانه.
 */

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.FigmaUi
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private lateinit var greetTitle: TextView
    private lateinit var greetSub: TextView
    private lateinit var chipWorked: TextView
    private lateinit var chipRequired: TextView
    private lateinit var chipLeave: TextView
    private lateinit var chipOt: TextView
    private val chipBars = mutableListOf<android.widget.ProgressBar>()
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var suggestedText: TextView
    private lateinit var attBox: LinearLayout
    private lateinit var logBox: LinearLayout
    private lateinit var runningText: TextView
    private lateinit var btnIn: MaterialButton
    private lateinit var btnOut: MaterialButton
    private var tickerJob: Job? = null

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: FigmaUi.PRIMARY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val d = FigmaUi.dp(ctx, 1)
        val isDark = (activity as? MainActivity)?.isDark == true
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FigmaUi.bg(isDark))
        }
        val scroll = ScrollView(ctx)
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, FigmaUi.dp(ctx, 24))
        }

        // —— هدر آبی
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(primary())
                val r = FigmaUi.dp(ctx, 20).toFloat()
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            }
            setPadding(FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 18))
            minimumHeight = FigmaUi.dp(ctx, 72)
        }
        val headRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        }
        headRow.addView(FigmaUi.circleIcon(ctx, "🔔"), LinearLayout.LayoutParams(FigmaUi.dp(ctx, 40), FigmaUi.dp(ctx, 40)).apply {
            marginEnd = FigmaUi.dp(ctx, 12)
        })
        val greetCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        greetTitle = TextView(ctx).apply {
            text = "سلام"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.END
        }
        greetSub = TextView(ctx).apply {
            textSize = 12f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.END
            setPadding(0, FigmaUi.dp(ctx, 4), 0, 0)
        }
        greetCol.addView(greetTitle)
        greetCol.addView(greetSub)
        headRow.addView(greetCol)
        header.addView(headRow)

        // چیپ‌های آمار روی هدر
        val chips = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        fun chipSlot(): Pair<MaterialCardView, TextView> {
            val card = FigmaUi.whiteCard(ctx, isDark).apply {
                radius = FigmaUi.dp(ctx, 16).toFloat()
                cardElevation = 2f * d
            }
            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(FigmaUi.dp(ctx, 4), FigmaUi.dp(ctx, 10), FigmaUi.dp(ctx, 4), FigmaUi.dp(ctx, 8))
            }
            val bar = android.widget.ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = 0
                // ارتفاع نازک شبیه نوار پیشرفت
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, FigmaUi.dp(ctx, 6)
                ).apply {
                    marginStart = FigmaUi.dp(ctx, 8)
                    marginEnd = FigmaUi.dp(ctx, 8)
                }
                progressDrawable?.setColorFilter(primary(), android.graphics.PorterDuff.Mode.SRC_IN)
            }
            chipBars.add(bar)
            box.addView(bar)
            val v = TextView(ctx).apply {
                text = "—"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(primary())
                gravity = Gravity.CENTER
                setPadding(0, FigmaUi.dp(ctx, 6), 0, 0)
            }
            box.addView(v)
            card.addView(box)
            return card to v
        }
        val labels = listOf("اضافه‌کار", "مرخصی", "موظفی", "امروز کار کرده")
        val values = mutableListOf<TextView>()
        labels.forEachIndexed { i, lab ->
            val (card, v) = chipSlot()
            values.add(v)
            val inner = (card.getChildAt(0) as LinearLayout)
            inner.addView(TextView(ctx).apply {
                text = lab
                textSize = 9f
                setTextColor(FigmaUi.muted(isDark))
                gravity = Gravity.CENTER
                setPadding(0, FigmaUi.dp(ctx, 2), 0, 0)
            })
            chips.addView(card, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i < labels.lastIndex) marginEnd = FigmaUi.dp(ctx, 6)
            })
        }
        chipOt = values[0]
        chipLeave = values[1]
        chipRequired = values[2]
        chipWorked = values[3]
        // رنگ‌های معنادار
        val chipColors = intArrayOf(
            0xFF2E7D32.toInt(), // اضافه‌کار سبز
            0xFFEF6C00.toInt(), // مرخصی نارنجی
            0xFF1565C0.toInt(), // موظفی آبی
            0xFF00897B.toInt()  // کارکرد تیل
        )
        listOf(chipOt, chipLeave, chipRequired, chipWorked).forEachIndexed { i, tv ->
            tv.setTextColor(chipColors[i])
        }
        chipBars.forEachIndexed { i, bar ->
            if (i < chipColors.size) {
                bar.progressDrawable?.setColorFilter(chipColors[i], android.graphics.PorterDuff.Mode.SRC_IN)
            }
        }
        content.addView(header)

        // بدنه — آمار زیر هدر (نه داخل هدر)
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 10), FigmaUi.dp(ctx, 12), 0)
        }
        body.addView(chips, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 8)))

        // کارت وضعیت
        val statusCard = FigmaUi.whiteCard(ctx, isDark)
        val statusInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 16))
        }
        val leftCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        leftCol.addView(TextView(ctx).apply {
            text = "خروج پیشنهادی"
            textSize = 11f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.START
        })
        suggestedText = TextView(ctx).apply {
            text = "—"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(FigmaUi.text(isDark))
            gravity = Gravity.START
            setPadding(0, FigmaUi.dp(ctx, 2), 0, 0)
        }
        leftCol.addView(suggestedText)
        statusInner.addView(leftCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val rightCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        val statusBadge = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = FigmaUi.rounded(FigmaUi.successBg(isDark), 20f, ctx)
            setPadding(FigmaUi.dp(ctx, 10), FigmaUi.dp(ctx, 6), FigmaUi.dp(ctx, 10), FigmaUi.dp(ctx, 6))
        }
        statusBadge.addView(TextView(ctx).apply {
            text = "✓"
            setTextColor(FigmaUi.PRIMARY)
            textSize = 14f
            setPadding(0, 0, FigmaUi.dp(ctx, 6), 0)
        })
        statusTitle = TextView(ctx).apply {
            text = "وضعیت فعلی"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(FigmaUi.PRIMARY)
        }
        statusBadge.addView(statusTitle)
        rightCol.addView(statusBadge)
        statusSub = TextView(ctx).apply {
            textSize = 11f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.END
            setPadding(0, FigmaUi.dp(ctx, 6), 0, 0)
        }
        rightCol.addView(statusSub)
        statusInner.addView(rightCol)
        val statusV = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        statusV.addView(statusInner)
        statusV.addView(View(ctx).apply {
            setBackgroundColor(FigmaUi.PRIMARY)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, FigmaUi.dp(ctx, 3))
        })
        statusCard.addView(statusV)
        body.addView(statusCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 12), top = 0))

        // دکمه‌های ورود/خروج
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnIn = MaterialButton(ctx).apply {
            text = "ورود"
            setBackgroundColor(primary())
            setTextColor(0xFFFFFFFF.toInt())
            cornerRadius = FigmaUi.dp(ctx, 14)
        }
        btnOut = MaterialButton(ctx).apply {
            text = "خروج"
            setBackgroundColor(0xFFE53935.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            cornerRadius = FigmaUi.dp(ctx, 14)
        }
        btnRow.addView(btnOut, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = FigmaUi.dp(ctx, 8)
        })
        btnRow.addView(btnIn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(btnRow, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 12)))

        // ترددهای امروز
        body.addView(FigmaUi.sectionLabel(ctx, "ترددهای امروز", "مشاهده همه", {
            (activity as? MainActivity)?.openCalendar()
        }, isDark))
        val attCard = FigmaUi.whiteCard(ctx, isDark)
        attBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 4), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 4))
        }
        attCard.addView(attBox)
        body.addView(attCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 12)))

        // لاگ‌های Jira امروز
        body.addView(FigmaUi.sectionLabel(ctx, "لاگ‌های امروز Jira", "ثبت لاگ", dark = isDark))
        val logCard = FigmaUi.whiteCard(ctx, isDark)
        logBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 4), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 4))
        }
        logCard.addView(logBox)
        body.addView(logCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 12)))

        // تسک فعال
        val runCard = FigmaUi.whiteCard(ctx, isDark)
        val runInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12))
        }
        runInner.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "توقف"
            setTextColor(primary())
            cornerRadius = FigmaUi.dp(ctx, 20)
            isEnabled = false
        })
        runningText = TextView(ctx).apply {
            text = "تسک فعالی نیست"
            textSize = 13f
            setTextColor(FigmaUi.text(isDark))
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        runInner.addView(runningText)
        runCard.addView(runInner)
        body.addView(runCard, FigmaUi.matchParent())

        content.addView(body)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val repo = (requireActivity().application as App).repository
        btnIn.setOnClickListener { lifecycleScope.launch { repo.checkIn() } }
        btnOut.setOnClickListener { lifecycleScope.launch { repo.checkOut() } }

        // تاریخ هدر
        greetSub.text = try {
            val j = TimeUtils.toJalali(java.util.Date())
            val weekDays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
            val months = listOf("", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
            val cal = java.util.Calendar.getInstance()
            // weekday: TimeUtils.weekdayOf returns 0=Sat
            val wd = TimeUtils.weekdayOf(TimeUtils.today())
            val dayName = weekDays.getOrElse(wd) { "" }
            val monthName = months.getOrElse(j[1]) { "" }
            "$dayName، ${TimeUtils.faNum(j[2])} $monthName ${TimeUtils.faNum(j[0])}"
        } catch (_: Exception) {
            TimeUtils.faNum(TimeUtils.today())
        }

        // نام کاربر از settings
        viewLifecycleOwner.lifecycleScope.launch {
            val s = repo.getSettings()
            var name = s.jiraUsername.ifBlank { "همکار" }
            try {
                val service = com.personal.timetracker.jira.JiraService.fromSettings(s)
                if (service != null) {
                    service.myself().onSuccess { me ->
                        val dn = me.displayName?.trim().orEmpty()
                        if (dn.isNotBlank()) name = dn
                    }
                }
            } catch (_: Exception) {}
            greetTitle.text = "سلام، $name"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeRunningTask().collectLatest { task ->
                if (task == null) {
                    runningText.text = "تسک فعالی نیست"
                    runningText.setTextColor(FigmaUi.muted(isDark))
                } else {
                    runningText.setTextColor(FigmaUi.text(isDark))
                    runningText.text = buildString {
                        append("تسک فعال  ·  ")
                        if (!task.jiraNumber.isNullOrBlank()) append(task.jiraNumber).append(" · ")
                        append(task.taskTitle)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repo.observeToday().collectLatest { list ->
                    val settings = repo.getSettings()
                    val today = TimeUtils.today()
                    val required = repo.requiredMinutesFor(today, settings)
                    var worked = 0
                    var leave = 0
                    var ot = 0
                    attBox.removeAllViews()
                    if (list.isEmpty()) {
                        attBox.addView(TextView(ctx).apply {
                            text = "ترددی ثبت نشده"
                            textSize = 12f
                            setTextColor(FigmaUi.muted(isDark))
                            gravity = Gravity.CENTER
                            setPadding(0, FigmaUi.dp(ctx, 16), 0, FigmaUi.dp(ctx, 16))
                        })
                    }
                    list.forEach { rec ->
                        val done = rec.exitTime != null
                        val dur = if (done) rec.duration else {
                            TimeUtils.minutesBetween(rec.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                        }
                        worked += dur
                        val title = if (done) "خروج" else "ورود به محل کار"
                        val sub = if (done) "${TimeUtils.faNum(rec.entryTime)} – ${TimeUtils.faNum(rec.exitTime!!)}"
                        else "از ${TimeUtils.faNum(rec.entryTime)}"
                        val trail = if (done) TimeUtils.formatDuration(dur) else TimeUtils.faNum(rec.entryTime)
                        attBox.addView(
                            FigmaUi.listRow(
                                ctx, title, sub, trail,
                                if (done) FigmaUi.WARNING else FigmaUi.PRIMARY,
                                isDark
                            )
                        )
                    }
                    // اضافه‌کار / مرخصی بر اساس جمع کارکرد در برابر موظفی روز
                    ot = (worked - required).coerceAtLeast(0)
                    leave = (required - worked).coerceAtLeast(0)
                    chipWorked.text = TimeUtils.formatDuration(worked)
                    chipRequired.text = TimeUtils.formatDuration(required)
                    chipLeave.text = TimeUtils.formatDuration(leave)
                    chipOt.text = TimeUtils.formatDuration(ot)
                    // نمودار پیشرفت: کار کرده نسبت به موظفی، مرخصی، اضافه‌کار
                    if (chipBars.size >= 4 && required > 0) {
                        val pctWork = ((worked * 100) / required).coerceIn(0, 100)
                        val pctReq = 100
                        val pctLeave = ((leave * 100) / required).coerceIn(0, 100)
                        val pctOt = ((ot * 100) / required).coerceIn(0, 100)
                        // ترتیب labels: اضافه، مرخصی، موظفی، امروز
                        chipBars[0].progress = pctOt
                        chipBars[1].progress = pctLeave
                        chipBars[2].progress = pctReq
                        chipBars[3].progress = pctWork
                    }

                    val active = list.firstOrNull { it.exitTime == null && it.status == "active" }
                    stopTicker()
                    if (active != null) {
                        statusTitle.text = "در محل کار"
                        statusTitle.setTextColor(FigmaUi.PRIMARY)
                        statusSub.text = "ورود در ساعت ${TimeUtils.faNum(active.entryTime)}"
                        val flex = TimeCalc.applyFlex(
                            active.entryTime,
                            settings.startWorkTime,
                            settings.flexibleMinutes,
                            required
                        )
                        suggestedText.text = TimeUtils.faNum(flex.suggestedEnd)
                        startTicker(active.entryTime, flex.suggestedEnd)
                    } else {
                        statusTitle.text = "خارج از شیفت"
                        statusTitle.setTextColor(FigmaUi.muted(isDark))
                        statusSub.text = if (list.isEmpty()) "هنوز ورود ثبت نشده" else "آخرین خروج انجام شد"
                        suggestedText.text = "—"
                    }
                }
            } catch (_: Exception) {
            }
        }

        // لاگ‌های امروز
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val today = TimeUtils.today()
                val logs = repo.getJiraWorklogsForDate(today)
                logBox.removeAllViews()
                if (logs.isNullOrEmpty()) {
                    logBox.addView(TextView(ctx).apply {
                        text = "لاگی برای امروز نیست"
                        textSize = 12f
                        setTextColor(FigmaUi.muted(isDark))
                        gravity = Gravity.CENTER
                        setPadding(0, FigmaUi.dp(ctx, 16), 0, FigmaUi.dp(ctx, 16))
                    })
                } else {
                    logs.take(8).forEach { w ->
                        val key = w.issueKey ?: "—"
                        val title = w.comment ?: key
                        logBox.addView(
                            FigmaUi.listRow(
                                ctx,
                                key,
                                title,
                                TimeUtils.formatDuration(w.durationMinutes),
                                primary(),
                                isDark
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                logBox.removeAllViews()
                logBox.addView(TextView(ctx).apply {
                    text = "لاگ در دسترس نیست"
                    textSize = 12f
                    setTextColor(FigmaUi.muted(isDark))
                    gravity = Gravity.CENTER
                    setPadding(0, FigmaUi.dp(ctx, 12), 0, FigmaUi.dp(ctx, 12))
                })
            }
        }

        return root
    }

    private fun startTicker(entryTime: String, targetEnd: String) {
        tickerJob?.cancel()
        tickerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val elapsed = TimeUtils.minutesBetween(entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                val (leave, liveOt) = TimeCalc.exitOutcome(TimeUtils.nowTime(), targetEnd)
                statusSub.text = buildString {
                    append("از ساعت "); append(TimeUtils.faNum(entryTime))
                    append("  ·  "); append(TimeUtils.formatDuration(elapsed))
                    if (liveOt > 0) {
                        append("  ·  اضافه‌کار ")
                        append(TimeUtils.formatDuration(liveOt))
                    }
                }
                delay(30_000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onDestroyView() {
        stopTicker()
        super.onDestroyView()
    }
}
