package com.mxl.driverpro

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object LicenseManager {

    fun verificar(context: Context, onResult: (activo: Boolean, mensaje: String) -> Unit) {
        val deviceId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID)
        val db = FirebaseFirestore.getInstance()

        db.collection("licencias").document(deviceId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(false, "Sin licencia activa\nContacta al administrador\nID: $deviceId")
                    return@addOnSuccessListener
                }
                val activo = doc.getBoolean("activo") ?: false
                val expira = doc.getDate("fecha_expiracion")
                val plan = doc.getString("plan") ?: ""

                when {
                    !activo -> onResult(false, "Licencia DESACTIVADA\nContacta al administrador")
                    expira == null -> onResult(false, "Error en licencia\nContacta al administrador")
                    expira.before(Date()) -> onResult(false, "Licencia EXPIRADA\nRenueva tu plan\nPlan anterior: $plan")
                    else -> onResult(true, "Activo hasta: $expira")
                }
            }
            .addOnFailureListener {
                // Sin internet = permitir (modo offline)
                onResult(true, "Modo offline")
            }
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}
