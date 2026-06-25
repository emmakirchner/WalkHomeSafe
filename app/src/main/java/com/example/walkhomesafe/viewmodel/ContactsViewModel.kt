package com.example.walkhomesafe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.data.emergencyContactsFlow
import com.example.walkhomesafe.data.saveEmergencyContacts
import com.example.walkhomesafe.model.EmergencyContact
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the emergency contacts list.
 * Observes persisted contacts from DataStore and handles add/delete operations.
 *
 * @property launchContactPicker SharedFlow that emits when the contact picker should open
 * @property contacts StateFlow of the current emergency contact list
 */
class ContactsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    private val _launchContactPicker = MutableSharedFlow<Unit>()

    val launchContactPicker = _launchContactPicker.asSharedFlow()
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    init {
        viewModelScope.launch {
            emergencyContactsFlow(application).collect { storedContacts ->
                _contacts.value = storedContacts
            }
        }
    }

    /**
     * Called when the READ_CONTACTS permission has been granted;
     * emits a signal to open the system contact picker.
     */
    fun onReadContactsGranted() {
        viewModelScope.launch {
            _launchContactPicker.emit(Unit)
        }
    }

    /**
     * Adds a new emergency contact if the phone number does not already exist in the list.
     *
     * @param name Display name of the contact
     * @param phone Phone number of the contact
     */
    fun addContact(name: String, phone: String) {
        if (_contacts.value.any { it.phone == phone }) return

        val contact = EmergencyContact(
            id = System.currentTimeMillis(),
            name = name,
            phone = phone
        )
        val updated = _contacts.value + contact
        _contacts.value = updated
        save(updated)
    }


    /**
     * Removes a contact from the emergency contacts list.
     *
     * @param contact The contact to remove
     */
    fun deleteContact(contact: EmergencyContact) {
        val updated =
            _contacts.value.filterNot { it.id == contact.id }

        _contacts.value = updated

        save(updated)
    }

    /**
     * Persists the contact list to DataStore.
     *
     * @param contacts The list of contacts to save
     */
    private fun save(contacts: List<EmergencyContact>) {
        viewModelScope.launch {
            saveEmergencyContacts(
                context = getApplication(),
                contacts = contacts
            )
        }
    }
}
