package com.personal.timetracker.ui.attendance

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.util.AttendanceEditor
import com.personal.timetracker.util.DialogHelper
import com.personal.timetracker.util.JalaliDatePickerDialog
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import com.personal.timetracker.ui.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AttendanceFragment : Fragment() {
    private lateinit var listContainer: LinearLayout
    private lateinit var titleText: TextView
    private var selectedDate = TimeUtils.today()

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)
        }

        root.addView(TextView(ctx).apply {
            text = "حضور و غیاب"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        })

        val repo = (requireActivity().application as App).repository
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val btnIn = MaterialButton(ctx).apply {
            text = "ورود الان"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { lifecycleScope.launch { repo.checkIn() } }
        }
        val btnOut = MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "خروج الان"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { lifecycleScope.launch { repo.checkOut() } }
        }
        btnRow.addView(btnIn)
        btnRow.addView(btnOut)
        root.addView(btnRow)

        val primary = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()

        val btnDate = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {

            text = "انتخاب تاریخ"

            ThemeHelper.applyButton(
                this,
                primary,
                false
            )

            setOnClickListener {
                JalaliDatePickerDialog.show(
                    ctx = ctx,
                    primary = primary,
                    dark = (activity as? MainActivity)?.isDark == true,
                    initialGregorianDate = selectedDate
                ) { gregStr, _ ->
                    selectedDate = gregStr
                    updateTitle()
                    loadAttendance()
                }
            }

        }

        root.addView(btnDate)

        ThemeHelper.applyButton(btnIn, primary, true)
        ThemeHelper.applyButton(btnOut, primary, false)
        root.setBackgroundColor(ThemeHelper.surface((activity as? MainActivity)?.isDark == true))

        titleText = TextView(ctx).apply {
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }

        root.addView(titleText)

        listContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(listContainer)
        }
        root.addView(scroll)

        val addBtn = MaterialButton(ctx).apply { text = "ثبت تردد (تاریخ دلخواه)" }
        ThemeHelper.applyButton(addBtn, primary(), true)
        addBtn.setOnClickListener { showForm(null) }
        root.addView(addBtn)

        updateTitle()

        loadAttendance()

        return root
    }

    private fun updateTitle() {

        titleText.text =
            "ترددهای ${TimeUtils.toJalaliDisplay(selectedDate)}"

    }

    private fun loadAttendance() {

        val repo =
            (requireActivity().application as App).repository

        viewLifecycleOwner.lifecycleScope.launch {

            repo.observeAttendance(selectedDate).collectLatest { list ->

                val minWork = try { repo.getSettings().minimumWorkMinutes.coerceAtLeast(1) } catch (_: Exception) { 480 }
                renderAttendance(list, minWork)

            }

        }

    }

    private fun renderAttendance(
        list: List<AttendanceEntity>,
        minWorkMinutes: Int
    ) {

        val ctx = requireContext()

        listContainer.removeAllViews()

        if (list.isEmpty()) {

            listContainer.addView(
                TextView(ctx).apply {
                    text = "ترددی ثبت نشده"
                }
            )

            return
        }

        val dark = (activity as? MainActivity)?.isDark == true
        list.forEach { item ->

            val primary =
                (activity as? MainActivity)?.primaryColor
                    ?: 0xFF1565C0.toInt()

            val card = MaterialCardView(ctx)
            ThemeHelper.applyCard(card, dark)

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 18, 24, 18)
            }

            val headRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            headRow.addView(TextView(ctx).apply {
                text = if (item.exitTime != null) "${item.entryTime}  →  ${item.exitTime}" else item.entryTime
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ThemeHelper.textPrimary(dark))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (item.exitTime == null) {
                headRow.addView(ThemeHelper.pill(ctx, "فعال", primary, ThemeHelper.onColor(primary)))
            }
            headRow.addView(ThemeHelper.iconButton(ctx, "✎", primary, dark, "ویرایش تردد") {
                showForm(item)
            })
            headRow.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark, "حذف تردد") {
                AttendanceEditor.confirmDelete(
                    ctx, item,
                    (requireActivity().application as App).repository,
                    lifecycleScope, primary, dark
                )
            })
            box.addView(headRow)

            val isActive = item.exitTime == null
            val displayDur = if (isActive)
                TimeUtils.minutesBetween(item.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
            else item.duration

            val tv = TextView(ctx).apply {
                text = buildString {
                    append("مدت: ")
                    append(TimeUtils.formatDuration(displayDur))
                    if (isActive) append("  (در حال کار)")
                    if (item.leaveDuration > 0) {
                        append("   ·   مرخصی: ")
                        append(TimeUtils.formatDuration(item.leaveDuration))
                    }
                    if (item.overtimeDuration > 0) {
                        append("   ·   اضافه‌کار: ")
                        append(TimeUtils.formatDuration(item.overtimeDuration))
                    }
                }
                textSize = 12.5f
                setTextColor(ThemeHelper.textSecondary(dark))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val infoRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 0)
            }
            infoRow.addView(tv)
            // نمودار دایره‌ای همیشه نشان داده می‌شود (حتی بدون خروج)
            infoRow.addView(com.personal.timetracker.util.ChartHelper.attendanceDonut(
                ctx, displayDur, item.overtimeDuration, item.leaveDuration,
                dark, primary, sizeDp = 48, isActive = isActive
            ))
            box.addView(infoRow)

            card.addView(box)

            listContainer.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 })

        }

    }

    private fun showForm(existing: AttendanceEntity?) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        AttendanceEditor.open(
            ctx, existing, repo, lifecycleScope,
            primary(), (activity as? MainActivity)?.isDark == true
        )
    }
}
