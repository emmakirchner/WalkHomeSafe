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

/**
 * Service object for CRUD operations on safety reports via the REST API.
 */
object ReportService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Creates a new safety report.
     *
     * @param request The report data to create
     * @return HTTP status code on success, null on failure
     */
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

    /**
     * Gets safety reports, optionally filtered by location and radius.
     *
     * @param latitude Optional center latitude for spatial search
     * @param longitude Optional center longitude for spatial search
     * @param radiusInMeters Optional search radius in meters
     * @return List of matching reports, empty list on error
     */
    suspend fun get(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusInMeters: Int? = null
    ): List<ReportDto> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$BASE_URL/api/reports")
            val params = mutableListOf<String>()
            latitude?.let { params.add("Latitude=$it") }
            longitude?.let { params.add("Longitude=$it") }
            radiusInMeters?.let { params.add("RadiusInMeters=$it") }
            if (params.isNotEmpty()) {
                append("?").append(params.joinToString("&"))
            }
        }
        val request = Request.Builder()
            .url(url)
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

    /**
     * Gets all reports created by the currently authenticated user.
     *
     * @return List of the user's reports, empty list on error
     */
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

    /**
     * Updates an existing safety report.
     *
     * @param id The ID of the report to update
     * @param request The updated report data
     * @return HTTP status code on success, null on failure
     */
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

    /**
     * Deletes a safety report by its ID.
     *
     * @param id The ID of the report to delete
     * @return true if deletion was successful, false otherwise
     */
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
