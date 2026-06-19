package com.example.walkhomesafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ReportVoteService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun vote(votes: List<SaveReportVoteDto>): Boolean = withContext(Dispatchers.IO) {
        val body = json.encodeToString(votes).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/api/report-votes")
            .put(body)
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
