package com.mxl.driverpro.logic

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ActionEngine : AccessibilityService() {

    companion object {
        const val TAG = "DriverPro"
        const val CHANNEL_ID = "driverpro_channel"
        const val PREFS_NAME = "driverpro_prefs"
        val SUPPORTED_PACKAGES = listOf(
            "com.indrive.passenger",
            "com.indriver.passenger",
            "sinet.startup.inDriver",
            "com.ubercab.driver"
        )
        val ACCEPT_TEXTS = listOf(
            "Aceptar", "Accept", "ACEPTAR",
            "Ofertar", "Offer", "OFERTAR",
            "Tomar viaje", "Take trip"
        )
    }

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var tripCount = 0
    private var lastAcceptTime = 0L
    private var isActive = false
    private var cooldownMs = 3000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isActive = true
        tripCount = prefs.getInt("trip_count", 0)
        setupNotificationChannel()
        Log.d(TAG, "DriverPro conectado")
        showNotification("DriverPro Activo", "Monitoreando viajes en InDrive...")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isActive) return
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (!SUPPORTED_PACKAGES.contains(pkg)) return
        val now = System.currentTimeMillis()
        if (now - lastAcceptTime < cooldownMs) return
        handler.post {
            try { scanAndAccept(rootInActiveWindow, pkg) }
            catch (e: Exception) { Log.e(TAG, "Error: " + e.message) }
        }
    }

    private fun scanAndAccept(node: AccessibilityNodeInfo?, pkg: String) {
        node ?: return
        for (text in ACCEPT_TEXTS) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            for (n in nodes) {
                if (n.isEnabled) {
                    val clicked = n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        tripCount++
                        lastAcceptTime = System.currentTimeMillis()
                        Log.d(TAG, "Viaje #" + tripCount + " aceptado")
                        showNotification("Viaje Aceptado #" + tripCount, "Bot tomo un viaje automaticamente")
                        prefs.edit().putInt("trip_count", tripCount).apply()
                        return
                    }
                }
            }
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "DriverPro", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, CHANNEL_ID)
            else Notification.Builder(this)
            manager.notify(1, builder
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true).build())
        } catch (e: Exception) { Log.e(TAG, "Error notif: " + e.message) }
    }

    override fun onInterrupt() { Log.d(TAG, "DriverPro interrumpido") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "DriverPro detenido") }
}
