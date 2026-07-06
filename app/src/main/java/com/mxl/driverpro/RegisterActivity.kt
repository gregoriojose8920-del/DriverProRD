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

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etCorreo = findViewById<EditText>(R.id.etCorreoReg)
        val etPassword = findViewById<EditText>(R.id.etPasswordReg)
        val etVehiculo = findViewById<EditText>(R.id.etVehiculoReg)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)
        val tvError = findViewById<TextView>(R.id.tvErrorReg)
        val progressBar = findViewById<ProgressBar>(R.id.progressReg)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val vehiculo = etVehiculo.text.toString().trim()

            if (nombre.isEmpty()) { tvError.text = "Ingresa tu nombre"; tvError.visibility = View.VISIBLE; return@setOnClickListener }
            if (telefono.isEmpty()) { tvError.text = "Ingresa tu telefono"; tvError.visibility = View.VISIBLE; return@setOnClickListener }
            if (correo.isEmpty()) { tvError.text = "Ingresa tu correo"; tvError.visibility = View.VISIBLE; return@setOnClickListener }
            if (pass.length < 6) { tvError.text = "Contrasena minimo 6 caracteres"; tvError.visibility = View.VISIBLE; return@setOnClickListener }
            if (vehiculo.isEmpty()) { tvError.text = "Ingresa tu vehiculo"; tvError.visibility = View.VISIBLE; return@setOnClickListener }

            progressBar.visibility = View.VISIBLE
            btnRegistrar.isEnabled = false
            tvError.visibility = View.GONE

            auth.createUserWithEmailAndPassword(correo, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    val deviceId = android.provider.Settings.Secure.getString(
                        contentResolver, android.provider.Settings.Secure.ANDROID_ID)

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
                            progressBar.visibility = View.GONE
                            Toast.makeText(this,
                                "Cuenta creada exitosamente",
                                Toast.LENGTH_LONG).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnRegistrar.isEnabled = true
                            tvError.text = "Error: " + e.message
                            tvError.visibility = View.VISIBLE
                        }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnRegistrar.isEnabled = true
                    tvError.text = when {
                        e.message?.contains("email") == true -> "Correo ya registrado"
                        e.message?.contains("network") == true -> "Sin conexion a internet"
                        e.message?.contains("CONFIGURATION_NOT_FOUND") == true -> "Error de configuracion Firebase"
                        else -> "Error: " + e.message
                    }
                    tvError.visibility = View.VISIBLE
                }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
