package com.example.walkhomesafe.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ReportCategoryService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAll(): List<ReportCategoryDto> = withContext(Dispatchers.IO) {
        val connection = URL("$BASE_URL/api/report-categories").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            val response = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<List<ReportCategoryDto>>(response)
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
