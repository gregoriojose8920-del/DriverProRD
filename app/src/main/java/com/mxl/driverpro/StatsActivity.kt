package com.mxl.driverpro

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)
        loadStats()
        findViewById<Button>(R.id.btnResetStats).setOnClickListener {
            prefs.edit()
                .putInt("trip_count", 0)
                .putLong("inicio_dia", System.currentTimeMillis())
                .apply()
            loadStats()
            Toast.makeText(this, "Estadisticas reiniciadas", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnVolverStats).setOnClickListener { finish() }
    }

    private fun loadStats() {
        val trips = prefs.getInt("trip_count", 0)
        val minPrice = prefs.getFloat("min_price", 150f)
        val inicioDia = prefs.getLong("inicio_dia", System.currentTimeMillis())
        val horasActivo = ((System.currentTimeMillis() - inicioDia) / 3600000f)
        val porHora = if (horasActivo > 0) trips / horasActivo else 0f
        val estimado = trips * minPrice

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        findViewById<TextView>(R.id.tvStatTrips).text = "Total viajes: $trips"
        findViewById<TextView>(R.id.tvStatGanancias).text = "Estimado ganado: RD$%.0f".format(estimado)
        findViewById<TextView>(R.id.tvStatPorHora).text = "Viajes/hora: %.1f".format(porHora)
        findViewById<TextView>(R.id.tvStatPrecioMin).text = "Precio minimo: RD$%.0f".format(minPrice)
        findViewById<TextView>(R.id.tvStatInicio).text = "Desde: " + sdf.format(Date(inicioDia))
    }
}
