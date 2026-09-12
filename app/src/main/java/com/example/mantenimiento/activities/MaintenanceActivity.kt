package com.example.mantenimiento.activities

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mantenimiento.R
import com.example.mantenimiento.models.Mantenimiento
import com.example.mantenimiento.repositories.OrdenRepository
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

class MaintenanceActivity : AppCompatActivity() {

    private lateinit var ordenRepository: OrdenRepository
    private var ordenId: Int = -1

    // Vistas del formulario
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var actvServiceType: AutoCompleteTextView
    private lateinit var etDiagnosis: EditText
    private lateinit var etWorkDone: EditText
    private lateinit var etObservations: EditText
    private lateinit var etRecommendations: EditText
    private lateinit var actvStatus: AutoCompleteTextView
    private lateinit var etTimeSpent: EditText
    private lateinit var etTechnician: EditText

    // Vistas de cabecera
    private lateinit var tvOrderNumber: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvClientPhone: TextView
    private lateinit var tvClientAddress: TextView
    private lateinit var tvEquipmentName: TextView
    private lateinit var tvStatusBadge: TextView

    // Contenedores para errores
    private lateinit var tilDate: TextInputLayout
    private lateinit var tilTime: TextInputLayout
    private lateinit var tilServiceType: TextInputLayout
    private lateinit var tilDiagnosis: TextInputLayout
    private lateinit var tilWorkDone: TextInputLayout
    private lateinit var tilRecommendations: TextInputLayout
    private lateinit var tilStatus: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maintenance)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ordenRepository = OrdenRepository(this)
        ordenId = intent.getIntExtra("ORDEN_ID", -1)

        if (ordenId == -1) {
            // Antes, si por algún motivo no llegaba un ORDEN_ID válido,
            // guardarDatos() lo reemplazaba en silencio por "1" y el
            // mantenimiento terminaba guardado sobre la orden equivocada.
            Toast.makeText(this, "No se pudo identificar la orden. Vuelve a intentarlo desde la lista de Órdenes.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupDropdowns()
        setupDateTimePickers()
        setupNavigation()
        cargarDatosOrden()

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (validarFormulario()) {
                guardarDatos()
            }
        }

        // Configuración de Toolbar y Flecha de Retroceso Funcional
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        actvServiceType = findViewById(R.id.actvServiceType)
        etDiagnosis = findViewById(R.id.etDiagnosis)
        etWorkDone = findViewById(R.id.etWorkDone)
        etObservations = findViewById(R.id.etObservations)
        etRecommendations = findViewById(R.id.etRecommendations)
        actvStatus = findViewById(R.id.actvStatus)
        etTimeSpent = findViewById(R.id.etTimeSpent)
        etTechnician = findViewById(R.id.etTechnician)

        tvOrderNumber = findViewById(R.id.tvOrderNumber)
        tvClientName = findViewById(R.id.tvClientName)
        tvClientPhone = findViewById(R.id.tvClientPhone)
        tvClientAddress = findViewById(R.id.tvClientAddress)
        tvEquipmentName = findViewById(R.id.tvEquipmentName)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)

        tilDate = findViewById(R.id.tilDate)
        tilTime = findViewById(R.id.tilTime)
        tilServiceType = findViewById(R.id.tilServiceType)
        tilDiagnosis = findViewById(R.id.tilDiagnosis)
        tilWorkDone = findViewById(R.id.tilWorkDone)
        tilRecommendations = findViewById(R.id.tilRecommendations)
        tilStatus = findViewById(R.id.tilStatus)

        val prefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
        etTechnician.setText(prefs.getString("NOMBRE_USUARIO", "Técnico") ?: "Técnico")
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        etDate.setText(fechaHoy)
    }

    private fun cargarDatosOrden() {
        if (ordenId != -1) {
            val datos = ordenRepository.obtenerDetalleOrdenConCliente(ordenId)
            tvOrderNumber.text = "Orden: ${datos["numero"]}"
            tvClientName.text = "Cliente: ${datos["cliente_nombre"]}"
            tvClientPhone.text = "Tel: ${datos["cliente_telefono"]}"
            tvClientAddress.text = "Dir: ${datos["cliente_direccion"]}"
            tvEquipmentName.text = "Equipo: ${datos["equipo_info"]}"
        }
    }

    private fun setupDropdowns() {
        val serviceTypes = arrayOf("Preventivo", "Correctivo", "Asesoría", "Inspección")
        val adapterService = ArrayAdapter(this, android.R.layout.simple_list_item_1, serviceTypes)
        actvServiceType.setAdapter(adapterService)

        val statuses = arrayOf("OPERATIVO", "EN MANTENIMIENTO", "FUERA DE SERVICIO")
        val adapterStatus = ArrayAdapter(this, android.R.layout.simple_list_item_1, statuses)
        actvStatus.setAdapter(adapterStatus)
    }

    private fun setupDateTimePickers() {
        etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etDate.setText(format.format(calendar.time))
                tilDate.error = null
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        etTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Seleccionar hora")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val formattedTime = String.format("%02d:%02d", timePicker.hour, timePicker.minute)
                etTime.setText(formattedTime)
                tilTime.error = null
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }
    }

    private fun setupNavigation() {
        findViewById<Button>(R.id.btnSpareParts).setOnClickListener {
            val mId = ordenRepository.obtenerMantenimientoIdPorOrden(ordenId)
            val intent = Intent(this, SparePartsActivity::class.java)
            intent.putExtra("ORDEN_ID", ordenId)
            intent.putExtra("MANTENIMIENTO_ID", mId)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnEvidences).setOnClickListener {
            val intent = Intent(this, EvidencesActivity::class.java)
            intent.putExtra("ORDEN_ID", ordenId)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnGeolocation).setOnClickListener {
            val datos = ordenRepository.obtenerDetalleOrdenConCliente(ordenId)
            val intent = Intent(this, GeolocationActivity::class.java).apply {
                putExtra("ORDEN_ID", ordenId)
                putExtra("CODIGO_ORDEN", datos["numero"] ?: "S/N")
                putExtra("NOMBRE", datos["cliente_nombre"] ?: "Cliente")
                putExtra("APELLIDO", "") // No hay campo apellido separado en la BD actual
                putExtra("DIRECCION", datos["cliente_direccion"] ?: "Sin dirección registrada")
                putExtra("CELULAR", datos["cliente_telefono"] ?: "Sin número registrado")
            }
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnApproval).setOnClickListener {
            val intent = Intent(this, ApprovalActivity::class.java)
            intent.putExtra("ORDEN_ID", ordenId)
            startActivity(intent)
        }
    }

    private fun validarFormulario(): Boolean {
        var esValido = true
        if (etDate.text.isNullOrEmpty()) { tilDate.error = "La fecha es obligatoria"; esValido = false }
        if (actvServiceType.text.isNullOrEmpty()) { tilServiceType.error = "Seleccione el tipo de servicio"; esValido = false }
        if (etDiagnosis.text.isNullOrEmpty()) { tilDiagnosis.error = "El diagnóstico es obligatorio"; esValido = false }
        if (etWorkDone.text.isNullOrEmpty()) { tilWorkDone.error = "Describa el trabajo realizado"; esValido = false }
        if (etRecommendations.text.isNullOrEmpty()) { tilRecommendations.error = "Las recomendaciones son obligatorias"; esValido = false } else { tilRecommendations.error = null }
        if (actvStatus.text.isNullOrEmpty()) { tilStatus.error = "Seleccione el estado final"; esValido = false }
        return esValido
    }

    private fun guardarDatos() {
        val mantenimiento = Mantenimiento(
            orden_id = ordenId,
            fecha = etDate.text.toString(),
            diagnostico = etDiagnosis.text.toString(),
            trabajo_realizado = etWorkDone.text.toString(),
            observaciones = etObservations.text.toString(),
            recomendaciones = etRecommendations.text.toString(),
            tiempoEmpleado = etTimeSpent.text.toString(),
            nombreTecnico = etTechnician.text.toString()
        )
        // El "Estado del equipo" seleccionado se refleja en la hoja de vida del equipo (Módulo 5)
        val exito = ordenRepository.guardarMantenimiento(mantenimiento, actvStatus.text.toString())
        if (exito) {
            Toast.makeText(this, "Mantenimiento registrado correctamente", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
        }
    }
}
