package com.example.mantenimiento.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.TechAgendaAdapter
import com.example.mantenimiento.repositories.ServiceRequestRepository

class TechAgendaActivity : AppCompatActivity() {

    private lateinit var rvAgenda: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var serviceRepo: ServiceRequestRepository
    private lateinit var adapter: TechAgendaAdapter
    private var usuarioTecnico: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tech_agenda)

        serviceRepo = ServiceRequestRepository(this)
        
        // Recuperar usuario de la sesión. IMPORTANTE: solicitudes_servicio.tecnico_asignado
        // guarda el usuario de LOGIN (ej. "alex01", ver AdminAssignActivity/
        // DashboardActivity.aceptarSolicitud), no el nombre visible (ej.
        // "Alex"). Comparar contra NOMBRE_USUARIO hacía que la agenda
        // apareciera siempre vacía para cualquier técnico.
        val prefs = getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
        usuarioTecnico = prefs.getString("USUARIO_LOGIN", "alex01") ?: "alex01"

        initViews()
        setupToolbar()
        setupRecyclerView()
        cargarAgenda()
    }

    private fun initViews() {
        rvAgenda = findViewById(R.id.rvAgenda)
        tvEmpty = findViewById(R.id.tvEmptyAgenda)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarAgenda)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        rvAgenda.layoutManager = LinearLayoutManager(this)
        adapter = TechAgendaAdapter(emptyList()) { solicitudId ->
            iniciarServicio(solicitudId)
        }
        rvAgenda.adapter = adapter
    }

    private fun cargarAgenda() {
        val agenda = serviceRepo.obtenerAgendaTecnico(usuarioTecnico)
        adapter.updateList(agenda)
        
        if (agenda.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvAgenda.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvAgenda.visibility = View.VISIBLE
        }
    }

    private fun iniciarServicio(solicitudId: Int) {
        // En una app real, aquí se crearía la Orden formal o se saltaría a Mantenimiento
        Toast.makeText(this, "Iniciando servicio... Por favor diríjase al domicilio", Toast.LENGTH_LONG).show()
        
        // Simulación: Pasar a la actividad de mantenimiento con el ID
        val intent = Intent(this, MaintenanceActivity::class.java)
        intent.putExtra("ORDEN_ID", solicitudId) 
        startActivity(intent)
    }
}
