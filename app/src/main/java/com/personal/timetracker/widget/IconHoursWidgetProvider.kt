package com.personal.timetracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** ویجت غیرفعال — نگه داشته شده فقط برای سازگاری کامپایل. */
class IconHoursWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // no-op
    }

    companion object {
        fun requestUpdate(@Suppress("UNUSED_PARAMETER") context: Context) { /* no-op */ }
    }
}
