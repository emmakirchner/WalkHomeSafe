package com.example.walkhomesafe.api

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ReportService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun create(request: SaveReportDto): Int? = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext null
        val idToken = try {
            user.getIdToken(true).await().token ?: return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$BASE_URL/api/reports")
            .put(body)
            .addHeader("Authorization", "Bearer $idToken")
            .build()
        try {
            val response = client.newCall(req).execute()
            response.use { it.code }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun get(): List<ReportDto> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/api/reports")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<ReportDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getForCurrentUser(): List<ReportDto> = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext emptyList()
        val idToken = try {
            user.getIdToken(true).await().token ?: return@withContext emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/reports/by-user")
            .get()
            .addHeader("Authorization", "Bearer $idToken")
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<ReportDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun update(id: Int, request: SaveReportDto): Int? = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext null
        val idToken = try {
            user.getIdToken(true).await().token ?: return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$BASE_URL/api/reports?id=$id")
            .put(body)
            .addHeader("Authorization", "Bearer $idToken")
            .build()
        try {
            val response = client.newCall(req).execute()
            response.use { it.code }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete(id: Int): Boolean = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext false
        val idToken = try {
            user.getIdToken(true).await().token ?: return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/reports/$id")
            .delete()
            .addHeader("Authorization", "Bearer $idToken")
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
