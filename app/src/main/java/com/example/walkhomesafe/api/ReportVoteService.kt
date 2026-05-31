package com.example.walkhomesafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ReportVoteService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun vote(votes: List<SaveReportVoteDto>): Boolean = withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL/api/report-votes").openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            val body = json.encodeToString(votes)
            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
