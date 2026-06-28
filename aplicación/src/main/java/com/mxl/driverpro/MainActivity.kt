package com.mxl.driverpro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)
        
        val switchBot = findViewById<Switch>(R.id.switchBot)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val tvTrips = findViewById<TextView>(R.id.tvTrips)
        val etMinPrice = findViewById<EditText>(R.id.etMinPrice)
        val etMaxDist = findViewById<EditText>(R.id.etMaxDist)
        val etMinRating = findViewById<EditText>(R.id.etMinRating)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Cargar valores guardados
        switchBot.isChecked = prefs.getBoolean("is_active", true)
        etMinPrice.setText(prefs.getFloat("min_price", 150f).toString())
        etMaxDist.setText(prefs.getFloat("max_pickup_dist", 2f).toString())
        etMinRating.setText(prefs.getFloat("min_rating", 4f).toString())
        tvTrips.text = "Viajes aceptados hoy: " + prefs.getInt("trip_count", 0)

        switchBot.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("is_active", checked).apply()
            Toast.makeText(this, if (checked) "Bot ACTIVADO" else "Bot DESACTIVADO", Toast.LENGTH_SHORT).show()
        }

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putFloat("min_price", etMinPrice.text.toString().toFloatOrNull() ?: 150f)
                .putFloat("max_pickup_dist", etMaxDist.text.toString().toFloatOrNull() ?: 2f)
                .putFloat("min_rating", etMinRating.text.toString().toFloatOrNull() ?: 4f)
                .apply()
            Toast.makeText(this, "Filtros guardados", Toast.LENGTH_SHORT).show()
        }
    }
}
