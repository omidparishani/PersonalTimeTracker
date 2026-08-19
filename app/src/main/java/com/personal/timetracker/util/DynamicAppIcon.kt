package com.personal.timetracker.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.widget.IconHoursWidgetProvider
import com.personal.timetracker.widget.WorkWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Updates the launcher icon (via activity-aliases for 0–10 hours) and the 1×1
 * hours widget so today's worked time is visible on the home screen.
 *
 * Stock Android cannot draw arbitrary text on the main launcher icon; aliases
 * with pre-built hour badges are the supported way. The 1×1 widget shows the
 * exact ساعت:دقیقه in Persian digits.
 */
object DynamicAppIcon {
    private const val PREFS = "dynamic_icon"
    private const val KEY_HOUR = "applied_hour"
    const val MAX_HOUR_BUCKET = 10

    fun aliasClassName(hour: Int): String =
        "com.personal.timetracker.ui.LauncherH${hour.coerceIn(0, MAX_HOUR_BUCKET)}"

    fun sync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncNow(context.applicationContext)
            } catch (e: Exception) {
                Log.w("PTT", "DynamicAppIcon.sync", e)
            }
        }
    }

    suspend fun syncNow(context: Context) {
        val minutes = todayWorkedMinutes(context)
        applyLauncherAlias(context, (minutes / 60).coerceIn(0, MAX_HOUR_BUCKET))
        try {
            WorkWidgetProvider.requestUpdate(context)
        } catch (_: Exception) {
        }
        try {
            IconHoursWidgetProvider.requestUpdate(context)
        } catch (_: Exception) {
        }
    }

    suspend fun todayWorkedMinutes(context: Context): Int {
        val db = AppDatabase.get(context)
        val today = db.attendanceDao().getByDateOnce(TimeUtils.today())
        return today.sumOf { rec ->
            when {
                rec.exitTime != null -> rec.duration
                rec.status == "active" ->
                    TimeUtils.minutesBetween(rec.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                else -> 0
            }
        }.coerceAtLeast(0)
    }

    fun applyLauncherAlias(context: Context, hour: Int) {
        val target = hour.coerceIn(0, MAX_HOUR_BUCKET)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_HOUR, -1) == target) return

        val pm = context.packageManager
        val targetCn = ComponentName(context, aliasClassName(target))
        pm.setComponentEnabledSetting(
            targetCn,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        for (h in 0..MAX_HOUR_BUCKET) {
            if (h == target) continue
            pm.setComponentEnabledSetting(
                ComponentName(context, aliasClassName(h)),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        prefs.edit().putInt(KEY_HOUR, target).apply()
        Log.i("PTT", "Launcher icon set to ${target}h")
    }

    /** Bitmap for the 1×1 home-screen icon widget (exact hours:minutes, Persian digits). */
    fun hoursBitmap(context: Context, minutes: Int, sizePx: Int = 192): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        val radius = sizePx * 0.22f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                0xFF1565C0.toInt(), 0xFF0D47A1.toInt(), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, radius, radius, bg)

        val h = minutes / 60
        val m = minutes % 60
        val time = TimeUtils.faNum("%d:%02d".format(h, m))
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.34f
        }
        val cx = sizePx / 2f
        val fm = timePaint.fontMetrics
        val timeCy = sizePx * 0.46f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(time, cx, timeCy, timePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.12f
        }
        canvas.drawText("امروز", cx, sizePx * 0.78f, labelPaint)
        return bmp
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<IconSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ptt_icon_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}

class IconSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            DynamicAppIcon.syncNow(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.w("PTT", "IconSyncWorker", e)
            Result.retry()
        }
    }
}
