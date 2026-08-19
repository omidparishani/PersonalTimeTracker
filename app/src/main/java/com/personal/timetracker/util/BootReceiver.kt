package com.personal.timetracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.personal.timetracker.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
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
