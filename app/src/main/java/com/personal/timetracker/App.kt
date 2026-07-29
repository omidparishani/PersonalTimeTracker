package com.personal.timetracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.personal.timetracker.data.repository.AppRepository

class App : Application() {
    lateinit var repository: AppRepository
        private set

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
    }
}
