package com.personal.timetracker.license

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * ذخیره محلی لایسنس و شناسه دستگاه.
 */
object LicenseStore {
    private const val PREF = "ptt_license"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_ACTIVATED = "activated"

    /** آدرس سرور مدیریت لایسنس — بعد از دیپلوی Vercel عوض کنید */
    const val DEFAULT_BASE_URL = "https://YOUR-PROJECT.vercel.app"

    fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isActivated(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ACTIVATED, false) &&
            !prefs(ctx).getString(KEY_LICENSE, null).isNullOrBlank()

    fun licenseKey(ctx: Context): String? = prefs(ctx).getString(KEY_LICENSE, null)

    fun saveLicense(ctx: Context, licenseKey: String) {
        prefs(ctx).edit()
            .putString(KEY_LICENSE, licenseKey)
            .putBoolean(KEY_ACTIVATED, true)
            .apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    fun deviceId(ctx: Context): String {
        return Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }

    fun deviceModel(): String = Build.MODEL ?: ""
    fun deviceBrand(): String = Build.BRAND ?: ""
    fun androidVersion(): String = Build.VERSION.RELEASE ?: ""

    fun baseUrl(ctx: Context): String {
        val custom = prefs(ctx).getString("base_url", null)?.trim()
        return if (!custom.isNullOrBlank()) custom.trimEnd('/')
        else DEFAULT_BASE_URL.trimEnd('/')
    }

    fun setBaseUrl(ctx: Context, url: String) {
        prefs(ctx).edit().putString("base_url", url.trim().trimEnd('/')).apply()
    }
}
