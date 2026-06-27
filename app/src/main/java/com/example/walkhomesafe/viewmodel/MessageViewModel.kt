package com.example.walkhomesafe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.data.emergencyMessageFlow
import com.example.walkhomesafe.data.saveEmergencyMessage
import com.example.walkhomesafe.model.EmergencyContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Placeholder string in the emergency message that gets replaced with a location link. */
const val LOCATION_PLACEHOLDER = "[STANDORT-LINK]"
/** Maximum allowed length for the emergency message text. */
const val MAX_MESSAGE_LENGTH = 110

/**
 * ViewModel for managing the emergency SMS message text.
 * Persists the message to DataStore and enforces the maximum length constraint.
 *
 * @property message StateFlow of the current emergency message text
 */
class MessageViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val DEFAULT_MESSAGE =
        "NOTFALL! Ich bin hier: [STANDORT-LINK]. Bitte schaut sofort nach mir! (automatisierte Nachricht)"

    private val _message = MutableStateFlow(DEFAULT_MESSAGE)
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            emergencyMessageFlow(application).collect { storedMessage ->
                _message.value = storedMessage ?: DEFAULT_MESSAGE
            }
        }
    }

    /**
     * Updates the emergency message text. Ensures only the location placeholder
     * appears at most once, enforces the maximum message length, and persists to DataStore.
     *
     * @param newMessage The new message text
     */
    fun updateMessage(newMessage: String) {
        val firstIdx = newMessage.indexOf(LOCATION_PLACEHOLDER)
        val cleaned = if (firstIdx != -1) {
            val before = newMessage.substring(0, firstIdx + LOCATION_PLACEHOLDER.length)
            val after = newMessage.substring(firstIdx + LOCATION_PLACEHOLDER.length)
            before + after.replace(LOCATION_PLACEHOLDER, "")
        } else {
            newMessage
        }

        val parts = cleaned.split(LOCATION_PLACEHOLDER)
        val placeholderCount = parts.size - 1
        val effectiveLength = cleaned.length - placeholderCount * LOCATION_PLACEHOLDER.length
        val truncated = if (effectiveLength <= MAX_MESSAGE_LENGTH) {
            cleaned
        } else  {
            cleaned.take(MAX_MESSAGE_LENGTH)
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