package com.example.walkhomesafe.services

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.walkhomesafe.model.EmergencyContact

class MessageSender(
    private val context: Context
) {
    fun send(
        contacts: List<EmergencyContact>,
        message: String
    ) {
        val smsManager = context.getSystemService(SmsManager::class.java)
            ?: run {
                Log.w(TAG, "SmsManager not available (no telephony on this device)")
                return
            }

        contacts.forEach { contact ->
            try {
                smsManager.sendTextMessage(
                    contact.phone,
                    null,
                    message,
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to ${contact.phone}", e)
            }
        }
    }

    companion object {
        private const val TAG = "MessageSender"
    }
}