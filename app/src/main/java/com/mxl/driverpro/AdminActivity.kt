package com.mxl.driverpro

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AdminActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        mostrarLogin()
    }

    private fun mostrarLogin() {
        val container = findViewById<LinearLayout>(R.id.adminContainer)
        container.removeAllViews()

        container.addView(TextView(this).apply {
            text = "Panel Administrador"
            textSize = 22f
            setTextColor(0xFF2196F3.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 16)
        })

        val etPin = EditText(this).apply {
            hint = "PIN de 4 digitos"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF546E7A.toInt())
            gravity = android.view.Gravity.CENTER
            textSize = 24f
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        }

        val btnEntrar = Button(this).apply {
            text = "ENTRAR"
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130).apply { setMargins(0, 16, 0, 0) }
            setOnClickListener {
                if (etPin.text.toString() == "8920") {
                    mostrarPanel()
                } else {
                    Toast.makeText(this@AdminActivity, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
        }
        container.addView(etPin)
        container.addView(btnEntrar)
    }

    private fun mostrarPanel() {
        val container = findViewById<LinearLayout>(R.id.adminContainer)
        container.removeAllViews()

        container.addView(TextView(this).apply {
            text = "Gestion de Licencias"
            textSize = 20f
            setTextColor(0xFF2196F3.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 24)
        })

        // ACTIVAR
        container.addView(labelView("ACTIVAR LICENCIA", 0xFF4CAF50.toInt()))
        val etDeviceId = inputView("Device ID del cliente", 0xFF4CAF50.toInt())
        val planes = arrayOf("1 Mes - RD500", "3 Meses - RD1300", "6 Meses - RD2400", "1 Ano - RD4200")
        val dias = intArrayOf(30, 90, 180, 365)
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, planes)
        val btnActivar = botonView("ACTIVAR AHORA", 0xFF2E7D32.toInt()) {
            val id = etDeviceId.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "Ingresa el Device ID", Toast.LENGTH_SHORT).show()
                return@botonView
            }
            val idx = spinner.selectedItemPosition
            activar(id, dias[idx], planes[idx])
        }
        container.addView(etDeviceId)
        container.addView(spinner)
        container.addView(btnActivar)

        // DESACTIVAR
        container.addView(labelView("DESACTIVAR LICENCIA", 0xFFFF5252.toInt()))
        val etDes = inputView("Device ID a desactivar", 0xFFFF5252.toInt())
        val btnDes = botonView("DESACTIVAR", 0xFFB71C1C.toInt()) {
            val id = etDes.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "Ingresa el Device ID", Toast.LENGTH_SHORT).show()
                return@botonView
            }
            desactivar(id)
        }
        container.addView(etDes)
        container.addView(btnDes)

        // CONDUCTORES REGISTRADOS
        container.addView(labelView("CONDUCTORES REGISTRADOS", 0xFFCE93D8.toInt()))
        val tvConductores = resultView()
        val btnCond = botonView("VER TODOS LOS REGISTROS", 0xFF4A148C.toInt()) {
            db.collection("conductores").get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        tvConductores.text = "Sin registros"
                        return@addOnSuccessListener
                    }
                    val sb = StringBuilder()
                    sb.append("Total: ")
                    sb.append(docs.size())
                    sb.append("\n\n")
                    for (doc in docs) {
                        sb.append("Nombre: ")
                        sb.append(doc.getString("nombre") ?: "N/A")
                        sb.append("\nCorreo: ")
                        sb.append(doc.getString("correo") ?: "N/A")
                        sb.append("\nTel: ")
                        sb.append(doc.getString("telefono") ?: "N/A")
                        sb.append("\nVehiculo: ")
                        sb.append(doc.getString("vehiculo") ?: "N/A")
                        sb.append("\nID: ")
                        sb.append(doc.getString("device_id") ?: "N/A")
                        sb.append("\n---\n")
                    }
                    tvConductores.text = sb.toString()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error cargando conductores", Toast.LENGTH_SHORT).show()
                }
        }
        container.addView(btnCond)
        container.addView(tvConductores)

        // CLIENTES ACTIVOS
        container.addView(labelView("CLIENTES ACTIVOS", 0xFF90CAF9.toInt()))
        val tvClientes = resultView()
        val btnClientes = botonView("VER TODOS LOS CLIENTES", 0xFF1565C0.toInt()) {
            db.collection("licencias").whereEqualTo("activo", true).get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        tvClientes.text = "Sin clientes activos"
                        return@addOnSuccessListener
                    }
                    val sb = StringBuilder()
                    sb.append("Total activos: ")
                    sb.append(docs.size())
                    sb.append("\n\n")
                    for (doc in docs) {
                        val shortId = doc.id.take(8) + "..."
                        val nombrePlan = doc.getString("plan") ?: "N/A"
                        val fechaExp = doc.getDate("fecha_expiracion")
                        sb.append("ID: ")
                        sb.append(shortId)
                        sb.append("\nPlan: ")
                        sb.append(nombrePlan)
                        sb.append("\nExpira: ")
                        sb.append(fechaExp)
                        sb.append("\n---\n")
                    }
                    tvClientes.text = sb.toString()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error cargando clientes", Toast.LENGTH_SHORT).show()
                }
        }
        container.addView(btnClientes)
        container.addView(tvClientes)
    }

    private fun labelView(texto: String, color: Int) = TextView(this).apply {
        text = texto
        textSize = 13f
        setTextColor(color)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0, 24, 0, 8)
    }

    private fun inputView(hint: String, color: Int) = EditText(this).apply {
        this.hint = hint
        backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF546E7A.toInt())
        setPadding(0, 8, 0, 8)
    }

    private fun resultView() = TextView(this).apply {
        setTextColor(0xFFFFFFFF.toInt())
        textSize = 12f
        setPadding(0, 8, 0, 8)
    }

    private fun botonView(texto: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = texto
        backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        setTextColor(0xFFFFFFFF.toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { setMargins(0, 8, 0, 0) }
        setOnClickListener { action() }
    }

    private fun activar(deviceId: String, diasNum: Int, nombrePlan: String) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, diasNum)
        val data = hashMapOf(
            "device_id" to deviceId,
            "plan" to nombrePlan,
            "dias" to diasNum,
            "activo" to true,
            "automatico_activo" to true,
            "fecha_activacion" to Date(),
            "fecha_expiracion" to cal.time,
            "activado_por" to "admin"
        )
        db.collection("licencias").document(deviceId).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Licencia ACTIVADA: " + nombrePlan, Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun desactivar(deviceId: String) {
        db.collection("licencias").document(deviceId).update("activo", false)
            .addOnSuccessListener {
                Toast.makeText(this, "Licencia DESACTIVADA", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            }
    }
}
