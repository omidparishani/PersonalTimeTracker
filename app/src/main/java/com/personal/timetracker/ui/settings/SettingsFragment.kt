package com.personal.timetracker.ui.settings

/**
 * تنظیمات کاربر — ظاهر مطابق Figma (ترجیحات + سرویس‌ها + شیفت فقط‌خواندنی).
 */

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.personal.timetracker.App
import com.personal.timetracker.data.entity.SettingsEntity
import com.personal.timetracker.jira.JiraService
import com.personal.timetracker.license.RemoteConfig
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.ui.support.SupportFragment
import com.personal.timetracker.util.AutoBackupWorker
import com.personal.timetracker.util.BiometricHelper
import com.personal.timetracker.util.FigmaUi
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private lateinit var settings: SettingsEntity
    private lateinit var darkSwitch: Switch
    private lateinit var bioSwitch: Switch
    private lateinit var notifSwitch: Switch
    private lateinit var jiraEnabledSwitch: Switch
    private lateinit var jiraUrlEdit: EditText
    private lateinit var jiraUserEdit: EditText
    private lateinit var jiraPassEdit: EditText
    private lateinit var jiraTokenEdit: EditText
    private lateinit var authTokenSwitch: Switch
    private lateinit var jiraStatusTv: TextView
    private lateinit var shiftInfo: TextView
    private lateinit var geoAutoInSwitch: Switch
    private lateinit var geoAutoOutSwitch: Switch
    private lateinit var geoAlertSwitch: Switch
    private lateinit var radiusEdit: EditText
    private lateinit var locationInfo: TextView

    private fun primary() = (activity as? MainActivity)?.primaryColor ?: FigmaUi.PRIMARY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val isDark = (activity as? MainActivity)?.isDark == true
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FigmaUi.bg(isDark))
        }
        val scroll = ScrollView(ctx)
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, FigmaUi.dp(ctx, 32))
        }

        // هدر
        content.addView(FigmaUi.screenHeader(ctx, "تنظیمات", "حساب و اتصال‌های سازمانی", "⚙️"))

        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 16), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 16), 0)
        }

        fun sectionTitle(t: String) = TextView(ctx).apply {
            text = t
            textSize = 12f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.END
            setPadding(0, FigmaUi.dp(ctx, 12), 0, FigmaUi.dp(ctx, 8))
        }

        fun groupCard(): MaterialCardView = FigmaUi.whiteCard(ctx, isDark).apply {
            radius = FigmaUi.dp(ctx, 16).toFloat()
        }

        fun field(hint: String, password: Boolean = false): EditText {
            return EditText(ctx).apply {
                this.hint = hint
                setSingleLine()
                textSize = 14f
                setTextColor(FigmaUi.text(isDark))
                setHintTextColor(FigmaUi.muted(isDark))
                setPadding(FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 12))
                background = FigmaUi.rounded(
                    if (isDark) 0xFF243447.toInt() else 0xFFF5F7FA.toInt(), 12f, ctx
                )
                gravity = Gravity.END
                if (password) {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
        }

        // ترجیحات
        body.addView(sectionTitle("ترجیحات"))
        val prefCard = groupCard()
        val prefCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        darkSwitch = Switch(ctx)
        prefCol.addView(rowWithSwitch(ctx, "ظاهر", "حالت روشن / تاریک", "🌙", darkSwitch))
        notifSwitch = Switch(ctx)
        prefCol.addView(divider(ctx))
        prefCol.addView(rowWithSwitch(ctx, "اعلان‌ها", "تردد، تسک‌ها و یادآوری‌ها", "🔔", notifSwitch))
        bioSwitch = Switch(ctx)
        if (!BiometricHelper.canAuthenticate(requireActivity())) bioSwitch.isEnabled = false
        prefCol.addView(divider(ctx))
        prefCol.addView(rowWithSwitch(ctx, "ورود با اثر انگشت", "ورود سریع و امن", "🛡️", bioSwitch))
        prefCard.addView(prefCol)
        body.addView(prefCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 8)))

        // سرویس‌ها
        body.addView(sectionTitle("سرویس‌ها"))
        val svcCard = groupCard()
        val svcCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12))
        }
        jiraEnabledSwitch = Switch(ctx).apply {
            text = "فعال‌سازی همگام‌سازی Jira"
            setTextColor(FigmaUi.text(isDark))
        }
        svcCol.addView(jiraEnabledSwitch)
        authTokenSwitch = Switch(ctx).apply {
            text = "احراز با Token (به‌جای رمز)"
            setTextColor(FigmaUi.text(isDark))
        }
        svcCol.addView(authTokenSwitch)
        jiraUrlEdit = field("آدرس سرور Jira")
        jiraUserEdit = field("نام کاربری / ایمیل")
        jiraPassEdit = field("رمز عبور", password = true)
        jiraTokenEdit = field("API Token / PAT", password = true)
        listOf(jiraUrlEdit, jiraUserEdit, jiraPassEdit, jiraTokenEdit).forEach {
            svcCol.addView(it, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = FigmaUi.dp(ctx, 8) })
        }
        fun refreshAuthFields() {
            val tokenMode = authTokenSwitch.isChecked
            jiraPassEdit.visibility = if (tokenMode) android.view.View.GONE else android.view.View.VISIBLE
            jiraTokenEdit.visibility = if (tokenMode) android.view.View.VISIBLE else android.view.View.GONE
            jiraUserEdit.hint = if (tokenMode) "ایمیل / نام کاربری (برای Basic+Token)" else "نام کاربری"
        }
        authTokenSwitch.setOnCheckedChangeListener { _, _ -> refreshAuthFields() }
        refreshAuthFields()
        jiraStatusTv = TextView(ctx).apply {
            textSize = 12f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.END
            setPadding(0, FigmaUi.dp(ctx, 8), 0, 0)
        }
        svcCol.addView(jiraStatusTv)
        val testBtn = MaterialButton(ctx).apply {
            text = "تست اتصال"
            setBackgroundColor(primary())
            setTextColor(0xFFFFFFFF.toInt())
            cornerRadius = FigmaUi.dp(ctx, 12)
            setOnClickListener { testJira() }
        }
        svcCol.addView(testBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = FigmaUi.dp(ctx, 8) })
        svcCard.addView(svcCol)
        body.addView(svcCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 8)))

        if (RemoteConfig.canSupport(ctx)) {
            val supportCard = groupCard()
            supportCard.addView(FigmaUi.settingsRow(ctx, "پشتیبانی", "گفت‌وگو با پشتیبانی سازمان", "💬") {
                parentFragmentManager.beginTransaction()
                    .replace(com.personal.timetracker.R.id.fragmentContainer, SupportFragment())
                    .addToBackStack("support")
                    .commit()
            })
            body.addView(supportCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 8)))
        }

        // موقعیت و ورود/خروج خودکار
        body.addView(sectionTitle("موقعیت محل کار"))
        val geoCard = groupCard()
        val geoCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12))
        }
        locationInfo = TextView(ctx).apply {
            textSize = 12f
            setTextColor(FigmaUi.muted(isDark))
            gravity = Gravity.END
        }
        geoCol.addView(locationInfo)
        val saveLocBtn = MaterialButton(ctx).apply {
            text = "ذخیره موقعیت فعلی به‌عنوان محل کار"
            setBackgroundColor(primary())
            setTextColor(0xFFFFFFFF.toInt())
            cornerRadius = FigmaUi.dp(ctx, 12)
            setOnClickListener { saveCurrentLocation() }
        }
        geoCol.addView(saveLocBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = FigmaUi.dp(ctx, 8) })
        radiusEdit = field("شعاع (متر) — پیش‌فرض ۱۵۰")
        geoCol.addView(radiusEdit, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = FigmaUi.dp(ctx, 8) })
        geoAutoInSwitch = Switch(ctx).apply {
            text = "ورود خودکار با رسیدن به محل کار"
            setTextColor(FigmaUi.text(isDark))
        }
        geoAutoOutSwitch = Switch(ctx).apply {
            text = "خروج خودکار با ترک محل کار"
            setTextColor(FigmaUi.text(isDark))
        }
        geoAlertSwitch = Switch(ctx).apply {
            text = "فقط هشدار (بدون ثبت خودکار)"
            setTextColor(FigmaUi.text(isDark))
        }
        geoCol.addView(geoAutoInSwitch)
        geoCol.addView(geoAutoOutSwitch)
        geoCol.addView(geoAlertSwitch)
        geoCard.addView(geoCol)
        body.addView(geoCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 8)))

        // شیفت فقط‌خواندنی
        body.addView(sectionTitle("اطلاعات شیفت"))
        val shiftCard = groupCard()
        shiftInfo = TextView(ctx).apply {
            textSize = 13f
            setTextColor(FigmaUi.text(isDark))
            gravity = Gravity.END
            setPadding(FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 14))
        }
        shiftCard.addView(shiftInfo)
        body.addView(shiftCard, FigmaUi.matchParent(bottom = FigmaUi.dp(ctx, 16)))

        val saveBtn = MaterialButton(ctx).apply {
            text = "ذخیره تنظیمات"
            setBackgroundColor(primary())
            setTextColor(0xFFFFFFFF.toInt())
            cornerRadius = FigmaUi.dp(ctx, 14)
            setOnClickListener { save() }
        }
        body.addView(saveBtn, FigmaUi.matchParent())

        content.addView(body)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        load()
        return root
    }

    private fun rowWithSwitch(
        ctx: android.content.Context,
        title: String,
        sub: String,
        emoji: String,
        sw: Switch
    ): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12), FigmaUi.dp(ctx, 14), FigmaUi.dp(ctx, 12))
            addView(sw)
            val texts = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(ctx).apply {
                text = title
                textSize = 14f
                setTextColor(FigmaUi.text((activity as? MainActivity)?.isDark == true))
                gravity = Gravity.END
            })
            texts.addView(TextView(ctx).apply {
                text = sub
                textSize = 11f
                setTextColor(FigmaUi.muted((activity as? MainActivity)?.isDark == true))
                gravity = Gravity.END
            })
            addView(texts)
            addView(TextView(ctx).apply {
                text = emoji
                textSize = 16f
                gravity = Gravity.CENTER
                background = FigmaUi.oval(0xFFE3F2FD.toInt())
                layoutParams = LinearLayout.LayoutParams(FigmaUi.dp(ctx, 40), FigmaUi.dp(ctx, 40)).apply {
                    marginStart = FigmaUi.dp(ctx, 10)
                }
            })
        }
    }

    private fun divider(ctx: android.content.Context) = View(ctx).apply {
        setBackgroundColor(0xFFE5E7EB.toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, FigmaUi.dp(ctx, 1)
        ).apply {
            marginStart = FigmaUi.dp(ctx, 14)
            marginEnd = FigmaUi.dp(ctx, 14)
        }
    }

    private fun load() {
        lifecycleScope.launch {
            val ctx = requireContext()
            settings = (requireActivity().application as App).repository.getSettings()
            darkSwitch.isChecked = settings.isDarkMode
            notifSwitch.isChecked = settings.notifEnabled
            bioSwitch.isChecked = settings.biometricEnabled
            jiraEnabledSwitch.isChecked = settings.jiraEnabled
            jiraUrlEdit.setText(settings.jiraBaseUrl)
            jiraUserEdit.setText(settings.jiraUsername)
            jiraPassEdit.setText(settings.jiraPassword)
            jiraTokenEdit.setText(settings.jiraToken)
            authTokenSwitch.isChecked = settings.jiraToken.isNotBlank() && settings.jiraPassword.isBlank()
            jiraPassEdit.visibility = if (authTokenSwitch.isChecked) android.view.View.GONE else android.view.View.VISIBLE
            jiraTokenEdit.visibility = if (authTokenSwitch.isChecked) android.view.View.VISIBLE else android.view.View.GONE
            jiraStatusTv.text = if (settings.jiraEnabled &&
                (settings.jiraUsername.isNotBlank() || settings.jiraToken.isNotBlank())
            ) "پیکربندی شده" else "هنوز پیکربندی نشده"
            geoAutoInSwitch.isChecked = settings.geoAutoCheckIn
            geoAutoOutSwitch.isChecked = settings.geoAutoCheckOut
            geoAlertSwitch.isChecked = settings.geoAlertOnly
            radiusEdit.setText(settings.workRadiusMeters.toInt().toString())
            locationInfo.text = if (settings.workLat != 0.0 || settings.workLng != 0.0)
                "محل کار: %.5f, %.5f".format(settings.workLat, settings.workLng)
            else "محل کار هنوز ذخیره نشده"

            shiftInfo.text = buildString {
                append("شروع: ${RemoteConfig.startWork(ctx)}   پایان: ${RemoteConfig.endWork(ctx)}\n")
                append("شناوری: ${RemoteConfig.flexible(ctx)} دقیقه\n")
                append("حداقل روزانه: ${RemoteConfig.minDaily(ctx)} دقیقه\n")
                append("حداقل هفتگی: ${RemoteConfig.weekly(ctx)} دقیقه\n")
                append(
                    if (RemoteConfig.thuWorking(ctx))
                        "پنج‌شنبه کاری (${RemoteConfig.thuMinutes(ctx)} دقیقه)"
                    else "پنج‌شنبه تعطیل"
                )
                append("\n\nاین مقادیر فقط از پنل مدیر تغییر می‌کنند.")
            }
        }
    }

    private fun buildSettings(): SettingsEntity {
        val ctx = requireContext()
        return settings.copy(
            startWorkTime = RemoteConfig.startWork(ctx),
            endWorkTime = RemoteConfig.endWork(ctx),
            flexibleMinutes = RemoteConfig.flexible(ctx),
            minimumWorkMinutes = RemoteConfig.minDaily(ctx),
            weeklyRequiredMinutes = RemoteConfig.weekly(ctx),
            thursdayWorking = RemoteConfig.thuWorking(ctx),
            thursdayMinutes = RemoteConfig.thuMinutes(ctx),
            isDarkMode = darkSwitch.isChecked,
            notifEnabled = notifSwitch.isChecked,
            biometricEnabled = bioSwitch.isChecked,
            jiraEnabled = jiraEnabledSwitch.isChecked,
            jiraBaseUrl = jiraUrlEdit.text?.toString()?.trim().orEmpty(),
            jiraUsername = jiraUserEdit.text?.toString()?.trim().orEmpty(),
            jiraPassword = if (authTokenSwitch.isChecked) "" else (jiraPassEdit.text?.toString() ?: ""),
            jiraToken = if (authTokenSwitch.isChecked) (jiraTokenEdit.text?.toString()?.trim() ?: "")
                else (jiraTokenEdit.text?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: settings.jiraToken),
            geoAutoCheckIn = geoAutoInSwitch.isChecked,
            geoAutoCheckOut = geoAutoOutSwitch.isChecked,
            geoAlertOnly = geoAlertSwitch.isChecked,
            workRadiusMeters = radiusEdit.text?.toString()?.toFloatOrNull() ?: settings.workRadiusMeters
        )
    }

    private fun save() {
        lifecycleScope.launch {
            val updated = buildSettings()
            (requireActivity().application as App).repository.saveSettings(updated)
            settings = updated
            (activity as? MainActivity)?.applyThemeMode(updated.isDarkMode)
            try {
                AutoBackupWorker.schedule(
                    requireContext(),
                    updated.autoBackupEnabled,
                    updated.autoBackupIntervalHours
                )
            } catch (_: Exception) {
            }
            Toast.makeText(requireContext(), "ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentLocation() {
        val act = activity ?: return
        lifecycleScope.launch {
            try {
                val loc = com.personal.timetracker.util.GeoHelper.lastLocation(act)
                if (loc == null) {
                    Toast.makeText(requireContext(), "موقعیت در دسترس نیست — مجوز را بررسی کنید", Toast.LENGTH_LONG).show()
                    return@launch
                }
                settings = settings.copy(workLat = loc.latitude, workLng = loc.longitude)
                locationInfo.text = "محل کار: %.5f, %.5f".format(loc.latitude, loc.longitude)
                Toast.makeText(requireContext(), "موقعیت گرفته شد — ذخیره تنظیمات را بزنید", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun testJira() {

        lifecycleScope.launch {
            try {
                val updated = buildSettings().copy(jiraEnabled = true)
                (requireActivity().application as App).repository.saveSettings(updated)
                settings = updated
                jiraEnabledSwitch.isChecked = true
                val service = JiraService.fromSettings(updated)
                    ?: run {
                        jiraStatusTv.text = "آدرس یا نام کاربری ناقص است"
                        return@launch
                    }
                val me = service.myself().getOrElse {
                    jiraStatusTv.text = "✗ ${it.message}"
                    return@launch
                }
                val issues = service.fetchAssigned(openOnly = true, maxResults = 5)
                val n = issues.getOrNull()?.size ?: -1
                jiraStatusTv.text = "✓ متصل: ${me.displayName ?: me.name} · Issue: $n"
                Toast.makeText(requireContext(), "اتصال موفق", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                jiraStatusTv.text = "✗ ${e.message}"
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
