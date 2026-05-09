package com.example.walkhomesafe.helper

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.walkhomesafe.model.EmergencyContact
import android.telephony.SmsManager
import android.content.Context

class ContactHelper(
    private val context: Context,
    activity: ComponentActivity,
    private val onContactPicked: (name: String, number: String) -> Unit
) {

    private val launcher: ActivityResultLauncher<Void?> =
        activity.registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let { readContact(activity, it) }
        }

    fun launch() {
        launcher.launch(null)
    }

    private fun readContact(activity: ComponentActivity, uri: Uri) {
        val cursor = activity.contentResolver.query(
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
            readPhoneNumber(activity, id, name)
        }
    }

    private fun readPhoneNumber(
        activity: ComponentActivity,
        contactId: String,
        name: String
    ) {
        val cursor = activity.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        if (cursor?.moveToFirst() == true) {
            val number = cursor.getString(0)
            onContactPicked(name, number)
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
