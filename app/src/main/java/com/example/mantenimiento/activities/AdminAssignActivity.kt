package com.example.mantenimiento.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.RadarAdapter
import com.example.mantenimiento.repositories.ServiceRequestRepository
import com.example.mantenimiento.repositories.UsuarioRepository

class AdminAssignActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var serviceRepo: ServiceRequestRepository
    private lateinit var usuarioRepo: UsuarioRepository
    private lateinit var adapter: RadarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_assign)

        serviceRepo = ServiceRequestRepository(this)
        usuarioRepo = UsuarioRepository(this)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        cargarSolicitudes()
    }

    private fun initViews() {
        rvRequests = findViewById(R.id.rvAdminRequests)
        tvNoRequests = findViewById(R.id.tvNoRequests)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminAssign)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        rvRequests.layoutManager = LinearLayoutManager(this)
        adapter = RadarAdapter(emptyList()) { solicitudId, accion ->
            val list = serviceRepo.obtenerSolicitudesPendientesAdmin()
            val item = list.find { it["id"] == solicitudId.toString() }
            if (accion == "ACEPTAR") {
                if (item != null) {
                    mostrarOpcionesSolicitud(solicitudId, item)
                } else {
                    mostrarDialogoTecnicos(solicitudId)
                }
            } else {
                serviceRepo.rechazarSolicitud(solicitudId)
                cargarSolicitudes()
            }
        }
        rvRequests.adapter = adapter
    }

    private fun cargarSolicitudes() {
        val list = serviceRepo.obtenerSolicitudesPendientesAdmin()
        adapter.updateList(list)
        
        if (list.isEmpty()) {
            tvNoRequests.visibility = View.VISIBLE
            rvRequests.visibility = View.GONE
        } else {
            tvNoRequests.visibility = View.GONE
            rvRequests.visibility = View.VISIBLE
        }
    }

    private fun mostrarOpcionesSolicitud(solicitudId: Int, item: Map<String, String>) {
        val cliente = item["cliente"] ?: "Cliente"
        val direccion = item["direccion"] ?: "Sin dirección registrada"
        val marca = item["marca"] ?: "Aire Acondicionado"
        val falla = item["falla"] ?: "Sin detalle de falla"

        val mensajeDetalle = "Cliente: $cliente\nDirección: $direccion\nMarca: $marca\nFalla: $falla"

        val opciones = arrayOf("📢 Publicar en Radar (Oferta Pública)", "👤 Asignar Técnico Directo")

        AlertDialog.Builder(this)
            .setTitle("Detalle de la Solicitud #$solicitudId")
            .setMessage(mensajeDetalle)
            .setItems(opciones) { _, which ->
                if (which == 0) {
                    val result = serviceRepo.publicarSolicitud(solicitudId)
                    if (result > 0) {
                        Toast.makeText(this, "¡Solicitud publicada en el radar!", Toast.LENGTH_SHORT).show()
                        cargarSolicitudes()
                    } else {
                        Toast.makeText(this, "No se pudo publicar la solicitud.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mostrarDialogoTecnicos(solicitudId)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarDialogoTecnicos(solicitudId: Int) {
        val tecnicos = usuarioRepo.obtenerTecnicos()
        if (tecnicos.isEmpty()) {
            Toast.makeText(this, "No hay técnicos registrados para asignar.", Toast.LENGTH_LONG).show()
            return
        }
        val nombres = tecnicos.map { "${it.nombre} (Técnico)" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar Técnico de Campo")
            .setItems(nombres) { _, which ->
                val tecnicoElegido = tecnicos[which].usuario
                pedirFechaYHora(solicitudId, tecnicoElegido)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pedirFechaYHora(solicitudId: Int, tecnicoUsuario: String) {
        // Obtenemos la fecha y hora actual para los pickers
        val c = java.util.Calendar.getInstance()
        val year = c.get(java.util.Calendar.YEAR)
        val month = c.get(java.util.Calendar.MONTH)
        val day = c.get(java.util.Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(this, { _, yearSel, monthSel, dayOfMonthSel ->
            val fechaStr = "$dayOfMonthSel/${monthSel + 1}/$yearSel"
            
            // Una vez seleccionada la fecha, pedimos la hora
            val timePickerDialog = android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                val horaStr = String.format("%02d:%02d", hourOfDay, minute)
                despacharOrden(solicitudId, tecnicoUsuario, fechaStr, horaStr)
            }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true)
            
            timePickerDialog.show()
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun despacharOrden(solicitudId: Int, tecnicoUsuario: String, fecha: String = "", hora: String = "") {
        // Candado anti-cruces de horario (Módulo 3.3): antes de agendar,
        // confirmamos que el técnico no tenga ya otro servicio en esa fecha y hora.
        if (fecha.isNotEmpty() && hora.isNotEmpty() &&
            !serviceRepo.validarHorarioDisponible(tecnicoUsuario, fecha, hora)
        ) {
            Toast.makeText(
                this,
                "$tecnicoUsuario ya tiene un servicio agendado ese día a esa hora. Elige otro horario.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val result = serviceRepo.asignarTecnico(solicitudId, tecnicoUsuario, fecha, hora)
        if (result > 0) {
            Toast.makeText(this, "Orden despachada a $tecnicoUsuario para el $fecha a las $hora", Toast.LENGTH_LONG).show()
            cargarSolicitudes() // Recargar lista
        }
    }
}
