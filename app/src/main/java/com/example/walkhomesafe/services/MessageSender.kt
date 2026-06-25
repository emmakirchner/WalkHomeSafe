package com.example.walkhomesafe.services

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.walkhomesafe.model.EmergencyContact

/**
 * Utility class for sending SMS messages to emergency contacts.
 *
 * @param context Context for accessing the SmsManager system service
 */
class MessageSender(
    private val context: Context
) {
    /**
     * Sends an SMS to each contact in the provided list.
     *
     * @param contacts List of emergency contacts to notify
     * @param message The SMS text to send
     */
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