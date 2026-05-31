package com.example.walkhomesafe.presentation.widget

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

const val EXTRA_SOS_ACTION = "com.example.walkhomesafe.SOS_ACTION"
const val ACTION_SMS = "sms"
const val ACTION_ALARM = "alarm"

enum class WidgetSosAction { SMS, ALARM }

object WidgetTrigger {
    private val _actions = MutableSharedFlow<WidgetSosAction>(extraBufferCapacity = 1)
    val actions = _actions.asSharedFlow()

    fun trigger(action: WidgetSosAction) {
        _actions.tryEmit(action)
    }
}
