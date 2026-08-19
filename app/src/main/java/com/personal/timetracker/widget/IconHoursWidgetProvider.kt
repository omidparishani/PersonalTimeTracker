package com.personal.timetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.personal.timetracker.R
import com.personal.timetracker.ui.MainActivity
import com.personal.timetracker.util.DynamicAppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 1×1 home-screen icon that shows today's worked hours in Persian digits. */
class IconHoursWidgetProvider : AppWidgetProvider() {

    companion object {
        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, IconHoursWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, IconHoursWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val minutes = try {
                DynamicAppIcon.todayWorkedMinutes(context)
            } catch (_: Exception) {
                0
            }
            val bmp = DynamicAppIcon.hoursBitmap(context, minutes)
            val open = PendingIntent.getActivity(
                context, 40, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_icon_hours)
                views.setImageViewBitmap(R.id.iconHoursImage, bmp)
                views.setOnClickPendingIntent(R.id.iconHoursRoot, open)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
