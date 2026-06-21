package com.mgo.driverprord
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.mgo.driverprord.logic.ActionEngine

class MainActivity : AppCompatActivity() {
    val engine = ActionEngine()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {
            val price = findViewById<EditText>(R.id.inputPrice).text.toString().toDouble()
            val dist = findViewById<EditText>(R.id.inputDist).text.toString().toDouble()
            engine.minPrice = price
            engine.maxDistance = dist
        }
    }
}
