package com.example.mantenimiento.activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mantenimiento.R
import com.example.mantenimiento.models.Equipo
import com.example.mantenimiento.repositories.EquipmentRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class EquipmentDetailActivity : AppCompatActivity() {

    private lateinit var repository: EquipmentRepository
    private var equipoId: Int = -1
    
    private lateinit var etCode: TextInputEditText
    private lateinit var etType: TextInputEditText
    private lateinit var etBrand: TextInputEditText
    private lateinit var etModel: TextInputEditText
    private lateinit var etSerial: TextInputEditText
    private lateinit var etCapacity: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var actvClient: AutoCompleteTextView
    private lateinit var actvStatus: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equipment_detail)

        repository = EquipmentRepository(this)
        equipoId = intent.getIntExtra("EQUIPO_ID", -1)

        initViews()
        setupDropdowns()
        setupToolbar()

        if (equipoId != -1) {
            cargarDatosEquipo()
            findViewById<MaterialButton>(R.id.btnDeleteEquip).visibility = View.VISIBLE
        }

        findViewById<MaterialButton>(R.id.btnSaveEquip).setOnClickListener { guardarEquipo() }
        findViewById<MaterialButton>(R.id.btnDeleteEquip).setOnClickListener { confirmarEliminacion() }
    }

    private fun initViews() {
        etCode = findViewById(R.id.etEquipCode)
        etType = findViewById(R.id.etEquipType)
        etBrand = findViewById(R.id.etEquipBrand)
        etModel = findViewById(R.id.etEquipModel)
        etSerial = findViewById(R.id.etEquipSerial)
        etCapacity = findViewById(R.id.etEquipCapacity)
        etLocation = findViewById(R.id.etEquipLocation)
        actvClient = findViewById(R.id.actvEquipClient)
        actvStatus = findViewById(R.id.actvEquipStatus)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarEquipDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        val clientesMap = repository.obtenerClientes()
        actvClient.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, clientesMap.values.toList()))

        val statuses = arrayOf("OPERATIVO", "EN MANTENIMIENTO", "FUERA DE SERVICIO")
        actvStatus.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, statuses))
    }

    private fun cargarDatosEquipo() {
        repository.getAllEquipment().find { it.id == equipoId }?.let {
            etCode.setText(it.codigo)
            etType.setText(it.tipo)
            etBrand.setText(it.marca)
            etModel.setText(it.modelo)
            etSerial.setText(it.serial)
            etCapacity.setText(it.capacidad)
            etLocation.setText(it.ubicacion)
            actvStatus.setText(it.estado, false)
            
            val clientesMap = repository.obtenerClientes()
            actvClient.setText(clientesMap[it.cliente_id] ?: "", false)
        }
    }

    private fun guardarEquipo() {
        if (etCode.text.isNullOrEmpty() || etType.text.isNullOrEmpty() || actvClient.text.isNullOrEmpty()) {
            Toast.makeText(this, "Complete los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val clienteNombre = actvClient.text.toString().trim()
        val cId = repository.obtenerOCrearCliente(clienteNombre, etLocation.text.toString().trim())

        val e = Equipo(
            id = if (equipoId == -1) 0 else equipoId,
            codigo = etCode.text.toString().trim(),
            tipo = etType.text.toString().trim(),
            marca = etBrand.text.toString().trim(),
            modelo = etModel.text.toString().trim(),
            serial = etSerial.text.toString().trim(),
            capacidad = etCapacity.text.toString().trim(),
            ubicacion = etLocation.text.toString().trim(),
            cliente_id = cId,
            estado = actvStatus.text.toString().ifEmpty { "OPERATIVO" }
        )

        val success = if (equipoId == -1) {
            repository.insertarEquipo(e) != -1L
        } else {
            repository.actualizarEquipo(e) > 0
        }

        if (success) {
            Toast.makeText(this, "Equipo guardado correctamente en SQLite", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error al guardar el equipo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminacion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Equipo")
            .setMessage("¿Está seguro de eliminar este equipo?")
            .setPositiveButton("ELIMINAR") { _, _ ->
                repository.eliminarEquipo(equipoId)
                finish()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }
}
