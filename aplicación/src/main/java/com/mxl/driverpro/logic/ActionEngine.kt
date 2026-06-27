package com.mxl.driverpro.logic

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ActionEngine : AccessibilityService() {

    private var isActive = false
    private var minPrice = 0.0
    private var maxDistance = 5.0
    private var minRating = 4.0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isActive) return
        event?.let {
            if (it.packageName == "com.indrive.passenger" ||
                it.packageName == "com.indriver.passenger") {
                scanAndAccept(rootInActiveWindow)
            }
        }
    }

    private fun scanAndAccept(node: AccessibilityNodeInfo?) {
        node ?: return
        // Buscar botón de aceptar viaje
        val acceptButtons = node.findAccessibilityNodeInfosByText("Aceptar")
        val offerButtons = node.findAccessibilityNodeInfosByText("Ofertar")
        
        acceptButtons.forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        offerButtons.forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    override fun onInterrupt() {}

    fun setActive(active: Boolean) { isActive = active }
    fun setFilters(price: Double, distance: Double, rating: Double) {
        minPrice = price
        maxDistance = distance
        minRating = rating
    }
}
