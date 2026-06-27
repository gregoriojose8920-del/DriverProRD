package com.mxl.driverpro.logic

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.RingtoneManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

class ActionEngine : AccessibilityService() {

    companion object { 
        private const val TAG = "DriverProEngine"
        private const val PREFS_NAME = "driverpro_prefs"
        private const val KEY_IS_ACTIVE = "is_active"
    }

    private var floatingPanel: FloatingPanel? = null
    private var mediaPlayer: MediaPlayer? = null
    private var lastSoundTime = 0L
    private val soundCooldown = 3000L
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessing = false

    private val pricePattern    = Pattern.compile("\\$?\\s?(\\d+(?:[.,]\\d{1,2})?)")
    private val distancePattern = Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)\\s?km", Pattern.CASE_INSENSITIVE)
    private val ratingPattern   = Pattern.compile("([1-5][.,]\\d{1,2})")

    private val prefs by lazy { 
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) 
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo = info

        if (Settings.canDrawOverlays(this)) {
            floatingPanel = FloatingPanel(this)
            floatingPanel?.onActiveChanged = { active -> Log.i(TAG, "Activo: $active") }
            floatingPanel?.onFiltersChanged = { _, _, _ -> }
            floatingPanel?.show()
        }
        Log.i(TAG, "ActionEngine conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.indrive.passenger" && pkg != "com.indriver.passenger") return
        val panel = floatingPanel ?: return
        if (!panel.isServiceActive()) return
        if (isProcessing) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> processTripData(panel)
        }
    }

    private fun processTripData(panel: FloatingPanel) {
        val root = rootInActiveWindow ?: return
        val allTexts = mutableListOf<String>()
        collectTexts(root, allTexts)

        val price    = extractFirstMatch(allTexts, pricePattern)
        val distance = extractFirstMatch(allTexts, distancePattern)
        val rating   = extractFirstMatch(allTexts, ratingPattern)

        if (price == null && distance == null) return

        val cumple = panel.evaluateFilters(price ?: 0.0, distance ?: 0.0, rating ?: 0.0)

        val msg = buildString {
            if (price != null)    append("RD$ ${"%.0f".format(price)}  ")
            if (distance != null) append("${"%.1f".format(distance)}km  ")
            if (rating != null)   append("Rating: ${"%.1f".format(rating)}  ")
            append(if (cumple) "✅ ACEPTA" else "❌ RECHAZA")
        }

        panel.updateAlerta(msg, cumple)

        if (cumple) {
            playAlertSound()
            performAutomaticAccept(root)
        }
    }

    private fun performAutomaticAccept(root: AccessibilityNodeInfo) {
        isProcessing = true
        
        handler.postDelayed({
            try {
                val acceptButton = findAcceptButton(root)
                
                if (acceptButton != null) {
                    acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.i(TAG, "✅ Viaje aceptado automáticamente")
                    floatingPanel?.updateAlerta("✅ ACEPTADO!", true)
                    
                    handler.postDelayed({
                        isProcessing = false
                    }, 2000)
                } else {
                    if (tryAlternativeAccept(root)) {
                        Log.i(TAG, "✅ Viaje aceptado (método alternativo)")
                        floatingPanel?.updateAlerta("✅ ACEPTADO!", true)
                    } else {
                        Log.w(TAG, "❌ No se encontró botón de aceptar")
                        floatingPanel?.updateAlerta("⚠️ No se pudo aceptar", false)
                    }
                    isProcessing = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al aceptar viaje: ${e.message}")
                isProcessing = false
            }
        }, 500)
    }

    private fun findAcceptButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val acceptTexts = listOf(
            "Aceptar", "ACEPTAR", "Aceptar viaje", "Tomar viaje",
            "Accept", "ACCEPT", "Accept ride", "Take ride",
            "Aceptar solicitud", "Tomar solicitud"
        )
        
        for (text in acceptTexts) {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable) return node
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) return parent
                    parent = parent.parent
                }
            }
        }
        
        val acceptIds = listOf(
            "accept_button", "btn_accept", "accept_ride",
            "take_ride", "button_accept", "accept_request"
        )
        
        for (id in acceptIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId("com.indrive.driver:id/$id")
            if (nodes.isNotEmpty()) {
                val node = nodes[0]
                if (node.isClickable) return node
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) return parent
                    parent = parent.parent
                }
            }
        }
        
        val allNodes = getAllNodes(root)
        for (node in allNodes) {
            val text = node.text?.toString() ?: continue
            if (text.contains("Aceptar", ignoreCase = true) || 
                text.contains("Accept", ignoreCase = true)) {
                if (node.isClickable) return node
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) return parent
                    parent = parent.parent
                }
            }
        }
        
        return null
    }

    private fun tryAlternativeAccept(root: AccessibilityNodeInfo): Boolean {
        try {
            val nodes = getAllNodes(root)
            for (node in nodes) {
                val text = node.text?.toString() ?: continue
                if (text.contains("Aceptar", ignoreCase = true) || 
                    text.contains("Accept", ignoreCase = true)) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return true
                        }
                        parent = parent.parent
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en método alternativo: ${e.message}")
        }
        return false
    }

    private fun getAllNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, result)
        return result
    }

    private fun collectNodes(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        node ?: return
        out.add(node)
        for (i in 0 until node.childCount) {
            collectNodes(node.getChild(i), out)
        }
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.toString()?.let { if (it.isNotBlank()) out.add(it) }
        for (i in 0 until node.childCount) collectTexts(node.getChild(i), out)
    }

    private fun extractFirstMatch(texts: List<String>, pattern: Pattern): Double? {
        for (t in texts) {
            val m = pattern.matcher(t)
            if (m.find()) return m.group(1)?.replace(",", ".")?.toDoubleOrNull()
        }
        return null
    }

    private fun playAlertSound() {
        val now = System.currentTimeMillis()
        if (now - lastSoundTime < soundCooldown) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.setOnCompletionListener { it.release(); mediaPlayer = null }
            mediaPlayer?.start()
            lastSoundTime = now
        } catch (e: Exception) { Log.e(TAG, "Error sonido: ${e.message}") }
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        isProcessing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        floatingPanel?.hide()
        floatingPanel = null
        isProcessing = false
    }
}
