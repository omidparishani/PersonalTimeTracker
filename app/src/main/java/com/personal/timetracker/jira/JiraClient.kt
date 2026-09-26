package com.personal.timetracker.jira

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a ready-to-use [JiraApi] instance for a given base URL + PAT.
 * Token is never logged.
 */
object JiraClient {

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun create(baseUrl: String, token: String): JiraApi {
        val normalized = baseUrl.trim().trimEnd('/') + "/"

        val authInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            // BODY would leak the token in Authorization header — keep at BASIC
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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

    /** Human-readable error from a failed response body. */
    fun parseError(body: String?): String {
        if (body.isNullOrBlank()) return "خطای ناشناخته از سرور جیرا"
        return try {
            val err = gson.fromJson(body, JiraError::class.java)
            val fieldNames = mapOf(
                "summary" to "عنوان (Summary)",
                "description" to "توضیحات",
                "assignee" to "اساینی",
                "priority" to "اولویت",
                "components" to "Component",
                "duedate" to "تاریخ سررسید",
                "issuetype" to "نوع Issue",
                "project" to "پروژه",
                "customfield_12900" to "ActivityType",
                "customfield_13600" to "DemisCustomer",
                "customfield_13601" to "BudgetType",
                "customfield_10815" to "تاریخ شروع",
                "customfield_10816" to "تاریخ پایان",
                "timetracking" to "تخمین زمان",
                "labels" to "برچسب‌ها"
            )
            val parts = mutableListOf<String>()
            err.errorMessages?.forEach { parts.add(it) }
            err.errors?.forEach { (field, msg) ->
                val label = fieldNames[field] ?: field
                val tip = when {
                    msg.contains("is required", true) || msg.contains("required", true) ->
                        "«$label» الزامی است — مقدار را وارد یا انتخاب کنید."
                    msg.contains("is invalid", true) || msg.contains("invalid", true) ->
                        "مقدار «$label» نامعتبر است: $msg"
                    msg.contains("does not exist", true) ->
                        "«$label» در این پروژه/نوع تعریف نشده."
                    else -> "$label: $msg"
                }
                parts.add(tip)
            }
            parts.joinToString("\n").ifBlank { body.take(400) }
        } catch (_: Exception) {
            body.take(400)
        }
    }
}
