package com.example.walkhomesafe.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Helper class for reading contact details (name and phone number) from the device contacts provider.
 *
 * @param context Context for accessing the content resolver
 */
class ContactHelper(
    private val context: Context
) {
    /**
     * Reads a contact's name and phone number from a contact picker URI.
     * Requires the READ_CONTACTS permission.
     *
     * @param uri The contact URI from the picker
     * @param onResult Callback with (name, phoneNumber) on success
     */
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

    /**
     * Queries the phone number for a given contact ID and invokes the callback.
     *
     * @param contactId The contact's database ID
     * @param name The contact's display name (passed through to the callback)
     * @param onResult Callback with (name, phoneNumber)
     */
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
}