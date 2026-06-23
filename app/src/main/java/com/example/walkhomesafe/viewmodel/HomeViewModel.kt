package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.MainActivity
import com.example.walkhomesafe.data.clearTimerData
import com.example.walkhomesafe.data.loadTimerDuration
import com.example.walkhomesafe.data.loadTimerEmergencySent
import com.example.walkhomesafe.data.loadTimerEndTime
import com.example.walkhomesafe.data.saveTimerDuration
import com.example.walkhomesafe.data.saveTimerEndTime
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.services.AlarmState
import com.example.walkhomesafe.services.EmergencyAlarmService
import com.example.walkhomesafe.services.MessageSender
import com.example.walkhomesafe.services.WalkHomeTimerReceiver
import com.example.walkhomesafe.services.WalkHomeTimerState
import com.example.walkhomesafe.services.WalkHomeTimerState.TimerPhase
import com.example.walkhomesafe.viewmodel.LOCATION_PLACEHOLDER
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val messageSender = MessageSender(context)

    val isAlarmActive: StateFlow<Boolean> = AlarmState.isActive
    val timerState: StateFlow<WalkHomeTimerState.TimerState> = WalkHomeTimerState.state

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _timerDurationInput = MutableStateFlow(10)
    val timerDurationInput: StateFlow<Int> = _timerDurationInput.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val endTime = loadTimerEndTime(context)
            if (endTime > 0L) {
                val remaining = (endTime - System.currentTimeMillis()) / 1000
                val duration = loadTimerDuration(context)
                val emergencySent = loadTimerEmergencySent(context)

                if (remaining > 0) {
                    WalkHomeTimerState.restore(TimerPhase.COUNTDOWN, endTime, duration)
                    startTicking(endTime)
                } else if (!emergencySent) {
                    WalkHomeTimerState.restore(TimerPhase.EXPIRED, endTime, duration)
                } else {
                    WalkHomeTimerState.restore(TimerPhase.EMERGENCY, endTime, duration)
                }
            }
        }
    }

    fun setTimerDuration(minutes: Int) {
        _timerDurationInput.value = minutes.coerceIn(1, 999)
    }

    fun startTimer() {
        val duration = _timerDurationInput.value
        if (duration <= 0) return

        val endTime = System.currentTimeMillis() + duration * 60_000L

        WalkHomeTimerState.start(duration)
        startTicking(endTime)

        viewModelScope.launch {
            saveTimerEndTime(context, endTime)
            saveTimerDuration(context, duration)
        }

        WalkHomeTimerReceiver.scheduleExpiry(context, endTime)
    }

    fun deactivateTimer() {
        tickJob?.cancel()
        tickJob = null
        WalkHomeTimerState.deactivate()
        WalkHomeTimerReceiver.cancelAll(context)
        viewModelScope.launch {
            clearTimerData(context)
        }
    }

    private fun startTicking(endTime: Long) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                _remainingSeconds.value = remaining
                if (remaining <= 0) {
                    WalkHomeTimerState.expire()
                    showTimerExpiredNotification()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun showTimerExpiredNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "walk_home_timer",
            "Heimweg-Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Benachrichtigungen für den Heimweg-Timer"
        }
        notificationManager.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "walk_home_timer")
            .setContentTitle("Heimweg-Timer abgelaufen")
            .setContentText("Bist du angekommen? Timer deaktivieren!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingOpenIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(100, notification)
    }

    fun onSendMessage(
        contacts: List<EmergencyContact>,
        message: String,
        locationLatLng: LatLng? = null
    ) {
        val finalMessage = if (locationLatLng != null) {
            val link = "https://maps.google.com/?q=${locationLatLng.latitude},${locationLatLng.longitude}"
            if (message.contains(LOCATION_PLACEHOLDER)) {
                message.replaceFirst(LOCATION_PLACEHOLDER, link)
            } else {
                "$message\n$link"
            }
        } else {
            message.replaceFirst(LOCATION_PLACEHOLDER, "")
        }
        messageSender.send(contacts, finalMessage)
    }

    fun startAlarmService() {
        AlarmState.setActive(true)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopAlarmService() {
        AlarmState.setActive(false)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        context.stopService(intent)
    }
}
