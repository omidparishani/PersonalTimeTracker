package com.personal.timetracker.ui.tasks

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class TasksFragment : Fragment() {
    private lateinit var listContainer: LinearLayout
    private var filterStatus: String = "active" // active = not done
    private var collectJob: Job? = null

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark == true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

            box.addView(TextView(ctx).apply {
                text = task.taskTitle
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ThemeHelper.textPrimary(dark()))
            })
            box.addView(TextView(ctx).apply {
                text = buildString {
                    append(task.projectName)
                    if (!task.jiraNumber.isNullOrBlank()) {
                        append("  ·  ")
                        append(task.jiraNumber)
                    }
                    append("  ·  ")
                    append(statusLabel(task.status))
                    if (task.isRunning) append("  ▶")
                }
                textSize = 12f
                setTextColor(ThemeHelper.textSecondary(dark()))
                setPadding(0, 4, 0, 10)
            })

            // Progress: done portion of required
            val req = task.requiredMinutes.coerceAtLeast(1)
            val done = (req - task.remainingMinutes).coerceIn(0, req)
            val pct = (done * 100 / req)
            box.addView(TextView(ctx).apply {
                text = "پیشرفت $pct٪  |  باقیمانده: ${TimeUtils.formatDuration(task.remainingMinutes)} از ${TimeUtils.formatDuration(task.requiredMinutes)}"
                textSize = 11f
                setTextColor(primary)
                setPadding(0, 0, 0, 6)
            })
            box.addView(ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = pct
                progressTintList = android.content.res.ColorStateList.valueOf(primary)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
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
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 6 }
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
            actions.addView(smallBtn("لاگ", false) { showLogForm(task) })
            actions.addView(smallBtn("ویرایش", false) { showTaskForm(task) })
            actions.addView(smallBtn("حذف", false) {
                AlertDialog.Builder(ctx).setTitle("حذف تسک").setMessage(task.taskTitle)
                    .setPositiveButton("حذف") { _, _ -> lifecycleScope.launch { repo.deleteTask(task) } }
                    .setNegativeButton("انصراف", null).show()
            })
            box.addView(actions)
            card.addView(box)
            listContainer.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 })
        }
    }

    private fun showTaskForm(existing: TaskEntity?) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 8)
        }
        fun field(hint: String, value: String, number: Boolean = false): Pair<TextInputLayout, TextInputEditText> {
            val e = TextInputEditText(ctx).apply {
                setText(value)
                if (number) inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            return TextInputLayout(ctx).apply { this.hint = hint; addView(e) } to e
        }
        val (jiraL, jira) = field("شماره Jira", existing?.jiraNumber ?: "")
        val (titleL, title) = field("عنوان *", existing?.taskTitle ?: "")
        val (descL, desc) = field("توضیحات", existing?.description ?: "")
        val (reqHL, reqH) = field("ساعت مورد نیاز", ((existing?.requiredMinutes ?: 0) / 60).toString(), true)
        val (reqML, reqM) = field("دقیقه مورد نیاز", ((existing?.requiredMinutes ?: 0) % 60).toString(), true)

        val statusSpinner = Spinner(ctx)
        val statuses = listOf("new" to "جدید", "in_progress" to "در حال انجام", "done" to "انجام‌شده")
        statusSpinner.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, statuses.map { it.second })
        statusSpinner.setSelection(statuses.indexOfFirst { it.first == (existing?.status ?: "new") }.coerceAtLeast(0))

        val projectSpinner = Spinner(ctx)
        val (projL, projE) = field("نام پروژه *", existing?.projectName ?: "")

        lifecycleScope.launch {
            val settings = repo.getSettings()
            val projects = settings.projects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (projects.isNotEmpty()) {
                projectSpinner.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, projects + "سایر...")
                val idx = projects.indexOf(existing?.projectName)
                if (idx >= 0) projectSpinner.setSelection(idx)
                layout.addView(TextView(ctx).apply { text = "پروژه"; setPadding(0, 8, 0, 4) })
                layout.addView(projectSpinner)
            }
            layout.addView(projL)
            layout.addView(jiraL)
            layout.addView(titleL)
            layout.addView(descL)
            layout.addView(reqHL)
            layout.addView(reqML)
            layout.addView(TextView(ctx).apply { text = "وضعیت"; setPadding(0, 8, 0, 4) })
            layout.addView(statusSpinner)

            AlertDialog.Builder(ctx)
                .setTitle(if (existing == null) "تسک جدید" else "ویرایش تسک")
                .setView(layout)
                .setPositiveButton("ذخیره") { _, _ ->
                    val proj = when {
                        projects.isNotEmpty() && projectSpinner.selectedItem?.toString() != "سایر..." ->
                            projectSpinner.selectedItem.toString()
                        else -> (projE.text?.toString() ?: "").trim()
                    }
                    val titleStr = (title.text?.toString() ?: "").trim()
                    if (proj.isEmpty() || titleStr.isEmpty()) {
                        Toast.makeText(ctx, "پروژه و عنوان الزامی است", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val req = ((reqH.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                        ((reqM.text?.toString() ?: "0").toIntOrNull() ?: 0)
                    val st = statuses.getOrNull(statusSpinner.selectedItemPosition)?.first ?: "new"
                    val remaining = if (existing == null) req else {
                        // keep remaining unless required changed upward
                        val logged = (existing.requiredMinutes - existing.remainingMinutes).coerceAtLeast(0)
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
                    lifecycleScope.launch { repo.saveTask(task) }
                }
                .setNegativeButton("انصراف", null)
                .show()
        }
    }

    private fun showLogForm(task: TaskEntity) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 8)
        }
        val jalaliHint = TextView(ctx).apply {
            text = TimeUtils.toJalaliDisplay(TimeUtils.today())
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(dark()))
        }
        val dateField = TextInputEditText(ctx).apply {
            setText(TimeUtils.today())
            isFocusable = false
            setOnClickListener {
                val c = Calendar.getInstance()
                DatePickerDialog(ctx, { _, y, m, d ->
                    val iso = "%04d-%02d-%02d".format(y, m + 1, d)
                    setText(iso)
                    jalaliHint.text = TimeUtils.toJalaliDisplay(iso)
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        val hours = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }
        val mins = TextInputEditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("0")
        }
        val note = TextInputEditText(ctx)
        layout.addView(TextInputLayout(ctx).apply { hint = "تاریخ لاگ (yyyy-MM-dd)"; addView(dateField) })
        layout.addView(jalaliHint)
        layout.addView(TextInputLayout(ctx).apply { hint = "ساعت کار"; addView(hours) })
        layout.addView(TextInputLayout(ctx).apply { hint = "دقیقه"; addView(mins) })
        layout.addView(TextInputLayout(ctx).apply { hint = "یادداشت"; addView(note) })

        AlertDialog.Builder(ctx)
            .setTitle("ثبت لاگ — ${task.taskTitle}")
            .setView(layout)
            .setPositiveButton("ثبت") { _, _ ->
                val date = dateField.text?.toString() ?: TimeUtils.today()
                val dur = ((hours.text?.toString() ?: "0").toIntOrNull() ?: 0) * 60 +
                    ((mins.text?.toString() ?: "0").toIntOrNull() ?: 0)
                if (dur <= 0) {
                    Toast.makeText(ctx, "مدت باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    repo.addLog(task, date, dur, note = note.text?.toString())
                    Toast.makeText(ctx, "لاگ ثبت شد", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }
}
