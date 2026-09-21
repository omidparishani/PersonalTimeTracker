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
            val msgs = buildList {
                err.errorMessages?.let { addAll(it) }
                err.errors?.values?.let { addAll(it) }
            }
            msgs.joinToString("\n").ifBlank { body.take(300) }
        } catch (_: Exception) {
            body.take(300)
        }
    }
}
