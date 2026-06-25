package com.example.walkhomesafe.viewmodel

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.MainActivity
import com.example.walkhomesafe.data.clearTimerData
import com.example.walkhomesafe.data.emergencyContactsFlow
import com.example.walkhomesafe.data.emergencyMessageFlow
import com.example.walkhomesafe.data.loadTimerDuration
import com.example.walkhomesafe.data.loadTimerEmergencySent
import com.example.walkhomesafe.data.loadTimerEndTime
import com.example.walkhomesafe.data.saveTimerDuration
import com.example.walkhomesafe.data.saveTimerEmergencySent
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the home screen. Manages the emergency alarm (SOS button),
 * the walk-home timer lifecycle, and sending emergency SMS messages.
 *
 * @property isAlarmActive StateFlow indicating whether the alarm sound is playing
 * @property timerState StateFlow of the walk-home timer state machine
 * @property remainingSeconds StateFlow of the countdown/emergency remaining seconds
 * @property timerDurationInput StateFlow of the user's selected timer duration input
 * @property timerEndTime StateFlow of the timer end timestamp
 */
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

    private val _timerEndTime = MutableStateFlow(0L)
    val timerEndTime: StateFlow<Long> = _timerEndTime.asStateFlow()

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
                _timerEndTime.value = endTime
            }
        }
    }

    /**
     * Sets the timer duration input, clamped between 1 and 999 minutes.
     *
     * @param minutes Desired duration in minutes
     */
    fun setTimerDuration(minutes: Int) {
        _timerDurationInput.value = minutes.coerceIn(1, 999)
    }

    /**
     * Starts the walk-home timer with the current duration input.
     * Saves the end time and duration to DataStore and schedules the expiry alarm.
     */
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

    /**
     * Deactivates the walk-home timer: cancels the tick coroutine, resets the
     * timer state machine, cancels all scheduled alarms, and clears timer data.
     */
    fun deactivateTimer() {
        tickJob?.cancel()
        tickJob = null
        WalkHomeTimerState.deactivate()
        WalkHomeTimerReceiver.cancelAll(context)
        _timerEndTime.value = 0L
        viewModelScope.launch {
            clearTimerData(context)
        }
    }

    /**
     * Starts a coroutine that ticks every second, updating the remaining time display
     * and transitioning the timer through EXPIRED, REMINDER, and EMERGENCY phases.
     *
     * @param endTime The timer end time in milliseconds since epoch
     */
    private fun startTicking(endTime: Long) {
        _timerEndTime.value = endTime
        tickJob?.cancel()
        var expiryHandled = false
        var reminderHandled = false
        var emergencyHandled = false
        tickJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endTime - now) / 1000).toInt().coerceAtLeast(0)

                if (remaining > 0) {
                    _remainingSeconds.value = remaining
                    delay(1000)
                    continue
                }

                val elapsedSinceExpiry = ((now - endTime) / 1000).toInt()

                if (elapsedSinceExpiry < 120) {
                    _remainingSeconds.value = 240 - elapsedSinceExpiry
                    if (!expiryHandled) {
                        expiryHandled = true
                        WalkHomeTimerState.expire()
                        showTimerExpiredNotification()
                    }
                    delay(1000)
                    continue
                }

                if (elapsedSinceExpiry < 240) {
                    _remainingSeconds.value = 240 - elapsedSinceExpiry
                    if (!reminderHandled) {
                        reminderHandled = true
                        WalkHomeTimerState.showReminder()
                        showTimerReminderNotification()
                    }
                    delay(1000)
                    continue
                }

                _remainingSeconds.value = 0
                if (!emergencyHandled) {
                    emergencyHandled = true
                    WalkHomeTimerState.triggerEmergency()
                    triggerEmergencySms()
                }
                break
            }
        }
    }

    /**
     * Sends the emergency SMS to all contacts with the current location,
     * if it hasn't been sent already. Marks the emergency as sent in DataStore.
     */
    private fun triggerEmergencySms() {
        viewModelScope.launch {
            val alreadySent = loadTimerEmergencySent(context)
            if (alreadySent) return@launch

            saveTimerEmergencySent(context, true)

            val contacts = emergencyContactsFlow(context).first()
            if (contacts.isEmpty()) return@launch

            var message = emergencyMessageFlow(context).first()
                ?: "NOTFALL! Ich bin hier: $LOCATION_PLACEHOLDER. Bitte schaut sofort nach mir! (automatisierte Nachricht)"

            val locationLink = if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("DEPRECATION")
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else null
            } else null

            if (locationLink != null && message.contains(LOCATION_PLACEHOLDER)) {
                message = message.replaceFirst(LOCATION_PLACEHOLDER, locationLink)
            } else if (locationLink != null) {
                message = "$message\n$locationLink"
            } else {
                message = message.replaceFirst(LOCATION_PLACEHOLDER, "")
            }

            MessageSender(context).send(contacts, message)

            showEmergencyNotification()
        }
    }

    /**
     * Shows a notification indicating the timer has expired.
     */
    private fun showTimerExpiredNotification() {
        showNotification(
            id = 100,
            title = "Heimweg-Timer abgelaufen",
            text = "Bist du angekommen? Timer deaktivieren!",
            icon = android.R.drawable.ic_dialog_info,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Shows a notification reminding the user that the timer has expired
     * and emergency contacts will be notified soon.
     */
    private fun showTimerReminderNotification() {
        showNotification(
            id = 101,
            title = "Heimweg-Timer - Erinnerung",
            text = "Bist du noch unterwegs? Dein Notfallkontakt wird gleich informiert!",
            icon = android.R.drawable.ic_dialog_alert,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Shows a notification confirming that emergency contacts have been notified.
     */
    private fun showEmergencyNotification() {
        showNotification(
            id = 102,
            title = "Notfall - Hilfe benachrichtigt",
            text = "Deine Notfallkontakte wurden informiert. Timer deaktivieren?",
            icon = android.R.drawable.ic_dialog_alert,
            priority = NotificationCompat.PRIORITY_MAX
        )
    }

    /**
     * Creates and displays a timer-related notification with a deactivate action.
     *
     * @param id Unique notification ID
     * @param title Notification title
     * @param text Notification body text
     * @param icon Resource ID of the notification icon
     * @param priority Notification priority (e.g., NotificationCompat.PRIORITY_HIGH)
     */
    private fun showNotification(id: Int, title: String, text: String, icon: Int, priority: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "walk_home_timer",
            "Heimweg-Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Benachrichtigungen für den Heimweg-Timer" }
        notificationManager.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deactivateIntent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
            action = WalkHomeTimerReceiver.ACTION_TIMER_DEACTIVATE
        }
        val pendingDeactivateIntent = PendingIntent.getBroadcast(
            context, 1, deactivateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "walk_home_timer")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_input_add, "Timer deaktivieren", pendingDeactivateIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(priority)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(id, notification)
    }

    /**
     * Sends an emergency message via SMS to the specified contacts.
     * Replaces the location placeholder with a Google Maps link if coordinates are provided.
     *
     * @param contacts List of emergency contacts to notify
     * @param message The message text, may contain the location placeholder
     * @param locationLatLng Optional coordinates to include as a maps link
     */
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

    /**
     * Starts the emergency alarm foreground service and sets the alarm state to active.
     */
    fun startAlarmService() {
        AlarmState.setActive(true)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stops the emergency alarm foreground service and sets the alarm state to inactive.
     */
    fun stopAlarmService() {
        AlarmState.setActive(false)
        val intent = Intent(context, EmergencyAlarmService::class.java)
        context.stopService(intent)
    }
}
