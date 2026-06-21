import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DriverBotService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        val precioNodes = rootNode.findAccessibilityNodeInfosByText("RD$")
        for (node in precioNodes) {
            if (esViajeRentable(node.text.toString())) {
                clickBotonAceptar(rootNode)
            }
        }
    }

    private fun clickBotonAceptar(rootNode: AccessibilityNodeInfo) {
        val aceptarButtons = rootNode.findAccessibilityNodeInfosByText("Aceptar")
        for (button in aceptarButtons) {
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun onInterrupt() {}
}