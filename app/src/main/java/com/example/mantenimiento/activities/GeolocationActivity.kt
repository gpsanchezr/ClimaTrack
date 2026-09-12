package com.example.mantenimiento.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.OrdenRepository
import com.example.mantenimiento.utils.MapConfig
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

/**
 * GeolocationActivity - Módulo 9: Geolocalización
 */
class GeolocationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: OrdenRepository
    private var ordenId: Int = -1

    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView

    private lateinit var map: MapView
    private var currentMarker: Marker? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            obtenerCoordenadas()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración de OSMdroid centralizada (ver MapConfig.kt: el error 403
        // era un bloqueo del servidor de OSM, no del User-Agent/caché)
        MapConfig.inicializar(this)

        setContentView(R.layout.activity_geolocation)

        repository = OrdenRepository(this)
        ordenId = intent.getIntExtra("ORDEN_ID", -1)
        if (ordenId == -1) {
            Toast.makeText(this, "No se pudo identificar la orden.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupToolbar()
        setupMap()
        recibirDatosSeguros()

        findViewById<MaterialButton>(R.id.btnUpdateLocation).setOnClickListener {
            verificarPermisos()
        }

        cargarDatosPrevios()
    }

    private fun setupMap() {
        // Motor del mapa configurado con CARTO Voyager (ver MapConfig.kt)
        map.setTileSource(MapConfig.tileSource())
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.5)
        map.controller.setCenter(GeoPoint(10.974211, -74.826389))
    }

    private fun actualizarUIYGuardar(location: Location) {
        val now = Calendar.getInstance().time
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

        tvLat.text = String.format(Locale.getDefault(), "%.6f", location.latitude)
        tvLon.text = String.format(Locale.getDefault(), "%.6f", location.longitude)
        tvDate.text = dateStr
        tvTime.text = timeStr

        val point = GeoPoint(location.latitude, location.longitude)
        map.controller.animateTo(point)

        if (currentMarker == null) {
            currentMarker = Marker(map)
            currentMarker?.title = "Ubicación del Servicio"
            currentMarker?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            currentMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(currentMarker)
        }
        currentMarker?.position = point
        map.invalidate()

        repository.insertarOActualizarUbicacion(ordenId, location.latitude, location.longitude, "$dateStr $timeStr")
        Toast.makeText(this, "GPS Capturado", Toast.LENGTH_SHORT).show()
    }

    private fun initViews() {
        tvLat = findViewById(R.id.tvLatitude)
        tvLon = findViewById(R.id.tvLongitude)
        tvDate = findViewById(R.id.tvGeoDate)
        tvTime = findViewById(R.id.tvGeoTime)
        map = findViewById(R.id.mapView)
    }

    private fun recibirDatosSeguros() {
        val codigoOrden = intent.getStringExtra("codigo_orden") ?: intent.getStringExtra("CODIGO_ORDEN") ?: "OT-%05d".format(ordenId)
        val nombre = intent.getStringExtra("nombre") ?: intent.getStringExtra("NOMBRE") ?: "Cliente"
        val apellido = intent.getStringExtra("apellido") ?: intent.getStringExtra("APELLIDO") ?: ""
        val direccion = intent.getStringExtra("direccion") ?: intent.getStringExtra("DIRECCION") ?: "Sin dirección registrada"
        val celular = intent.getStringExtra("celular") ?: intent.getStringExtra("CELULAR") ?: "Sin número registrado"

        findViewById<TextView>(R.id.tvGeoOrderHeader)?.text = "Orden: $codigoOrden"
        findViewById<TextView>(R.id.tvGeoClientName)?.text = "Cliente: $nombre $apellido".trim()

        // Uso de variables opcionales para evitar crashes si la vista no existe en el XML
        findViewById<TextView>(R.id.tvGeoClientAddress)?.text = "Dirección: $direccion"
        findViewById<TextView>(R.id.tvGeoClientPhone)?.text = "Celular: $celular"
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarGeolocation)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun verificarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerCoordenadas()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerCoordenadas() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) actualizarUIYGuardar(location)
            else solicitarNuevaUbicacion()
        }
    }

    @SuppressLint("MissingPermission")
    private fun solicitarNuevaUbicacion() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMaxUpdates(1).build()
        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { actualizarUIYGuardar(it) }
            }
        }, Looper.getMainLooper())
    }

    private fun cargarDatosPrevios() {
        repository.obtenerUbicacionPorOrden(ordenId)?.let { ubi ->
            tvLat.text = String.format(Locale.getDefault(), "%.6f", ubi.latitud)
            tvLon.text = String.format(Locale.getDefault(), "%.6f", ubi.longitud)
            val point = GeoPoint(ubi.latitud, ubi.longitud)
            map.controller.setCenter(point)
            if (currentMarker == null) {
                currentMarker = Marker(map)
                currentMarker?.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
                currentMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(currentMarker)
            }
            currentMarker?.position = point
            map.invalidate()
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}