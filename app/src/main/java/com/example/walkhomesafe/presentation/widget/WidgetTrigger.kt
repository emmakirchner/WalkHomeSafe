package com.example.walkhomesafe.presentation.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val EXTRA_SOS_ACTION = "com.example.walkhomesafe.SOS_ACTION"
const val ACTION_SMS = "sms"
const val ACTION_ALARM = "alarm"

enum class WidgetSosAction { SMS, ALARM }

object WidgetTrigger {
    private val _action = MutableStateFlow<WidgetSosAction?>(null)
    val action: StateFlow<WidgetSosAction?> = _action.asStateFlow()

    fun trigger(action: WidgetSosAction) {
        _action.value = action
    }

    fun consume(): WidgetSosAction? {
        val a = _action.value
        if (a != null) {
            _action.value = null
        }
        return a
    }
}
