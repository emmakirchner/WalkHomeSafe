package com.example.walkhomesafe.helper

import android.Manifest
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.walkhomesafe.model.EmergencyContact
import android.telephony.SmsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class ContactHelper(private val context: Context) {

    fun readContact(uri: Uri, onResult: (String, String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cursor = context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )

        if (cursor?.moveToFirst() == true) {
            val id = cursor.getString(0)
            val name = cursor.getString(1)
            cursor.close()
            readPhoneNumber(id, name, onResult)
        }
    }

    private fun readPhoneNumber(
        contactId: String,
        name: String,
        onResult: (String, String) -> Unit
    ) {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        if (cursor?.moveToFirst() == true) {
            val number = cursor.getString(0)
            onResult(name, number)
        }

        cursor?.close()
    }

    fun sendEmergencyMessage(
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
