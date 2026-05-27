package com.example.walkhomesafe.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.walkhomesafe.R
import androidx.core.net.toUri

class EmergencyAlarmService : Service() {

    private lateinit var player: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!::player.isInitialized) {
            player = MediaPlayer().apply {
                try {
                    setDataSource(
                        applicationContext,
                        "android.resource://$packageName/${R.raw.emergency_alarm}".toUri()
                    )
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                } catch (e: Exception) {
                    stopSelf()
                }
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

    @SuppressLint("LaunchActivityFromNotification")
    private fun createNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notfallalarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, EmergencyAlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Notfallalarm aktiv")
            .setContentText("Tippen zum Stoppen")
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(stopPendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "emergency_alarm"
        private const val ACTION_STOP = "com.example.walkhomesafe.ACTION_STOP_ALARM"
    }
}
