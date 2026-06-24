package com.example.walkhomesafe.services

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.walkhomesafe.MainActivity
import com.example.walkhomesafe.data.emergencyContactsFlow
import com.example.walkhomesafe.data.emergencyMessageFlow
import com.example.walkhomesafe.data.loadTimerEmergencySent
import com.example.walkhomesafe.data.saveTimerEmergencySent
import com.example.walkhomesafe.services.WalkHomeTimerState.TimerPhase
import com.example.walkhomesafe.viewmodel.LOCATION_PLACEHOLDER
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WalkHomeTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TIMER_EXPIRED -> handleExpired(context)
            ACTION_TIMER_REMINDER -> handleReminder(context)
            ACTION_TIMER_EMERGENCY -> handleEmergency(context)
            ACTION_TIMER_DEACTIVATE -> handleDeactivate(context)
        }
    }

    private fun handleExpired(context: Context) {
        val currentPhase = WalkHomeTimerState.state.value.phase
        if (currentPhase == TimerPhase.IDLE || currentPhase == TimerPhase.COUNTDOWN) {
            WalkHomeTimerState.expire()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createTimerChannel(notificationManager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val deactivateIntent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
            action = ACTION_TIMER_DEACTIVATE
        }
        val pendingDeactivateIntent = PendingIntent.getBroadcast(
            context, 1, deactivateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setContentTitle("Heimweg-Timer abgelaufen")
            .setContentText("Bist du angekommen? Timer deaktivieren!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_input_add, "Angekommen", pendingDeactivateIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_TIMER_EXPIRED, notification)

        if (currentPhase != TimerPhase.REMINDER && currentPhase != TimerPhase.EMERGENCY) {
            scheduleAlarm(context, ACTION_TIMER_REMINDER, REMINDER_DELAY_MS, REQUEST_REMINDER)
            scheduleAlarm(context, ACTION_TIMER_EMERGENCY, EMERGENCY_DELAY_MS, REQUEST_EMERGENCY)
        }
    }

    private fun handleReminder(context: Context) {
        val alreadySent = runBlocking { loadTimerEmergencySent(context) }
        if (alreadySent) return

        val currentPhase = WalkHomeTimerState.state.value.phase
        if (currentPhase == TimerPhase.EXPIRED) {
            WalkHomeTimerState.showReminder()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createTimerChannel(notificationManager)

        val deactivateIntent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
            action = ACTION_TIMER_DEACTIVATE
        }
        val pendingDeactivateIntent = PendingIntent.getBroadcast(
            context, 1, deactivateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setContentTitle("Heimweg-Timer - Erinnerung")
            .setContentText("Bist du noch unterwegs? Dein Notfallkontakt wird gleich informiert!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .addAction(android.R.drawable.ic_input_add, "Angekommen", pendingDeactivateIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_TIMER_REMINDER, notification)
    }

    private fun handleEmergency(context: Context) {
        WalkHomeTimerState.triggerEmergency()

        val alreadySent = runBlocking {
            loadTimerEmergencySent(context)
        }
        if (alreadySent) return

        runBlocking {
            saveTimerEmergencySent(context, true)
        }

        val contacts = runBlocking {
            emergencyContactsFlow(context).first()
        }

        if (contacts.isNotEmpty()) {
            var message = runBlocking {
                emergencyMessageFlow(context).first()
            } ?: "NOTFALL! Ich bin hier: $LOCATION_PLACEHOLDER. Bitte schaut sofort nach mir! (automatisierte Nachricht)"

            val locationLink = if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                @Suppress("DEPRECATION")
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                } else null
            } else null

            if (locationLink != null && message.contains(LOCATION_PLACEHOLDER)) {
                message = message.replaceFirst(LOCATION_PLACEHOLDER, locationLink)
            } else if (locationLink != null) {
                message = "$message\n$locationLink"
            } else {
                message = message.replaceFirst(LOCATION_PLACEHOLDER, "")
            }

            MessageSender(context).send(contacts, message)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createTimerChannel(notificationManager)

        val deactivateIntent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
            action = ACTION_TIMER_DEACTIVATE
        }
        val pendingDeactivateIntent = PendingIntent.getBroadcast(
            context, 1, deactivateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val body = if (contacts.isEmpty()) {
            "Keine Notfallkontakte hinterlegt. Bitte richte Kontakte in der App ein."
        } else {
            "Deine Notfallkontakte wurden informiert. Timer deaktivieren?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setContentTitle("Notfall - Hilfe benachrichtigt")
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .addAction(android.R.drawable.ic_input_add, "Timer deaktivieren", pendingDeactivateIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_TIMER_EMERGENCY, notification)
    }

    private fun handleDeactivate(context: Context) {
        cancelAll(context)
        WalkHomeTimerState.deactivate()
    }

    private fun scheduleAlarm(context: Context, action: String, delayMs: Long, requestCode: Int) {
        val intent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        try {
            alarmManager.setAlarmClock(
                android.app.AlarmManager.AlarmClockInfo(System.currentTimeMillis() + delayMs, null),
                pendingIntent
            )
        } catch (e: Exception) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delayMs,
                    pendingIntent
                )
            } catch (e2: SecurityException) {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delayMs,
                    pendingIntent
                )
            }
        }
    }

    companion object {
        const val ACTION_TIMER_EXPIRED = "com.example.walkhomesafe.TIMER_EXPIRED"
        const val ACTION_TIMER_REMINDER = "com.example.walkhomesafe.TIMER_REMINDER"
        const val ACTION_TIMER_EMERGENCY = "com.example.walkhomesafe.TIMER_EMERGENCY"
        const val ACTION_TIMER_DEACTIVATE = "com.example.walkhomesafe.TIMER_DEACTIVATE"

        private const val CHANNEL_TIMER = "walk_home_timer"
        private const val NOTIFICATION_TIMER_EXPIRED = 100
        private const val NOTIFICATION_TIMER_REMINDER = 101
        private const val NOTIFICATION_TIMER_EMERGENCY = 102

        private const val REQUEST_EXPIRED = 1000
        private const val REQUEST_REMINDER = 1001
        private const val REQUEST_EMERGENCY = 1002

        private const val REMINDER_DELAY_MS = 120_000L
        private const val EMERGENCY_DELAY_MS = 240_000L

        private fun createTimerChannel(notificationManager: NotificationManager) {
            val channel = android.app.NotificationChannel(
                CHANNEL_TIMER,
                "Heimweg-Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für den Heimweg-Timer"
            }
            notificationManager.createNotificationChannel(channel)
        }

        fun scheduleExpiry(context: Context, endTimeMillis: Long) {
            val intent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
                action = ACTION_TIMER_EXPIRED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_EXPIRED, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            try {
                alarmManager.setAlarmClock(
                    android.app.AlarmManager.AlarmClockInfo(endTimeMillis, null),
                    pendingIntent
                )
            } catch (e: Exception) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                } catch (e2: SecurityException) {
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                }
            }
        }

        fun cancelAll(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            cancelAlarm(context, alarmManager, ACTION_TIMER_EXPIRED, REQUEST_EXPIRED)
            cancelAlarm(context, alarmManager, ACTION_TIMER_REMINDER, REQUEST_REMINDER)
            cancelAlarm(context, alarmManager, ACTION_TIMER_EMERGENCY, REQUEST_EMERGENCY)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_TIMER_EXPIRED)
            notificationManager.cancel(NOTIFICATION_TIMER_REMINDER)
            notificationManager.cancel(NOTIFICATION_TIMER_EMERGENCY)
        }

        private fun cancelAlarm(context: Context, alarmManager: android.app.AlarmManager, action: String, requestCode: Int) {
            val intent = Intent(context, WalkHomeTimerReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
