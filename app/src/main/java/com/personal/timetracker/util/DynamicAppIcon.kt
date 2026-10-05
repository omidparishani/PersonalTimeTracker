package com.personal.timetracker.util

import android.content.Context

/**
 * غیرفعال — آیکن لانچر دیگر بر اساس ساعت کار تغییر نمی‌کند.
 */
object DynamicAppIcon {
    @Suppress("UNUSED_PARAMETER")
    fun sync(context: Context) { /* no-op */ }

    @Suppress("UNUSED_PARAMETER")
    suspend fun syncNow(context: Context) { /* no-op */ }

    @Suppress("UNUSED_PARAMETER")
    fun schedule(context: Context) { /* no-op */ }
}
