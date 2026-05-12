package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.walkhomesafe.services.MessageSender
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.services.EmergencyAlarmService

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val messageSender = MessageSender(context)

    fun onSendMessage(
        contacts: List<EmergencyContact>,
        message: String
    ) {
        messageSender.send(
            contacts = contacts,
            message = message
        )
    }

    fun onSendMessageAndAlarm(
        contacts: List<EmergencyContact>,
        message: String
    ) {
        onSendMessage(contacts, message)
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