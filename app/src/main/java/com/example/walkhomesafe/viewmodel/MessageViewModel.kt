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

    private companion object {
        private const val LOCATION_PLACEHOLDER = "[STANDORT-LINK]"
        private const val MAX_MESSAGE_LENGTH = 110
    }

    private val DEFAULT_MESSAGE =
        "NOTFALL! Ich bin hier: [STANDORT-LINK]. Bitte schaut sofort nach mir. Automatisierte Nachricht."

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
        val parts = newMessage.split(LOCATION_PLACEHOLDER)
        val placeholderCount = parts.size - 1
        val effectiveLength = newMessage.length - placeholderCount * LOCATION_PLACEHOLDER.length
        val truncated = if (effectiveLength <= MAX_MESSAGE_LENGTH) {
            newMessage
        } else if (placeholderCount == 0) {
            newMessage.take(MAX_MESSAGE_LENGTH)
        } else {
            val overflow = effectiveLength - MAX_MESSAGE_LENGTH
            val trimmedLast = parts.last().dropLast(overflow.coerceAtMost(parts.last().length))
            parts.dropLast(1).joinToString(LOCATION_PLACEHOLDER) +
                LOCATION_PLACEHOLDER + trimmedLast
        }
        _message.value = truncated

        viewModelScope.launch {
            saveEmergencyMessage(
                context = getApplication(),
                message = truncated
            )
        }
    }
}