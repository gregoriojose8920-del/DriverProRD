package com.mgo.driverpro

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DriverBotService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val precioNodes = rootNode.findAccessibilityNodeInfosByText("RD$")
        
        // Aquí añadimos la lógica: ¿Qué nos conviene?
        var mejorViaje: AccessibilityNodeInfo? = null
        var precioMasAlto = 0

        for (node in precioNodes) {
            val textoPrecio = node.text.toString()
            val valor = textoPrecio.replace("RD$", "").replace(",", "").trim().toIntOrNull() ?: 0
            
            // Filtro: Solo nos interesa si es mayor a 200 y es el mejor de los disponibles
            if (valor > 200 && valor > precioMasAlto) {
                precioMasAlto = valor
                mejorViaje = node
            }
        }

        if (mejorViaje != null) {
            clicEnBotonAceptar(rootNode)
        }
    }

    private fun clicEnBotonAceptar(rootNode: AccessibilityNodeInfo) {
        val botones = rootNode.findAccessibilityNodeInfosByText("Aceptar")
        for (btn in botones) {
            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun onInterrupt() {}
}