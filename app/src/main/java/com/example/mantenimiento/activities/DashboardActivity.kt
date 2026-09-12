package com.example.mantenimiento.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.RadarAdapter
import com.example.mantenimiento.repositories.OrdenRepository
import com.example.mantenimiento.repositories.ServiceRequestRepository
import com.example.mantenimiento.utils.NavigationUtils.configurarPorRol
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class DashboardActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var serviceRepo: ServiceRequestRepository
    private lateinit var usuarioRepo: com.example.mantenimiento.repositories.UsuarioRepository
    private lateinit var radarAdapter: RadarAdapter
    private lateinit var rvRadar: RecyclerView
    private lateinit var tvRadarTitle: TextView

    private var nombreUsuario: String = ""
    private var rolUsuario: String = "Técnico"
    private var generoUsuario: String = "M"
    private var usuarioLogin: String = ""

    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = applicationContext.contentResolver
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val uriStr = uri.toString()
            val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("ID_USUARIO", -1)
            if (userId != -1) {
                usuarioRepo.actualizarFotoPerfil(userId, uriStr)
                prefs.edit().putString("FOTO_PERFIL_$userId", uriStr).apply()
            }
            try {
                ivAvatar.setImageURI(uri)
                Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        serviceRepo = ServiceRequestRepository(this)
        usuarioRepo = com.example.mantenimiento.repositories.UsuarioRepository(this)

        cargarSesion()
        initViews()
        setupRadar()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        cargarSesion()
        actualizarUI()
        if (rolUsuario != "Cliente") {
            cargarDatos()
        }
    }

    private fun cargarSesion() {
        val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
        nombreUsuario = intent.getStringExtra("NOMBRE_USUARIO") ?: prefs.getString("NOMBRE_USUARIO", "Usuario") ?: "Usuario"
        rolUsuario = intent.getStringExtra("ROL_USUARIO") ?: prefs.getString("ROL_USUARIO", "Técnico") ?: "Técnico"
        generoUsuario = intent.getStringExtra("GENERO_USUARIO") ?: prefs.getString("GENERO_USUARIO", "M") ?: "M"
        usuarioLogin = intent.getStringExtra("USUARIO_LOGIN") ?: prefs.getString("USUARIO_LOGIN", "") ?: ""
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.ivAvatar)
        tvRadarTitle = findViewById(R.id.tvRadarTitle)
        rvRadar = findViewById(R.id.rvRadar)

        findViewById<MaterialCardView>(R.id.cardSolicitar).setOnClickListener {
            startActivity(Intent(this, ClientMapActivity::class.java).apply {
                putExtra("NOMBRE_USUARIO", nombreUsuario)
            })
        }

        findViewById<MaterialCardView>(R.id.cardHistorial).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java).apply {
                putExtra("ROL_USUARIO", rolUsuario)
            })
        }

        findViewById<MaterialCardView>(R.id.cardLogout).setOnClickListener {
            val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            intent.replaceExtras(Bundle())

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ivAvatar.setOnClickListener { seleccionarFoto.launch("image/*") }

        val switchAvailability = findViewById<SwitchCompat>(R.id.switchAvailability)
        switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                switchAvailability.text = "ESTADO: DISPONIBLE"
                tvRadarTitle.visibility = View.VISIBLE
                rvRadar.visibility = View.VISIBLE
                actualizarRadar()
            } else {
                switchAvailability.text = "ESTADO: DESCONECTADO"
                tvRadarTitle.visibility = View.GONE
                rvRadar.visibility = View.GONE
            }
        }
    }

    private fun actualizarUI() {
        val tvWelcomeUser = findViewById<TextView>(R.id.tvWelcomeUser)
        val tvWelcomeBack = findViewById<TextView>(R.id.tvWelcomeBack)
        val cardSolicitar = findViewById<MaterialCardView>(R.id.cardSolicitar)
        val cardOrdenes = findViewById<MaterialCardView>(R.id.cardOrdenes)
        val cardEquipos = findViewById<MaterialCardView>(R.id.cardEquipos)
        val cardAsignar = findViewById<MaterialCardView>(R.id.cardAsignar)
        val switchAvailability = findViewById<SwitchCompat>(R.id.switchAvailability)
        val layoutResumen = findViewById<View>(R.id.layoutResumenOrdenes)
        val tvSummaryTitle = findViewById<TextView>(R.id.tvSummaryTitle)

        val nombrePila = nombreUsuario.split(" ")[0]

        val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("ID_USUARIO", -1)
        var fotoCargada = false
        if (userId != -1) {
            val fotoUriStr = usuarioRepo.obtenerFotoPerfil(userId) ?: prefs.getString("FOTO_PERFIL_$userId", null)
            if (!fotoUriStr.isNullOrBlank()) {
                try {
                    ivAvatar.setImageURI(Uri.parse(fotoUriStr))
                    fotoCargada = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (!fotoCargada) {
            if (generoUsuario == "F") ivAvatar.setImageResource(R.drawable.avatar_tecnica)
            else ivAvatar.setImageResource(R.drawable.avatar_tecnico)
        }

        tvWelcomeUser.text = when(rolUsuario) {
            "Administrador" -> if (generoUsuario == "F") "Hola, Administradora $nombrePila" else "Hola, Administrador $nombrePila"
            "Técnico" -> if (generoUsuario == "F") "Hola, Técnica $nombrePila" else "Hola, Técnico $nombrePila"
            else -> "Hola, $nombrePila"
        }

        tvWelcomeBack.text = if (rolUsuario == "Cliente") "¿Necesitas un mantenimiento hoy?" else "¿List@ para un gran día de trabajo?"

        when (rolUsuario) {
            "Administrador" -> {
                layoutResumen.visibility = View.VISIBLE
                tvSummaryTitle.visibility = View.VISIBLE
                cardSolicitar.visibility = View.GONE
                cardOrdenes.visibility = View.VISIBLE
                cardEquipos.visibility = View.VISIBLE
                cardAsignar.visibility = View.VISIBLE
                switchAvailability.visibility = View.GONE

                cardOrdenes.setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }
                cardEquipos.setOnClickListener { startActivity(Intent(this, EquipmentActivity::class.java)) }
                findViewById<android.widget.ImageView>(R.id.ivAsignarIcon).setImageResource(android.R.drawable.ic_menu_edit)
                findViewById<TextView>(R.id.tvAsignarLabel).text = "Asignar"
                cardAsignar.setOnClickListener { 
                    startActivity(Intent(this, AdminAssignActivity::class.java))
                }
            }
            "Técnico" -> {
                layoutResumen.visibility = View.VISIBLE
                tvSummaryTitle.visibility = View.VISIBLE
                cardSolicitar.visibility = View.GONE
                cardOrdenes.visibility = View.VISIBLE
                cardEquipos.visibility = View.GONE
                switchAvailability.visibility = View.VISIBLE

                // Reutilizamos el mismo espacio de "Asignar" (solo Admin) como
                // "Mi Agenda": aquí el técnico ve los servicios que el
                // Administrador ya le programó con fecha/hora (antes esta
                // pantalla -TechAgendaActivity- existía pero no era
                // alcanzable desde ninguna parte de la interfaz).
                cardAsignar.visibility = View.VISIBLE
                findViewById<android.widget.ImageView>(R.id.ivAsignarIcon).setImageResource(android.R.drawable.ic_menu_my_calendar)
                findViewById<TextView>(R.id.tvAsignarLabel).text = "Mi Agenda"
                cardAsignar.setOnClickListener {
                    startActivity(Intent(this, TechAgendaActivity::class.java))
                }

                cardOrdenes.setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }
            }
            "Cliente" -> {
                layoutResumen.visibility = View.GONE
                tvSummaryTitle.visibility = View.GONE
                cardSolicitar.visibility = View.VISIBLE
                cardOrdenes.visibility = View.GONE
                cardEquipos.visibility = View.GONE
                cardAsignar.visibility = View.GONE
                switchAvailability.visibility = View.GONE
            }
        }
    }

    private fun setupRadar() {
        rvRadar.layoutManager = LinearLayoutManager(this)
        radarAdapter = RadarAdapter(emptyList()) { solicitudId, accion ->
            if (accion == "ACEPTAR") {
                aceptarSolicitud(solicitudId)
            } else {
                rechazarSolicitud(solicitudId)
            }
        }
        rvRadar.adapter = radarAdapter
    }

    private fun cargarDatos() {
        try {
            val ordenRepository = OrdenRepository(this)
            val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
            val userId = prefs.getInt("ID_USUARIO", -1)
            // Un Técnico ve solo el resumen de SUS órdenes; el Administrador
            // conserva el resumen global de la empresa (tecnicoId = null).
            val tecnicoIdFiltro = if (rolUsuario == "Técnico" && userId != -1) userId else null
            findViewById<TextView>(R.id.tvCountPending).text = ordenRepository.getCountByEstado("PENDIENTE", tecnicoIdFiltro).toString()
            findViewById<TextView>(R.id.tvCountInProgress).text = ordenRepository.getCountByEstado("EN PROCESO", tecnicoIdFiltro).toString()
            findViewById<TextView>(R.id.tvCountFinalized).text = ordenRepository.getCountByEstado("FINALIZADA", tecnicoIdFiltro).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun actualizarRadar() {
        val pendientes = serviceRepo.obtenerSolicitudesPendientes()
        radarAdapter.updateList(pendientes)
        if (pendientes.isEmpty() && findViewById<SwitchCompat>(R.id.switchAvailability).isChecked) {
            Toast.makeText(this, "No hay solicitudes nuevas en el área", Toast.LENGTH_SHORT).show()
        }
    }

    private fun aceptarSolicitud(solicitudId: Int) {
        val res = serviceRepo.aceptarSolicitud(solicitudId, usuarioLogin)
        if (res > 0) {
            Toast.makeText(this, "¡Trabajo aceptado!", Toast.LENGTH_LONG).show()
            actualizarRadar()
            cargarDatos()
        } else {
            // rowsAffected == 0: otro técnico la aceptó primero (candado de concurrencia)
            Toast.makeText(this, "Esa solicitud ya fue tomada por otro técnico", Toast.LENGTH_SHORT).show()
            actualizarRadar()
        }
    }

    private fun rechazarSolicitud(solicitudId: Int) {
        val res = serviceRepo.rechazarSolicitud(solicitudId)
        if (res > 0) {
            Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
            actualizarRadar()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val menu = bottomNav.menu

        when (rolUsuario) {
            "Cliente" -> {
                menu.findItem(R.id.nav_ordenes)?.isVisible = false
                menu.findItem(R.id.nav_equipos)?.isVisible = false
            }
            "Técnico" -> {
                menu.findItem(R.id.nav_ordenes)?.isVisible = true
                menu.findItem(R.id.nav_equipos)?.isVisible = false
            }
            "Administrador" -> {
                menu.findItem(R.id.nav_ordenes)?.isVisible = true
                menu.findItem(R.id.nav_equipos)?.isVisible = true
            }
        }

        bottomNav.selectedItemId = R.id.nav_inicio

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_ordenes -> {
                    if (rolUsuario == "Administrador") {
                        startActivity(Intent(this, AdminAssignActivity::class.java).apply {
                            putExtra("ROL_USUARIO", rolUsuario)
                        })
                    } else if (rolUsuario != "Cliente") {
                        startActivity(Intent(this, OrdersActivity::class.java).apply {
                            putExtra("ROL_USUARIO", rolUsuario)
                        })
                    }
                    true
                }
                R.id.nav_equipos -> {
                    if (rolUsuario == "Administrador") startActivity(Intent(this, EquipmentActivity::class.java).apply {
                        putExtra("ROL_USUARIO", rolUsuario)
                    })
                    true
                }
                R.id.nav_historial -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        putExtra("ROL_USUARIO", rolUsuario)
                    })
                    true
                }
                else -> false
            }
        }
    }
}