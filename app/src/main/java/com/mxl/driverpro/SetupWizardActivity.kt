package com.mxl.driverpro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private var currentStep = 0
    private var modoAuto = true
    private var metaDiaria = 2500f
    private var minPrice = 200f
    private var maxDist = 3f
    private var minRating = 4.0f
    private var plataforma = "InDrive"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)
        prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)
        showStep(0)
    }

    private fun showStep(step: Int) {
        currentStep = step
        val container = findViewById<LinearLayout>(R.id.wizardContainer)
        container.removeAllViews()

        when (step) {
            0 -> buildStepModo(container)
            1 -> buildStepConfig(container)
            2 -> buildStepPlataforma(container)
            3 -> buildStepResumen(container)
        }
    }

    private fun titulo(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 22f
            setTextColor(0xFF2196F3.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
    }

    private fun subtitulo(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFF90CAF9.toInt())
            setPadding(0, 0, 0, 24)
        }
    }

    private fun boton(text: String, color: Int, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140).apply { setMargins(0, 12, 0, 0) }
            setOnClickListener { action() }
        }
    }

    private fun buildStepModo(c: LinearLayout) {
        c.addView(titulo("Paso 1 de 4"))
        c.addView(subtitulo("¿Cómo quieres configurar el bot?"))

        val btnAuto = boton("AUTOMATICO - El bot configura todo", 0xFF1565C0.toInt()) {
            modoAuto = true
            guardarModo(true)
            showStep(1)
        }
        val btnManual = boton("MANUAL - Yo configuro mis filtros", 0xFF37474F.toInt()) {
            modoAuto = false
            guardarModo(false)
            showStep(1)
        }
        c.addView(btnAuto)
        c.addView(btnManual)
    }

    private fun guardarModo(auto: Boolean) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        db.collection("licencias").document(deviceId)
            .update("automatico_activo", auto)
            .addOnFailureListener {
                // Si no existe el doc, crear
                db.collection("licencias").document(deviceId)
                    .set(mapOf("automatico_activo" to auto), com.google.firebase.firestore.SetOptions.merge())
            }
        prefs.edit().putBoolean("modo_automatico", auto).apply()
    }

    private fun buildStepConfig(c: LinearLayout) {
        c.addView(titulo("Paso 2 de 4"))

        if (modoAuto) {
            c.addView(subtitulo("¿Cuánto quieres ganar por día?"))

            val tvMeta = TextView(this).apply {
                text = "Meta: RD$2,500"
                textSize = 18f
                setTextColor(0xFF4CAF50.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 8)
            }
            val tvCalculo = TextView(this).apply {
                text = "Precio min: RD$200 · Dist max: 3km · Rating: 4.0"
                textSize = 12f
                setTextColor(0xFF607D8B.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }
            val slider = SeekBar(this).apply {
                max = 75 // (8000-500)/100
                progress = 20
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                        metaDiaria = (500 + p * 100).toFloat()
                        minPrice = (metaDiaria / 12).toInt().toFloat()
                        maxDist = if (metaDiaria < 2000) 4f else if (metaDiaria < 3500) 3f else 2f
                        minRating = if (metaDiaria > 4000) 4.5f else 4.0f
                        tvMeta.text = "Meta: RD$%.0f".format(metaDiaria)
                        tvCalculo.text = "Precio min: RD$%.0f · Dist max: ${maxDist.toInt()}km · Rating: ${"%.1f".format(minRating)}".format(minPrice)
                    }
                    override fun onStartTrackingTouch(sb: SeekBar) {}
                    override fun onStopTrackingTouch(sb: SeekBar) {}
                })
            }

            // Chips rapidos
            val chips = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            listOf("RD$1,500" to 1500, "RD$2,500" to 2500, "RD$3,500" to 3500, "RD$5,000" to 5000).forEach { (label, valor) ->
                Button(this).apply {
                    text = label
                    textSize = 11f
                    backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1A237E.toInt())
                    setTextColor(0xFF82B1FF.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, 80, 1f).apply { setMargins(4, 0, 4, 0) }
                    setOnClickListener {
                        metaDiaria = valor.toFloat()
                        minPrice = (metaDiaria / 12).toInt().toFloat()
                        maxDist = if (metaDiaria < 2000) 4f else if (metaDiaria < 3500) 3f else 2f
                        minRating = if (metaDiaria > 4000) 4.5f else 4.0f
                        tvMeta.text = "Meta: RD$$valor"
                        tvCalculo.text = "Precio min: RD$%.0f · Dist max: ${maxDist.toInt()}km · Rating: ${"%.1f".format(minRating)}".format(minPrice)
                        slider.progress = ((valor - 500) / 100)
                    }
                    chips.addView(this)
                }
            }
            c.addView(tvMeta)
            c.addView(slider)
            c.addView(tvCalculo)
            c.addView(chips)
        } else {
            // Manual
            c.addView(subtitulo("Configura tus filtros manualmente"))
            listOf(
                Triple("Precio mínimo (RD$)", 50..800, 150),
                Triple("Distancia máx recojo (km)", 1..15, 3),
                Triple("Rating mínimo pasajero", 1..5, 4)
            ).forEachIndexed { idx, (label, range, default) ->
                val tv = TextView(this).apply {
                    text = "$label: $default"
                    textSize = 13f; setTextColor(0xFF90CAF9.toInt()); setPadding(0, 12, 0, 4)
                }
                val sb = SeekBar(this).apply {
                    max = range.last - range.first
                    progress = default - range.first
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) {
                            val v = range.first + p
                            tv.text = "$label: $v"
                            when (idx) { 0 -> minPrice = v.toFloat(); 1 -> maxDist = v.toFloat(); 2 -> minRating = v.toFloat() }
                        }
                        override fun onStartTrackingTouch(s: SeekBar) {}
                        override fun onStopTrackingTouch(s: SeekBar) {}
                    })
                }
                c.addView(tv); c.addView(sb)
            }
        }

        c.addView(boton("Continuar →", 0xFF2196F3.toInt()) { showStep(2) })
        c.addView(boton("← Atrás", 0xFF37474F.toInt()) { showStep(0) })
    }

    private fun buildStepPlataforma(c: LinearLayout) {
        c.addView(titulo("Paso 3 de 4"))
        c.addView(subtitulo("¿En qué plataforma trabajas?"))
        listOf("InDrive" to 0xFF1565C0.toInt(), "Uber" to 0xFF37474F.toInt(), "DiDi" to 0xFF37474F.toInt(), "Todas" to 0xFF2E7D32.toInt()).forEach { (nombre, color) ->
            c.addView(boton(nombre, color) {
                plataforma = nombre
                prefs.edit().putString("plataforma", nombre).apply()
                showStep(3)
            })
        }
        c.addView(boton("← Atrás", 0xFF37474F.toInt()) { showStep(1) })
    }

    private fun buildStepResumen(c: LinearLayout) {
        c.addView(titulo("Tu configuración lista"))
        listOf(
            "Modo" to if (modoAuto) "Automático" else "Manual",
            "Plataforma" to plataforma,
            "Precio mínimo" to "RD$%.0f".format(minPrice),
            "Distancia máx" to "${maxDist.toInt()} km",
            "Rating mínimo" to "%.1f ★".format(minRating),
            if (modoAuto) "Meta diaria" to "RD$%.0f".format(metaDiaria) else "Modo" to "Manual"
        ).forEach { (k, v) ->
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                setBackgroundColor(0x11FFFFFF)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 2, 0, 2)
                layoutParams = lp
                addView(TextView(this@SetupWizardActivity).apply {
                    text = k; textSize = 13f; setTextColor(0xFF607D8B.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(12, 8, 0, 8)
                })
                addView(TextView(this@SetupWizardActivity).apply {
                    text = v; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, 8, 12, 8)
                })
                c.addView(this)
            }
        }

        c.addView(boton("APLICAR Y ACTIVAR BOT", 0xFF2196F3.toInt()) {
            // Guardar todo
            prefs.edit()
                .putFloat("min_price", minPrice)
                .putFloat("max_distance", maxDist)
                .putFloat("min_rating", minRating)
                .putBoolean("is_active", true)
                .putBoolean("modo_automatico", modoAuto)
                .putString("plataforma", plataforma)
                .apply()
            Toast.makeText(this, "Configuracion aplicada. Bot activado.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        })
        c.addView(boton("← Atrás", 0xFF37474F.toInt()) { showStep(2) })
    }
}
