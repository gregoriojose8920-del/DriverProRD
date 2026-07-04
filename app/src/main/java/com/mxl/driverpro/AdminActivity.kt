package com.mxl.driverpro

import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AdminActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val ADMIN_PASSWORD = "8920"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        verificarAdmin()
    }

    private fun verificarAdmin() {
        val container = findViewById<LinearLayout>(R.id.adminContainer)
        container.removeAllViews()

        // Password de acceso
        val tvTitulo = TextView(this).apply {
            text = "Panel Administrador"
            textSize = 22f
            setTextColor(0xFF2196F3.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 16)
        }

        val etPass = EditText(this).apply {
            hint = "Contraseña de admin"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF546E7A.toInt())
            setPadding(0, 8, 0, 8)
        }

        val btnEntrar = Button(this).apply {
            text = "ENTRAR"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130).apply { setMargins(0, 16, 0, 0) }
            setOnClickListener {
                if (etPass.text.toString() == ADMIN_PASSWORD) {
                    mostrarPanel(container)
                } else {
                    Toast.makeText(this@AdminActivity, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
        }

        container.addView(tvTitulo)
        container.addView(etPass)
        container.addView(btnEntrar)
    }

    private fun mostrarPanel(container: LinearLayout) {
        container.removeAllViews()

        // Titulo
        container.addView(TextView(this).apply {
            text = "Gestión de Licencias"
            textSize = 20f
            setTextColor(0xFF2196F3.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 24)
        })

        // ACTIVAR LICENCIA
        container.addView(TextView(this).apply {
            text = "ACTIVAR LICENCIA"
            textSize = 13f
            setTextColor(0xFF4CAF50.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 8)
        })

        val etDeviceId = EditText(this).apply {
            hint = "Device ID del cliente"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF546E7A.toInt())
            setPadding(0, 8, 0, 8)
        }

        // Selector de plan
        val spinnerPlan = Spinner(this)
        val planes = arrayOf(
            "1 Mes - RD\$500",
            "3 Meses - RD\$1,300",
            "6 Meses - RD\$2,400",
            "1 Año - RD\$4,200"
        )
        val diasPlan = intArrayOf(30, 90, 180, 365)
        spinnerPlan.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, planes)
        spinnerPlan.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())

        val btnActivar = Button(this).apply {
            text = "ACTIVAR AHORA"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130).apply { setMargins(0, 12, 0, 0) }
            setOnClickListener {
                val deviceId = etDeviceId.text.toString().trim()
                if (deviceId.isEmpty()) {
                    Toast.makeText(this@AdminActivity, "Ingresa el Device ID", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val planIdx = spinnerPlan.selectedItemPosition
                val dias = diasPlan[planIdx]
                val planNombre = planes[planIdx]
                activarLicencia(deviceId, dias, planNombre, container)
            }
        }

        container.addView(etDeviceId)
        container.addView(spinnerPlan)
        container.addView(btnActivar)

        // DESACTIVAR LICENCIA
        container.addView(TextView(this).apply {
            text = "DESACTIVAR LICENCIA"
            textSize = 13f
            setTextColor(0xFFFF5252.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 24, 0, 8)
        })

        val etDeviceIdDes = EditText(this).apply {
            hint = "Device ID a desactivar"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF5252.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF546E7A.toInt())
            setPadding(0, 8, 0, 8)
        }

        val btnDesactivar = Button(this).apply {
            text = "DESACTIVAR"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFB71C1C.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130).apply { setMargins(0, 12, 0, 0) }
            setOnClickListener {
                val deviceId = etDeviceIdDes.text.toString().trim()
                if (deviceId.isEmpty()) {
                    Toast.makeText(this@AdminActivity, "Ingresa el Device ID", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                desactivarLicencia(deviceId, container)
            }
        }

        container.addView(etDeviceIdDes)
        container.addView(btnDesactivar)

        // VER TODOS LOS CLIENTES
        container.addView(TextView(this).apply {
            text = "CLIENTES ACTIVOS"
            textSize = 13f
            setTextColor(0xFF90CAF9.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 24, 0, 8)
        })

        val btnVerClientes = Button(this).apply {
            text = "VER TODOS LOS CLIENTES"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { setMargins(0, 8, 0, 0) }
            setOnClickListener { cargarClientes(container) }
        }
        container.addView(btnVerClientes)
    }

    private fun activarLicencia(deviceId: String, dias: Int, plan: String, container: LinearLayout) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dias)
        val expira = cal.time

        val data = hashMapOf(
            "device_id" to deviceId,
            "plan" to plan,
            "dias" to dias,
            "activo" to true,
            "automatico_activo" to true,
            "fecha_activacion" to Date(),
            "fecha_expiracion" to expira,
            "activado_por" to "admin"
        )

        db.collection("licencias").document(deviceId)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this,
                    "✅ Licencia ACTIVADA\n$plan\nExpira: $expira",
                    Toast.LENGTH_LONG).show()
                // Log en Firebase
                db.collection("logs_admin").add(mapOf(
                    "accion" to "ACTIVAR",
                    "device_id" to deviceId,
                    "plan" to plan,
                    "fecha" to Date()
                ))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: " + it.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun desactivarLicencia(deviceId: String, container: LinearLayout) {
        db.collection("licencias").document(deviceId)
            .update("activo", false)
            .addOnSuccessListener {
                Toast.makeText(this, "❌ Licencia DESACTIVADA", Toast.LENGTH_SHORT).show()
                db.collection("logs_admin").add(mapOf(
                    "accion" to "DESACTIVAR",
                    "device_id" to deviceId,
                    "fecha" to Date()
                ))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: " + it.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarClientes(container: LinearLayout) {
        db.collection("licencias")
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { docs ->
                val tvResultado = TextView(this).apply {
                    text = "Cargando..."
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 12f
                    setPadding(0, 8, 0, 8)
                }
                container.addView(tvResultado)

                if (docs.isEmpty) {
                    tvResultado.text = "Sin clientes activos"
                    return@addOnSuccessListener
                }

                val sb = StringBuilder()
                sb.append("Total activos: " + docs.size() + "\n\n")
                for (doc in docs) {
                    val plan = doc.getString("plan") ?: "N/A"
                    val expira = doc.getDate("fecha_expiracion")
                    val id = doc.id.take(8) + "..."
                    sb.append("ID: " + id + "\nPlan: " + plan + "\nExpira: " + expira + "\n---\n")
                }
                tvResultado.text = sb.toString()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: " + it.message, Toast.LENGTH_SHORT).show()
            }
    }
}
