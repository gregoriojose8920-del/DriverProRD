package com.mxl.driverpro

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object LicenseManager {

    private val db = FirebaseFirestore.getInstance()

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    fun checkLicense(context: Context, onResult: (LicenseStatus) -> Unit) {
        val deviceId = getDeviceId(context)
        db.collection("licencias").document(deviceId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(LicenseStatus.NOT_FOUND)
                    return@addOnSuccessListener
                }
                val activa = doc.getBoolean("activa") ?: false
                val expira = doc.getDate("expira")
                when {
                    !activa -> onResult(LicenseStatus.INACTIVE)
                    expira != null && expira.before(Date()) -> onResult(LicenseStatus.EXPIRED)
                    else -> onResult(LicenseStatus.VALID)
                }
            }
            .addOnFailureListener {
                onResult(LicenseStatus.ERROR)
            }
    }

    enum class LicenseStatus {
        VALID, EXPIRED, INACTIVE, NOT_FOUND, ERROR
    }
}
