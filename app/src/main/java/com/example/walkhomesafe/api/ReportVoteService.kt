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
 * Service object for voting operations on safety reports via the REST API.
 */
object ReportVoteService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Retrieves a Firebase ID token for the current user.
     *
     * @return The ID token string, or null if no user is authenticated
     */
    private suspend fun getIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            user.getIdToken(true).await().token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets all votes cast by the currently authenticated user.
     *
     * @return List of the user's votes, empty list on error
     */
    suspend fun getMyVotes(): List<ReportVoteDto> = withContext(Dispatchers.IO) {
        val idToken = getIdToken() ?: return@withContext emptyList()
        val request = Request.Builder()
            .url("$BASE_URL/api/report-votes/by-user")
            .get()
            .addHeader("Authorization", "Bearer $idToken")
            .build()
        try {
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string() ?: return@use emptyList()
                json.decodeFromString<List<ReportVoteDto>>(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Casts votes on reports (upvote, downvote, or vote removal).
     *
     * @param votes List of votes to submit
     * @return true if the vote operation was successful, false otherwise
     */
    suspend fun vote(votes: List<SaveReportVoteDto>): Boolean = withContext(Dispatchers.IO) {
        val idToken = getIdToken() ?: return@withContext false
        val body = json.encodeToString(votes).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/api/report-votes")
            .put(body)
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
