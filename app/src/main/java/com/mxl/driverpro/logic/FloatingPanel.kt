package com.mxl.driverpro.logic

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.mxl.driverpro.R

class FloatingPanel(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "driverpro_prefs"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_MIN_PRICE = "min_price"
        private const val KEY_MAX_DISTANCE = "max_distance"
        private const val KEY_MIN_RATING = "min_rating"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var panelView: View? = null
    private var isAttached = false
    private var isMinimized = false

    private lateinit var switchActive: Switch
    private lateinit var txtStatus: TextView
    private lateinit var txtAlerta: TextView
    private lateinit var inputMinPrice: EditText
    private lateinit var inputMaxDistance: EditText
    private lateinit var inputMinRating: EditText
    private lateinit var btnSaveFilters: Button
    private lateinit var btnMinimize: Button
    private lateinit var panelTitle: TextView

    var onActiveChanged: ((Boolean) -> Unit)? = null
    var onFiltersChanged: ((Double, Double, Double) -> Unit)? = null

    init {
        createPanelView()
    }

    private fun createPanelView() {
        val inflater = LayoutInflater.from(context)
        panelView = inflater.inflate(R.layout.floating_layout, null)

        switchActive = panelView!!.findViewById(R.id.switchActive)
        txtStatus = panelView!!.findViewById(R.id.txtStatus)
        txtAlerta = panelView!!.findViewById(R.id.txtAlerta)
        inputMinPrice = panelView!!.findViewById(R.id.inputMinPrice)
        inputMaxDistance = panelView!!.findViewById(R.id.inputMaxDistance)
        inputMinRating = panelView!!.findViewById(R.id.inputMinRating)
        btnSaveFilters = panelView!!.findViewById(R.id.btnSaveFilters)
        btnMinimize = panelView!!.findViewById(R.id.btnMinimize)
        panelTitle = panelView!!.findViewById(R.id.panelTitle)

        loadFilters()

        switchActive.isChecked = isServiceActive()
        switchActive.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_IS_ACTIVE, isChecked).apply()
            updateStatusText(isChecked)
            onActiveChanged?.invoke(isChecked)
        }

        btnSaveFilters.setOnClickListener { saveFilters() }
        btnMinimize.setOnClickListener { toggleMinimize() }
        setupDragging()
    }

    private fun setupDragging() {
        var initialX = 0f
        var initialY = 0f
        var initialTouchX = 0f
        var initialTouchY = 0f

        panelView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = panelView?.x ?: 0f
                    initialY = panelView?.y ?: 0f
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isMinimized) {
                        panelView?.x = initialX + event.rawX - initialTouchX
                        panelView?.y = initialY + event.rawY - initialTouchY
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateStatusText(isActive: Boolean) {
        if (isActive) {
            txtStatus.text = "🟢 ACTIVO"
            txtStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            txtStatus.text = "🔴 INACTIVO"
            txtStatus.setTextColor(0xFFF44336.toInt())
        }
    }

    private fun loadFilters() {
        inputMinPrice.setText(prefs.getFloat(KEY_MIN_PRICE, 0f).toString())
        inputMaxDistance.setText(prefs.getFloat(KEY_MAX_DISTANCE, 0f).toString())
        inputMinRating.setText(prefs.getFloat(KEY_MIN_RATING, 0f).toString())
    }

    private fun saveFilters() {
        try {
            val minPrice = inputMinPrice.text.toString().toFloatOrNull() ?: 0f
            val maxDistance = inputMaxDistance.text.toString().toFloatOrNull() ?: 0f
            val minRating = inputMinRating.text.toString().toFloatOrNull() ?: 0f

            prefs.edit().apply {
                putFloat(KEY_MIN_PRICE, minPrice)
                putFloat(KEY_MAX_DISTANCE, maxDistance)
                putFloat(KEY_MIN_RATING, minRating)
                apply()
            }

            onFiltersChanged?.invoke(minPrice.toDouble(), maxDistance.toDouble(), minRating.toDouble())
            txtAlerta.text = "✅ Filtros guardados"
            txtAlerta.setTextColor(0xFF4CAF50.toInt())
            
            handler.postDelayed({
                if (txtAlerta.text == "✅ Filtros guardados") {
                    txtAlerta.text = ""
                }
            }, 2000)
        } catch (e: Exception) {
            txtAlerta.text = "❌ Error al guardar"
            txtAlerta.setTextColor(0xFFF44336.toInt())
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        if (isMinimized) {
            switchActive.visibility = View.GONE
            txtStatus.visibility = View.GONE
            txtAlerta.visibility = View.GONE
            inputMinPrice.visibility = View.GONE
            inputMaxDistance.visibility = View.GONE
            inputMinRating.visibility = View.GONE
            btnSaveFilters.visibility = View.GONE
            panelTitle.text = "▶ DriverPro"
            btnMinimize.text = "▲"
        } else {
            switchActive.visibility = View.VISIBLE
            txtStatus.visibility = View.VISIBLE
            txtAlerta.visibility = View.VISIBLE
            inputMinPrice.visibility = View.VISIBLE
            inputMaxDistance.visibility = View.VISIBLE
            inputMinRating.visibility = View.VISIBLE
            btnSaveFilters.visibility = View.VISIBLE
            panelTitle.text = "DriverPro"
            btnMinimize.text = "▼"
        }
    }

    fun isServiceActive(): Boolean = prefs.getBoolean(KEY_IS_ACTIVE, false)

    fun evaluateFilters(price: Double, distance: Double, rating: Double): Boolean {
        val minPrice = prefs.getFloat(KEY_MIN_PRICE, 0f).toDouble()
        val maxDistance = prefs.getFloat(KEY_MAX_DISTANCE, 0f).toDouble()
        val minRating = prefs.getFloat(KEY_MIN_RATING, 0f).toDouble()

        if (minPrice == 0.0 && maxDistance == 0.0 && minRating == 0.0) return true

        val priceOk = minPrice == 0.0 || price >= minPrice
        val distanceOk = maxDistance == 0.0 || distance <= maxDistance
        val ratingOk = minRating == 0.0 || rating >= minRating

        return priceOk && distanceOk && ratingOk
    }

    fun updateAlerta(message: String, cumple: Boolean) {
        txtAlerta.text = message
        txtAlerta.setTextColor(if (cumple) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
    }

    fun show() {
        if (!isAttached) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                params.x = 100
                params.y = 100

                windowManager.addView(panelView, params)
                isAttached = true
                updateStatusText(isServiceActive())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        if (isAttached) {
            try {
                windowManager.removeView(panelView)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleActive() {
        switchActive.isChecked = !switchActive.isChecked
    }

    fun destroy() {
        hide()
        handler.removeCallbacksAndMessages(null)
    }
}
