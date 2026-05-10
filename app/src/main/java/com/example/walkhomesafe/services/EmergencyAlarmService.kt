package com.example.walkhomesafe.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.walkhomesafe.R

class EmergencyAlarmService : Service() {

    private lateinit var player: MediaPlayer

    override fun onCreate() {
        super.onCreate()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::player.isInitialized) {
            player = MediaPlayer.create(this, R.raw.emergency_alarm).apply {
                isLooping = true
                setVolume(1f, 1f)
                start()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (::player.isInitialized) {
            player.stop()
            player.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "emergency_alarm"
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Notfallalarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notfallalarm aktiv")
            .setContentText("Alarm läuft – Tippen zum Stoppen")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}