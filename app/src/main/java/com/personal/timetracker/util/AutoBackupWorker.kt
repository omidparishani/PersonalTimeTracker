package com.personal.timetracker.util

/**
 * توضیح فایل: Worker بکاپ خودکار دوره‌ای.
 * بسته: com.personal.timetracker.util
 * زبان توضیحات: فارسی — برای توسعه‌دهنده جاواکار.
 */

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personal.timetracker.data.db.AppDatabase
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker بکاپ خودکار دوره‌ای.
 */
class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val file = BackupHelper.exportJson(applicationContext)
            val settings = AppDatabase.get(applicationContext).settingsDao().get()
            val custom = settings?.autoBackupDir?.trim().orEmpty()
            val destDir = if (custom.isNotBlank()) {
                File(custom).apply { mkdirs() }
            } else {
                File(applicationContext.getExternalFilesDir(null), "PTT_Backups").apply { mkdirs() }
            }
            val destFile = File(destDir, file.name)
            file.copyTo(destFile, overwrite = true)
            NotifHelper.show(
                applicationContext,
                "پشتیبان‌گیری خودکار",
                "پشتیبان در ${destFile.absolutePath} ذخیره شد"
            )
            Log.i("PTT", "Auto backup saved: ${destFile.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e("PTT", "Auto backup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ptt_auto_backup"

        fun schedule(ctx: Context, enabled: Boolean, intervalHours: Int) {
            val wm = WorkManager.getInstance(ctx)
            if (!enabled || intervalHours <= 0) {
                wm.cancelUniqueWork(WORK_NAME)
                Log.i("PTT", "Auto backup cancelled")
                return
            }
            val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours.toLong().coerceAtLeast(1),
                TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
            Log.i("PTT", "Auto backup scheduled every ${intervalHours}h")
        }
    }
}
