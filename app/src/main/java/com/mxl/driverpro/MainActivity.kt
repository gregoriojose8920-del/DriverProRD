package com.mxl.driverpro

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mxl.driverpro.logic.FloatingPanel

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var switchBot: Switch
    private lateinit var tvTrips: TextView

    // Receptor de cambios desde el panel flotante
    private val panelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                FloatingPanel.ACTION_SYNC -> {
                    val active = intent.getBooleanExtra(FloatingPanel.EXTRA_ACTIVE, true)
                    switchBot.isChecked = active
                }
                FloatingPanel.ACTION_TRIP_ACCEPTED -> {
                    val trips = prefs.getInt("trip_count", 0)
                    val estimado = (trips * prefs.getFloat("min_price", 150f)).toInt()
                    tvTrips.text = "Viajes hoy: \$trips  |  Est: RD\$$estimado"
                }
                FloatingPanel.ACTION_FILTERS_SAVED -> {
                    // Actualizar campos en pantalla con valores del panel
                    val price = intent.getFloatExtra(FloatingPanel.EXTRA_PRICE, 150f)
                    val dist = intent.getFloatExtra(FloatingPanel.EXTRA_DIST, 3f)
                    val rating = intent.getFloatExtra(FloatingPanel.EXTRA_RATING, 4f)
                    findViewById<android.widget.EditText>(R.id.etMinPrice).setText(price.toInt().toString())
                    findViewById<android.widget.EditText>(R.id.etMaxDist).setText(dist.toInt().toString())
                    findViewById<android.widget.EditText>(R.id.etMinRating).setText(rating.toString())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)

        if (!prefs.contains("inicio_dia")) {
            prefs.edit().putLong("inicio_dia", System.currentTimeMillis()).apply()
        }

        switchBot = findViewById(R.id.switchBot)
        tvTrips = findViewById(R.id.tvTrips)
        val etMinPrice = findViewById<EditText>(R.id.etMinPrice)
        val etMaxDist = findViewById<EditText>(R.id.etMaxDist)
        val etMinRating = findViewById<EditText>(R.id.etMinRating)

        etMinPrice.setText(prefs.getFloat("min_price", 150f).toInt().toString())
        etMaxDist.setText(prefs.getFloat("max_distance", 3f).toInt().toString())
        etMinRating.setText(prefs.getFloat("min_rating", 4f).toString())
        switchBot.isChecked = prefs.getBoolean("is_active", true)
        updateTripsDisplay()

        switchBot.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("is_active", checked).apply()
            Toast.makeText(this, if (checked) "Bot ACTIVADO" else "Bot PAUSADO", Toast.LENGTH_SHORT).show()
            // Sincronizar con panel flotante
            sendBroadcast(Intent(FloatingPanel.ACTION_SYNC).putExtra(FloatingPanel.EXTRA_ACTIVE, checked))
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:\$packageName")))
        }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putFloat("min_price", etMinPrice.text.toString().toFloatOrNull() ?: 150f)
                .putFloat("max_distance", etMaxDist.text.toString().toFloatOrNull() ?: 3f)
                .putFloat("min_rating", etMinRating.text.toString().toFloatOrNull() ?: 4f)
                .apply()
            Toast.makeText(this, "Filtros guardados", Toast.LENGTH_SHORT).show()
            // Sincronizar con panel flotante
            sendBroadcast(Intent(FloatingPanel.ACTION_FILTERS_SAVED).apply {
                putExtra(FloatingPanel.EXTRA_PRICE, etMinPrice.text.toString().toFloatOrNull() ?: 150f)
                putExtra(FloatingPanel.EXTRA_DIST, etMaxDist.text.toString().toFloatOrNull() ?: 3f)
                putExtra(FloatingPanel.EXTRA_RATING, etMinRating.text.toString().toFloatOrNull() ?: 4f)
            })
        }
        findViewById<Button>(R.id.btnSetup).setOnClickListener {
            startActivity(Intent(this, SetupWizardActivity::class.java))
        }
        findViewById<Button>(R.id.btnMapa).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        findViewById<Button>(R.id.btnStats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSuscripcion).setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(FloatingPanel.ACTION_SYNC)
            addAction(FloatingPanel.ACTION_TRIP_ACCEPTED)
            addAction(FloatingPanel.ACTION_FILTERS_SAVED)
        }
        registerReceiver(panelReceiver, filter)

        val overlayOk = Settings.canDrawOverlays(this)
        val accessOk = isAccessibilityEnabled()
        switchBot.isChecked = prefs.getBoolean("is_active", true)

        findViewById<TextView>(R.id.tvOverlayStatus).apply {
            text = if (overlayOk) "Panel Flotante: ACTIVO" else "Panel Flotante: INACTIVO"
            setTextColor(if (overlayOk) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt())
        }
        findViewById<TextView>(R.id.tvAccessStatus).apply {
            text = if (accessOk) "Accesibilidad: ACTIVA" else "Accesibilidad: INACTIVA"
            setTextColor(if (accessOk) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt())
        }
        updateTripsDisplay()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(panelReceiver) } catch (e: Exception) {}
    }

    private fun updateTripsDisplay() {
        val trips = prefs.getInt("trip_count", 0)
        val estimado = (trips * prefs.getFloat("min_price", 150f)).toInt()
        tvTrips.text = "Viajes hoy: \$trips  |  Est: RD\$$estimado"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val cn = ComponentName.unflattenFromString(splitter.next()) ?: continue
            if (cn.packageName == packageName) return true
        }
        return false
    }
}
