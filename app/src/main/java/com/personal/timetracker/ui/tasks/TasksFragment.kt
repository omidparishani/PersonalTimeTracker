package com.personal.timetracker.ui.tasks

import android.graphics.Typeface
import android.os.Bundle
import com.personal.timetracker.data.entity.TaskLogEntity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
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
import com.personal.timetracker.data.entity.TaskEntity
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.TaskLogEditor
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {
    private lateinit var listContainer: LinearLayout
    private var filterStatus: String = "active" // active = not done
    private var collectJob: Job? = null
    private val expandedTaskIds = mutableSetOf<Long>()
    private var currentList: List<TaskEntity> = emptyList()
    private var lastQuery: String = ""
    /** لاگ‌های همه‌ی تسک‌ها، گروه‌بندی‌شده با taskId — هرگز از روی ایندکس لیست خوانده نمی‌شود */
    private var logsByTaskId: Map<Long, List<TaskLogEntity>> = emptyMap()

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val rootBg = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.surface(dark()))
            setPadding(16)
        }

        rootBg.addView(ThemeHelper.pageTitle(ctx, "تسک‌ها", dark()))

        val search = TextInputEditText(ctx).apply { hint = "جستجو..." }
        rootBg.addView(TextInputLayout(ctx).apply {
            this.hint = "جستجو"
            addView(search)
        })

        // Status filter chips
        val filters = ChipGroup(ctx).apply { isSingleSelection = true }
        fun addFilter(label: String, value: String, checked: Boolean = false) {
            filters.addView(Chip(ctx).apply {
                text = label
                isCheckable = true
                isChecked = checked
                setOnClickListener {
                    filterStatus = value
                    reload(repo, search.text?.toString().orEmpty())
                }
            })
        }
        addFilter("فعال", "active", true)
        addFilter("جدید", "new")
        addFilter("در حال انجام", "in_progress")
        addFilter("انجام‌شده", "done")
        addFilter("همه", "all")
        rootBg.addView(filters)

        val addBtn = MaterialButton(ctx).apply { text = "＋ تسک جدید" }
        ThemeHelper.applyButton(addBtn, primary(), true)
        addBtn.setOnClickListener { showTaskForm(null) }
        rootBg.addView(addBtn)

        listContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        rootBg.addView(android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(listContainer)
        })

        reload(repo, "")
        search.addTextChangedListener {
            reload(repo, it?.toString().orEmpty())
        }
        return rootBg
    }

    private fun reload(repo: com.personal.timetracker.data.repository.AppRepository, q: String) {
        lastQuery = q
        collectJob?.cancel()
        collectJob = viewLifecycleOwner.lifecycleScope.launch {
            val flow = when {
                q.isNotBlank() -> repo.searchTasks(q.trim())
                filterStatus == "all" -> repo.observeTasks()
                filterStatus == "active" -> repo.observeActiveTasks()
                else -> repo.observeTasksByStatus(filterStatus)
            }
            flow.collectLatest { list ->
                val filtered = if (q.isNotBlank() && filterStatus == "active") {
                    list.filter { it.status != "done" }
                } else if (q.isNotBlank() && filterStatus != "all") {
                    list.filter { it.status == filterStatus }
                } else list
                currentList = filtered
                // یک کوئری برای همه لاگ‌ها، بعد گروه‌بندی با taskId — بدون coroutine جدا برای هر کارت
                logsByTaskId = repo.getAllLogsOnce().groupBy { it.taskId }
                bind(filtered)
            }
        }
    }

    private fun statusLabel(s: String) = when (s) {
        "new" -> "جدید"
        "in_progress" -> "در حال انجام"
        "done" -> "انجام‌شده"
        else -> s
    }

    private fun bind(list: List<TaskEntity>) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val primary = primary()
        listContainer.removeAllViews()
        if (list.isEmpty()) {
            listContainer.addView(TextView(ctx).apply {
                text = "تسکی نیست"
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(16, 24, 16, 16)
            })
            return
        }
        list.forEach { task ->
            val card = MaterialCardView(ctx)
            ThemeHelper.applyCard(card, dark())

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(22, 18, 22, 18)
            }

            // Progress: done portion of required — shown as a small circular chart next
            // to the title instead of a background fill.
            val req = task.requiredMinutes.coerceAtLeast(1)
            val done = (req - task.remainingMinutes).coerceIn(0, req)
            val remaining = (req - done).coerceAtLeast(0)
            val pct = (done * 100 / req)

            val headerRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(ctx).apply {
                text = task.taskTitle
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ThemeHelper.textPrimary(dark()))
            })
            textCol.addView(TextView(ctx).apply {
                text = buildString {
                    append(task.projectName)
                    if (!task.jiraNumber.isNullOrBlank()) {
                        append("  ·  ")
                        append(task.jiraNumber)
                        // نمایش عنوان جیرا (همان عنوان تسک) در کنار شماره
                        if (!task.taskTitle.isNullOrBlank()) {
                            append(" — ")
                            append(task.taskTitle)
                        }
                    }
                    append("  ·  ")
                    append(statusLabel(task.status))
                    if (task.isRunning) append("  ▶")
                }
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(0, 4, 0, 0)
            })
            headerRow.addView(textCol)
            headerRow.addView(com.personal.timetracker.util.DonutChartView(ctx).apply {
                items = listOf(
                    com.personal.timetracker.util.DonutItem("", done, primary),
                    com.personal.timetracker.util.DonutItem("", remaining, ThemeHelper.outline(dark()))
                )
                trackColor = ThemeHelper.outline(dark())
                centerTitle = "${TimeUtils.faNum(pct)}٪"
                titleColor = ThemeHelper.textPrimary(dark())
                layoutParams = LinearLayout.LayoutParams(DialogHelper.dp(ctx, 54), DialogHelper.dp(ctx, 54)).apply {
                    marginStart = DialogHelper.dp(ctx, 10)
                }
            })
            box.addView(headerRow)

            box.addView(TextView(ctx).apply {
                text = buildString {
                    append("باقیمانده: ")
                    append(TimeUtils.formatDuration(task.remainingMinutes))
                    append(" از ")
                    append(TimeUtils.formatDuration(task.requiredMinutes))
                }
                textSize = 11f
                setTextColor(primary)
                setPadding(0, 10, 0, 6)
            })

            val actions = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 14, 0, 0)
            }

            fun smallBtn(label: String, filled: Boolean, onClick: () -> Unit): MaterialButton {
                return MaterialButton(
                    ctx, null,
                    if (filled) com.google.android.material.R.attr.materialButtonStyle
                    else com.google.android.material.R.attr.materialButtonOutlinedStyle
                ).apply {
                    text = label
                    textSize = 11f
                    layoutParams =
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            .apply { marginEnd = 6 }
                    ThemeHelper.applyButton(this, primary, filled)
                    setOnClickListener { onClick() }
                }
            }
            if (task.isRunning) {
                actions.addView(smallBtn("توقف", true) {
                    lifecycleScope.launch { repo.stopTimer(task) }
                })
            } else if (task.status != "done") {
                actions.addView(smallBtn("شروع", true) {
                    lifecycleScope.launch { repo.startTimer(task) }
                })
            }
            actions.addView(smallBtn("ویرایش", false) { showTaskForm(task) })
            actions.addView(smallBtn("حذف", false) {
                DialogHelper.confirm(
                    ctx = ctx, title = "حذف تسک", message = task.taskTitle,
                    primary = primary, dark = dark()
                ) { lifecycleScope.launch { repo.deleteTask(task) } }
            })
            box.addView(actions)

            val taskId = task.id
            val taskLogs = logsByTaskId[taskId].orEmpty().filter { it.taskId == taskId }

            val drawerBody = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 0)
                tag = taskId
            }
            paintLogRows(drawerBody, task, taskLogs)

            val addLogBtn = smallBtn("＋ لاگ جدید", true) {
                TaskLogEditor.openNew(ctx, task, repo, viewLifecycleOwner.lifecycleScope, primary, dark()) {
                    refreshLogsFor(taskId, drawerBody)
                }
            }
            addLogBtn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = DialogHelper.dp(ctx, 10) }

            val drawerContainer = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 0)
                visibility = if (taskId in expandedTaskIds) View.VISIBLE else View.GONE
                tag = taskId
            }
            drawerContainer.addView(ThemeHelper.divider(ctx, dark()))
            drawerContainer.addView(drawerBody)
            drawerContainer.addView(addLogBtn)

            val arrowTv = TextView(ctx).apply {
                text = if (taskId in expandedTaskIds) "▴" else "▾"
                textSize = 12.5f
                setTextColor(primary)
            }
            val logsToggle = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(4, 12, 4, 4)
                setOnClickListener {
                    if (taskId in expandedTaskIds) {
                        expandedTaskIds.remove(taskId)
                        drawerContainer.visibility = View.GONE
                        arrowTv.text = "▾"
                    } else {
                        expandedTaskIds.add(taskId)
                        drawerContainer.visibility = View.VISIBLE
                        arrowTv.text = "▴"
                    }
                }
            }
            logsToggle.addView(TextView(ctx).apply {
                text = "لاگ‌های تسک"
                textSize = 12.5f
                setTextColor(primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            logsToggle.addView(arrowTv)
            box.addView(logsToggle)
            box.addView(drawerContainer)

            card.addView(box)
            listContainer.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 })
        }
    }

    /** فقط لاگ‌هایی که taskId دقیقاً برابر تسک همین کارت است رسم می‌شوند. */
    private fun paintLogRows(
        drawerBody: LinearLayout,
        task: TaskEntity,
        logs: List<TaskLogEntity>
    ) {
        val ctx = drawerBody.context
        val primary = primary()
        val dark = dark()
        val repo = (requireActivity().application as App).repository
        drawerBody.tag = task.id
        drawerBody.removeAllViews()
        val mine = logs.filter { it.taskId == task.id }.sortedByDescending { it.date }
        if (mine.isEmpty()) {
            drawerBody.addView(TextView(ctx).apply {
                text = "هنوز لاگی ثبت نشده"
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark))
            })
            return
        }
        mine.forEach { log ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val info = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val startStr = if (!log.startTime.isNullOrBlank()) "  ·  ${log.startTime}" else ""
            info.addView(TextView(ctx).apply {
                text = "${TimeUtils.toJalaliDisplay(log.date)}$startStr  ·  ${TimeUtils.formatDuration(log.duration)}"
                textSize = 12.5f
                setTextColor(ThemeHelper.textPrimary(dark))
            })
            if (!log.note.isNullOrBlank()) {
                info.addView(TextView(ctx).apply {
                    text = log.note
                    textSize = 11f
                    setTextColor(ThemeHelper.textSecondary(dark))
                })
            }
            row.addView(info)
            row.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش لاگ") {
                TaskLogEditor.openEdit(ctx, task, log, repo, viewLifecycleOwner.lifecycleScope, primary, dark) {
                    refreshLogsFor(task.id, drawerBody)
                }
            })
            row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف لاگ") {
                TaskLogEditor.confirmDelete(ctx, task, log, repo, viewLifecycleOwner.lifecycleScope, primary, dark) {
                    refreshLogsFor(task.id, drawerBody)
                }
            })
            drawerBody.addView(row)
        }
    }

    private fun refreshLogsFor(taskId: Long, drawerBody: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch {
            val repo = (requireActivity().application as App).repository
            val task = repo.getTask(taskId) ?: return@launch
            val logs = repo.getLogsByTask(taskId).filter { it.taskId == taskId }
            logsByTaskId = logsByTaskId + (taskId to logs)
            if (!isAdded) return@launch
            if ((drawerBody.tag as? Long) != taskId) return@launch
            paintLogRows(drawerBody, task, logs)
        }
    }

    private fun showTaskForm(existing: TaskEntity?) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val primary = primary()
        val dark = dark()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val (titleL, title) = DialogHelper.inputField(ctx, "عنوان *", existing?.taskTitle ?: "", primary)
        val (jiraL, jira) = DialogHelper.inputField(ctx, "شماره Jira", existing?.jiraNumber ?: "", primary)
        val (descL, desc) = DialogHelper.inputField(ctx, "توضیحات", existing?.description ?: "", primary, multiline = true)
        val (reqHL, reqH) = DialogHelper.inputField(ctx, "ساعت مورد نیاز", ((existing?.requiredMinutes ?: 0) / 60).toString(), primary, number = true)
        val (reqML, reqM) = DialogHelper.inputField(ctx, "دقیقه مورد نیاز", ((existing?.requiredMinutes ?: 0) % 60).toString(), primary, number = true)
        reqHL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = DialogHelper.dp(ctx, 10); topMargin = DialogHelper.dp(ctx, 14)
        }
        reqML.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            topMargin = DialogHelper.dp(ctx, 14)
        }
        val reqRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        reqRow.addView(reqHL)
        reqRow.addView(reqML)

        val statusSpinner = Spinner(ctx)
        val statuses =
            listOf("new" to "جدید", "in_progress" to "در حال انجام", "done" to "انجام‌شده")
        statusSpinner.adapter = ArrayAdapter(
            ctx,
            android.R.layout.simple_spinner_dropdown_item,
            statuses.map { it.second })
        statusSpinner.setSelection(statuses.indexOfFirst { it.first == (existing?.status ?: "new") }
            .coerceAtLeast(0))

        val projectSpinner = Spinner(ctx)
        val (projL, projE) = DialogHelper.inputField(ctx, "نام پروژه *", existing?.projectName ?: "", primary)
        var projects: List<String> = emptyList()

        layout.addView(titleL)
        val projectBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(projectBox)
        layout.addView(jiraL)
        layout.addView(descL)
        layout.addView(DialogHelper.sectionLabel(ctx, "زمان مورد نیاز", dark))
        layout.addView(reqRow)
        layout.addView(DialogHelper.sectionLabel(ctx, "وضعیت", dark))
        layout.addView(statusSpinner)

        lifecycleScope.launch {
            val settings = repo.getSettings()
            projects = settings.projects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (projects.isNotEmpty()) {
                projectSpinner.adapter = ArrayAdapter(
                    ctx,
                    android.R.layout.simple_spinner_dropdown_item,
                    projects + "سایر..."
                )
                val idx = projects.indexOf(existing?.projectName)
                if (idx >= 0) projectSpinner.setSelection(idx)
                projectBox.addView(DialogHelper.sectionLabel(ctx, "پروژه", dark))
                projectBox.addView(projectSpinner)
            } else {
                projectBox.addView(projL)
            }
        }

        DialogHelper.show(
            ctx = ctx,
            icon = if (existing == null) "＋" else "✎",
            title = if (existing == null) "تسک جدید" else "ویرایش تسک",
            subtitle = existing?.projectName,
            primary = primary, dark = dark, body = layout, positiveText = "ذخیره",
            onPositive = {
                val proj = when {
                    projects.isNotEmpty() && projectSpinner.selectedItem?.toString() != "سایر..." ->
                        projectSpinner.selectedItem.toString()
                    else -> (projE.text?.toString() ?: "").trim()
                }
                val titleStr = (title.text?.toString() ?: "").trim()
                if (proj.isEmpty() || titleStr.isEmpty()) {
                    Toast.makeText(ctx, "پروژه و عنوان الزامی است", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    val req = ((reqH.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                            ((reqM.text?.toString() ?: "0").toIntOrNull() ?: 0)
                    val st = statuses.getOrNull(statusSpinner.selectedItemPosition)?.first ?: "new"
                    val remaining = if (existing == null) req else {
                        // keep remaining unless required changed upward
                        val logged =
                            (existing.requiredMinutes - existing.remainingMinutes).coerceAtLeast(0)
                        (req - logged).coerceAtLeast(0)
                    }
                    val task = (existing ?: TaskEntity(
                        projectName = proj,
                        taskTitle = titleStr,
                        createdAt = TimeUtils.nowDateTime(),
                        requiredMinutes = req,
                        remainingMinutes = req
                    )).copy(
                        jiraNumber = (jira.text?.toString() ?: "").ifBlank { null },
                        projectName = proj,
                        taskTitle = titleStr,
                        description = (desc.text?.toString() ?: "").ifBlank { null },
                        requiredMinutes = req,
                        remainingMinutes = remaining,
                        status = st
                    )
                    lifecycleScope.launch {
                        repo.saveTask(task)
                        Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
        )
    }
}
