package com.example.walkhomesafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ReportCategoryService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getAll(): List<ReportCategoryDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/report-categories")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<ReportCategoryDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
