package com.personal.timetracker.license

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/** صفحه فعال‌سازی مطابق Figma. */
object ActivationGate {
    private const val PRIMARY = 0xFF1565C0.toInt()

    fun ensure(
        activity: Activity,
        owner: LifecycleOwner,
        appVersion: String? = null,
        username: String? = null,
        onReady: () -> Unit
    ) {
        val ctx = activity.applicationContext
        owner.lifecycleScope.launch {
            try {
            if (!LicenseStore.isActivated(ctx)) {
                showActivationScreen(activity, owner, appVersion, username, onReady)
                return@launch
            }
            val result = try {
                LicenseApi.validate(ctx, username, appVersion)
            } catch (e: Exception) {
                // آفلاین / خطای شبکه → اجازه ورود
                onReady()
                return@launch
            }
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
                        showActivationScreen(activity, owner, appVersion, username, onReady)
                    }
                    .setNegativeButton("خروج") { _, _ -> activity.finish() }
                    .show()
            }
            } catch (t: Throwable) {
                // هر خطای غیرمنتظره → حداقل UI اصلی را نشان بده
                try { onReady() } catch (_: Exception) {}
            }
        }
    }

    private fun showActivationScreen(
        activity: Activity,
        owner: LifecycleOwner,
        appVersion: String?,
        username: String?,
        onReady: () -> Unit
    ) {
        val d = activity.resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PRIMARY)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // بخش بالا
        val top = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(px(24), px(48), px(24), px(32))
        }
        val iconBox = TextView(activity).apply {
            text = "⏱"
            textSize = 36f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = px(24).toFloat()
            }
            setPadding(px(20), px(20), px(20), px(20))
        }
        top.addView(iconBox, LinearLayout.LayoutParams(px(88), px(88)).apply { bottomMargin = px(16) })
        top.addView(TextView(activity).apply {
            text = "Personal Time Tracker"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        top.addView(TextView(activity).apply {
            text = "فعال‌سازی دسترسی سازمانی"
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, px(6), 0, 0)
        })
        root.addView(top)

        // کارت سفید پایین
        val card = MaterialCardView(activity).apply {
            radius = px(28).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val formScroll = ScrollView(activity)
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(24), px(28), px(24), px(24))
        }
        form.addView(TextView(activity).apply {
            text = "خوش آمدید"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF1A1A2E.toInt())
            gravity = Gravity.END
        })
        form.addView(TextView(activity).apply {
            text = "برای شروع، اطلاعات فعال‌سازی دریافت‌شده از سازمان را وارد کنید."
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.END
            setPadding(0, px(8), 0, px(20))
        })

        fun labeledField(label: String, hint: String, value: String = ""): EditText {
            form.addView(TextView(activity).apply {
                text = label
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
                gravity = Gravity.END
                setPadding(0, px(8), 0, px(6))
            })
            val edit = EditText(activity).apply {
                setText(value)
                setHint(hint)
                setSingleLine()
                textSize = 15f
                setPadding(px(14), px(14), px(14), px(14))
                background = GradientDrawable().apply {
                    setColor(0xFFF5F7FA.toInt())
                    cornerRadius = px(14).toFloat()
                    setStroke(px(1), 0xFFE5E7EB.toInt())
                }
                gravity = Gravity.END
            }
            form.addView(edit, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = px(8) })
            return edit
        }

        val codeEdit = labeledField("کد فعال‌سازی", "PTT – …")
        val nameEdit = labeledField("نام (اختیاری)", "نام و نام خانوادگی", username ?: "")
        val serverEdit = labeledField("آدرس سرور لایسنس", "https://…", LicenseStore.baseUrl(activity))

        val status = TextView(activity).apply {
            textSize = 12f
            setTextColor(0xFFD32F2F.toInt())
            gravity = Gravity.CENTER
            setPadding(0, px(4), 0, px(8))
        }
        form.addView(status)

        val btn = MaterialButton(activity).apply {
            text = "فعال‌سازی و ورود"
            setBackgroundColor(PRIMARY)
            setTextColor(Color.WHITE)
            cornerRadius = px(16)
            setPadding(0, px(14), 0, px(14))
        }
        form.addView(btn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = px(8) })

        form.addView(TextView(activity).apply {
            text = "اطلاعات شما با اتصال امن سازمانی بررسی می‌شود."
            textSize = 11f
            setTextColor(0xFF9E9E9E.toInt())
            gravity = Gravity.CENTER
            setPadding(0, px(16), 0, 0)
        })

        formScroll.addView(form)
        card.addView(formScroll)
        root.addView(card)
        activity.setContentView(root)

        btn.setOnClickListener {
            val code = codeEdit.text?.toString()?.trim().orEmpty()
            if (code.length < 4) {
                status.text = "کد فعال‌سازی را وارد کنید"
                return@setOnClickListener
            }
            val server = serverEdit.text?.toString()?.trim().orEmpty()
            if (server.isNotBlank()) LicenseStore.setBaseUrl(activity, server)
            val name = nameEdit.text?.toString()?.trim()
            btn.isEnabled = false
            status.setTextColor(PRIMARY)
            status.text = "در حال بررسی…"
            owner.lifecycleScope.launch {
                val r = LicenseApi.activate(activity, code, name, appVersion)
                if (r.ok) {
                    Toast.makeText(activity, "فعال‌سازی موفق", Toast.LENGTH_SHORT).show()
                    activity.recreate()
                } else {
                    status.setTextColor(0xFFD32F2F.toInt())
                    status.text = r.error ?: "فعال‌سازی ناموفق"
                    btn.isEnabled = true
                }
            }
        }
    }
}
