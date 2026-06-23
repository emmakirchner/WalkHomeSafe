package com.example.walkhomesafe.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WalkHomeTimerState {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    enum class TimerPhase { IDLE, COUNTDOWN, EXPIRED, REMINDER, EMERGENCY }

    data class TimerState(
        val phase: TimerPhase = TimerPhase.IDLE,
        val endTimeMillis: Long = 0L,
        val durationMinutes: Int = 0
    )

    fun start(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + durationMinutes * 60_000L
        _state.value = TimerState(TimerPhase.COUNTDOWN, endTime, durationMinutes)
    }

    fun expire() {
        _state.value = _state.value.copy(phase = TimerPhase.EXPIRED)
    }

    fun showReminder() {
        _state.value = _state.value.copy(phase = TimerPhase.REMINDER)
    }

    fun triggerEmergency() {
        _state.value = _state.value.copy(phase = TimerPhase.EMERGENCY)
    }

    fun deactivate() {
        _state.value = TimerState()
    }

    fun restore(phase: TimerPhase, endTimeMillis: Long, durationMinutes: Int) {
        _state.value = TimerState(phase, endTimeMillis, durationMinutes)
    }
}
