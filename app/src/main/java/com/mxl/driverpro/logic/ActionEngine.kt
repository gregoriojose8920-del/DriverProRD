import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

class ActionEngine : AccessibilityService() {

    companion object {
        private const val TAG = "DriverProEngine"
        private const val DEBUG_LOG = true
    }

    private var isActive = false
    private var minPrice = 0.0
    private var maxDistance = 5.0
    private var minRating = 4.0

    private val pricePattern = Pattern.compile("\\$?\\s?(\\d+(?:[.,]\\d{1,2})?)")
    private val distancePattern = Pattern.compile("(\\d+(?:[.,]\\d{1,2})?)\\s?km", Pattern.CASE_INSENSITIVE)
    private val ratingPattern = Pattern.compile("([1-5][.,]\\d{1,2})")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isActive) return
        event?.let {
            if (it.packageName == "com.indrive.passenger" ||
                it.packageName == "com.indriver.passenger") {
                scanAndAccept(rootInActiveWindow)
            }
        }
    }

    private fun scanAndAccept(root: AccessibilityNodeInfo?) {
        root ?: return
        val acceptButtons = root.findAccessibilityNodeInfosByText("Aceptar")
        val offerButtons = root.findAccessibilityNodeInfosByText("Ofertar")
        val actionButtons = acceptButtons + offerButtons
        if (actionButtons.isEmpty()) return

        val allTexts = mutableListOf<String>()
        collectTexts(root, allTexts)

        val price = extractFirstMatch(allTexts, pricePattern)
        val distance = extractFirstMatch(allTexts, distancePattern)
        val rating = extractFirstMatch(allTexts, ratingPattern)

        if (DEBUG_LOG) {
            Log.d(TAG, "Textos detectados en pantalla: $allTexts")
            Log.d(TAG, "Extraído -> price=$price distance=$distance rating=$rating")
        }

        if (!passesFilters(price, distance, rating)) {
            if (DEBUG_LOG) Log.d(TAG, "Viaje descartado: no cumple filtros (min=$minPrice max=$maxDistance minRating=$minRating)")
            return
        }

        if (DEBUG_LOG) Log.d(TAG, "Viaje ACEPTADO automáticamente")
        actionButtons.forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.toString()?.let { if (it.isNotBlank()) out.add(it) }
        for (i in 0 until node.childCount) {
            collectTexts(node.getChild(i), out)
        }
    }

    private fun extractFirstMatch(texts: List<String>, pattern: Pattern): Double? {
        for (t in texts) {
            val m = pattern.matcher(t)
            if (m.find()) {
                val raw = m.group(1).replace(",", ".")
                return raw.toDoubleOrNull()
            }
        }
        return null
    }

    private fun passesFilters(price: Double?, distance: Double?, rating: Double?): Boolean {
        if (price == null || distance == null) return false
        if (price < minPrice) return false
        if (distance > maxDistance) return false
        if (rating != null && rating < minRating) return false
        return true
    }

    override fun onInterrupt() {}

    fun setActive(active: Boolean) {
        isActive = active
    }

    fun setFilters(price: Double, distance: Double, rating: Double) {
        minPrice = price
        maxDistance = distance
        minRating = rating
    }
}
