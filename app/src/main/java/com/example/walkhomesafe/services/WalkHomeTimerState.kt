package com.example.walkhomesafe.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton state machine for the walk-home timer.
 * Manages timer phases (IDLE, COUNTDOWN, EXPIRED, REMINDER, EMERGENCY) via StateFlow.
 *
 * @property state StateFlow of the current timer state
 */
object WalkHomeTimerState {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    /**
     * Phase of the walk-home timer lifecycle.
     */
    enum class TimerPhase { IDLE, COUNTDOWN, EXPIRED, REMINDER, EMERGENCY }

    /**
     * State of the walk-home timer at a given point in time.
     *
     * @property phase Current phase of the timer
     * @property endTimeMillis End time as milliseconds since epoch
     * @property durationMinutes Original timer duration in minutes
     */
    data class TimerState(
        val phase: TimerPhase = TimerPhase.IDLE,
        val endTimeMillis: Long = 0L,
        val durationMinutes: Int = 0
    )

    /**
     * Starts the timer with a given duration.
     *
     * @param durationMinutes Duration in minutes
     */
    fun start(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + durationMinutes * 60_000L
        _state.value = TimerState(TimerPhase.COUNTDOWN, endTime, durationMinutes)
    }

    /**
     * Transitions the timer to the EXPIRED phase.
     */
    fun expire() {
        _state.value = _state.value.copy(phase = TimerPhase.EXPIRED)
    }

    /**
     * Transitions the timer to the REMINDER phase.
     */
    fun showReminder() {
        _state.value = _state.value.copy(phase = TimerPhase.REMINDER)
    }

    /**
     * Transitions the timer to the EMERGENCY phase.
     */
    fun triggerEmergency() {
        _state.value = _state.value.copy(phase = TimerPhase.EMERGENCY)
    }

    /**
     * Resets the timer to the IDLE phase with default values.
     */
    fun deactivate() {
        _state.value = TimerState()
    }

    /**
     * Restores the timer to a previously saved state (e.g., after app restart).
     *
     * @param phase The phase to restore
     * @param endTimeMillis The end time in milliseconds
     * @param durationMinutes The duration in minutes
     */
    fun restore(phase: TimerPhase, endTimeMillis: Long, durationMinutes: Int) {
        _state.value = TimerState(phase, endTimeMillis, durationMinutes)
    }
}
