package com.mgo.driverpro

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class DriverAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Aquí vive la lógica para detectar el botón de aceptar
    }

    override fun onInterrupt() {}
}
