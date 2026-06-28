package com.mxl.driverpro

import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SubscriptionActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        val btn1Mes = findViewById<Button>(R.id.btn1Mes)
        val btn3Meses = findViewById<Button>(R.id.btn3Meses)
        val btn6Meses = findViewById<Button>(R.id.btn6Meses)
        val btn1Ano = findViewById<Button>(R.id.btn1Ano)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        checkSubscription(tvStatus)

        btn1Mes.setOnClickListener { activarPlan(30, "1 Mes - RD$500", tvStatus) }
        btn3Meses.setOnClickListener { activarPlan(90, "3 Meses - RD$1300", tvStatus) }
        btn6Meses.setOnClickListener { activarPlan(180, "6 Meses - RD$2400", tvStatus) }
        btn1Ano.setOnClickListener { activarPlan(365, "1 Ano - RD$4200", tvStatus) }
    }

    private fun activarPlan(dias: Int, nombre: String, tvStatus: TextView) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dias)
        val expira = cal.time
        val data = hashMapOf(
            "device_id" to deviceId,
            "plan" to nombre,
            "dias" to dias,
            "activo" to true,
            "fecha_activacion" to Date(),
            "fecha_expiracion" to expira
        )
        db.collection("licencias").document(deviceId)
            .set(data)
            .addOnSuccessListener {
                tvStatus.text = "Plan ACTIVO: " + nombre
                tvStatus.setTextColor(0xFF4CAF50.toInt())
                Toast.makeText(this, "Plan activado", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun checkSubscription(tvStatus: TextView) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        db.collection("licencias").document(deviceId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val expira = doc.getDate("fecha_expiracion")
                    val activo = doc.getBoolean("activo") ?: false
                    if (activo && expira != null && expira.after(Date())) {
                        tvStatus.text = "ACTIVO hasta: " + expira.toString()
                        tvStatus.setTextColor(0xFF4CAF50.toInt())
                    } else {
                        tvStatus.text = "Suscripcion EXPIRADA"
                        tvStatus.setTextColor(0xFFFF5252.toInt())
                    }
                } else {
                    tvStatus.text = "Sin suscripcion activa"
                    tvStatus.setTextColor(0xFFFF5252.toInt())
                }
            }
    }
}
