package com.personal.timetracker.ui.attendance

import android.app.DatePickerDialog
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
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import com.personal.timetracker.ui.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

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

                val cal = Calendar.getInstance()

                DatePickerDialog(
                    ctx,
                    { _, y, m, d ->

                        selectedDate =
                            "%04d-%02d-%02d".format(
                                y,
                                m + 1,
                                d
                            )

                        updateTitle()

                        loadAttendance()

                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()

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

            val tv = TextView(ctx).apply {
                text = buildString {
                    append("مدت: ")
                    append(TimeUtils.formatDuration(item.duration))
                    if (item.leaveDuration > 0) {
                        append("   ·   مرخصی: ")
                        append(TimeUtils.formatDuration(item.leaveDuration))
                    }
                }
                textSize = 12.5f
                setTextColor(ThemeHelper.textSecondary(dark))
                setPadding(0, 6, 0, 0)
            }
            box.addView(tv)

            // Progress relative to the minimum work hours set in Settings, shown as the
            // card's own background fill.
            val effectiveMinutes = if (item.exitTime != null) item.duration
            else TimeUtils.minutesBetween(item.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
            val fraction = (effectiveMinutes.toFloat() / minWorkMinutes).coerceIn(0f, 1f)

            val wrapper = android.widget.FrameLayout(ctx)
            wrapper.addView(
                ThemeHelper.progressBackdrop(ctx, primary, dark, fraction),
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            wrapper.addView(
                box,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            card.addView(wrapper)

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
