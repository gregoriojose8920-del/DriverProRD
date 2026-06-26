package com.mxl.driverpro.logic

import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.Button
import com.mxl.driverpro.R

class FloatingPanel(context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val view: View = LayoutInflater.from(context).inflate(R.layout.floating_layout, null)

    init {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(view, params)

        // Botón para activar/desactivar el bot
        view.findViewById<Button>(R.id.btn_toggle).setOnClickListener {
            ActionEngine.isBotActive = !ActionEngine.isBotActive
        }
    }
}
