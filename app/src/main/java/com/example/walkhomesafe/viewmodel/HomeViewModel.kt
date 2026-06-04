package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.walkhomesafe.services.AlarmState
import com.example.walkhomesafe.services.MessageSender
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.services.EmergencyAlarmService
import com.example.walkhomesafe.viewmodel.LOCATION_PLACEHOLDER
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val messageSender = MessageSender(context)

    val isAlarmActive: StateFlow<Boolean> = AlarmState.isActive

    fun onSendMessage(
        contacts: List<EmergencyContact>,
        message: String,
        locationLatLng: LatLng? = null
    ) {
        val finalMessage = if (locationLatLng != null) {
            val link = "https://maps.google.com/?q=${locationLatLng.latitude},${locationLatLng.longitude}"
            if (message.contains(LOCATION_PLACEHOLDER)) {
                message.replaceFirst(LOCATION_PLACEHOLDER, link)
            } else {
                "$message\n$link"
            }
        } else {
            message.replaceFirst(LOCATION_PLACEHOLDER, "")
        }
        messageSender.send(contacts, finalMessage)
    }

    fun startAlarmService() {
        AlarmState.setActive(true)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopAlarmService() {
        AlarmState.setActive(false)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        context.stopService(intent)
    }
}