package com.personal.timetracker.license

import android.app.Activity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * دروازه فعال‌سازی: قبل از ورود به اپ، کد را می‌پرسد و لایسنس را چک می‌کند.
 */
object ActivationGate {

    /**
     * @param onReady وقتی لایسنس معتبر است (یا آفلاین با لایسنس قبلی)
     */
    fun ensure(
        activity: Activity,
        owner: LifecycleOwner,
        appVersion: String? = null,
        username: String? = null,
        onReady: () -> Unit
    ) {
        val ctx = activity.applicationContext
        owner.lifecycleScope.launch {
            if (!LicenseStore.isActivated(ctx)) {
                showActivationDialog(activity, owner, appVersion, username, onReady)
                return@launch
            }
            val result = LicenseApi.validate(ctx, username, appVersion)
            if (result.active) {
                onReady()
            } else {
                val msg = result.message
                    ?: when (result.reason) {
                        "disabled" -> "دسترسی شما توسط مدیر غیرفعال شده است"
                        "device_mismatch" -> "این لایسنس برای دستگاه دیگری است"
                        else -> "لایسنس نامعتبر است — دوباره فعال‌سازی کنید"
                    }
                LicenseStore.clear(ctx)
                AlertDialog.Builder(activity)
                    .setTitle("دسترسی قطع شد")
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("فعال‌سازی مجدد") { _, _ ->
                        showActivationDialog(activity, owner, appVersion, username, onReady)
                    }
                    .setNegativeButton("خروج") { _, _ -> activity.finish() }
                    .show()
            }
        }
    }

    private fun showActivationDialog(
        activity: Activity,
        owner: LifecycleOwner,
        appVersion: String?,
        username: String?,
        onReady: () -> Unit
    ) {
        val pad = (16 * activity.resources.displayMetrics.density).toInt()
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        box.addView(TextView(activity).apply {
            text = "برای استفاده از اپ، کد فعال‌سازی را از مدیر دریافت کنید و وارد نمایید."
            textSize = 14f
        })
        val codeEdit = EditText(activity).apply {
            hint = "مثلاً PTT-XXXX-XXXX"
            setSingleLine()
        }
        box.addView(codeEdit)
        val nameEdit = EditText(activity).apply {
            hint = "نام شما (اختیاری)"
            setSingleLine()
            setText(username ?: "")
        }
        box.addView(nameEdit)
        val serverEdit = EditText(activity).apply {
            hint = "آدرس سرور لایسنس"
            setSingleLine()
            setText(LicenseStore.baseUrl(activity))
        }
        box.addView(serverEdit)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("فعال‌سازی")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("فعال‌سازی", null)
            .setNegativeButton("خروج") { _, _ -> activity.finish() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = codeEdit.text?.toString()?.trim().orEmpty()
                if (code.length < 4) {
                    Toast.makeText(activity, "کد را وارد کنید", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val server = serverEdit.text?.toString()?.trim().orEmpty()
                if (server.isNotBlank()) LicenseStore.setBaseUrl(activity, server)
                val name = nameEdit.text?.toString()?.trim()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                owner.lifecycleScope.launch {
                    val r = LicenseApi.activate(activity, code, name, appVersion)
                    if (r.ok) {
                        Toast.makeText(activity, "فعال‌سازی موفق", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        onReady()
                    } else {
                        Toast.makeText(activity, r.error ?: "ناموفق", Toast.LENGTH_LONG).show()
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                }
            }
        }
        dialog.show()
    }
}
