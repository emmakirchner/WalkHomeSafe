package com.example.walkhomesafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ReportService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun create(request: SaveReportDto): ReportDto? = withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL/api/reports").openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            val body = json.encodeToString(request)
            OutputStreamWriter(connection.outputStream).use { it.write(body) }
            val response = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<ReportDto>(response)
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun get(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusInMeters: Int? = null
    ): List<ReportDto> = withContext(Dispatchers.IO) {
        val params = mutableListOf<String>()
        latitude?.let { params.add("Latitude=$it") }
        longitude?.let { params.add("Longitude=$it") }
        radiusInMeters?.let { params.add("RadiusInMeters=$it") }
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val connection = URL("$BASE_URL/api/reports$query").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            val response = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<List<ReportDto>>(response)
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    suspend fun delete(id: Int): Boolean = withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL/api/reports/$id").openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
