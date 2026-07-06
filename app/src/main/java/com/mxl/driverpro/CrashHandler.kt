package com.mxl.driverpro

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val folder = context.getExternalFilesDir(null)
            val file = File(folder, "crash_$timestamp.txt")
            file.writeText(sw.toString())
        } catch (e: Exception) {
            // ignorar
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
