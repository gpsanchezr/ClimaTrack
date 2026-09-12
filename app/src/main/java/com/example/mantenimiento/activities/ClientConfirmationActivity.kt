package com.example.mantenimiento.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.ServiceRequestRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class ClientConfirmationActivity : AppCompatActivity() {

    private lateinit var serviceRepo: ServiceRequestRepository

    private var nombre: String = ""
    private var marca: String = ""
    private var falla: String = ""
    private var lat: Double = 0.0
    private var lon: Double = 0.0

    // Resultado del Geocoder: es solo informativo/aproximado. Lo que de
    // verdad se guarda como dirección es lo que el cliente escribe a mano
    // en los campos de abajo (más confiable: el Geocoder puede fallar sin
    // Internet o dar una aproximación imprecisa de la zona).
    private var direccionAproximada: String = "Obteniendo dirección aproximada..."

    private lateinit var tvConfAddress: TextView
    private lateinit var tilCalleCarrera: TextInputLayout
    private lateinit var tilBarrio: TextInputLayout
    private lateinit var etCalleCarrera: EditText
    private lateinit var etBarrio: EditText
    private lateinit var etPiso: EditText
    private lateinit var etApartamento: EditText
    private lateinit var etReferencia: EditText
    private lateinit var ivBrandLogo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_confirmation)

        serviceRepo = ServiceRequestRepository(this)

        // Recuperar datos del Intent
        nombre = intent.getStringExtra("NOMBRE_USUARIO") ?: "Cliente"
        marca = intent.getStringExtra("MARCA") ?: ""
        falla = intent.getStringExtra("FALLA") ?: ""
        lat = intent.getDoubleExtra("LATITUD", 0.0)
        lon = intent.getDoubleExtra("LONGITUD", 0.0)

        initViews()
        setupToolbar()
        obtenerDireccionAproximada()
    }

    private fun initViews() {
        findViewById<TextView>(R.id.tvConfBrand).text = marca
        findViewById<TextView>(R.id.tvConfFault).text = falla

        ivBrandLogo = findViewById(R.id.ivBrandLogo)
        cargarLogoMarca(marca, ivBrandLogo)

        tvConfAddress = findViewById(R.id.tvConfAddress)
        tvConfAddress.text = direccionAproximada

        tilCalleCarrera = findViewById(R.id.tilCalleCarrera)
        tilBarrio = findViewById(R.id.tilBarrio)
        etCalleCarrera = findViewById(R.id.etCalleCarrera)
        etBarrio = findViewById(R.id.etBarrio)
        etPiso = findViewById(R.id.etPiso)
        etApartamento = findViewById(R.id.etApartamento)
        etReferencia = findViewById(R.id.etReferencia)

        findViewById<MaterialButton>(R.id.btnConfirmAndSearch).setOnClickListener {
            if (validarDireccion()) {
                guardarYSolicitar()
            }
        }
    }

    /** Calle/Carrera y Barrio son obligatorios: son lo mínimo con lo que un técnico puede ubicar el sitio. */
    private fun validarDireccion(): Boolean {
        var esValido = true
        if (etCalleCarrera.text.isNullOrBlank()) {
            tilCalleCarrera.error = "Indica la calle o carrera"
            esValido = false
        } else {
            tilCalleCarrera.error = null
        }
        if (etBarrio.text.isNullOrBlank()) {
            tilBarrio.error = "Indica el barrio"
            esValido = false
        } else {
            tilBarrio.error = null
        }
        return esValido
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarConfirmation)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * Traduce las coordenadas GPS a una dirección aproximada de referencia.
     * Geocoder.getFromLocation() es asíncrono desde Android 13 (API 33) y
     * síncrono (y deprecado) antes de eso; minSdk de este proyecto es 24,
     * así que se soportan las dos rutas.
     */
    private fun obtenerDireccionAproximada() {
        if (!Geocoder.isPresent()) {
            mostrarDireccion(null)
            return
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(lat, lon, 1) { direcciones ->
                    runOnUiThread { mostrarDireccion(direcciones.firstOrNull()?.getAddressLine(0)) }
                }
            } else {
                @Suppress("DEPRECATION")
                val direcciones = geocoder.getFromLocation(lat, lon, 1)
                mostrarDireccion(direcciones?.firstOrNull()?.getAddressLine(0))
            }
        } catch (e: Exception) {
            // Sin Internet o sin servicio de geocodificación disponible: no es
            // grave, el cliente puede seguir con los campos manuales de abajo.
            mostrarDireccion(null)
        }
    }

    private fun mostrarDireccion(direccion: String?) {
        direccionAproximada = direccion ?: "No se pudo obtener automáticamente (Lat: $lat, Lon: $lon)"
        tvConfAddress.text = direccionAproximada
    }

    private fun guardarYSolicitar() {
        // Obtenemos el ID del usuario cliente desde SharedPreferences para asegurar el vínculo
        val prefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
        val clienteIdSesion = prefs.getInt("ID_USUARIO", 0)

        // Se concatena todo en una sola variable descriptiva para guardarla en solicitudes_servicio
        val direccionCompleta = buildString {
            append(etCalleCarrera.text.toString().trim())
            append(", Barrio ").append(etBarrio.text.toString().trim())
            if (!etPiso.text.isNullOrBlank()) append(", Piso ").append(etPiso.text.toString().trim())
            if (!etApartamento.text.isNullOrBlank()) append(", Apto ").append(etApartamento.text.toString().trim())
            if (!etReferencia.text.isNullOrBlank()) append(". Referencia: ").append(etReferencia.text.toString().trim())
            append(" (Aprox. GPS: ").append(direccionAproximada).append(")")
        }

        val res = serviceRepo.crearSolicitud(
            clienteId = clienteIdSesion,
            nombre = nombre,
            dir = direccionAproximada,
            lat = lat,
            lon = lon,
            marca = marca,
            falla = falla,
            direccionExacta = direccionCompleta
        )

        if (res != -1L) {
            Toast.makeText(this, "¡Solicitud registrada con éxito!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, WaitingActivity::class.java).apply {
                putExtra("SOLICITUD_ID", res.toInt())
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Error al procesar la solicitud", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Carga dinámicamente el logotipo de la marca desde los recursos drawable.
     * Limpia el nombre (minúsculas, quita espacios/caracteres especiales) y realiza
     * un mapeo seguro vía getIdentifier. Si no lo encuentra, usa el icono por defecto.
     */
    private fun cargarLogoMarca(marcaText: String, ivLogo: ImageView) {
        if (marcaText.isBlank()) {
            aplicarIconoPorDefecto(ivLogo)
            return
        }

        // 1. Limpieza de texto: "LG" -> "lg", "Mabe" -> "mabe", "Samsung" -> "samsung"
        val nombreRecursoLower = marcaText.trim()
            .lowercase(Locale.ROOT)
            .replace(" ", "_")
            .replace("[^a-z0-9_]".toRegex(), "")

        // 2. Nombre alternativo por si el archivo conserva mayúsculas en el sistema de archivos
        val nombreRecursoOriginal = marcaText.trim().replace(" ", "_")

        // 3. Obtener el ID del recurso dinámicamente
        var resId = resources.getIdentifier(nombreRecursoLower, "drawable", packageName)
        if (resId == 0) {
            resId = resources.getIdentifier(nombreRecursoOriginal, "drawable", packageName)
        }

        // 4. Carga segura o fallback
        if (resId != 0) {
            try {
                // Quitar el tinte para mostrar los colores originales del logotipo PNG
                ivLogo.imageTintList = null
                ivLogo.setImageResource(resId)
            } catch (e: Exception) {
                aplicarIconoPorDefecto(ivLogo)
            }
        } else {
            aplicarIconoPorDefecto(ivLogo)
        }
    }

    private fun aplicarIconoPorDefecto(ivLogo: ImageView) {
        ivLogo.setImageResource(R.drawable.ic_hvac_unit)
        ivLogo.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primary)
        )
    }
}