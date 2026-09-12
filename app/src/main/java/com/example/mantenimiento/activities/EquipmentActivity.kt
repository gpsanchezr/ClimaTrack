package com.example.mantenimiento.activities

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.EquipmentAdapter
import com.example.mantenimiento.repositories.EquipmentRepository
import com.example.mantenimiento.utils.NavigationUtils.configurarPorRol
import com.google.android.material.bottomnavigation.BottomNavigationView

class EquipmentActivity : AppCompatActivity() {

    private lateinit var adapter: EquipmentAdapter
    private lateinit var repository: EquipmentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equipment)

        repository = EquipmentRepository(this)

        setupRecyclerView()
        setupNavigation()
        setupSearch()

        findViewById<android.widget.ImageView>(R.id.btnAddEquipment).setOnClickListener {
            startActivity(Intent(this, EquipmentDetailActivity::class.java))
        }

        // Flecha de retroceso funcional
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadEquipment()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvEquipment)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = EquipmentAdapter(emptyList()) { equipo ->
            val intent = Intent(this, EquipmentDetailActivity::class.java)
            intent.putExtra("EQUIPO_ID", equipo.id)
            startActivity(intent)
        }
        rv.adapter = adapter
    }

    private var isAscending = true

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearchEquipment)
        etSearch.addTextChangedListener { text ->
            val list = repository.buscarEquipos(text.toString())
            val sortedList = if (isAscending) list.sortedBy { it.marca } else list.sortedByDescending { it.marca }
            adapter.updateList(sortedList)
        }

        findViewById<android.widget.ImageView>(R.id.btnSortEquipment)?.setOnClickListener {
            isAscending = !isAscending
            val currentText = etSearch.text.toString()
            val list = repository.buscarEquipos(currentText)
            val sortedList = if (isAscending) list.sortedBy { it.marca } else list.sortedByDescending { it.marca }
            adapter.updateList(sortedList)
            val msg = if (isAscending) "Equipos ordenados A-Z por marca" else "Equipos ordenados Z-A por marca"
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadEquipment() {
        val list = repository.getAllEquipment()
        adapter.updateList(list)
    }

    private fun setupNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_equipos
        bottomNavigation.configurarPorRol(this)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    finish()
                    true
                }
                R.id.nav_ordenes -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_equipos -> true
                R.id.nav_historial -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
