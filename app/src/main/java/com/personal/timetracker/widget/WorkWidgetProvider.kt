package com.personal.timetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.personal.timetracker.App
import com.personal.timetracker.R
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Functional home-screen widget: shows current status + today's worked time, and lets
 * the person check in / check out directly from the home screen without opening the app.
 */
class WorkWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CHECK_IN = "com.personal.timetracker.widget.CHECK_IN"
        const val ACTION_CHECK_OUT = "com.personal.timetracker.widget.CHECK_OUT"

        /** Call after any attendance change in the app so widgets stay in sync. */
        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WorkWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, WorkWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK_IN, ACTION_CHECK_OUT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = (context.applicationContext as App).repository
                        if (intent.action == ACTION_CHECK_IN) repo.checkIn() else repo.checkOut()
                    } catch (_: Exception) {
                    } finally {
                        requestUpdate(context)
                        pending.finish()
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(context)
            val settings = db.settingsDao().get()
            val active = db.attendanceDao().getActive()
            val today = db.attendanceDao().getByDateOnce(TimeUtils.today())
            val status: String
            val detail: String
            val isWorking = active != null && active.exitTime == null
            if (isWorking) {
                status = "▶ در حال کار"
                val end = if (settings != null)
                    TimeCalc.applyFlex(
                        active!!.entryTime, settings.startWorkTime, settings.endWorkTime, settings.flexibleMinutes
                    ).suggestedEnd
                else "—"
                detail = "ورود ${active!!.entryTime}  ·  پایان پیشنهادی $end"
            } else {
                status = "■ خارج از کار"
                detail = TimeUtils.toJalaliShort()
            }
            val workedToday = today.sumOf {
                if (it.exitTime != null) it.duration
                else if (it.status == "active") TimeUtils.minutesBetween(it.entryTime, TimeUtils.nowTime()).coerceAtLeast(0)
                else 0
            }

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val checkInPi = PendingIntent.getBroadcast(
                context, 1, Intent(context, WorkWidgetProvider::class.java).setAction(ACTION_CHECK_IN),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val checkOutPi = PendingIntent.getBroadcast(
                context, 2, Intent(context, WorkWidgetProvider::class.java).setAction(ACTION_CHECK_OUT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_work)
                views.setTextViewText(R.id.widgetStatus, status)
                views.setTextViewText(R.id.widgetDetail, detail)
                views.setTextViewText(R.id.widgetWorkedToday, "امروز: ${TimeUtils.formatDuration(workedToday)}")
                views.setOnClickPendingIntent(R.id.widgetRoot, open)
                views.setTextViewText(R.id.widgetCheckIn, "ورود")
                views.setTextViewText(R.id.widgetCheckOut, "خروج")
                views.setFloat(R.id.widgetCheckIn, "setAlpha", if (isWorking) 0.5f else 1f)
                views.setFloat(R.id.widgetCheckOut, "setAlpha", if (isWorking) 1f else 0.5f)
                views.setOnClickPendingIntent(R.id.widgetCheckIn, checkInPi)
                views.setOnClickPendingIntent(R.id.widgetCheckOut, checkOutPi)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
