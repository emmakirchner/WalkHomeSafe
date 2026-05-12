package com.example.walkhomesafe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.data.emergencyContactsFlow
import com.example.walkhomesafe.data.saveEmergencyContacts
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.presentation.permissions.PermissionIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun onReadContactsGranted() {
        viewModelScope.launch {
            _launchContactPicker.emit(Unit)
        }
    }

    fun addContact(name: String, phone: String) {
        val contact = EmergencyContact(
            id = System.currentTimeMillis(),
            name = name,
            phone = phone
        )

        val updated = _contacts.value + contact
        _contacts.value = updated
        save(updated)
    }


    fun deleteContact(contact: EmergencyContact) {
        val updated =
            _contacts.value.filterNot { it.id == contact.id }

        _contacts.value = updated

        save(updated)
    }

    private fun save(contacts: List<EmergencyContact>) {
        viewModelScope.launch {
            saveEmergencyContacts(
                context = getApplication(),
                contacts = contacts
            )
        }
    }
}
