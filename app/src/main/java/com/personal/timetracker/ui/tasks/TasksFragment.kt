package com.personal.timetracker.ui.tasks

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.personal.timetracker.App
import com.personal.timetracker.data.entity.JiraIssueCacheEntity
import com.personal.timetracker.data.entity.JiraWorklogCacheEntity
import com.personal.timetracker.data.repository.AppRepository
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.JalaliDatePickerDialog
import com.personal.timetracker.util.ChartHelper
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * تسک‌ها = Issueهای جیرای شرکت (کش محلی + سینک).
 * لاگ = Worklog جیرا (با صف آفلاین).
 */
class TasksFragment : Fragment() {

    private lateinit var repo: AppRepository
    private lateinit var listContainer: LinearLayout
    private lateinit var statusTv: TextView
    private lateinit var searchEdit: TextInputEditText

    /** assigned | favorites | open | all */
    private var mode = "assigned"
    /** فیلتر چندوضعیتی؛ خالی = بدون محدودیت (یا محدود به تنظیمات) */
    private val statusFilter = linkedSetOf<String>()
    private val projectFilter = linkedSetOf<String>()
    private var allowedStatusesFromSettings: List<String> = emptyList()
    private var allowedProjectsFromSettings: List<String> = emptyList()
    private lateinit var statusFilterBtn: MaterialButton
    private lateinit var projectFilterBtn: MaterialButton
    private lateinit var loadMoreBtn: MaterialButton
    private var pageStartAt: Int = 0
    private var pageTotal: Int = 0
    private var isLoadingPage = false
    private var projectCatalog: List<String> = emptyList()
    private var filtersExpanded = false
    private lateinit var filtersPanel: LinearLayout
    private lateinit var filtersToggle: MaterialButton
    private var collectJob: Job? = null
    private val expanded = mutableSetOf<String>()
    private var cache: List<JiraIssueCacheEntity> = emptyList()
    private var statuses: List<String> = emptyList()

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        repo = (requireActivity().application as App).repository

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.surface(dark()))
            setPadding(16)
        }
        root.addView(ThemeHelper.pageTitle(ctx, "تسک‌ها (جیرا)", dark()))

        filtersToggle = MaterialButton(ctx).apply { text = "فیلتر و جستجو ▸" }
        ThemeHelper.applyButton(filtersToggle, primary(), false)
        filtersToggle.setOnClickListener {
            filtersExpanded = !filtersExpanded
            filtersPanel.visibility = if (filtersExpanded) View.VISIBLE else View.GONE
            filtersToggle.text = if (filtersExpanded) "فیلتر و جستجو ▾" else "فیلتر و جستجو ▸"
        }
        root.addView(filtersToggle)

        filtersPanel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(filtersPanel)

        // Mode chips
        val modes = ChipGroup(ctx).apply { isSingleSelection = true }
        fun modeChip(label: String, value: String, checked: Boolean = false) {
            modes.addView(Chip(ctx).apply {
                text = label
                isCheckable = true
                isChecked = checked
                setOnClickListener {
                    mode = value
                    viewLifecycleOwner.lifecycleScope.launch {
                        loadPage(reset = true)
                    }
                }
            })
        }
        modeChip("اساین به من", "assigned", true)
        modeChip("علاقه‌مندی", "favorites")
        modeChip("باز", "open")
        modeChip("همه", "all")
        filtersPanel.addView(modes)

        // فیلتر وضعیت جمع‌وجور (مالتی‌سلکت در دیالوگ — فضای لیست را نمی‌گیرد)
        statusFilterBtn = MaterialButton(ctx).apply { text = "وضعیت: همه ▾" }
        ThemeHelper.applyButton(statusFilterBtn, primary(), false)
        statusFilterBtn.setOnClickListener { openStatusFilterDialog() }
        filtersPanel.addView(statusFilterBtn)

        projectFilterBtn = MaterialButton(ctx).apply { text = "پروژه: همه ▾" }
        ThemeHelper.applyButton(projectFilterBtn, primary(), false)
        projectFilterBtn.setOnClickListener { openProjectFilterDialog() }
        filtersPanel.addView(projectFilterBtn)

        searchEdit = TextInputEditText(ctx).apply { hint = "جستجو کلید / عنوان / پروژه..." }
        filtersPanel.addView(TextInputLayout(ctx).apply {
            hint = "جستجو در پروژه‌های فیلتر"
            addView(searchEdit)
        })
        searchEdit.addTextChangedListener {
            bindList()
            val q = it?.toString()?.trim().orEmpty()
            if (q.length >= 2 || q.isEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(400)
                    if (searchEdit.text?.toString()?.trim().orEmpty() == q) {
                        loadPage(reset = true)
                    }
                }
            }
        }

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val refreshBtn = MaterialButton(ctx).apply { text = "↻ همگام‌سازی" }
        ThemeHelper.applyButton(refreshBtn, primary(), true)
        refreshBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = 8
        }
        refreshBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                refreshBtn.isEnabled = false
                refreshBtn.text = "..."
                try {
                    repo.flushPendingJiraWorklogs()
                    repo.refreshJiraStatuses()
                    loadPage(reset = true)
                } finally {
                    refreshBtn.isEnabled = true
                    refreshBtn.text = "↻ همگام‌سازی"
                }
            }
        }
        btnRow.addView(refreshBtn)

        val addFavBtn = MaterialButton(ctx).apply { text = "＋ کلید Issue" }
        ThemeHelper.applyButton(addFavBtn, primary(), false)
        addFavBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        addFavBtn.setOnClickListener { showAddByKeyDialog() }
        btnRow.addView(addFavBtn)
        root.addView(btnRow)

        statusTv = TextView(ctx).apply {
            textSize = 12.5f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(4, 8, 4, 4)
            text = "برای دریافت از سرور، همگام‌سازی را بزنید"
        }
        root.addView(statusTv)

        loadMoreBtn = MaterialButton(ctx).apply {
            text = "بارگذاری بیشتر"
            visibility = View.GONE
        }
        ThemeHelper.applyButton(loadMoreBtn, primary(), false)
        loadMoreBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { loadPage(reset = false) }
        }
        root.addView(loadMoreBtn)

        listContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(listContainer)
        })

        // Observe cache
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            repo.observeJiraIssues().collectLatest {
                cache = it
                bindList()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = repo.getSettings()
            allowedStatusesFromSettings = settings.jiraFilterStatuses.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            allowedProjectsFromSettings = settings.jiraFilterProjects.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            projectCatalog = repo.getJiraProjectCatalog()
            statusFilter.clear()
            statusFilter.addAll(allowedStatusesFromSettings)
            projectFilter.clear()
            projectFilter.addAll(allowedProjectsFromSettings)
            statuses = repo.getJiraStatuses().map { it.name }.distinct().sorted()
            updateStatusFilterButton()
            updateProjectFilterButton()
            bindList()
            if (repo.jiraServiceOrNull() != null) {
                loadPage(reset = true)
            }
        }

        // Auto-refresh if empty and jira configured
        viewLifecycleOwner.lifecycleScope.launch {
            if (repo.getJiraStatuses().isEmpty()) {
                repo.refreshJiraStatuses()
            }
        }

        return root
    }

    private fun bindList() {
        if (!isAdded) return
        val ctx = requireContext()
        val q = searchEdit.text?.toString().orEmpty().trim().lowercase()
        var list = when (mode) {
            "assigned" -> cache.filter { it.isAssignedToMe }
            "favorites" -> cache.filter { it.isFavorite }
            "open" -> cache.filter { it.statusCategory != "done" }
            else -> cache
        }
        if (statusFilter.isNotEmpty()) {
            list = list.filter { it.statusName in statusFilter }
        }
        if (projectFilter.isNotEmpty()) {
            list = list.filter {
                it.projectKey in projectFilter || it.projectName in projectFilter
            }
        }
        if (q.isNotEmpty()) {
            list = list.filter {
                it.issueKey.lowercase().contains(q) ||
                    it.summary.lowercase().contains(q) ||
                    it.projectKey.lowercase().contains(q) ||
                    it.statusName.lowercase().contains(q)
            }
        }
        listContainer.removeAllViews()
        if (list.isEmpty()) {
            listContainer.addView(TextView(ctx).apply {
                text = "موردی نیست — همگام‌سازی کنید یا کلید Issue اضافه کنید"
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(12, 28, 12, 12)
                gravity = Gravity.CENTER
            })
            return
        }
        statusTv.text = "${list.size} مورد"
        list.forEach { issue -> listContainer.addView(issueCard(issue)) }
    }

    private fun issueCard(issue: JiraIssueCacheEntity): View {
        val ctx = requireContext()
        val card = MaterialCardView(ctx)
        ThemeHelper.applyCard(card, dark())
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val keyCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        keyCol.addView(TextView(ctx).apply {
            text = issue.issueKey
            setTypeface(null, Typeface.BOLD)
            setTextColor(primary())
            textSize = 15f
        })
        // آیکن علاقه‌مندی زیر شماره جیرا (بزرگ‌تر)
        keyCol.addView(TextView(ctx).apply {
            text = if (issue.isFavorite) "★" else "☆"
            textSize = 22f
            setTextColor(if (issue.isFavorite) 0xFFFFC107.toInt() else ThemeHelper.textSecondary(dark()))
            setPadding(0, 2, 0, 0)
            setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val on = repo.toggleJiraFavorite(
                        issue.issueKey, issue.summary, issue.projectKey, issue.projectName
                    )
                    Toast.makeText(ctx, if (on) "علاقه‌مندی شد" else "حذف از علاقه‌مندی", Toast.LENGTH_SHORT).show()
                }
            }
        })
        header.addView(keyCol)
        // دونات پیشرفت: صرف‌شده / تخمین
        val estimate = issue.requiredMinutes.takeIf { it > 0 } ?: (issue.remainingMinutes + issue.timeSpentMinutes)
        header.addView(
            ChartHelper.jiraProgressDonut(
                ctx,
                spentMinutes = issue.timeSpentMinutes,
                estimateMinutes = estimate,
                primary = primary(),
                dark = dark(),
                sizeDp = 42
            )
        )
        header.addView(TextView(ctx).apply {
            text = issue.statusName.ifBlank { "—" }
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(10, 0, 0, 0)
        })
        box.addView(header)

        box.addView(TextView(ctx).apply {
            text = issue.summary
            textSize = 15.5f
            setTextColor(ThemeHelper.textPrimary(dark()))
            setPadding(0, 6, 0, 2)
        })

        val meta = buildString {
            if (issue.projectKey.isNotBlank()) append(issue.projectKey)
            if (issue.priorityName.isNotBlank()) {
                if (isNotEmpty()) append(" · "); append(issue.priorityName)
            }
            if (issue.timeSpentMinutes > 0 || estimate > 0) {
                if (isNotEmpty()) append(" · ")
                append("${fmt(issue.timeSpentMinutes)}")
                if (estimate > 0) append(" / ${fmt(estimate)}")
            }
            if (issue.isAssignedToMe) {
                if (isNotEmpty()) append(" · "); append("اساین من")
            }
        }
        if (meta.isNotBlank()) {
            box.addView(TextView(ctx).apply {
                text = meta
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }

        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 0)
        }
        actions.addView(smallBtn("⏱ لاگ", true) { showAddLog(issue) })
        actions.addView(smallBtn("جزئیات", false) { openIssueDetail(issue) })
        actions.addView(smallBtn("لاگ‌ها", false) {
            if (issue.issueKey in expanded) expanded.remove(issue.issueKey)
            else expanded.add(issue.issueKey)
            bindList()
        })
        box.addView(actions)

        if (issue.issueKey in expanded) {
            val drawer = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 0)
            }
            drawer.addView(ThemeHelper.divider(ctx, dark()))
            drawer.addView(TextView(ctx).apply {
                text = "Worklogها (از سرور / کش)"
                textSize = 12.5f
                setTextColor(primary())
                setPadding(0, 8, 0, 6)
            })
            val logsBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            drawer.addView(logsBox)
            box.addView(drawer)
            viewLifecycleOwner.lifecycleScope.launch {
                // refresh remote then show
                repo.refreshJiraWorklogs(issue.issueKey)
                val logs = repo.getJiraWorklogsOnce(issue.issueKey)
                logsBox.removeAllViews()
                if (logs.isEmpty()) {
                    logsBox.addView(TextView(ctx).apply {
                        text = "لاگی نیست"
                        setTextColor(ThemeHelper.textSecondary(dark()))
                        textSize = 12f
                    })
                } else {
                    logs.forEach { wl -> logsBox.addView(worklogRow(issue, wl)) }
                }
            }
        }

        card.addView(box)
        return card.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
    }

    private fun worklogRow(issue: JiraIssueCacheEntity, wl: JiraWorklogCacheEntity): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 6)
        }
        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val pending = when (wl.syncStatus) {
            "pending_add" -> " ⏳ارسال"
            "pending_update" -> " ⏳ویرایش"
            "pending_delete" -> " ⏳حذف"
            else -> ""
        }
        info.addView(TextView(ctx).apply {
            text = "${TimeUtils.toJalaliDisplay(wl.date)} · ${fmt(wl.durationMinutes)}$pending"
            textSize = 12.5f
            setTextColor(ThemeHelper.textPrimary(dark()))
        })
        if (!wl.comment.isNullOrBlank()) {
            info.addView(TextView(ctx).apply {
                text = wl.comment
                textSize = 11f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }
        if (!wl.authorName.isNullOrBlank()) {
            info.addView(TextView(ctx).apply {
                text = wl.authorName
                textSize = 11f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }
        row.addView(info)
        row.addView(ThemeHelper.iconButton(ctx, "✎", primary(), dark(), "ویرایش") {
            showEditLog(issue, wl)
        })
        row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark(), "حذف") {
            DialogHelper.confirm(
                ctx, title = "حذف Worklog",
                message = "${TimeUtils.toJalaliDisplay(wl.date)} — ${fmt(wl.durationMinutes)}",
                primary = primary(), dark = dark()
            ) {
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteJiraTaskLog(wl.localId)
                    bindList()
                }
            }
        })
        return row
    }

    private fun smallBtn(label: String, filled: Boolean, onClick: () -> Unit): MaterialButton {
        val ctx = requireContext()
        return MaterialButton(
            ctx, null,
            if (filled) com.google.android.material.R.attr.materialButtonStyle
            else com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = 6 }
            ThemeHelper.applyButton(this, primary(), filled)
            setOnClickListener { onClick() }
        }
    }

    private fun showAddByKeyDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val (keyL, keyE) = DialogHelper.inputField(ctx, "کلید Issue (مثلاً PROJ-123)", "", primary())
        layout.addView(keyL)
        DialogHelper.show(
            ctx, icon = "＋", title = "افزودن Issue",
            subtitle = "از سرور دریافت و به علاقه‌مندی اضافه می‌شود",
            primary = primary(), dark = dark(), body = layout, positiveText = "افزودن",
            onPositive = {
                val key = keyE.text?.toString()?.trim().orEmpty()
                if (key.isBlank()) {
                    Toast.makeText(ctx, "کلید لازم است", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.toggleJiraFavorite(key)
                    repo.refreshJiraIssues(openOnly = false)
                    mode = "favorites"
                    Toast.makeText(ctx, "اضافه شد", Toast.LENGTH_SHORT).show()
                }
                true
            }
        )
    }

    private fun showAddLog(issue: JiraIssueCacheEntity) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark()))
        val today = TimeUtils.today()
        val (dateL, dateField) = DialogHelper.inputField(
            ctx, "تاریخ", TimeUtils.toJalaliShort(TimeUtils.parseDate(today)), primary()
        )
        dateField.tag = today
        dateField.isFocusable = false
        dateField.setOnClickListener {
            JalaliDatePickerDialog.show(
                ctx, primary = primary(), dark = dark(),
                initialGregorianDate = dateField.tag as? String
            ) { g, j ->
                dateField.tag = g
                dateField.setText(j)
            }
        }
        layout.addView(dateL)
        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark()))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hL, hE) = DialogHelper.inputField(ctx, "ساعت", "1", primary(), number = true)
        val (mL, mE) = DialogHelper.inputField(ctx, "دقیقه", "0", primary(), number = true)
        hL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        mL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hL); row.addView(mL)
        layout.addView(row)
        layout.addView(DialogHelper.sectionLabel(ctx, "توضیح", dark()))
        val (nL, nE) = DialogHelper.inputField(ctx, "توضیح", "", primary(), multiline = true)
        layout.addView(nL)

        DialogHelper.show(
            ctx, icon = "⏱", title = "ثبت Worklog",
            subtitle = "${issue.issueKey} — ${issue.summary}",
            primary = primary(), dark = dark(), body = layout, positiveText = "ثبت و ارسال",
            onPositive = {
                val date = (dateField.tag as? String)?.ifBlank { today } ?: today
                val dur = ((hE.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                    ((mE.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت نامعتبر", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = repo.addJiraTaskLog(issue.issueKey, dur, date, nE.text?.toString())
                    r.fold(
                        onSuccess = {
                            val msg = if (it.syncStatus == "synced") "ثبت و به جیرا ارسال شد"
                            else "ذخیره شد — بعداً همگام می‌شود"
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            expanded.add(issue.issueKey)
                            bindList()
                        },
                        onFailure = { e -> Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
                    )
                }
                true
            }
        )
    }

    private fun showEditLog(issue: JiraIssueCacheEntity, wl: JiraWorklogCacheEntity) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark()))
        val (dateL, dateField) = DialogHelper.inputField(
            ctx, "تاریخ", TimeUtils.toJalaliShort(TimeUtils.parseDate(wl.date)), primary()
        )
        dateField.tag = wl.date
        dateField.isFocusable = false
        dateField.setOnClickListener {
            JalaliDatePickerDialog.show(
                ctx, primary = primary(), dark = dark(),
                initialGregorianDate = dateField.tag as? String
            ) { g, j ->
                dateField.tag = g
                dateField.setText(j)
            }
        }
        layout.addView(dateL)
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hL, hE) = DialogHelper.inputField(ctx, "ساعت", (wl.durationMinutes / 60).toString(), primary(), number = true)
        val (mL, mE) = DialogHelper.inputField(ctx, "دقیقه", (wl.durationMinutes % 60).toString(), primary(), number = true)
        hL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        mL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hL); row.addView(mL)
        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark()))
        layout.addView(row)
        val (nL, nE) = DialogHelper.inputField(ctx, "توضیح", wl.comment ?: "", primary(), multiline = true)
        layout.addView(nL)

        DialogHelper.show(
            ctx, icon = "✎", title = "ویرایش Worklog",
            subtitle = issue.issueKey,
            primary = primary(), dark = dark(), body = layout, positiveText = "ذخیره",
            onPositive = {
                val date = (dateField.tag as? String) ?: wl.date
                val dur = ((hE.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                    ((mE.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت نامعتبر", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.updateJiraTaskLog(wl.localId, dur, date, nE.text?.toString())
                    Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                    bindList()
                }
                true
            }
        )
    }



    private suspend fun loadPage(reset: Boolean) {
        if (isLoadingPage) return
        isLoadingPage = true
        try {
            if (reset) {
                pageStartAt = 0
                pageTotal = 0
            }
            val projects = projectFilter.toList()
            val statuses = statusFilter.toList()
            val q = searchEdit.text?.toString()?.trim().orEmpty().ifBlank { null }
            statusTv.text = "در حال بارگذاری از سرور…"
            val result = repo.refreshJiraIssues(
                openOnly = (mode == "open"),
                projectKeys = projects,
                textQuery = q,
                startAt = pageStartAt,
                pageSize = 50,
                append = !reset || pageStartAt > 0,
                assignedToMe = (mode == "assigned"),
                statusNames = statuses
            )
            result.fold(
                onSuccess = { (count, next, total) ->
                    pageStartAt = next
                    pageTotal = total
                    statusTv.text = if (projects.isNotEmpty()) {
                        "نمایش از کش · سرور: $next از $total (این صفحه: $count)"
                    } else {
                        "اساین‌شده / بدون فیلتر پروژه · $count مورد"
                    }
                    if (::loadMoreBtn.isInitialized) {
                        loadMoreBtn.visibility =
                            if (next < total && projects.isNotEmpty()) View.VISIBLE else View.GONE
                        loadMoreBtn.text = "بارگذاری بیشتر ($next / $total)"
                    }
                    if (count == 0 && reset) {
                        Toast.makeText(requireContext(), "موردی یافت نشد", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { e ->
                    statusTv.text = "خطا: ${e.message}"
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            )
        } finally {
            isLoadingPage = false
        }
    }

    private fun updateStatusFilterButton() {
        if (!::statusFilterBtn.isInitialized) return
        statusFilterBtn.text = when {
            statusFilter.isEmpty() -> "وضعیت: همه ▾"
            statusFilter.size == 1 -> "وضعیت: ${statusFilter.first()} ▾"
            else -> "وضعیت: ${statusFilter.size} مورد ▾"
        }
    }

    private fun openStatusFilterDialog() {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            if (statuses.isEmpty()) {
                repo.refreshJiraStatuses()
                statuses = repo.getJiraStatuses().map { it.name }.distinct().sorted()
            }
            val settings = repo.getSettings()
            allowedStatusesFromSettings = settings.jiraFilterStatuses.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            // همه وضعیت‌ها در لیست؛ تیک‌خورده‌ها = فیلتر فعلی
            val options = statuses.ifEmpty {
                repo.getJiraStatuses().map { it.name }.distinct().sorted()
            }
            if (options.isEmpty()) {
                Toast.makeText(ctx, "وضعیتی موجود نیست — همگام‌سازی یا تنظیمات جیرا", Toast.LENGTH_LONG).show()
                return@launch
            }
            val checked = BooleanArray(options.size) { options[it] in statusFilter }
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("فیلتر وضعیت (چندتایی)")
                .setMultiChoiceItems(options.toTypedArray(), checked) { _, which, isChecked ->
                    if (isChecked) statusFilter.add(options[which])
                    else statusFilter.remove(options[which])
                }
                .setPositiveButton("اعمال") { _, _ ->
                    updateStatusFilterButton()
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.saveJiraListFilters(projectFilter, statusFilter)
                        loadPage(reset = true)
                    }
                    bindList()
                }
                .setNeutralButton("پاک کردن") { _, _ ->
                    statusFilter.clear()
                    updateStatusFilterButton()
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.saveJiraListFilters(projectFilter, statusFilter)
                        loadPage(reset = true)
                    }
                    bindList()
                }
                .setNegativeButton("انصراف", null)
                .show()
        }
    }


    private fun updateProjectFilterButton() {
        if (!::projectFilterBtn.isInitialized) return
        projectFilterBtn.text = when {
            projectFilter.isEmpty() -> "پروژه: همه ▾"
            projectFilter.size == 1 -> "پروژه: ${projectFilter.first()} ▾"
            else -> "پروژه: ${projectFilter.size} مورد ▾"
        }
    }

    private fun openProjectFilterDialog() {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            projectCatalog = repo.getJiraProjectCatalog()
            val fromCache = cache.map { it.projectKey.ifBlank { it.projectName } }
                .filter { it.isNotBlank() }.distinct()
            val options = (projectCatalog + fromCache).distinct().sorted()
            if (options.isEmpty()) {
                Toast.makeText(
                    ctx,
                    "لیست پروژه خالی است — از تنظیمات «بروزرسانی پروژه‌ها» را بزنید",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val checked = BooleanArray(options.size) { options[it] in projectFilter }
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("فیلتر پروژه (چندتایی)")
                .setMultiChoiceItems(options.toTypedArray(), checked) { _, which, isChecked ->
                    if (isChecked) projectFilter.add(options[which])
                    else projectFilter.remove(options[which])
                }
                .setPositiveButton("اعمال") { _, _ ->
                    updateProjectFilterButton()
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.saveJiraListFilters(projectFilter, statusFilter)
                        loadPage(reset = true)
                    }
                    bindList()
                }
                .setNeutralButton("پاک کردن") { _, _ ->
                    projectFilter.clear()
                    updateProjectFilterButton()
                    viewLifecycleOwner.lifecycleScope.launch {
                        repo.saveJiraListFilters(projectFilter, statusFilter)
                        loadPage(reset = true)
                    }
                    bindList()
                }
                .setNegativeButton("انصراف", null)
                .show()
        }
    }

    private fun openIssueDetail(issue: JiraIssueCacheEntity) {
        val ctx = requireContext()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
        }
        val infoTv = TextView(ctx).apply {
            text = "در حال بارگذاری…"
            setTextColor(ThemeHelper.textPrimary(dark()))
            textSize = 13.5f
        }
        body.addView(infoTv)
        body.addView(DialogHelper.sectionLabel(ctx, "توضیحات / درخواست", dark()))
        val descTv = TextView(ctx).apply {
            text = issue.description?.take(2000) ?: "—"
            setTextColor(ThemeHelper.textPrimary(dark()))
            textSize = 13f
            setPadding(0, 4, 0, 8)
        }
        body.addView(descTv)
        body.addView(DialogHelper.sectionLabel(ctx, "کامنت‌ها", dark()))
        val commentsBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(commentsBox)
        body.addView(DialogHelper.sectionLabel(ctx, "تغییر وضعیت (Workflow)", dark()))
        val transitionsBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 8)
        }
        body.addView(transitionsBox)

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(smallBtn("＋ کامنت", false) { showAddComment(issue.issueKey) })
        btnRow.addView(smallBtn("⏱ لاگ", true) { showAddLog(issue) })
        body.addView(btnRow)

        DialogHelper.show(
            ctx = ctx, icon = "📋", title = issue.issueKey,
            subtitle = issue.summary,
            primary = primary(), dark = dark(), body = body,
            positiveText = "بستن", negativeText = null,
            onPositive = { true }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val service = repo.jiraServiceOrNull()
            if (service != null) {
                service.getIssue(issue.issueKey).onSuccess { full ->
                    infoTv.text = buildString {
                        append(full.summary)
                        append("\nوضعیت: ${full.statusName}")
                        full.assigneeName?.let { append(" · مسئول: $it") }
                        append("\nپروژه: ${full.projectKey}")
                        if (full.priorityName.isNotBlank()) append(" · اولویت: ${full.priorityName}")
                        if (full.timeSpentMinutes > 0) append("\nصرف‌شده: ${fmt(full.timeSpentMinutes)}")
                        if (full.remainingMinutes > 0) append(" · باقی: ${fmt(full.remainingMinutes)}")
                    }
                    descTv.text = full.description?.ifBlank { "—" } ?: "—"
                }
            } else {
                infoTv.text = "${issue.summary}\nوضعیت: ${issue.statusName}\nپروژه: ${issue.projectKey}"
            }
            repo.getJiraComments(issue.issueKey).fold(
                onSuccess = { list ->
                    commentsBox.removeAllViews()
                    if (list.isEmpty()) {
                        commentsBox.addView(TextView(ctx).apply {
                            text = "کامنتی نیست"
                            setTextColor(ThemeHelper.textSecondary(dark()))
                            textSize = 12f
                        })
                    } else {
                        list.take(15).forEach { c ->
                            commentsBox.addView(TextView(ctx).apply {
                                text = "${c.author?.displayName ?: "?"}:\n${c.body?.take(500) ?: ""}"
                                setTextColor(ThemeHelper.textPrimary(dark()))
                                textSize = 12.5f
                                setPadding(0, 6, 0, 6)
                            })
                        }
                    }
                },
                onFailure = {
                    commentsBox.addView(TextView(ctx).apply {
                        text = "خطا در دریافت کامنت: ${it.message}"
                        setTextColor(ThemeHelper.textSecondary(dark()))
                    })
                }
            )
            repo.fetchJiraTransitions(issue.issueKey).fold(
                onSuccess = { list ->
                    transitionsBox.removeAllViews()
                    if (list.isEmpty()) {
                        transitionsBox.addView(TextView(ctx).apply {
                            text = "انتقالی در دسترس نیست"
                            setTextColor(ThemeHelper.textSecondary(dark()))
                        })
                    } else {
                        list.forEach { tr ->
                            val label = buildString {
                                append(tr.name ?: tr.id)
                                tr.to?.name?.let { append(" → $it") }
                            }
                            transitionsBox.addView(
                                MaterialButton(ctx).apply {
                                    text = label
                                    ThemeHelper.applyButton(this, primary(), false)
                                    setOnClickListener {
                                        viewLifecycleOwner.lifecycleScope.launch {
                                            repo.transitionJiraIssue(issue.issueKey, tr.id!!).fold(
                                                onSuccess = {
                                                    Toast.makeText(ctx, "وضعیت تغییر کرد", Toast.LENGTH_SHORT).show()
                                                    repo.refreshJiraIssues(openOnly = false)
                                                },
                                                onFailure = { e ->
                                                    Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                onFailure = {
                    transitionsBox.addView(TextView(ctx).apply {
                        text = "خطا: ${it.message}"
                        setTextColor(ThemeHelper.textSecondary(dark()))
                    })
                }
            )
        }
    }

    private fun showAddComment(issueKey: String) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val (bL, bE) = DialogHelper.inputField(ctx, "متن کامنت", "", primary(), multiline = true)
        layout.addView(bL)
        DialogHelper.show(
            ctx, icon = "💬", title = "کامنت جدید", subtitle = issueKey,
            primary = primary(), dark = dark(), body = layout, positiveText = "ارسال",
            onPositive = {
                val text = bE.text?.toString().orEmpty()
                if (text.isBlank()) {
                    Toast.makeText(ctx, "متن خالی است", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.addJiraComment(issueKey, text).fold(
                        onSuccess = { Toast.makeText(ctx, "کامنت ثبت شد", Toast.LENGTH_SHORT).show() },
                        onFailure = { e -> Toast.makeText(ctx, e.message, Toast.LENGTH_LONG).show() }
                    )
                }
                true
            }
        )
    }

    private fun fmt(m: Int): String {
        val h = m / 60
        val r = m % 60
        return when {
            h > 0 && r > 0 -> "${h}س ${r}د"
            h > 0 -> "${h}س"
            else -> "${r}د"
        }
    }
}
