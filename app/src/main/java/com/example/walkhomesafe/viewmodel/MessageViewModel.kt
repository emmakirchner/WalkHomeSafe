package com.example.walkhomesafe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.data.emergencyContactsFlow
import com.example.walkhomesafe.data.emergencyMessageFlow
import com.example.walkhomesafe.data.saveEmergencyContacts
import com.example.walkhomesafe.data.saveEmergencyMessage
import com.example.walkhomesafe.model.EmergencyContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessageViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val DEFAULT_MESSAGE =
        "NOTFALL: Ich brauche Hilfe! Ich bin hier: [STANDORT-LINK]. Bitte schaut sofort nach mir. Dies ist eine automatische Nachricht von WalkHomeSafe."

    private val _message = MutableStateFlow(DEFAULT_MESSAGE)
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            emergencyMessageFlow(application).collect { storedMessage ->
                _message.value = storedMessage ?: DEFAULT_MESSAGE
            }
        }
    }

    fun updateMessage(newMessage: String) {
        _message.value = newMessage

        viewModelScope.launch {
            saveEmergencyMessage(
                context = getApplication(),
                message = newMessage
            )
        }
    }
}