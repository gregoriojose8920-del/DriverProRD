package com.mxl.driverpro

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class BotForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        acquireWakeLock()
    }

    private fun startForegroundNotification() {
        val channelId = "driverpro_foreground"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DriverPro Bot Activo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mantiene el bot activo en segundo plano"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notifIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("DriverPro ACTIVO")
                .setContentText("Bot monitoreando viajes en InDrive...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("DriverPro ACTIVO")
                .setContentText("Bot monitoreando viajes en InDrive...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }
        startForeground(2, notification)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DriverPro::BotWakeLock"
        ).apply { acquire(10 * 60 * 60 * 1000L) } // 10 horas
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Se reinicia automaticamente si el sistema lo mata
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Reiniciar si el usuario cierra la app
        val restartIntent = Intent(applicationContext, BotForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000, pendingIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        wakeLock?.release()
        // Auto-reinicio
        val restartIntent = Intent(applicationContext, BotForegroundService::class.java)
        startService(restartIntent)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
