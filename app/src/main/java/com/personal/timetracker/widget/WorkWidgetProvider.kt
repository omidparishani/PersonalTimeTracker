package com.personal.timetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.personal.timetracker.R
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.TimeCalc
import com.personal.timetracker.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(context)
            val settings = db.settingsDao().get()
            val active = db.attendanceDao().getActive()
            val status: String
            val detail: String
            if (active != null && active.exitTime == null) {
                status = "در حال کار"
                val end = if (settings != null)
                    TimeCalc.suggestedEnd(active.entryTime, settings.minimumWorkMinutes)
                else "—"
                detail = "ورود ${active.entryTime} | خروج پیشنهادی $end"
            } else {
                status = "خارج از کار"
                detail = TimeUtils.toJalaliShort()
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_work)
                views.setTextViewText(R.id.widgetStatus, status)
                views.setTextViewText(R.id.widgetDetail, detail)
                views.setOnClickPendingIntent(R.id.widgetRoot, open)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
