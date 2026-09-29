package com.personal.timetracker.jira

/**
 * کلاینت Retrofit/OkHttp با پشتیبانی از:
 * - Bearer Token (PAT)
 * - Basic Auth (نام کاربری + رمز)
 * - Session Cookie از /rest/auth/1/session (مناسب Jira Server شرکت)
 */

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object JiraClient {

    private val gson: Gson = GsonBuilder().setLenient().create()

    /** CookieJar ساده در حافظه برای JSESSIONID */
    private class MemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            store[url.host].orEmpty()
        fun clear() = store.clear()
    }

    /**
     * @param token اگر پر باشد Bearer
     * @param username/password برای Basic و Session
     */
    fun create(
        baseUrl: String,
        token: String = "",
        username: String = "",
        password: String = ""
    ): JiraApi {
        val normalized = baseUrl.trim().trimEnd('/') + "/"
        val user = username.trim()
        val pass = password
        val pat = token.trim()
        val cookieJar = MemoryCookieJar()

        // Session login برای Jira Server (اگر user/pass داریم)
        if (pat.isBlank() && user.isNotBlank() && pass.isNotBlank()) {
            try {
                establishSession(normalized, user, pass, cookieJar)
            } catch (_: Exception) {
                // اگر session شکست خورد، Basic Auth کافی است
            }
        }

        val authHeader: String = when {
            pat.isNotBlank() -> "Bearer $pat"
            user.isNotBlank() && pass.isNotBlank() -> {
                val raw = "$user:$pass"
                val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "Basic $encoded"
            }
            else -> ""
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val b = original.newBuilder()
                .header("Accept", "application/json")
                .header("X-Atlassian-Token", "no-check")
            // Content-Type فقط وقتی body داریم
            if (original.body != null) {
                b.header("Content-Type", "application/json")
            }
            if (authHeader.isNotBlank()) {
                b.header("Authorization", authHeader)
            }
            var resp = chain.proceed(b.build())
            // اگر 401 و user/pass داریم، یک‌بار session را دوباره بساز
            if (resp.code == 401 && pat.isBlank() && user.isNotBlank() && pass.isNotBlank()) {
                resp.close()
                try {
                    cookieJar.clear()
                    establishSession(normalized, user, pass, cookieJar)
                } catch (_: Exception) { }
                resp = chain.proceed(b.build())
            }
            resp
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(JiraApi::class.java)
    }

    /**
     * لاگین session استاندارد Jira Server.
     * POST /rest/auth/1/session  {"username","password"}
     */
    private fun establishSession(
        baseUrl: String,
        username: String,
        password: String,
        cookieJar: MemoryCookieJar
    ) {
        val url = baseUrl.trimEnd('/') + "/rest/auth/1/session"
        val bodyJson = JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
        }
        val media = "application/json; charset=utf-8".toMediaType()
        val body = bodyJson.toString().toRequestBody(media)
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .build()
        val req = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Atlassian-Token", "no-check")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string().orEmpty()
                throw Exception("لاگین session ناموفق (${resp.code}): ${err.take(200)}")
            }
        }
    }

    fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "خطای ناشناخته از سرور جیرا"
        return try {
            val err = gson.fromJson(body, JiraError::class.java)
            val parts = mutableListOf<String>()
            err.errorMessages?.forEach { parts.add(it) }
            err.errors?.forEach { (k, msg) -> parts.add("$k: $msg") }
            parts.joinToString("\n").ifBlank { body.take(400) }
        } catch (_: Exception) {
            body.take(400)
        }
    }
}
