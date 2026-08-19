package com.personal.timetracker.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.personal.timetracker.App
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.AutoBackupWorker
import com.personal.timetracker.util.BackupHelper
import com.personal.timetracker.util.BiometricHelper
import com.personal.timetracker.util.GeoHelper
import com.personal.timetracker.util.NotifHelper
import com.personal.timetracker.util.ThemeHelper
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var settings = SettingsEntity()
    private lateinit var startBtn: MaterialButton
    private lateinit var endBtn: MaterialButton
    private lateinit var flexEdit: TextInputEditText
    private lateinit var minHoursEdit: TextInputEditText
    private lateinit var minMinsEdit: TextInputEditText
    private lateinit var weeklyHoursEdit: TextInputEditText
    private lateinit var weeklyMinsEdit: TextInputEditText
    private lateinit var thursdaySwitch: Switch
    private lateinit var thursdayHoursEdit: TextInputEditText
    private lateinit var thursdayMinsEdit: TextInputEditText
    private lateinit var holidayListBox: LinearLayout
    private lateinit var holidayDateEdit: TextInputEditText
    private lateinit var holidayTitleEdit: TextInputEditText
    private lateinit var projectEdit: TextInputEditText
    private lateinit var notifTitleEdit: TextInputEditText
    private lateinit var notifBodyEdit: TextInputEditText
    private lateinit var notifBeforeEdit: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var darkSwitch: Switch
    private lateinit var notifSwitch: Switch
    private lateinit var bioSwitch: Switch
    private lateinit var geoAutoSwitch: Switch
    private lateinit var geoAutoOutSwitch: Switch
    private lateinit var geoAlertSwitch: Switch
    private lateinit var radiusEdit: TextInputEditText
    private lateinit var locationInfo: TextView
    private lateinit var autoBackupSwitch: Switch
    private lateinit var autoBackupIntervalEdit: TextInputEditText
    private val projects = mutableListOf<String>()
    private var themeColor = -10983104

    private val colors = listOf(
        0xFF1565C0.toInt(), 0xFF2E7D32.toInt(), 0xFFC62828.toInt(),
        0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFFEF6C00.toInt(),
        0xFF4527A0.toInt(), 0xFF37474F.toInt(), 0xFFAD1457.toInt()
    )

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()
    private fun dark() = (activity as? MainActivity)?.isDark ?: false

    private val pickBackup =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                try {
                    val json =
                        requireContext().contentResolver.openInputStream(uri)?.bufferedReader()
                            ?.readText()
                            ?: throw Exception("خواندن ممکن نیست")
                    BackupHelper.restoreJson(requireContext(), json)
                    Toast.makeText(requireContext(), "بازیابی شد", Toast.LENGTH_LONG).show()
                    load()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    private val locPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { m ->
            if (m.values.any { it }) saveCurrentLocation()
            else Toast.makeText(requireContext(), "مجوز موقعیت لازم است", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val content =
            LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(20) }
        val scroll = android.widget.ScrollView(ctx).apply { addView(content) }

        fun title(t: String) = TextView(ctx).apply {
            text = t; textSize = 18f; setPadding(0, 28, 0, 10)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        fun til(hint: String, single: Boolean = true): Pair<TextInputLayout, TextInputEditText> {
            val edit = TextInputEditText(ctx)
            val layout = TextInputLayout(ctx).apply {
                this.hint = hint
                addView(edit)
                if (!single) {
                    edit.minLines = 2
                    edit.gravity = Gravity.TOP or Gravity.START
                }
            }
            return layout to edit
        }

        content.addView(TextView(ctx).apply { text = "تنظیمات"; textSize = 22f })

        // Dark
        darkSwitch = Switch(ctx).apply { text = "حالت تاریک" }
        content.addView(darkSwitch)

        // Theme colors
        content.addView(title("رنگ تم"))
        val colorRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        colors.forEach { c ->
            val v = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(72, 72).apply { marginEnd = 12 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                }
                setOnClickListener {
                    themeColor = c
                    (activity as? MainActivity)?.applyThemeColor(c)
                    Toast.makeText(ctx, "رنگ انتخاب شد — ذخیره را بزنید", Toast.LENGTH_SHORT).show()
                }
            }
            colorRow.addView(v)
        }
        content.addView(android.widget.HorizontalScrollView(ctx).apply { addView(colorRow) })

        // Work hours
        content.addView(title("ساعات کاری"))
        startBtn = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        endBtn = MaterialButton(
            ctx,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        )
        content.addView(startBtn); content.addView(endBtn)
        val (flexL, flexE) = til("شناوری (دقیقه)"); flexEdit = flexE; content.addView(flexL)
        content.addView(TextView(ctx).apply {
            text = "اگر ورود بین ساعت شروع کار و پایان بازه‌ی شناوری باشد، ساعت پایان کار به همان اندازه شیفت می‌کند و مرخصی ثبت نمی‌شود. اگر دیرتر باشد، فاصله تا پایان بازه‌ی شناوری مرخصی محسوب می‌شود."
            textSize = 11.5f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(4, 4, 4, 8)
        })
        val minRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (mhL, mhE) = til("حداقل کار — ساعت"); minHoursEdit = mhE
        val (mmL, mmE) = til("حداقل کار — دقیقه"); minMinsEdit = mmE
        mhL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = 8 }
        mmL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        minRow.addView(mhL); minRow.addView(mmL); content.addView(minRow)

        // Weekly shift schedule
        content.addView(title("شیفت کاری هفتگی"))
        val weeklyRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (whL, whE) = til("ساعت موظف هفته — ساعت"); weeklyHoursEdit = whE
        val (wmL, wmE) = til("ساعت موظف هفته — دقیقه"); weeklyMinsEdit = wmE
        whL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        wmL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        weeklyRow.addView(whL); weeklyRow.addView(wmL)
        content.addView(weeklyRow)

        thursdaySwitch = Switch(ctx).apply { text = "پنجشنبه روز کاری است" }
        content.addView(thursdaySwitch)

        val thuRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (thL, thE) = til("ساعت موظف پنجشنبه — ساعت"); thursdayHoursEdit = thE
        val (tmL, tmE) = til("ساعت موظف پنجشنبه — دقیقه"); thursdayMinsEdit = tmE
        thL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        tmL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        thuRow.addView(thL); thuRow.addView(tmL)
        content.addView(thuRow)
        content.addView(TextView(ctx).apply {
            text = "اگر پنجشنبه تعطیل باشد: ساعت موظف هفته به‌طور مساوی بین شنبه تا چهارشنبه تقسیم می‌شود و پنجشنبه/جمعه هر دو تعطیل‌اند.\nاگر پنجشنبه کاری باشد: ابتدا ساعت موظف پنجشنبه از جمع کل کم شده، باقی‌مانده بین شنبه تا چهارشنبه تقسیم می‌شود و فقط جمعه تعطیل است."
            textSize = 11.5f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(4, 4, 4, 4)
        })

        content.addView(
            MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "بازمحاسبه ترددهای قبلی با تنظیمات فعلی"
                ThemeHelper.applyButton(this, primary(), false)
                setOnClickListener {
                    lifecycleScope.launch {
                        val updated = buildSettings()
                        (requireActivity().application as App).repository.saveSettings(updated)
                        settings = updated
                        Toast.makeText(ctx, "در حال بازمحاسبه...", Toast.LENGTH_SHORT).show()
                        val n = (requireActivity().application as App).repository.recalculateAllAttendance()
                        Toast.makeText(ctx, "${TimeUtils.faNum(n)} رکورد بازمحاسبه شد", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        // Holidays
        content.addView(title("تعطیلات رسمی"))
        holidayListBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        content.addView(holidayListBox)

        // نمایش تاریخ انتخاب‌شده به صورت شمسی روی دکمه
        val selectedHolidayDate = arrayOf<String?>(null)
        val datePickerBtn = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "📅 انتخاب تاریخ شمسی"
            ThemeHelper.applyButton(this, primary(), false)
        }

        val (htL, htE) = til("عنوان (اختیاری)")
        holidayTitleEdit = htE
        // مقداردهی اولیه برای holidayDateEdit - مقدار واقعی از selectedHolidayDate آرایه
        val (hdL, hdE) = til("تاریخ (پر می‌شود با انتخاب تاریخ)")
        holidayDateEdit = hdE
        hdL.visibility = android.view.View.GONE // مخفی، فقط برای backward compat

        datePickerBtn.setOnClickListener {
            com.personal.timetracker.util.JalaliDatePickerDialog.show(
                ctx = ctx,
                primary = primary(),
                dark = dark(),
                initialGregorianDate = selectedHolidayDate[0]
            ) { gregStr, jalDisplay ->
                selectedHolidayDate[0] = gregStr
                datePickerBtn.text = "📅 $jalDisplay"
            }
        }

        content.addView(datePickerBtn)
        htL.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        content.addView(htL)

        content.addView(MaterialButton(ctx).apply {
            text = "افزودن تعطیلی"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener {
                val d = selectedHolidayDate[0]
                val t = holidayTitleEdit.text?.toString()?.trim().orEmpty()
                if (d.isNullOrEmpty()) {
                    Toast.makeText(ctx, "ابتدا تاریخ را انتخاب کنید", Toast.LENGTH_SHORT).show()
                } else {
                    lifecycleScope.launch {
                        (requireActivity().application as App).repository.addHoliday(d, t.ifEmpty { "تعطیل رسمی" })
                        selectedHolidayDate[0] = null
                        datePickerBtn.text = "📅 انتخاب تاریخ شمسی"
                        holidayTitleEdit.text?.clear()
                        loadHolidays()
                    }
                }
            }
        })
        content.addView(
            MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "دریافت تعطیلات امسال از اینترنت (تلاش بهترین حالت)"
                ThemeHelper.applyButton(this, primary(), false)
                setOnClickListener {
                    lifecycleScope.launch {
                        Toast.makeText(ctx, "در حال دریافت...", Toast.LENGTH_SHORT).show()
                        try {
                            val jy = TimeUtils.toJalali(java.util.Date())[0]
                            val n = (requireActivity().application as App).repository
                                .fetchHolidaysFromInternet(jy)
                            Toast.makeText(ctx, "${TimeUtils.faNum(n)} تعطیلی اضافه شد", Toast.LENGTH_LONG).show()
                            loadHolidays()
                        } catch (e: Exception) {
                            Toast.makeText(
                                ctx,
                                "دریافت خودکار ناموفق بود — می‌توانید به‌صورت دستی وارد کنید",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )

        // Projects
        content.addView(title("پروژه‌های Jira"))
        val (pL, pE) = til("نام پروژه"); projectEdit = pE; content.addView(pL)
        content.addView(MaterialButton(ctx).apply {
            text = "افزودن پروژه"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener {
                val n = projectEdit.text?.toString()?.trim().orEmpty()
                if (n.isNotEmpty() && n !in projects) {
                    projects.add(n); projectEdit.text?.clear(); refreshChips()
                }
            }
        })
        chipGroup = ChipGroup(ctx); content.addView(chipGroup)

        // Notifications
        content.addView(title("اعلان‌ها"))
        notifSwitch = Switch(ctx).apply { text = "فعال بودن اعلان پایان کار" }
        content.addView(notifSwitch)
        val (nbL, nbE) = til("دقایق قبل از پایان"); notifBeforeEdit = nbE; content.addView(nbL)
        val (ntL, ntE) = til("عنوان اعلان"); notifTitleEdit = ntE; content.addView(ntL)
        val (nobodyL, nobodyE) = til("متن اعلان", single = false); notifBodyEdit =
            nobodyE; content.addView(nobodyL)
        content.addView(
            MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "تست اعلان پایان کار (الان)"
                setOnClickListener {
                    NotifHelper.show(
                        ctx,
                        notifTitleEdit.text?.toString() ?: "یادآوری",
                        notifBodyEdit.text?.toString() ?: "تست"
                    )
                }
            })
        content.addView(
            MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "بررسی موقعیت محل کار (الان)"
                setOnClickListener { GeoHelper.requestAndCheck(ctx) }
            })

        // Biometric
        content.addView(title("امنیت"))
        bioSwitch = Switch(ctx).apply { text = "قفل بیومتریک (اثرانگشت / چهره)" }
        content.addView(bioSwitch)
        if (!BiometricHelper.canAuthenticate(requireActivity())) {
            bioSwitch.isEnabled = false
            content.addView(TextView(ctx).apply {
                text = "بیومتریک روی این دستگاه در دسترس نیست"; textSize = 12f
            })
        }

        // Location
        content.addView(title("موقعیت محل کار"))
        locationInfo = TextView(ctx).apply { textSize = 13f }
        content.addView(locationInfo)
        content.addView(MaterialButton(ctx).apply {
            text = "ذخیره موقعیت فعلی به‌عنوان محل کار"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener { requestAndSaveLocation() }
        })
        geoAlertSwitch = Switch(ctx).apply { text = "هشدار ورود/خروج ثبت‌نشده" }
        geoAutoSwitch = Switch(ctx).apply { text = "ورود خودکار هنگام رسیدن به محل کار" }
        geoAutoOutSwitch = Switch(ctx).apply { text = "خروج خودکار هنگام ترک محل کار" }
        content.addView(geoAlertSwitch); content.addView(geoAutoSwitch); content.addView(geoAutoOutSwitch)
        val (radiusL, radiusE) = til("شعاع تشخیص محل کار (متر)"); radiusEdit = radiusE
        radiusEdit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        content.addView(radiusL)

        content.addView(MaterialButton(ctx).apply {
            text = "ذخیره تنظیمات"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener { save() }
        })

        // Auto Backup
        content.addView(title("پشتیبان‌گیری خودکار"))
        autoBackupSwitch = Switch(ctx).apply { text = "پشتیبان‌گیری خودکار فعال باشد" }
        content.addView(autoBackupSwitch)
        val (abiL, abiE) = til("فاصله زمانی (ساعت) — پیش‌فرض: ۲۴"); autoBackupIntervalEdit = abiE
        autoBackupIntervalEdit.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        content.addView(abiL)
        content.addView(TextView(ctx).apply {
            text = "پشتیبان در پوشه PTT_Backups در حافظه خارجی ذخیره می‌شود. حداقل فاصله ۱ ساعت است."
            textSize = 11.5f
            setTextColor(ThemeHelper.textSecondary(dark()))
            setPadding(4, 4, 4, 8)
        })
        content.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "اعمال زمانبندی پشتیبان‌گیری"
            ThemeHelper.applyButton(this, primary(), false)
            setOnClickListener {
                val enabled = autoBackupSwitch.isChecked
                val interval = autoBackupIntervalEdit.text?.toString()?.toIntOrNull() ?: 24
                AutoBackupWorker.schedule(requireContext(), enabled, interval)
                Toast.makeText(ctx,
                    if (enabled) "پشتیبان‌گیری خودکار هر ${interval} ساعت فعال شد"
                    else "پشتیبان‌گیری خودکار غیرفعال شد",
                    Toast.LENGTH_SHORT).show()
            }
        })

        // Backup
        content.addView(title("پشتیبان و داده"))
        content.addView(MaterialButton(ctx).apply {
            text = "تهیه پشتیبان JSON"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener {
                lifecycleScope.launch {
                    val f = BackupHelper.exportJson(requireContext())
                    BackupHelper.shareFile(requireContext(), f)
                }
            }
        })
        content.addView(
            MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "بازیابی از پشتیبان"
                ThemeHelper.applyButton(this, primary(), false)
                setOnClickListener {
                    AlertDialog.Builder(ctx).setTitle("بازیابی")
                        .setMessage("داده‌های فعلی جایگزین می‌شوند")
                        .setPositiveButton("ادامه") { _, _ -> pickBackup.launch("application/json") }
                        .setNegativeButton("انصراف", null).show()
                }
            })
        content.addView(
            MaterialButton(
                ctx,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "خالی کردن داده‌ها"
                ThemeHelper.applyButton(this, primary(), false)
                setOnClickListener {
                    AlertDialog.Builder(ctx).setTitle("حذف")
                        .setPositiveButton("فقط داده‌ها") { _, _ ->
                            lifecycleScope.launch {
                                BackupHelper.clearAll(requireContext(), false)
                                Toast.makeText(ctx, "پاک شد", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNeutralButton("همه + تنظیمات") { _, _ ->
                            lifecycleScope.launch {
                                BackupHelper.clearAll(requireContext(), true)
                                load()
                            }
                        }
                        .setNegativeButton("انصراف", null).show()
                }
            })

        startBtn.setOnClickListener { pickTime(true) }
        endBtn.setOnClickListener { pickTime(false) }
        // dark applied on save to recreate activity cleanly

        load()
        loadHolidays()
        return scroll
    }

    private fun refreshChips() {
        chipGroup.removeAllViews()
        projects.forEach { p ->
            chipGroup.addView(Chip(requireContext()).apply {
                text = p
                isCloseIconVisible = true
                setOnCloseIconClickListener { projects.remove(p); refreshChips() }
            })
        }
    }

    private fun loadHolidays() {
        lifecycleScope.launch {
            val repo = (requireActivity().application as App).repository
            val list = repo.getHolidaysOnce()
            val ctx = requireContext()
            holidayListBox.removeAllViews()
            if (list.isEmpty()) {
                holidayListBox.addView(TextView(ctx).apply {
                    text = "تعطیلی‌ای ثبت نشده"
                    textSize = 12f
                    setTextColor(ThemeHelper.textSecondary(dark()))
                    setPadding(4, 6, 4, 6)
                })
            } else {
                list.forEach { h ->
                    val row = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 6, 0, 6)
                    }
                    row.addView(TextView(ctx).apply {
                        text = buildString {
                            append(TimeUtils.toJalaliDisplay(h.date))
                            if (h.title.isNotBlank()) { append(" — "); append(h.title) }
                        }
                        textSize = 12.5f
                        setTextColor(ThemeHelper.textPrimary(dark()))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    // دکمه ویرایش
                    row.addView(ThemeHelper.iconButton(ctx, "✎", primary(), dark(), "ویرایش تعطیلی") {
                        showEditHolidayDialog(h)
                    })
                    // دکمه حذف
                    row.addView(ThemeHelper.iconButton(ctx, "🗑", ThemeHelper.deleteColor, dark(), "حذف تعطیلی") {
                        lifecycleScope.launch {
                            repo.deleteHoliday(h)
                            loadHolidays()
                        }
                    })
                    holidayListBox.addView(row)
                }
            }
        }
    }

    private fun showEditHolidayDialog(existing: com.personal.timetracker.data.entity.HolidayEntity) {
        val ctx = requireContext()
        val repo = (requireActivity().application as App).repository
        val primary = primary()
        val dark = dark()

        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(
                com.personal.timetracker.util.DialogHelper.dp(ctx, 24), 0,
                com.personal.timetracker.util.DialogHelper.dp(ctx, 24), 0
            )
        }

        // انتخاب تاریخ با دیت‌پیکر شمسی
        val selectedDate = arrayOf(existing.date)
        val dateBtn = com.google.android.material.button.MaterialButton(ctx, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "📅 ${TimeUtils.toJalaliShort(TimeUtils.parseDate(existing.date))}"
            ThemeHelper.applyButton(this, primary, false)
            setOnClickListener {
                com.personal.timetracker.util.JalaliDatePickerDialog.show(
                    ctx = ctx, primary = primary, dark = dark,
                    initialGregorianDate = selectedDate[0]
                ) { gregStr, jalDisplay ->
                    selectedDate[0] = gregStr
                    text = "📅 $jalDisplay"
                }
            }
        }

        val (titleLayout, titleEdit) = com.personal.timetracker.util.DialogHelper.inputField(
            ctx, "عنوان تعطیلی", existing.title, primary
        )
        titleLayout.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = com.personal.timetracker.util.DialogHelper.dp(ctx, 12) }

        layout.addView(dateBtn)
        layout.addView(titleLayout)

        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("ویرایش تعطیلی")
            .setView(layout)
            .setPositiveButton("ذخیره") { _, _ ->
                val newDate = selectedDate[0]
                val newTitle = titleEdit.text?.toString()?.trim().orEmpty().ifBlank { "تعطیل رسمی" }
                lifecycleScope.launch {
                    // اگر تاریخ عوض شد، رکورد قدیمی را حذف کن
                    if (newDate != existing.date) {
                        repo.deleteHoliday(existing)
                    }
                    repo.addHoliday(newDate, newTitle)
                    loadHolidays()
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun pickTime(isStart: Boolean) {
        val cur = if (isStart) settings.startWorkTime else settings.endWorkTime
        val p = cur.split(":").map { it.toIntOrNull() ?: 0 }
        TimePickerDialog(requireContext(), { _, h, m ->
            val t = "%02d:%02d".format(h, m)
            if (isStart) {
                settings = settings.copy(startWorkTime = t); startBtn.text = buildString {
                    append("شروع کار: ")
                    append(t)
                }
            } else {
                settings = settings.copy(endWorkTime = t); endBtn.text = buildString {
                    append("پایان کار: ")
                    append(t)
                }
            }
        }, p.getOrElse(0) { 9 }, p.getOrElse(1) { 0 }, true).show()
    }

    private fun requestAndSaveLocation() {
        val ctx = requireContext()
        if (GeoHelper.hasLocationPermission(ctx)) saveCurrentLocation()
        else locPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun saveCurrentLocation() {
        val loc = GeoHelper.lastLocation(requireContext())
        if (loc == null) {
            Toast.makeText(
                requireContext(),
                "موقعیت در دسترس نیست. GPS را روشن کنید",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        settings = settings.copy(workLat = loc.latitude, workLng = loc.longitude)
        locationInfo.text =
            buildString {
                append("محل کار: ")
                append("%.5f".format(loc.latitude))
                append(", ")
                append("%.5f".format(loc.longitude))
            }
        Toast.makeText(
            requireContext(),
            "موقعیت ذخیره شد — دکمه ذخیره تنظیمات را بزنید",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun load() {
        lifecycleScope.launch {
            settings = (requireActivity().application as App).repository.getSettings()
            startBtn.text = buildString {
                append("شروع کار: ")
                append(settings.startWorkTime)
            }
            endBtn.text = buildString {
                append("پایان کار: ")
                append(settings.endWorkTime)
            }
            flexEdit.setText(settings.flexibleMinutes.toString())
            minHoursEdit.setText((settings.minimumWorkMinutes / 60).toString())
            minMinsEdit.setText((settings.minimumWorkMinutes % 60).toString())
            weeklyHoursEdit.setText((settings.weeklyRequiredMinutes / 60).toString())
            weeklyMinsEdit.setText((settings.weeklyRequiredMinutes % 60).toString())
            thursdaySwitch.isChecked = settings.thursdayWorking
            thursdayHoursEdit.setText((settings.thursdayMinutes / 60).toString())
            thursdayMinsEdit.setText((settings.thursdayMinutes % 60).toString())
            projects.clear()
            projects.addAll(settings.projects.split(",").map { it.trim() }
                .filter { it.isNotEmpty() })
            refreshChips()
            darkSwitch.isChecked = settings.isDarkMode
            themeColor = settings.themeColor
            notifSwitch.isChecked = settings.notifEnabled
            notifBeforeEdit.setText(settings.notifMinutesBefore.toString())
            notifTitleEdit.setText(settings.notifTitle)
            notifBodyEdit.setText(settings.notifBody)
            bioSwitch.isChecked = settings.biometricEnabled
            geoAutoSwitch.isChecked = settings.geoAutoCheckIn
            geoAutoOutSwitch.isChecked = settings.geoAutoCheckOut
            geoAlertSwitch.isChecked = settings.geoAlertOnly
            radiusEdit.setText(settings.workRadiusMeters.toInt().toString())
            locationInfo.text = if (settings.workLat != 0.0 || settings.workLng != 0.0)
                "محل کار: ${"%.5f".format(settings.workLat)}, ${"%.5f".format(settings.workLng)} (شعاع ${settings.workRadiusMeters.toInt()} متر)"
            else "محل کار تنظیم نشده"
            autoBackupSwitch.isChecked = settings.autoBackupEnabled
            autoBackupIntervalEdit.setText(settings.autoBackupIntervalHours.toString())
        }
    }

    private fun buildSettings(): SettingsEntity {
        val hours = minHoursEdit.text?.toString()?.toIntOrNull() ?: 8
        val mins = minMinsEdit.text?.toString()?.toIntOrNull() ?: 0
        val weeklyH = weeklyHoursEdit.text?.toString()?.toIntOrNull() ?: 46
        val weeklyM = weeklyMinsEdit.text?.toString()?.toIntOrNull() ?: 15
        val thuH = thursdayHoursEdit.text?.toString()?.toIntOrNull() ?: 5
        val thuM = thursdayMinsEdit.text?.toString()?.toIntOrNull() ?: 0
        return settings.copy(
            flexibleMinutes = flexEdit.text?.toString()?.toIntOrNull() ?: 30,
            minimumWorkMinutes = hours * 60 + mins,
            weeklyRequiredMinutes = weeklyH * 60 + weeklyM,
            thursdayWorking = thursdaySwitch.isChecked,
            thursdayMinutes = thuH * 60 + thuM,
            isDarkMode = darkSwitch.isChecked,
            themeColor = themeColor,
            projects = projects.joinToString(","),
            notifEnabled = notifSwitch.isChecked,
            notifMinutesBefore = notifBeforeEdit.text?.toString()?.toIntOrNull() ?: 30,
            notifTitle = notifTitleEdit.text?.toString()?.ifBlank { "یادآوری پایان کار" }
                ?: "یادآوری پایان کار",
            notifBody = notifBodyEdit.text?.toString()?.ifBlank { "زمان پایان کار نزدیک است" }
                ?: "زمان پایان کار نزدیک است",
            biometricEnabled = bioSwitch.isChecked,
            geoAutoCheckIn = geoAutoSwitch.isChecked,
            geoAutoCheckOut = geoAutoOutSwitch.isChecked,
            geoAlertOnly = geoAlertSwitch.isChecked,
            workRadiusMeters = (radiusEdit.text?.toString()?.toFloatOrNull() ?: settings.workRadiusMeters).coerceAtLeast(20f),
            autoBackupEnabled = autoBackupSwitch.isChecked,
            autoBackupIntervalHours = (autoBackupIntervalEdit.text?.toString()?.toIntOrNull() ?: 24).coerceAtLeast(1)
        )
    }

    private fun save() {
        lifecycleScope.launch {
            val updated = buildSettings()
            (requireActivity().application as App).repository.saveSettings(updated)
            settings = updated
            (activity as? MainActivity)?.applyThemeMode(updated.isDarkMode)
            (activity as? MainActivity)?.applyThemeColor(updated.themeColor)
            // اعمال پشتیبان‌گیری خودکار
            AutoBackupWorker.schedule(requireContext(), updated.autoBackupEnabled, updated.autoBackupIntervalHours)
            Toast.makeText(requireContext(), "ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }
}
