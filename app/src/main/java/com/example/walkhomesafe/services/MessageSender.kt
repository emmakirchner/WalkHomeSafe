package com.example.walkhomesafe.services

import android.content.Context
import android.telephony.SmsManager
import com.example.walkhomesafe.model.EmergencyContact

class MessageSender(
    private val context: Context
) {
    fun send(
        contacts: List<EmergencyContact>,
        message: String
    ) {
        val smsManager = context.getSystemService(SmsManager::class.java)

        contacts.forEach { contact ->
            smsManager.sendTextMessage(
                contact.phone,
                null,
                message,
                null,
                null
            )
        }
    }
}