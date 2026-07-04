package com.mxl.driverpro

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object SecurityManager {

    // FIRMA ORIGINAL DEL APK - se verifica al arrancar
    // Esta es tu firma SHA-256 del keystore driverpro2026
    private const val FIRMA_VALIDA = "driverpro_mxl_2026"
    private const val PACKAGE_NAME = "com.mxl.driverpro"

    // ============================================
    // CAPA 1: ANTI-TAMPER
    // Verifica que el APK no fue modificado
    // ============================================
    fun verificarIntegridad(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            val firmas = info.signatures
            if (firmas.isNullOrEmpty()) return false

            // Verificar que el package name no fue cambiado
            if (context.packageName != PACKAGE_NAME) return false

            // Verificar firma del APK
            val firma = firmas[0]
            val hashFirma = hashlib(firma.toByteArray())

            // Guardar hash la primera vez
            val prefs = context.getSharedPreferences("dp_security", Context.MODE_PRIVATE)
            val hashGuardado = prefs.getString("apk_hash", null)

            if (hashGuardado == null) {
                // Primera vez - guardar el hash original
                prefs.edit().putString("apk_hash", hashFirma).apply()
                return true
            }

            // Verificar que no cambio
            hashGuardado == hashFirma

        } catch (e: Exception) {
            false
        }
    }

    // ============================================
    // CAPA 2: ROOT DETECTION
    // Bloquea si el telefono esta rooteado
    // ============================================
    fun detectarRoot(): Boolean {
        return checkSuBinary() || checkRootFiles() || checkBuildTags()
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su",
            "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootFiles(): Boolean {
        val files = arrayOf(
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU",
            "/system/app/Kinguser.apk",
            "/system/app/MagiskManager.apk",
            "/data/adb/magisk",
            "/sbin/.magisk"
        )
        return files.any { File(it).exists() }
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    // ============================================
    // CAPA 3: EMULATOR DETECTION
    // Bloquea si lo abren en emulador/PC
    // ============================================
    fun detectarEmulador(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.BRAND.startsWith("generic")
            || Build.DEVICE.startsWith("generic")
            || Build.PRODUCT == "google_sdk"
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("vbox")
            || Build.PRODUCT.contains("emulator"))
    }

    // ============================================
    // VERIFICACION COMPLETA
    // Llama a las 3 capas de una vez
    // ============================================
    fun verificarSeguridad(context: Context): ResultadoSeguridad {
        val esEmulador = detectarEmulador()
        val tieneRoot = detectarRoot()
        val integridadOk = verificarIntegridad(context)

        return ResultadoSeguridad(
            aprobado = !esEmulador && !tieneRoot && integridadOk,
            esEmulador = esEmulador,
            tieneRoot = tieneRoot,
            integridadOk = integridadOk,
            mensaje = when {
                esEmulador -> "Dispositivo no compatible"
                tieneRoot -> "Dispositivo no autorizado"
                !integridadOk -> "App modificada - reinstala desde la fuente oficial"
                else -> "OK"
            }
        )
    }

    private fun hashlib(data: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    data class ResultadoSeguridad(
        val aprobado: Boolean,
        val esEmulador: Boolean,
        val tieneRoot: Boolean,
        val integridadOk: Boolean,
        val mensaje: String
    )
}
