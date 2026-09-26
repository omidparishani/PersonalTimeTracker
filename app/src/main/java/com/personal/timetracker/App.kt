package com.personal.timetracker

/**
 * توضیح فایل: نقطه ورود سراسری اپلیکیشن؛ ساخت Repository و زمان‌بندی کارهای پس‌زمینه.
 * بسته: com.personal.timetracker
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.personal.timetracker.data.repository.AppRepository
import com.personal.timetracker.util.NotifHelper

/**
 * نقطه ورود سراسری اپلیکیشن؛ ساخت Repository و زمان‌بندی کارهای پس‌زمینه.
 */
class App : Application() {
    lateinit var repository: AppRepository
        private set

    /**
     * راه‌اندازی اولیه هنگام ساخت شیء.
     */
    override fun onCreate() {
        super.onCreate()
        try {
            repository = AppRepository(this)
        } catch (e: Exception) {
            Log.e("PTT", "Repository init failed", e)
            // last resort: try again
            repository = AppRepository(this)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    "work_reminders",
                    "یادآوری کار",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(ch)
            }
        } catch (e: Exception) {
            Log.e("PTT", "Notification channel failed", e)
        }
        try {
            com.personal.timetracker.util.DynamicAppIcon.schedule(this)
            com.personal.timetracker.util.DynamicAppIcon.sync(this)
        } catch (e: Exception) {
            Log.e("PTT", "Icon sync failed", e)
        }
        try {
            // Keeps geofence auto check-in/out and reminders running in the background,
            // not just while the app is open in the foreground.
            NotifHelper.scheduleGeoBackgroundCheck(this)
        } catch (e: Exception) {
            Log.e("PTT", "Background geo scheduling failed", e)
        }
    }
}
