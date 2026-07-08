package com.mxl.driverpro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etCorreo = findViewById<EditText>(R.id.etCorreoReg)
        val etPassword = findViewById<EditText>(R.id.etPasswordReg)
        val etVehiculo = findViewById<EditText>(R.id.etVehiculoReg)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)
        val tvError = findViewById<TextView>(R.id.tvErrorReg)
        val progress = findViewById<ProgressBar>(R.id.progressReg)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val vehiculo = etVehiculo.text.toString().trim()

            if (nombre.isEmpty() || telefono.isEmpty() || correo.isEmpty() || vehiculo.isEmpty()) {
                tvError.text = "Todos los campos son obligatorios"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (pass.length < 6) {
                tvError.text = "Contrasena minimo 6 caracteres"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnRegistrar.isEnabled = false
            tvError.visibility = View.GONE

            try {
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver, android.provider.Settings.Secure.ANDROID_ID)

                auth.createUserWithEmailAndPassword(correo, pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: ""
                        val perfil = hashMapOf(
                            "uid" to uid,
                            "nombre" to nombre,
                            "telefono" to telefono,
                            "correo" to correo,
                            "vehiculo" to vehiculo,
                            "device_id" to deviceId,
                            "fecha_registro" to Date(),
                            "licencia_activa" to false,
                            "plan" to "Sin plan"
                        )
                        db.collection("conductores").document(uid).set(perfil)
                            .addOnSuccessListener {
                                progress.visibility = View.GONE
                                Toast.makeText(this, "Cuenta creada. Contacta al admin para activar.", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                })
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progress.visibility = View.GONE
                                btnRegistrar.isEnabled = true
                                tvError.text = "Error guardando datos: " + e.message
                                tvError.visibility = View.VISIBLE
                            }
                    }
                    .addOnFailureListener { e ->
                        progress.visibility = View.GONE
                        btnRegistrar.isEnabled = true
                        tvError.text = when {
                            e.message?.contains("email") == true -> "Este correo ya esta registrado"
                            e.message?.contains("network") == true -> "Sin internet. Conectate y vuelve a intentar"
                            e.message?.contains("weak-password") == true -> "Contrasena muy debil"
                            else -> "Error: " + e.message
                        }
                        tvError.visibility = View.VISIBLE
                    }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                btnRegistrar.isEnabled = true
                tvError.text = "Error inesperado: " + e.message
                tvError.visibility = View.VISIBLE
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
