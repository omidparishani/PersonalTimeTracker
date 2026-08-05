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
import com.personal.timetracker.util.BackupHelper
import com.personal.timetracker.util.BiometricHelper
import com.personal.timetracker.util.GeoHelper
import com.personal.timetracker.util.NotifHelper
import com.personal.timetracker.util.ThemeHelper
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var settings = SettingsEntity()
    private lateinit var startBtn: MaterialButton
    private lateinit var endBtn: MaterialButton
    private lateinit var flexEdit: TextInputEditText
    private lateinit var minHoursEdit: TextInputEditText
    private lateinit var minMinsEdit: TextInputEditText
    private lateinit var projectEdit: TextInputEditText
    private lateinit var notifTitleEdit: TextInputEditText
    private lateinit var notifBodyEdit: TextInputEditText
    private lateinit var notifBeforeEdit: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var darkSwitch: Switch
    private lateinit var notifSwitch: Switch
    private lateinit var bioSwitch: Switch
    private lateinit var geoAutoSwitch: Switch
    private lateinit var geoAlertSwitch: Switch
    private lateinit var locationInfo: TextView
    private val projects = mutableListOf<String>()
    private var themeColor = -10983104

    private val colors = listOf(
        0xFF1565C0.toInt(), 0xFF2E7D32.toInt(), 0xFFC62828.toInt(),
        0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFFEF6C00.toInt(),
        0xFF4527A0.toInt(), 0xFF37474F.toInt(), 0xFFAD1457.toInt()
    )

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: 0xFF1565C0.toInt()

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
        val minRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val (mhL, mhE) = til("حداقل کار — ساعت"); minHoursEdit = mhE
        val (mmL, mmE) = til("حداقل کار — دقیقه"); minMinsEdit = mmE
        mhL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = 8 }
        mmL.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        minRow.addView(mhL); minRow.addView(mmL); content.addView(minRow)

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
        content.addView(geoAlertSwitch); content.addView(geoAutoSwitch)

        content.addView(MaterialButton(ctx).apply {
            text = "ذخیره تنظیمات"
            ThemeHelper.applyButton(this, primary(), true)
            setOnClickListener { save() }
        })

        // Backup
        content.addView(title("پشتیبان و داده"))
        content.addView(MaterialButton(ctx).apply {
            text = buildString {
                append("تهیه پشتیبان JSON")
            }
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
            geoAlertSwitch.isChecked = settings.geoAlertOnly
            locationInfo.text = if (settings.workLat != 0.0 || settings.workLng != 0.0)
                "محل کار: ${"%.5f".format(settings.workLat)}, ${"%.5f".format(settings.workLng)} (شعاع ${settings.workRadiusMeters.toInt()} متر)"
            else "محل کار تنظیم نشده"
        }
    }

    private fun save() {
        lifecycleScope.launch {
            val hours = minHoursEdit.text?.toString()?.toIntOrNull() ?: 8
            val mins = minMinsEdit.text?.toString()?.toIntOrNull() ?: 0
            val updated = settings.copy(
                flexibleMinutes = flexEdit.text?.toString()?.toIntOrNull() ?: 30,
                minimumWorkMinutes = hours * 60 + mins,
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
                geoAlertOnly = geoAlertSwitch.isChecked
            )
            (requireActivity().application as App).repository.saveSettings(updated)
            settings = updated
            (activity as? MainActivity)?.applyThemeMode(updated.isDarkMode)
            (activity as? MainActivity)?.applyThemeColor(updated.themeColor)
            Toast.makeText(requireContext(), "ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }
}
