package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.walkhomesafe.services.MessageSender
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.services.EmergencyAlarmService
import com.google.android.gms.maps.model.LatLng

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val messageSender = MessageSender(context)

    private companion object {
        private const val LOCATION_PLACEHOLDER = "[STANDORT-LINK]"
    }

    fun onSendMessage(
        contacts: List<EmergencyContact>,
        message: String,
        locationLatLng: LatLng? = null
    ) {
        val finalMessage = if (locationLatLng != null) {
            val link = "https://maps.google.com/?q=${locationLatLng.latitude},${locationLatLng.longitude}"
            if (message.contains(LOCATION_PLACEHOLDER)) {
                message.replace(LOCATION_PLACEHOLDER, link)
            } else {
                "$message\n$link"
            }
        } else {
            message.replace(LOCATION_PLACEHOLDER, "")
        }
        messageSender.send(contacts, finalMessage)
    }

    fun startAlarm() {
        startAlarmService()
    }

    fun onCancelAlarm() {
        stopAlarmService()
    }

    private fun startAlarmService() {
        val intent = Intent(context, EmergencyAlarmService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopAlarmService() {
        val intent = Intent(context, EmergencyAlarmService::class.java)
        context.stopService(intent)
    }
}