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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class ActionEngine : AccessibilityService() {

    companion object {
        const val TAG = "DriverPro"
        const val CHANNEL_ID = "driverpro_channel"
        val SUPPORTED_PACKAGES = listOf(
            "com.indrive.passenger",
            "com.indriver.passenger",
            "sinet.startup.inDriver"
        )
        val ACCEPT_TEXTS = listOf(
            "Aceptar", "Accept", "ACEPTAR",
            "Ofertar", "Offer", "OFERTAR",
            "Tomar viaje"
        )
    }

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var tripCount = 0
    private var lastAcceptTime = 0L
    private var floatingPanel: FloatingPanel? = null
    private var licenciaActiva = false
    private var automaticoActivo = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("driverpro_prefs", Context.MODE_PRIVATE)
        tripCount = prefs.getInt("trip_count", 0)
        setupNotificationChannel()
        showNotification("DriverPro Activo", "Verificando licencia...")
        verificarLicencia()

        if (android.provider.Settings.canDrawOverlays(this)) {
            handler.postDelayed({
                try {
                    floatingPanel = FloatingPanel(this)
                    floatingPanel?.show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error panel: " + e.message)
                }
            }, 800)
        }
    }

    private fun verificarLicencia() {
        val deviceId = android.provider.Settings.Secure.getString(
            contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        FirebaseFirestore.getInstance()
            .collection("licencias").document(deviceId).get()
            .addOnSuccessListener { doc ->
                automaticoActivo = doc.getBoolean("automatico_activo") ?: true
                if (doc.exists()) {
                    val expira = doc.getDate("fecha_expiracion")
                    val activo = doc.getBoolean("activo") ?: false
                    licenciaActiva = activo && expira != null && expira.after(Date())
                    val status = if (licenciaActiva) "ACTIVA" else "EXPIRADA"
                    floatingPanel?.updateLicencia(status, licenciaActiva)
                    showNotification(
                        if (licenciaActiva) "DriverPro - Licencia ACTIVA" else "DriverPro - Licencia EXPIRADA",
                        if (licenciaActiva) "Bot funcionando correctamente" else "Renueva tu plan para continuar"
                    )
                } else {
                    licenciaActiva = false
                    floatingPanel?.updateLicencia("Sin plan", false)
                    showNotification("DriverPro", "Sin licencia activa - contacta al administrador")
                }
            }
            .addOnFailureListener {
                // Sin internet = modo offline, permite funcionar
                licenciaActiva = true
                floatingPanel?.updateLicencia("Offline", true)
            }

        // Re-verificar cada 6 horas
        handler.postDelayed({ verificarLicencia() }, 6 * 60 * 60 * 1000L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!licenciaActiva) return
        if (!automaticoActivo) return  // Firebase puede desactivar remotamente
        val isActive = prefs.getBoolean("is_active", true)
        if (!isActive) return
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (!SUPPORTED_PACKAGES.contains(pkg)) return
        val now = System.currentTimeMillis()
        if (now - lastAcceptTime < 3000L) return
        handler.post {
            try { scanAndAccept(rootInActiveWindow, pkg) }
            catch (e: Exception) { Log.e(TAG, "Error: " + e.message) }
        }
    }

    private fun scanAndAccept(node: AccessibilityNodeInfo?, pkg: String) {
        node ?: return
        val minPrice = prefs.getFloat("min_price", 150f).toDouble()
        val maxDist = prefs.getFloat("max_distance", 3f).toDouble()

        for (text in ACCEPT_TEXTS) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            for (n in nodes) {
                if (n.isEnabled) {
                    val clicked = n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        tripCount++
                        lastAcceptTime = System.currentTimeMillis()
                        prefs.edit().putInt("trip_count", tripCount).apply()
                        Log.d(TAG, "Viaje #$tripCount aceptado")
                        floatingPanel?.incrementTrip()
                        showNotification(
                            "Viaje #$tripCount Aceptado",
                            "DriverPro acepto un viaje automaticamente"
                        )
                        return
                    }
                }
            }
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "DriverPro", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun showNotification(title: String, message: String) {
        try {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
            mgr.notify(1, b.setContentTitle(title).setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true).build())
        } catch (e: Exception) { Log.e(TAG, "Notif: " + e.message) }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { floatingPanel?.destroy(); super.onDestroy() }
}
