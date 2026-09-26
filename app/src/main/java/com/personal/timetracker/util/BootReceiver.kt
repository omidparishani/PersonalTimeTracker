package com.personal.timetracker.util

/**
 * توضیح فایل: اجرای مجدد زمان‌بندی بعد از روشن شدن گوشی.
 * بسته: com.personal.timetracker.util
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.timetracker.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * اجرای مجدد زمان‌بندی بعد از روشن شدن گوشی.
 */
class BootReceiver : BroadcastReceiver() {
    /**
     * دریافت Intent (کلیک ویجت یا broadcast).
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DynamicAppIcon.schedule(context)
                DynamicAppIcon.syncNow(context)
                NotifHelper.scheduleGeoBackgroundCheck(context)
                val settings = (context.applicationContext as? App)?.repository?.getSettings()
                if (settings != null) {
                    AutoBackupWorker.schedule(
                        context,
                        settings.autoBackupEnabled,
                        settings.autoBackupIntervalHours
                    )
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
