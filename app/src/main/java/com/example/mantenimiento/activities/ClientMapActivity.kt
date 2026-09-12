package com.example.mantenimiento.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mantenimiento.R
import com.example.mantenimiento.utils.MapConfig
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class ClientMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var nombreUsuario: String = "Cliente"
    private var currentLat: Double = 10.96854
    private var currentLon: Double = -74.78132

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            obtenerUbicacionYPasar()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado. Usando ubicación por defecto.", Toast.LENGTH_SHORT).show()
            validarYPasarDirecto()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración obligatoria de OSMdroid centralizada
        MapConfig.inicializar(this)

        setContentView(R.layout.activity_client_map)

        // 2. Configurar la barra superior (Toolbar) y el botón de retroceso
        val toolbar = findViewById<Toolbar>(R.id.toolbarMap)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO") ?: "Cliente"
        findViewById<TextView>(R.id.tvClientGreeting).text = "Hola, $nombreUsuario"

        // 3. Inicializar MapView usando el motor centralizado de MapConfig
        map = findViewById(R.id.map)
        map.setTileSource(MapConfig.tileSource())
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(16.0)

        // Punto inicial por defecto (Barranquilla)
        val barranquilla = GeoPoint(10.96854, -74.78132)
        mapController.setCenter(barranquilla)
        agregarMarcador(barranquilla, "Buscando ubicación...")

        // 4. Programar la lupa de búsqueda de direcciones (Geocoder)
        val etBuscarDireccion = findViewById<EditText>(R.id.etBuscarDireccion)
        findViewById<ImageButton>(R.id.btnBuscarMapa).setOnClickListener {
            val query = etBuscarDireccion.text.toString().trim()
            if (query.isNotEmpty()) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        currentLat = address.latitude
                        currentLon = address.longitude
                        val puntoBuscado = GeoPoint(currentLat, currentLon)

                        map.controller.setCenter(puntoBuscado)
                        map.controller.setZoom(17.0)
                        agregarMarcador(puntoBuscado, query)

                        Toast.makeText(this, "¡Dirección encontrada!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No se encontró la dirección exacta. Intenta detallar más.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error en la búsqueda: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Escribe una dirección para buscar", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuración de marcas de aire acondicionado
        val marcasAires = arrayOf(
            "York", "Carrier", "Gree", "Haier", "LG",
            "Samsung", "Mabe", "Whirlpool", "Lennox",
            "Trane", "Panasonic", "Haceb", "Otra / Personalizada"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, marcasAires)
        val actvMarca = findViewById<AutoCompleteTextView>(R.id.actvMarca)
        actvMarca.setAdapter(adapter)
        actvMarca.threshold = 1
        actvMarca.setOnClickListener { actvMarca.showDropDown() }

        findViewById<Button>(R.id.btnRequestNow).setOnClickListener {
            val etFalla = findViewById<EditText>(R.id.etFallaRapida).text.toString().trim()
            if (etFalla.isEmpty()) {
                Toast.makeText(this, "Por favor describe la falla del equipo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verificarPermisosYContinuar()
        }
    }

    private fun agregarMarcador(p: GeoPoint, titulo: String) {
        map.overlays.clear()
        val marker = Marker(map)
        marker.position = p
        marker.title = titulo
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun verificarPermisosYContinuar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYPasar()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionYPasar() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
            }
            validarYPasarDirecto()
        }.addOnFailureListener {
            validarYPasarDirecto()
        }
    }

    private fun validarYPasarDirecto() {
        val miUbicacion = GeoPoint(currentLat, currentLon)
        map.controller.setCenter(miUbicacion)
        agregarMarcador(miUbicacion, "Mi Ubicación Actual")

        val etFalla = findViewById<EditText>(R.id.etFallaRapida).text.toString().trim()
        val actvMarca = findViewById<AutoCompleteTextView>(R.id.actvMarca)
        val marcaSeleccionada = actvMarca.text.toString().trim().ifEmpty { "Genérica" }

        val intentDestino = Intent(this, ClientConfirmationActivity::class.java).apply {
            putExtra("NOMBRE_USUARIO", nombreUsuario)
            putExtra("MARCA", marcaSeleccionada)
            putExtra("FALLA", etFalla)
            putExtra("LATITUD", currentLat)
            putExtra("LONGITUD", currentLon)
        }
        startActivity(intentDestino)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}