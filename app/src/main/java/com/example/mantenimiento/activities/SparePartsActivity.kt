package com.example.mantenimiento.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.SparePartAdapter
import com.example.mantenimiento.models.Repuesto
import com.example.mantenimiento.repositories.OrdenRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SparePartsActivity : AppCompatActivity() {

    private lateinit var adapter: SparePartAdapter
    private lateinit var ordenRepository: OrdenRepository
    private var ordenId: Int = -1
    private var mantenimientoId: Int = -1
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ordenRepository = OrdenRepository(this)
        ordenId = intent.getIntExtra("ORDEN_ID", -1)
        if (ordenId == -1) {
            Toast.makeText(this, "No se pudo identificar la orden.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        mantenimientoId = ordenRepository.obtenerMantenimientoIdPorOrden(ordenId)

        tvTotal = findViewById(R.id.tvTotalSpareParts)
        val rvSpareParts = findViewById<RecyclerView>(R.id.rvSpareParts)
        rvSpareParts.layoutManager = LinearLayoutManager(this)

        adapter = SparePartAdapter(emptyList()) { repuesto ->
            eliminarRepuesto(repuesto)
        }
        rvSpareParts.adapter = adapter

        // Flecha de retroceso
        val toolbar = findViewById<Toolbar>(R.id.toolbarSpareParts)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnAddSparePart).setOnClickListener {
            if (mantenimientoId == -1) {
                Toast.makeText(this, "Debe iniciar el mantenimiento primero", Toast.LENGTH_SHORT).show()
            } else {
                mostrarDialogoAgregar()
            }
        }

        if (mantenimientoId != -1) {
            cargarRepuestos()
        }
    }

    private fun cargarRepuestos() {
        val list = ordenRepository.obtenerRepuestosPorMantenimiento(mantenimientoId)
        adapter.updateList(list)
        tvTotal.text = "Total de repuestos: ${list.size}"
    }

    private fun mostrarDialogoAgregar() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_spare_part, null)
        val actvRepuestos = dialogView.findViewById<AutoCompleteTextView>(R.id.actvSelectSparePart)
        val etQty = dialogView.findViewById<EditText>(R.id.etSparePartQty)

        val catalogo = ordenRepository.obtenerCatalogoRepuestos()
        val nombres = catalogo.map { "${it.codigo} - ${it.nombre}" }
        actvRepuestos.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, nombres))

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("AGREGAR") { _, _ ->
                val seleccion = actvRepuestos.text.toString()
                val cantidadStr = etQty.text.toString()

                if (seleccion.isNotEmpty() && cantidadStr.isNotEmpty()) {
                    val index = nombres.indexOf(seleccion)
                    if (index != -1) {
                        ordenRepository.insertarDetalleRepuesto(mantenimientoId, catalogo[index].id, cantidadStr.toInt())
                        cargarRepuestos()
                    }
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun eliminarRepuesto(repuesto: Repuesto) {
        ordenRepository.eliminarDetalleRepuesto(mantenimientoId, repuesto.id)
        cargarRepuestos()
        Toast.makeText(this, "Repuesto eliminado", Toast.LENGTH_SHORT).show()
    }
}
