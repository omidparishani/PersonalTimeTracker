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

                renderAttendance(list)

            }

        }

    }

    private fun renderAttendance(
        list: List<AttendanceEntity>
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

        list.forEach { item ->

            val card = MaterialCardView(ctx).apply {
                radius = 18f
                cardElevation = 4f
                setContentPadding(20, 16, 20, 16)
            }

            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
            }

            val tv = TextView(ctx).apply {

                text = buildString {

                    if (item.exitTime != null) {

                        append(item.entryTime)
                        append(" → ")
                        append(item.exitTime)

                    } else {

                        append(item.entryTime)
                        append(" (فعال)")

                    }

                    append("\n")

                    append("مدت: ")
                    append(TimeUtils.formatDuration(item.duration))

                    if (item.leaveDuration > 0) {

                        append(" | مرخصی: ")
                        append(TimeUtils.formatDuration(item.leaveDuration))

                    }

                }

            }

            box.addView(tv)

            val actions = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val primary =
                (activity as? MainActivity)?.primaryColor
                    ?: 0xFF1565C0.toInt()

            val edit =
                MaterialButton(
                    ctx,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
                ).apply {

                    text = "ویرایش"

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            marginEnd = 8
                        }

                    ThemeHelper.applyButton(
                        this,
                        primary,
                        false
                    )

                    setOnClickListener {

                        showForm(item)

                    }

                }

            val delete =
                MaterialButton(
                    ctx,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
                ).apply {

                    text = "حذف"

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )

                    ThemeHelper.applyButton(
                        this,
                        primary,
                        false
                    )

                    setOnClickListener {

                        AlertDialog.Builder(ctx)
                            .setTitle("حذف تردد")
                            .setMessage("این تردد حذف شود؟")
                            .setPositiveButton("حذف") { _, _ ->

                                lifecycleScope.launch {

                                    (requireActivity().application as App)
                                        .repository
                                        .deleteAttendance(item)

                                }

                            }
                            .setNegativeButton("انصراف", null)
                            .show()

                    }

                }

            actions.addView(edit)
            actions.addView(delete)

            box.addView(actions)

            card.addView(box)

            listContainer.addView(card)

        }

    }

    private fun showForm(existing: AttendanceEntity?) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val dateField = EditText(ctx).apply {
            hint = "تاریخ (yyyy-MM-dd)"
            setText(existing?.date ?: TimeUtils.today())
            isFocusable = false
            setOnClickListener {
                val c = Calendar.getInstance()
                DatePickerDialog(
                    ctx,
                    { _, y, m, d -> setText("%04d-%02d-%02d".format(y, m + 1, d)) },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        val entryField = EditText(ctx).apply {
            hint = "ساعت ورود HH:mm"
            setText(existing?.entryTime ?: TimeUtils.nowTime())
            isFocusable = false
            setOnClickListener {
                val parts = text.toString().split(":")
                TimePickerDialog(
                    ctx,
                    { _, h, m -> setText("%02d:%02d".format(h, m)) },
                    parts.getOrNull(0)?.toIntOrNull() ?: 9,
                    parts.getOrNull(1)?.toIntOrNull() ?: 0,
                    true
                ).show()
            }
        }
        val exitField = EditText(ctx).apply {
            hint = "ساعت خروج (خالی = بدون خروج)"
            setText(existing?.exitTime ?: "")
            isFocusable = false
            setOnClickListener {
                val raw = text.ifEmpty { "17:00" }.toString()
                val parts = raw.split(":")
                TimePickerDialog(
                    ctx,
                    { _, h, m -> setText("%02d:%02d".format(h, m)) },
                    parts.getOrNull(0)?.toIntOrNull() ?: 17,
                    parts.getOrNull(1)?.toIntOrNull() ?: 0,
                    true
                ).show()
            }
        }
        layout.addView(dateField)
        layout.addView(entryField)
        layout.addView(exitField)

        AlertDialog.Builder(ctx)
            .setTitle(if (existing == null) "ثبت تردد" else "ویرایش تردد")
            .setView(layout)
            .setPositiveButton("ذخیره") { _, _ ->
                val date = dateField.text.toString()
                val entry = entryField.text.toString()
                val exit = exitField.text.toString().ifBlank { null }
                lifecycleScope.launch {
                    if (existing == null) {
                        repo.addAttendance(date, entry, exit)
                    } else {
                        repo.updateAttendance(
                            existing.copy(date = date, entryTime = entry, exitTime = exit)
                        )
                    }
                    Toast.makeText(ctx, "ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }
}
