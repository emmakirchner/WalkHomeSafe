package com.example.walkhomesafe.presentation.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Intent extra key for the SOS action type passed to MainActivity. */
const val EXTRA_SOS_ACTION = "com.example.walkhomesafe.SOS_ACTION"
/** SOS action identifier for sending an emergency SMS. */
const val ACTION_SMS = "sms"
/** SOS action identifier for triggering the alarm. */
const val ACTION_ALARM = "alarm"

/** Possible SOS actions that can be triggered from the widget. */
enum class WidgetSosAction { SMS, ALARM }

/**
 * Singleton that acts as a communication bridge between the
 * home screen widget and the app's composable UI.
 * Uses a StateFlow to pass the triggered action.
 */
object WidgetTrigger {
    private val _action = MutableStateFlow<WidgetSosAction?>(null)
    /** Observable stream of the most recent widget action. */
    val action: StateFlow<WidgetSosAction?> = _action.asStateFlow()

    /**
     * Set the pending SOS action from the widget.
     *
     * @param action The action that was triggered (SMS or ALARM)
     */
    fun trigger(action: WidgetSosAction) {
        _action.value = action
    }

    /**
     * Consume and clear the current pending action.
     *
     * @return The pending action, or null if none exists
     */
    fun consume(): WidgetSosAction? {
        val a = _action.value
        if (a != null) {
            _action.value = null
        }
        return a
    }
}
