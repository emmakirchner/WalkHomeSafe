package com.example.walkhomesafe.services

import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UserService {

    private const val BASE_URL = "https://walkhomesafe-frfgcrdtfkaqg3cd.germanywestcentral-01.azurewebsites.net"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun deleteUser(onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(false, "Kein Benutzer angemeldet")
            return
        }

        user.getIdToken(false)
            .addOnCompleteListener { tokenTask ->
                if (!tokenTask.isSuccessful || tokenTask.result?.token == null) {
                    onResult(false, "Kein ID-Token verf\u00fcgbar")
                    return@addOnCompleteListener
                }

                val idToken = tokenTask.result!!.token!!
                val uid = user.uid

                val json = JSONObject().apply {
                    put("uid", uid)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/api/users")
                    .delete(body)
                    .addHeader("Authorization", "Bearer $idToken")
                    .build()

                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        onResult(false, e.message ?: "Netzwerkfehler")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use {
                            onResult(it.isSuccessful, if (it.isSuccessful) null else "API-Fehler: ${it.code}")
                        }
                    }
                })
            }
    }
}
