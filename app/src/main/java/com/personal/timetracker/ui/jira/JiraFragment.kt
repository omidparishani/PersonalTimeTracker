package com.personal.timetracker.ui.jira

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
import com.personal.timetracker.data.entity.JiraFavoriteEntity
import com.personal.timetracker.data.repository.AppRepository
import com.personal.timetracker.jira.JiraService
import com.personal.timetracker.jira.JiraWorklog
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.JalaliDatePickerDialog
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * صفحه اصلی جیرا:
 * - تب اساین‌شده (با فیلتر وضعیت / جستجو)
 * - تب علاقه‌مندی‌ها (Issueهای غیراساین مثل Daily و جلسات)
 * - جزئیات Issue: Worklogها از سرور + ثبت لاگ + کامنت + افزودن به علاقه‌مندی
 */
class JiraFragment : Fragment() {

    private lateinit var repo: AppRepository
    private lateinit var listContainer: LinearLayout
    private lateinit var statusTv: TextView
    private lateinit var searchEdit: TextInputEditText

    /** assigned | favorites | search */
    private var mode: String = "assigned"
    private var openOnly: Boolean = true
    private var loadJob: Job? = null
    private var favorites: List<JiraFavoriteEntity> = emptyList()
    private var assignedCache: List<JiraService.IssueItem> = emptyList()

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

        root.addView(ThemeHelper.pageTitle(ctx, "جیرا", dark()))

        // Mode chips
        val modes = ChipGroup(ctx).apply { isSingleSelection = true }
        fun addMode(label: String, value: String, checked: Boolean = false) {
            modes.addView(Chip(ctx).apply {
                text = label
                isCheckable = true
                isChecked = checked
                setOnClickListener {
                    mode = value
                    reload()
                }
            })
        }
        addMode("اساین‌شده", "assigned", true)
        addMode("علاقه‌مندی‌ها", "favorites")
        addMode("جستجو", "search")
        root.addView(modes)

        // Search / filter row
        searchEdit = TextInputEditText(ctx).apply {
            hint = when (mode) {
                "search" -> "کلید Issue یا متن..."
                else -> "فیلتر عنوان / کلید..."
            }
        }
        root.addView(TextInputLayout(ctx).apply {
            this.hint = "جستجو"
            addView(searchEdit)
        })
        searchEdit.addTextChangedListener { renderList() }

