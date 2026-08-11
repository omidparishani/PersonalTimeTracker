package com.personal.timetracker.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.personal.timetracker.ui.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotifHelper {
    const val CHANNEL_ID = "work_reminders"
    const val NOTIF_ID = 1001
    const val GEO_NOTIF_ID = 1002
    const val ACTION_WORK_END = "com.personal.timetracker.WORK_END_REMINDER"
    private const val WORK_NAME = "work_end_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "یادآوری کار",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری پایان کار و موقعیت"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    fun canPost(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, title: String, body: String, id: Int = NOTIF_ID) {
        ensureChannel(context)
        if (!canPost(context)) {
            Log.w("PTT", "Notifications disabled by user")
            return
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            Log.e("PTT", "notif permission", e)
        }
    }

    /**
     * Schedule reminder at (suggestedEnd - minutesBefore).
     * Uses both AlarmManager and WorkManager for reliability.
     */
    fun scheduleWorkEnd(
        context: Context,
        suggestedEndHHmm: String,
        minutesBefore: Int,
        title: String,
        body: String
    ) {
        cancel(context)
        ensureChannel(context)
        val parts = suggestedEndHHmm.split(":")
        if (parts.size < 2) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 17)
            set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore.coerceAtLeast(0))
        }
        var trigger = cal.timeInMillis
        if (trigger <= System.currentTimeMillis()) {
            // if already past, fire in 1 minute for testing visibility
            trigger = System.currentTimeMillis() + 60_000
            Log.i("PTT", "Work end time past; scheduling in 1 min")
        }
        val delay = (trigger - System.currentTimeMillis()).coerceAtLeast(5_000)

        // WorkManager
        val data = Data.Builder()
            .putString("title", title)
            .putString("body", body)
            .build()
        val req = OneTimeWorkRequestBuilder<WorkEndWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.REPLACE, req
        )

        // AlarmManager backup
        val intent = Intent(context, WorkEndReceiver::class.java).apply {
            action = ACTION_WORK_END
            putExtra("title", title)
            putExtra("body", body)
        }
        val pi = PendingIntent.getBroadcast(
            context, NOTIF_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (e: SecurityException) {
            Log.e("PTT", "exact alarm denied", e)
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi) } catch (_: Exception) {}
        }
        Log.i("PTT", "Scheduled work-end in ${delay / 1000}s → $suggestedEndHHmm (-$minutesBefore)")
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        val intent = Intent(context, WorkEndReceiver::class.java).apply { action = ACTION_WORK_END }
        val pi = PendingIntent.getBroadcast(
            context, NOTIF_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    private const val GEO_WORK_NAME = "geo_background_check"

    /**
     * Schedules a periodic background check (every 15 minutes — the minimum interval
     * WorkManager allows) so geofence-based auto check-in and location reminders keep
     * working while the app is closed, not just while it's open in the foreground.
     * WorkManager re-registers this itself after device reboot, so it only needs to be
     * scheduled once (KEEP policy makes repeated calls cheap no-ops).
     * The worker itself is a no-op if the workplace location or the geo features aren't
     * configured, so this is safe to always schedule.
     */
    fun scheduleGeoBackgroundCheck(context: Context) {
        val req = PeriodicWorkRequestBuilder<GeoCheckWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GEO_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req
        )
    }
}

class WorkEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "یادآوری پایان کار"
        val body = intent?.getStringExtra("body") ?: "زمان پایان کار نزدیک است"
        NotifHelper.show(context, title, body)
    }
}

class WorkEndWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "یادآوری پایان کار"
        val body = inputData.getString("body") ?: "زمان پایان کار نزدیک است"
        NotifHelper.show(applicationContext, title, body)
        return Result.success()
    }
}

/** Runs every ~15 minutes in the background to keep geofence auto check-in/out and
 *  location reminders working while the app isn't open. */
class GeoCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            if (GeoHelper.hasLocationPermission(applicationContext)) {
                GeoHelper.checkWorkplaceSuspend(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("PTT", "geo background worker", e)
            Result.success()
        }
    }
}
