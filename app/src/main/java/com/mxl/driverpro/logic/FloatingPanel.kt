package com.mxl.driverpro.logic

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.*
import com.mxl.driverpro.R

class FloatingPanel(private val context: Context) {

    companion object {
        const val PREFS_NAME = "driverpro_prefs"
        const val ACTION_SYNC = "com.mxl.driverpro.SYNC_STATE"
        const val EXTRA_ACTIVE = "is_active"
        const val ACTION_TRIP_ACCEPTED = "com.mxl.driverpro.TRIP_ACCEPTED"
        const val ACTION_FILTERS_SAVED = "com.mxl.driverpro.FILTERS_SAVED"
        const val EXTRA_PRICE = "min_price"
        const val EXTRA_DIST = "max_distance"
        const val EXTRA_RATING = "min_rating"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var panelView: View? = null
    private var isAttached = false
    private var isMinimized = false
    private var pulseAnimator: ValueAnimator? = null
    private var radarAnimator: ObjectAnimator? = null

    private lateinit var switchActive: Switch
    private lateinit var txtStatus: TextView
    private lateinit var tvBuscando: TextView
    private lateinit var txtAlerta: TextView
    private lateinit var tvTripCounter: TextView
    private lateinit var tvGanancias: TextView
    private lateinit var tvLicencia: TextView
    private lateinit var tvRadar: TextView
    private lateinit var viewPulse: View
    private lateinit var inputMinPrice: EditText
    private lateinit var inputMaxDistance: EditText
    private lateinit var inputMinRating: EditText
    private lateinit var btnSaveFilters: Button
    private lateinit var btnMinimize: Button
    private lateinit var panelTitle: TextView
    private lateinit var panelContent: View

    // Receptor de sincronizacion desde MainActivity
    private val syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SYNC -> {
                    val active = intent.getBooleanExtra(EXTRA_ACTIVE, true)
                    switchActive.isChecked = active
                    updateStatusText(active)
                }
                ACTION_TRIP_ACCEPTED -> {
                    refreshStats()
                    flashTripAlert()
                }
                ACTION_FILTERS_SAVED -> {
                    // Actualizar campos del panel con valores de MainActivity
                    val price = intent.getFloatExtra(EXTRA_PRICE, 150f)
                    val dist = intent.getFloatExtra(EXTRA_DIST, 3f)
                    val rating = intent.getFloatExtra(EXTRA_RATING, 4f)
                    inputMinPrice.setText(price.toInt().toString())
                    inputMaxDistance.setText(dist.toInt().toString())
                    inputMinRating.setText(rating.toString())
                }
            }
        }
    }

    var onActiveChanged: ((Boolean) -> Unit)? = null

    init { createPanelView() }

    private fun createPanelView() {
        panelView = LayoutInflater.from(context).inflate(R.layout.floating_layout, null)

        switchActive = panelView!!.findViewById(R.id.switchActive)
        txtStatus = panelView!!.findViewById(R.id.txtStatus)
        tvBuscando = panelView!!.findViewById(R.id.tvBuscando)
        txtAlerta = panelView!!.findViewById(R.id.txtAlerta)
        tvTripCounter = panelView!!.findViewById(R.id.tvTripCounter)
        tvGanancias = panelView!!.findViewById(R.id.tvGanancias)
        tvLicencia = panelView!!.findViewById(R.id.tvLicencia)
        tvRadar = panelView!!.findViewById(R.id.tvRadar)
        viewPulse = panelView!!.findViewById(R.id.viewPulse)
        inputMinPrice = panelView!!.findViewById(R.id.inputMinPrice)
        inputMaxDistance = panelView!!.findViewById(R.id.inputMaxDistance)
        inputMinRating = panelView!!.findViewById(R.id.inputMinRating)
        btnSaveFilters = panelView!!.findViewById(R.id.btnSaveFilters)
        btnMinimize = panelView!!.findViewById(R.id.btnMinimize)
        panelTitle = panelView!!.findViewById(R.id.panelTitle)
        panelContent = panelView!!.findViewById(R.id.panelContent)

        loadFilters()
        val isActive = prefs.getBoolean("is_active", true)
        switchActive.isChecked = isActive
        updateStatusText(isActive)
        refreshStats()

        switchActive.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_active", isChecked).apply()
            updateStatusText(isChecked)
            onActiveChanged?.invoke(isChecked)
            // Broadcast a MainActivity
            val intent = Intent(ACTION_SYNC).putExtra(EXTRA_ACTIVE, isChecked)
            context.sendBroadcast(intent)
        }

        btnSaveFilters.setOnClickListener { saveFilters() }
        btnMinimize.setOnClickListener { toggleMinimize() }
        setupDragging()

        // Registrar receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_SYNC)
            addAction(ACTION_TRIP_ACCEPTED)
            addAction(ACTION_FILTERS_SAVED)
        }
        context.registerReceiver(syncReceiver, filter)

        // Actualizar stats cada 10 segundos
        handler.post(object : Runnable {
            override fun run() {
                refreshStats()
                handler.postDelayed(this, 10000)
            }
        })
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                viewPulse.layoutParams = (viewPulse.layoutParams as LinearLayout.LayoutParams).also {
                    it.weight = fraction
                }
                viewPulse.requestLayout()
            }
            start()
        }

        // Animacion del radar emoji
        val busquedaTextos = arrayOf(
            "Buscando viajes...",
            "Escaneando InDrive...",
            "Monitoreando...",
            "En busqueda activa...",
            "Listo para aceptar..."
        )
        var idx = 0
        handler.post(object : Runnable {
            override fun run() {
                if (prefs.getBoolean("is_active", true)) {
                    tvBuscando.text = busquedaTextos[idx % busquedaTextos.size]
                    idx++
                    handler.postDelayed(this, 2000)
                }
            }
        })
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        viewPulse.layoutParams = (viewPulse.layoutParams as LinearLayout.LayoutParams).also {
            it.weight = 0f
        }
        tvBuscando.text = "Bot pausado"
    }

    private fun flashTripAlert() {
        txtAlerta.text = "🚗 VIAJE ACEPTADO!"
        txtAlerta.setTextColor(0xFF4CAF50.toInt())
        handler.postDelayed({ txtAlerta.text = "" }, 3000)
    }

    private fun refreshStats() {
        val trips = prefs.getInt("trip_count", 0)
        val minPrice = prefs.getFloat("min_price", 150f)
        val estimado = (trips * minPrice).toInt()
        tvTripCounter.text = "Viajes hoy: $trips"
        tvGanancias.text = "Estimado: RD$$estimado"
    }

    fun updateLicencia(status: String, activo: Boolean) {
        tvLicencia.text = "Licencia: $status"
        tvLicencia.setTextColor(if (activo) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt())
    }

    fun incrementTrip() {
        refreshStats()
        flashTripAlert()
        context.sendBroadcast(Intent(ACTION_TRIP_ACCEPTED))
    }

    private fun updateStatusText(isActive: Boolean) {
        if (isActive) {
            txtStatus.text = "🟢 ACTIVO"
            txtStatus.setTextColor(0xFF4CAF50.toInt())
            startPulseAnimation()
        } else {
            txtStatus.text = "🔴 INACTIVO"
            txtStatus.setTextColor(0xFFF44336.toInt())
            stopPulseAnimation()
        }
    }

    private fun loadFilters() {
        inputMinPrice.setText(prefs.getFloat("min_price", 150f).toInt().toString())
        inputMaxDistance.setText(prefs.getFloat("max_distance", 3f).toInt().toString())
        inputMinRating.setText(prefs.getFloat("min_rating", 4f).toString())
    }

    private fun saveFilters() {
        try {
            val price = inputMinPrice.text.toString().toFloatOrNull() ?: 150f
            val dist = inputMaxDistance.text.toString().toFloatOrNull() ?: 3f
            val rating = inputMinRating.text.toString().toFloatOrNull() ?: 4f
            prefs.edit()
                .putFloat("min_price", price)
                .putFloat("max_distance", dist)
                .putFloat("min_rating", rating)
                .apply()
            txtAlerta.text = "✅ Guardado"
            txtAlerta.setTextColor(0xFF4CAF50.toInt())
            handler.postDelayed({ txtAlerta.text = "" }, 2000)
            // Sincronizar filtros con MainActivity
            val syncIntent = Intent(ACTION_FILTERS_SAVED).apply {
                putExtra(EXTRA_PRICE, price)
                putExtra(EXTRA_DIST, dist)
                putExtra(EXTRA_RATING, rating)
            }
            context.sendBroadcast(syncIntent)
        } catch (e: Exception) {
            txtAlerta.text = "❌ Error"
        }
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        panelContent.visibility = if (isMinimized) View.GONE else View.VISIBLE
        btnMinimize.text = if (isMinimized) "▲" else "▼"
        panelTitle.text = if (isMinimized) "▶ DriverPro" else "DriverPro"
        if (!isMinimized) refreshStats()
    }

    private fun setupDragging() {
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        var params: WindowManager.LayoutParams? = null
        panelView?.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (params == null) params = panelView?.layoutParams as? WindowManager.LayoutParams
                    ix = params?.x ?: 0; iy = params?.y ?: 0
                    tx = event.rawX; ty = event.rawY; true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params?.x = ix + (event.rawX - tx).toInt()
                    params?.y = iy + (event.rawY - ty).toInt()
                    if (isAttached) windowManager.updateViewLayout(panelView, params)
                    true
                }
                else -> false
            }
        }
    }

    fun show() {
        if (!isAttached) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                params.x = 20; params.y = 150
                windowManager.addView(panelView, params)
                isAttached = true
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun destroy() {
        try { context.unregisterReceiver(syncReceiver) } catch (e: Exception) {}
        pulseAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        if (isAttached) {
            try { windowManager.removeView(panelView); isAttached = false }
            catch (e: Exception) {}
        }
    }
}
