package com.personal.timetracker.license

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * کلاینت HTTP برای فعال‌سازی و اعتبارسنجی لایسنس.
 */
object LicenseApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class ActivateResult(val ok: Boolean, val licenseKey: String? = null, val error: String? = null)
    data class ValidateResult(val ok: Boolean, val active: Boolean, val reason: String? = null, val message: String? = null)

    suspend fun activate(
        ctx: Context,
        code: String,
        username: String? = null,
        appVersion: String? = null
    ): ActivateResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("code", code.trim())
                put("deviceId", LicenseStore.deviceId(ctx))
                put("deviceModel", LicenseStore.deviceModel())
                put("deviceBrand", LicenseStore.deviceBrand())
                put("androidVersion", LicenseStore.androidVersion())
                if (!appVersion.isNullOrBlank()) put("appVersion", appVersion)
                if (!username.isNullOrBlank()) put("username", username)
            }
            val req = Request.Builder()
                .url("${LicenseStore.baseUrl(ctx)}/api/v1/activate")
                .post(body.toString().toRequestBody(jsonMedia))
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (json?.optBoolean("ok") == true) {
                    val key = json.optString("licenseKey")
                    if (key.isNotBlank()) {
                        LicenseStore.saveLicense(ctx, key)
                        return@withContext ActivateResult(true, key)
                    }
                }
                val err = json?.optString("error")?.ifBlank { null }
                    ?: "فعال‌سازی ناموفق (${resp.code})"
                ActivateResult(false, error = err)
            }
        } catch (e: Exception) {
            ActivateResult(false, error = e.message ?: "خطای شبکه")
        }
    }

    suspend fun validate(
        ctx: Context,
        username: String? = null,
        appVersion: String? = null
    ): ValidateResult = withContext(Dispatchers.IO) {
        val key = LicenseStore.licenseKey(ctx)
            ?: return@withContext ValidateResult(false, active = false, reason = "no_license")
        try {
            val body = JSONObject().apply {
                put("licenseKey", key)
                put("deviceId", LicenseStore.deviceId(ctx))
                if (!username.isNullOrBlank()) put("username", username)
                if (!appVersion.isNullOrBlank()) put("appVersion", appVersion)
            }
            val req = Request.Builder()
                .url("${LicenseStore.baseUrl(ctx)}/api/v1/validate")
                .post(body.toString().toRequestBody(jsonMedia))
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (json == null) {
                    // آفلاین: اگر قبلاً فعال بوده، موقتاً اجازه بده
                    return@withContext ValidateResult(true, active = true, reason = "offline")
                }
                val active = json.optBoolean("active", false)
                if (active) {
                    RemoteConfig.saveFromValidate(ctx, json.optJSONObject("config"))
                }
                ValidateResult(
                    ok = json.optBoolean("ok", false),
                    active = active,
                    reason = json.optString("reason").ifBlank { null },
                    message = json.optString("message").ifBlank { null }
                )
            }
        } catch (_: Exception) {
            // بدون اینترنت: اجازه استفاده با لایسنس ذخیره‌شده
            ValidateResult(true, active = true, reason = "offline")
        }
    }
}
