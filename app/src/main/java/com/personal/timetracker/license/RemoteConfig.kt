package com.personal.timetracker.license

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * تنظیمات و دسترسی‌های دریافتی از سرور لایسنس — کش محلی.
 */
object RemoteConfig {
    private const val PREF = "ptt_remote_config"

    fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun saveFromValidate(ctx: Context, config: JSONObject?) {
        if (config == null) return
        val perms = config.optJSONObject("permissions")
        val work = config.optJSONObject("work")
        val e = prefs(ctx).edit()
        if (perms != null) {
            e.putBoolean("can_jira", perms.optBoolean("jira", true))
            e.putBoolean("can_attendance", perms.optBoolean("attendance", true))
            e.putBoolean("can_reports", perms.optBoolean("reports", true))
            e.putBoolean("can_support", perms.optBoolean("support", true))
        }
        if (work != null) {
            e.putString("start_work", work.optString("startWorkTime", "09:00"))
            e.putString("end_work", work.optString("endWorkTime", "17:00"))
            e.putInt("flexible", work.optInt("flexibleMinutes", 30))
            e.putInt("min_daily", work.optInt("minimumWorkMinutes", 480))
            e.putInt("weekly", work.optInt("weeklyRequiredMinutes", 2775))
            e.putBoolean("thu_working", work.optBoolean("thursdayWorking", false))
            e.putInt("thu_minutes", work.optInt("thursdayMinutes", 300))
            val hol = work.optJSONArray("holidays") ?: JSONArray()
            e.putString("holidays", hol.toString())
        }
        e.putLong("synced_at", System.currentTimeMillis())
        e.apply()
    }

    fun canJira(ctx: Context) = prefs(ctx).getBoolean("can_jira", true)
    fun canAttendance(ctx: Context) = prefs(ctx).getBoolean("can_attendance", true)
    fun canReports(ctx: Context) = prefs(ctx).getBoolean("can_reports", true)
    fun canSupport(ctx: Context) = prefs(ctx).getBoolean("can_support", true)

    fun startWork(ctx: Context) = prefs(ctx).getString("start_work", "09:00") ?: "09:00"
    fun endWork(ctx: Context) = prefs(ctx).getString("end_work", "17:00") ?: "17:00"
    fun flexible(ctx: Context) = prefs(ctx).getInt("flexible", 30)
    fun minDaily(ctx: Context) = prefs(ctx).getInt("min_daily", 480)
    fun weekly(ctx: Context) = prefs(ctx).getInt("weekly", 2775)
    fun thuWorking(ctx: Context) = prefs(ctx).getBoolean("thu_working", false)
    fun thuMinutes(ctx: Context) = prefs(ctx).getInt("thu_minutes", 300)
    fun holidays(ctx: Context): List<String> {
        return try {
            val a = JSONArray(prefs(ctx).getString("holidays", "[]"))
            (0 until a.length()).map { a.getString(it) }
        } catch (_: Exception) { emptyList() }
    }
}
