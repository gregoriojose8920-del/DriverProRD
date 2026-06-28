package com.mxl.driverpro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private var myMarker: Marker? = null
    private val zonasBloqueadas = mutableListOf<Circle>()
    private val zonaMarkers = mutableListOf<Marker>()
    private var modoBloqueo = false
    private var searchMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // BUSCADOR
        val etSearch = findViewById<EditText>(R.id.etSearchLocation)
        val btnSearch = findViewById<Button>(R.id.btnSearch)

        val buscar = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) buscarLugar(query)
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
        }

        btnSearch.setOnClickListener { buscar() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { buscar(); true } else false
        }

        // Sugerencias rapidas Santo Domingo
        val chipSD = findViewById<Button>(R.id.chipSD)
        val chipNorte = findViewById<Button>(R.id.chipNorte)
        val chipEste = findViewById<Button>(R.id.chipEste)
        val chipOeste = findViewById<Button>(R.id.chipOeste)

        chipSD.setOnClickListener { moverA(18.4861, -69.9312, "Centro SD", 13f) }
        chipNorte.setOnClickListener { moverA(18.5437, -69.8887, "Norte SD", 13f) }
        chipEste.setOnClickListener { moverA(18.4742, -69.8513, "Este SD", 13f) }
        chipOeste.setOnClickListener { moverA(18.4725, -70.0012, "Oeste SD", 13f) }

        findViewById<Button>(R.id.btnToggleBloqueo).setOnClickListener {
            modoBloqueo = !modoBloqueo
            val btn = findViewById<Button>(R.id.btnToggleBloqueo)
            btn.text = if (modoBloqueo) "✋ CANCELAR" else "🚫 BLOQUEAR ZONA"
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (modoBloqueo) 0xFFFF5252.toInt() else 0xFF37474F.toInt()
            )
            if (modoBloqueo) Toast.makeText(this, "Toca el mapa para bloquear una zona de 500m", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnClearZonas).setOnClickListener {
            zonasBloqueadas.forEach { it.remove() }
            zonaMarkers.forEach { it.remove() }
            zonasBloqueadas.clear()
            zonaMarkers.clear()
            guardarZonasBloqueadas()
            Toast.makeText(this, "Zonas bloqueadas eliminadas", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnToggleHeatmap).setOnClickListener {
            Toast.makeText(this, "Heatmap disponible con mas conductores activos", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnVolverMap).setOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // Centro Santo Domingo por defecto
        moverA(18.4861, -69.9312, "Santo Domingo", 12f)
        cargarZonasBloqueadas()

        mMap.setOnMapClickListener { latLng ->
            if (modoBloqueo) agregarZonaBloqueada(latLng)
        }

        mMap.setOnMapLongClickListener { latLng ->
            // Long press siempre bloquea zona
            agregarZonaBloqueada(latLng)
        }
    }

    private fun buscarLugar(query: String) {
        // Buscar usando Geocoder
        val handler = Handler(Looper.getMainLooper())
        Thread {
            try {
                val geocoder = android.location.Geocoder(this)
                // Buscar en RD primero
                val queryRD = if (!query.contains("Dominican") && !query.contains("RD") && !query.contains("Santo"))
                    "$query, Dominican Republic" else query
                val resultados = geocoder.getFromLocationName(queryRD, 5)
                handler.post {
                    if (!resultados.isNullOrEmpty()) {
                        val lugar = resultados[0]
                        val latLng = LatLng(lugar.latitude, lugar.longitude)
                        val nombre = lugar.featureName ?: query
                        searchMarker?.remove()
                        searchMarker = mMap.addMarker(MarkerOptions()
                            .position(latLng)
                            .title(nombre)
                            .snippet("Toca larga para bloquear esta zona")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)))
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        searchMarker?.showInfoWindow()
                        Toast.makeText(this, "Encontrado: $nombre", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Lugar no encontrado. Intenta: 'Zona Colonial, SD'", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(this, "Error de busqueda: " + e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun moverA(lat: Double, lng: Double, nombre: String, zoom: Float) {
        if (!::mMap.isInitialized) return
        val pos = LatLng(lat, lng)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, zoom))
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val pos = LatLng(loc.latitude, loc.longitude)
                    db.collection("ubicaciones")
                        .document(android.provider.Settings.Secure.getString(
                            contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                        .set(mapOf("lat" to loc.latitude, "lng" to loc.longitude,
                            "timestamp" to com.google.firebase.Timestamp.now()))
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude), 14f))
                    findViewById<TextView>(R.id.tvMapInfo).text = "Mi ubicacion: %.4f, %.4f".format(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun agregarZonaBloqueada(latLng: LatLng) {
        val circle = mMap.addCircle(CircleOptions()
            .center(latLng).radius(500.0)
            .strokeColor(Color.RED).fillColor(0x44FF0000).strokeWidth(3f))
        val marker = mMap.addMarker(MarkerOptions()
            .position(latLng).title("Zona Bloqueada")
            .snippet("500m - No acepto viajes aqui")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        if (marker != null) zonaMarkers.add(marker)
        zonasBloqueadas.add(circle)
        guardarZonasBloqueadas()
        modoBloqueo = false
        val btn = findViewById<Button>(R.id.btnToggleBloqueo)
        btn.text = "🚫 BLOQUEAR ZONA"
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF37474F.toInt())
        Toast.makeText(this, "Zona bloqueada (500m)", Toast.LENGTH_SHORT).show()
    }

    private fun guardarZonasBloqueadas() {
        val prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)
        val lista = zonasBloqueadas.joinToString("|") {
            "\${it.center.latitude},\${it.center.longitude}"
        }
        prefs.edit().putString("zonas_bloqueadas", lista).apply()
    }

    private fun cargarZonasBloqueadas() {
        val prefs = getSharedPreferences("driverpro_prefs", MODE_PRIVATE)
        val lista = prefs.getString("zonas_bloqueadas", "") ?: return
        if (lista.isEmpty()) return
        lista.split("|").forEach { punto ->
            val parts = punto.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull() ?: return@forEach
                val lng = parts[1].toDoubleOrNull() ?: return@forEach
                val ll = LatLng(lat, lng)
                val circle = mMap.addCircle(CircleOptions()
                    .center(ll).radius(500.0)
                    .strokeColor(Color.RED).fillColor(0x44FF0000).strokeWidth(3f))
                val marker = mMap.addMarker(MarkerOptions()
                    .position(ll).title("Zona Bloqueada")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                if (marker != null) zonaMarkers.add(marker)
                zonasBloqueadas.add(circle)
            }
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (rc == 1 && r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                startLocationUpdates()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