        // Open-only filter (assigned mode)
        val filterRow = ChipGroup(ctx).apply { isSingleSelection = true }
        val chipOpen = Chip(ctx).apply {
            text = "فقط باز"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                openOnly = true
                if (mode == "assigned") reload()
            }
        }
        val chipAll = Chip(ctx).apply {
            text = "همه"
            isCheckable = true
            setOnClickListener {
                openOnly = false
                if (mode == "assigned") reload()
            }
        }
        filterRow.addView(chipOpen)
        filterRow.addView(chipAll)
        root.addView(filterRow)

        // Action buttons
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val refreshBtn = MaterialButton(ctx).apply { text = "↻ بروزرسانی" }
        ThemeHelper.applyButton(refreshBtn, primary(), true)
        refreshBtn.setOnClickListener { reload() }
        refreshBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = 8
        }
        btnRow.addView(refreshBtn)

        val addFavBtn = MaterialButton(ctx).apply { text = "＋ علاقه‌مندی" }
        ThemeHelper.applyButton(addFavBtn, primary(), false)
        addFavBtn.setOnClickListener { showAddFavoriteDialog() }
        addFavBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        btnRow.addView(addFavBtn)
        root.addView(btnRow)

        val logAnyBtn = MaterialButton(ctx).apply { text = "⏱ لاگ روی Issue دلخواه" }
        ThemeHelper.applyButton(logAnyBtn, primary(), false)
        logAnyBtn.setOnClickListener { showLogOnAnyIssueDialog() }
        root.addView(logAnyBtn)

        statusTv = TextView(ctx).apply {
            textSize = 12.5f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(4, 8, 4, 4)
        }
        root.addView(statusTv)

        listContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(listContainer)
        })

        // Observe favorites
        viewLifecycleOwner.lifecycleScope.launch {
            repo.observeJiraFavorites().collectLatest {
                favorites = it
                if (mode == "favorites") renderList()
            }
        }

        reload()
        return root
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            statusTv.text = "در حال بارگذاری..."
            listContainer.removeAllViews()
            when (mode) {
                "assigned" -> loadAssigned()
                "favorites" -> {
                    statusTv.text = if (favorites.isEmpty()) "علاقه‌مندی خالی است — با «＋ علاقه‌مندی» اضافه کنید"
                    else "${favorites.size} مورد"
                    renderList()
                }
                "search" -> {
                    val q = searchEdit.text?.toString().orEmpty().trim()
                    if (q.length < 2) {
                        statusTv.text = "حداقل ۲ حرف برای جستجو وارد کنید"
                        return@launch
                    }
                    loadSearch(q)
                }
            }
        }
    }

    private suspend fun loadAssigned() {
        val service = repo.jiraServiceOrNull()
        if (service == null) {
            statusTv.text = "جیرا پیکربندی نشده — به تنظیمات بروید"
            return
        }
        val result = service.fetchAssigned(openOnly = openOnly)
        result.fold(
            onSuccess = {
                assignedCache = it
                statusTv.text = "${it.size} Issue"
                renderList()
            },
            onFailure = {
                statusTv.text = "خطا: ${it.message}"
                Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
            }
        )
    }

    private suspend fun loadSearch(q: String) {
        val service = repo.jiraServiceOrNull()
        if (service == null) {
            statusTv.text = "جیرا پیکربندی نشده"
            return
        }
        service.search(q).fold(
            onSuccess = { page ->
                assignedCache = page.items
                statusTv.text = "${page.items.size} / ${page.total} نتیجه"
                renderList()
            },
            onFailure = {
                statusTv.text = "خطا: ${it.message}"
            }
        )
    }

    private fun renderList() {
        listContainer.removeAllViews()
        val ctx = requireContext()
        val filter = searchEdit.text?.toString().orEmpty().trim().lowercase()

        when (mode) {
            "favorites" -> {
                val list = favorites.filter {
                    filter.isEmpty() ||
                        it.issueKey.lowercase().contains(filter) ||
                        it.summary.lowercase().contains(filter) ||
                        it.projectKey.lowercase().contains(filter)
                }
                if (list.isEmpty()) {
                    listContainer.addView(emptyHint("علاقه‌مندی‌ای یافت نشد"))
                    return
                }
                list.forEach { fav ->
                    listContainer.addView(favoriteCard(fav))
                }
            }
            else -> {
                val list = assignedCache.filter {
                    filter.isEmpty() ||
                        it.key.lowercase().contains(filter) ||
                        it.summary.lowercase().contains(filter) ||
                        it.projectKey.lowercase().contains(filter) ||
                        it.statusName.lowercase().contains(filter)
                }
                if (list.isEmpty()) {
                    listContainer.addView(emptyHint("موردی یافت نشد"))
                    return
                }
                list.forEach { issue ->
                    listContainer.addView(issueCard(issue))
                }
            }
        }
    }

    private fun emptyHint(msg: String): TextView {
        return TextView(requireContext()).apply {
            text = msg
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(16, 32, 16, 32)
        }
    }

    private fun issueCard(issue: JiraService.IssueItem): View {
        val ctx = requireContext()
        val card = MaterialCardView(ctx).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(ThemeHelper.surfaceCard(dark()))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14)
        }

        // Header: KEY + status
        val header = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(ctx).apply {
            text = issue.key
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(primary())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(ctx).apply {
            text = issue.statusName
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(dark()))
        })
        box.addView(header)

        box.addView(TextView(ctx).apply {
            text = issue.summary
            textSize = 15f
            setTextColor(ThemeHelper.textPrimary(dark()))
            setPadding(0, 4, 0, 2)
        })

        val meta = buildString {
            if (issue.projectKey.isNotBlank()) append(issue.projectKey)
            if (issue.priorityName.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(issue.priorityName)
            }
            if (issue.timeSpentMinutes > 0) {
                if (isNotEmpty()) append(" · ")
                append("ثبت‌شده: ${formatMin(issue.timeSpentMinutes)}")
            }
        }
        if (meta.isNotBlank()) {
            box.addView(TextView(ctx).apply {
                text = meta
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }

        // Actions
        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 0)
        }
        actions.addView(smallBtn("⏱ لاگ") { showAddWorklogDialog(issue.key, issue.summary) })
        actions.addView(smallBtn("📋 جزئیات") { openIssueDetail(issue.key) })
        actions.addView(smallBtn("★") {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.addJiraFavorite(
                    issue.key, issue.summary, issue.projectKey, issue.projectName
                )
                Toast.makeText(ctx, "به علاقه‌مندی‌ها اضافه شد", Toast.LENGTH_SHORT).show()
            }
        })
        box.addView(actions)

        card.addView(box)
        return card
    }

    private fun favoriteCard(fav: JiraFavoriteEntity): View {
        val ctx = requireContext()
        val card = MaterialCardView(ctx).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(ThemeHelper.surfaceCard(dark()))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14)
        }
        box.addView(TextView(ctx).apply {
            text = fav.issueKey
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(primary())
            textSize = 14f
        })
        box.addView(TextView(ctx).apply {
            text = fav.summary.ifBlank { "—" }
            textSize = 15f
            setTextColor(ThemeHelper.textPrimary(dark()))
        })
        if (!fav.note.isNullOrBlank()) {
            box.addView(TextView(ctx).apply {
                text = fav.note
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
            })
        }
        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 0)
        }
        actions.addView(smallBtn("⏱ لاگ") { showAddWorklogDialog(fav.issueKey, fav.summary) })
        actions.addView(smallBtn("📋 جزئیات") { openIssueDetail(fav.issueKey) })
        actions.addView(smallBtn("حذف ★") {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.removeJiraFavorite(fav.issueKey)
                Toast.makeText(ctx, "از علاقه‌مندی‌ها حذف شد", Toast.LENGTH_SHORT).show()
            }
        })
        box.addView(actions)
        card.addView(box)
        return card
    }

    private fun smallBtn(label: String, onClick: () -> Unit): MaterialButton {
        val ctx = requireContext()
        return MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            textSize = 12f
            minimumHeight = 0
            minHeight = 0
            setPadding(DialogHelper.dp(ctx, 10), DialogHelper.dp(ctx, 6), DialogHelper.dp(ctx, 10), DialogHelper.dp(ctx, 6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = DialogHelper.dp(ctx, 6) }
            setOnClickListener { onClick() }
        }
    }

    // ---------- Dialogs ----------

    private fun showAddFavoriteDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(DialogHelper.sectionLabel(ctx, "کلید Issue (مثلاً PROJ-123)", dark()))
        val (keyL, keyE) = DialogHelper.inputField(ctx, "کلید", "", primary())
        layout.addView(keyL)
        layout.addView(DialogHelper.sectionLabel(ctx, "یادداشت شخصی (اختیاری)", dark()))
        val (noteL, noteE) = DialogHelper.inputField(ctx, "یادداشت", "", primary())
        layout.addView(noteL)

        DialogHelper.show(
            ctx = ctx, icon = "★", title = "افزودن علاقه‌مندی",
            subtitle = "Issue لازم نیست به شما assign باشد",
            primary = primary(), dark = dark(), body = layout, positiveText = "افزودن",
            onPositive = {
                val key = keyE.text?.toString()?.trim().orEmpty()
                if (key.isBlank()) {
                    Toast.makeText(ctx, "کلید را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    statusTv.text = "در حال دریافت $key ..."
                    val service = repo.jiraServiceOrNull()
                    if (service != null) {
                        service.getIssue(key).fold(
                            onSuccess = { issue ->
                                repo.addJiraFavorite(
                                    issue.key, issue.summary, issue.projectKey, issue.projectName,
                                    noteE.text?.toString()
                                )
                                Toast.makeText(ctx, "اضافه شد: ${issue.key}", Toast.LENGTH_SHORT).show()
                                mode = "favorites"
                                reload()
                            },
                            onFailure = { e ->
                                // حتی اگر دریافت نشد، با کلید خالی ذخیره کن
                                repo.addJiraFavorite(key, note = noteE.text?.toString())
                                Toast.makeText(ctx, "ذخیره شد (جزئیات دریافت نشد: ${e.message})", Toast.LENGTH_LONG).show()
                                mode = "favorites"
                                reload()
                            }
                        )
                    } else {
                        repo.addJiraFavorite(key, note = noteE.text?.toString())
                        Toast.makeText(ctx, "ذخیره شد (بدون اتصال جیرا)", Toast.LENGTH_SHORT).show()
                        mode = "favorites"
                        reload()
                    }
                }
                true
            }
        )
    }

    private fun showLogOnAnyIssueDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(DialogHelper.sectionLabel(ctx, "کلید Issue", dark()))
        val (keyL, keyE) = DialogHelper.inputField(ctx, "مثلاً DAILY-1", "", primary())
        layout.addView(keyL)
        addWorklogFields(layout) { dateTag, hours, mins, note ->
            Triple(dateTag, hours to mins, note)
        }
        // We'll rebuild more carefully:
        layout.removeAllViews()
        layout.addView(DialogHelper.sectionLabel(ctx, "کلید Issue", dark()))
        layout.addView(keyL)

        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark()))
        val todayIso = TimeUtils.today()
        val (dateL, dateField) = DialogHelper.inputField(
            ctx, "تاریخ", TimeUtils.toJalaliShort(TimeUtils.parseDate(todayIso)), primary()
        )
        dateField.tag = todayIso
        dateField.isFocusable = false
        dateField.setOnClickListener {
            JalaliDatePickerDialog.show(
                ctx = ctx, primary = primary(), dark = dark(),
                initialGregorianDate = dateField.tag as? String
            ) { gregStr, jalDisplay ->
                dateField.tag = gregStr
                dateField.setText(jalDisplay)
            }
        }
        layout.addView(dateL)

        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark()))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hoursL, hours) = DialogHelper.inputField(ctx, "ساعت", "1", primary(), number = true)
        val (minsL, mins) = DialogHelper.inputField(ctx, "دقیقه", "0", primary(), number = true)
        hoursL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        minsL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hoursL)
        row.addView(minsL)
        layout.addView(row)

        layout.addView(DialogHelper.sectionLabel(ctx, "توضیح (اختیاری)", dark()))
        val (noteL, note) = DialogHelper.inputField(ctx, "توضیح", "", primary(), multiline = true)
        layout.addView(noteL)

        DialogHelper.show(
            ctx = ctx, icon = "⏱", title = "ثبت لاگ روی Issue",
            subtitle = "حتی اگر به شما assign نباشد",
            primary = primary(), dark = dark(), body = layout, positiveText = "ارسال به جیرا",
            onPositive = {
                val key = keyE.text?.toString()?.trim().orEmpty()
                val date = (dateField.tag as? String)?.trim().orEmpty().ifEmpty { TimeUtils.today() }
                val dur = ((hours.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                    ((mins.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (key.isBlank() || dur <= 0) {
                    Toast.makeText(ctx, "کلید و مدت الزامی است", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = repo.addJiraWorklog(key, dur, date, note.text?.toString())
                    result.fold(
                        onSuccess = {
                            Toast.makeText(ctx, "لاگ روی $key ثبت شد", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Toast.makeText(ctx, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
                true
            }
        )
    }

    private fun showAddWorklogDialog(issueKey: String, summary: String) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        layout.addView(DialogHelper.sectionLabel(ctx, "تاریخ", dark()))
        val todayIso = TimeUtils.today()
        val (dateL, dateField) = DialogHelper.inputField(
            ctx, "تاریخ", TimeUtils.toJalaliShort(TimeUtils.parseDate(todayIso)), primary()
        )
        dateField.tag = todayIso
        dateField.isFocusable = false
        dateField.setOnClickListener {
            JalaliDatePickerDialog.show(
                ctx = ctx, primary = primary(), dark = dark(),
                initialGregorianDate = dateField.tag as? String
            ) { gregStr, jalDisplay ->
                dateField.tag = gregStr
                dateField.setText(jalDisplay)
            }
        }
        layout.addView(dateL)

        layout.addView(DialogHelper.sectionLabel(ctx, "مدت", dark()))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (hoursL, hours) = DialogHelper.inputField(ctx, "ساعت", "1", primary(), number = true)
        val (minsL, mins) = DialogHelper.inputField(ctx, "دقیقه", "0", primary(), number = true)
        hoursL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10)
        }
        minsL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(hoursL)
        row.addView(minsL)
        layout.addView(row)

        layout.addView(DialogHelper.sectionLabel(ctx, "توضیح (اختیاری)", dark()))
        val (noteL, note) = DialogHelper.inputField(ctx, "توضیح", "", primary(), multiline = true)
        layout.addView(noteL)

        DialogHelper.show(
            ctx = ctx, icon = "⏱", title = "ثبت Worklog",
            subtitle = "$issueKey — $summary",
            primary = primary(), dark = dark(), body = layout, positiveText = "ارسال به جیرا",
            onPositive = {
                val date = (dateField.tag as? String)?.trim().orEmpty().ifEmpty { TimeUtils.today() }
                val dur = ((hours.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                    ((mins.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = repo.addJiraWorklog(issueKey, dur, date, note.text?.toString())
                    result.fold(
                        onSuccess = {
                            Toast.makeText(ctx, "Worklog ثبت شد", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Toast.makeText(ctx, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
                true
            }
        )
    }

    private fun openIssueDetail(issueKey: String) {
        val ctx = requireContext()
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4)
        }
        val infoTv = TextView(ctx).apply {
            text = "در حال دریافت..."
            setTextColor(ThemeHelper.textPrimary(dark()))
            textSize = 13.5f
        }
        body.addView(infoTv)

        val worklogsBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 0)
        }
        body.addView(DialogHelper.sectionLabel(ctx, "Worklogها از سرور", dark()))
        body.addView(worklogsBox)

        val commentsBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 0)
        }
        body.addView(DialogHelper.sectionLabel(ctx, "کامنت‌ها", dark()))
        body.addView(commentsBox)

        DialogHelper.show(
            ctx = ctx, icon = "📋", title = issueKey,
            subtitle = "جزئیات از سرور شرکت",
            primary = primary(), dark = dark(), body = body,
            positiveText = "ثبت لاگ", negativeText = "بستن",
            onPositive = {
                showAddWorklogDialog(issueKey, "")
                true
            }
        )

        // Extra buttons via a second row inside body
        val extra = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }
        extra.addView(smallBtn("＋ کامنت") { showAddCommentDialog(issueKey) })
        extra.addView(smallBtn("★ علاقه‌مندی") {
            viewLifecycleOwner.lifecycleScope.launch {
                repo.addJiraFavorite(issueKey)
                Toast.makeText(ctx, "اضافه شد", Toast.LENGTH_SHORT).show()
            }
        })
        body.addView(extra)

        viewLifecycleOwner.lifecycleScope.launch {
            val service = repo.jiraServiceOrNull()
            if (service == null) {
                infoTv.text = "جیرا پیکربندی نشده"
                return@launch
            }
            // Issue info
            service.getIssue(issueKey).fold(
                onSuccess = { issue ->
                    infoTv.text = buildString {
                        append(issue.summary)
                        append("\n")
                        append("وضعیت: ${issue.statusName}")
                        if (issue.assigneeName != null) append(" · مسئول: ${issue.assigneeName}")
                        append("\n")
                        append("پروژه: ${issue.projectKey}")
                        if (issue.timeSpentMinutes > 0) append(" · صرف‌شده: ${formatMin(issue.timeSpentMinutes)}")
                        if (issue.remainingMinutes > 0) append(" · باقی: ${formatMin(issue.remainingMinutes)}")
                    }
                },
                onFailure = { infoTv.text = "خطا در دریافت Issue: ${it.message}" }
            )
            // Worklogs
            service.getWorklogs(issueKey).fold(
                onSuccess = { logs ->
                    worklogsBox.removeAllViews()
                    if (logs.isEmpty()) {
                        worklogsBox.addView(TextView(ctx).apply {
                            text = "Worklogی ثبت نشده"
                            setTextColor(ThemeHelper.textSecondary(dark()))
                            textSize = 12.5f
                        })
                    } else {
                        logs.take(20).forEach { wl ->
                            worklogsBox.addView(worklogRow(wl))
                        }
                        if (logs.size > 20) {
                            worklogsBox.addView(TextView(ctx).apply {
                                text = "... و ${logs.size - 20} مورد دیگر"
                                textSize = 11.5f
                                setTextColor(ThemeHelper.textSecondary(dark()))
                            })
                        }
                    }
                },
                onFailure = {
                    worklogsBox.addView(TextView(ctx).apply {
                        text = "خطا: ${it.message}"
                        setTextColor(ThemeHelper.textSecondary(dark()))
                    })
                }
            )
            // Comments
            service.getComments(issueKey).fold(
                onSuccess = { comments ->
                    commentsBox.removeAllViews()
                    if (comments.isEmpty()) {
                        commentsBox.addView(TextView(ctx).apply {
                            text = "کامنتی نیست"
                            setTextColor(ThemeHelper.textSecondary(dark()))
                            textSize = 12.5f
                        })
                    } else {
                        comments.take(10).forEach { c ->
                            commentsBox.addView(TextView(ctx).apply {
                                text = "${c.author?.displayName ?: "?"}: ${c.body?.take(200) ?: ""}"
                                textSize = 12.5f
                                setTextColor(ThemeHelper.textPrimary(dark()))
                                setPadding(0, 4, 0, 4)
                            })
                        }
                    }
                },
                onFailure = {
                    commentsBox.addView(TextView(ctx).apply {
                        text = "خطا: ${it.message}"
                        setTextColor(ThemeHelper.textSecondary(dark()))
                    })
                }
            )
        }
    }

    private fun worklogRow(wl: JiraWorklog): View {
        val ctx = requireContext()
        return TextView(ctx).apply {
            val author = wl.author?.displayName ?: wl.author?.name ?: "?"
            val spent = wl.timeSpent ?: JiraService.formatSeconds(wl.timeSpentSeconds)
            val started = wl.started?.take(16)?.replace("T", " ") ?: ""
            text = "• $spent — $author — $started"
            if (!wl.comment.isNullOrBlank()) text = "$text\n  ${wl.comment.take(120)}"
            textSize = 12.5f
            setTextColor(ThemeHelper.textPrimary(dark()))
            setPadding(0, 4, 0, 4)
        }
    }

    private fun showAddCommentDialog(issueKey: String) {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val (bodyL, bodyE) = DialogHelper.inputField(ctx, "متن کامنت", "", primary(), multiline = true)
        layout.addView(bodyL)
        DialogHelper.show(
            ctx = ctx, icon = "💬", title = "کامنت جدید",
            subtitle = issueKey,
            primary = primary(), dark = dark(), body = layout, positiveText = "ارسال",
            onPositive = {
                val text = bodyE.text?.toString().orEmpty()
                if (text.isBlank()) {
                    Toast.makeText(ctx, "متن خالی است", Toast.LENGTH_SHORT).show()
                    return@show false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val service = repo.jiraServiceOrNull()
                    if (service == null) {
                        Toast.makeText(ctx, "جیرا پیکربندی نشده", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    service.addComment(issueKey, text).fold(
                        onSuccess = { Toast.makeText(ctx, "کامنت ثبت شد", Toast.LENGTH_SHORT).show() },
                        onFailure = { e -> Toast.makeText(ctx, "خطا: ${e.message}", Toast.LENGTH_LONG).show() }
                    )
                }
                true
            }
        )
    }

    // helper unused - kept for structure
    private fun addWorklogFields(
        layout: LinearLayout,
        block: (Any?, TextInputEditText, TextInputEditText, TextInputEditText) -> Any
    ) { /* no-op placeholder */ }

    private fun formatMin(m: Int): String {
        val h = m / 60
        val rm = m % 60
        return when {
            h > 0 && rm > 0 -> "${h}س ${rm}د"
            h > 0 -> "${h}س"
            else -> "${rm}د"
        }
    }
}
