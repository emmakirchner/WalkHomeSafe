package com.example.walkhomesafe.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds the current state of the emergency alarm (sound).
 *
 * @property isActive StateFlow emitting whether the alarm sound is currently playing
 */
object AlarmState {
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /**
     * Sets the alarm active state.
     *
     * @param active true to mark the alarm as active, false to mark it as inactive
     */
    fun setActive(active: Boolean) {
        _isActive.value = active
    }
}
