package com.example.mantenimiento.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.HistoryAdapter
import com.example.mantenimiento.repositories.OrdenRepository
import com.example.mantenimiento.utils.NavigationUtils.configurarPorRol
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {

    private lateinit var adapter: HistoryAdapter
    private lateinit var repository: OrdenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = OrdenRepository(this)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Flecha de retroceso funcional
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        rvHistory.layoutManager = LinearLayoutManager(this)

        // Cargar datos reales con JOINs filtrados por Rol y Sesión de manera segura
        val prefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
        val rol = intent.getStringExtra("ROL_USUARIO") ?: prefs.getString("ROL_USUARIO", "Cliente") ?: "Cliente"
        val userId = prefs.getInt("ID_USUARIO", -1)

        val history = repository.obtenerHistorialSeguro(rol, userId)
        adapter = HistoryAdapter(history)
        rvHistory.adapter = adapter

        // Buscador real en tiempo real
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchViewHistory)
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return true
            }
        })

        // Botón de ordenamiento A-Z / Z-A
        var isAscending = true
        findViewById<android.widget.ImageView>(R.id.btnSortHistory)?.setOnClickListener {
            isAscending = !isAscending
            adapter.ordenarAlfabeticamente(isAscending)
            val msg = if (isAscending) "Historial ordenado A-Z" else "Historial ordenado Z-A"
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        }

        // --- LÓGICA DE ROLES PARA LA UI (Centralizada) ---
        bottomNavigation.configurarPorRol(this)

        bottomNavigation.selectedItemId = R.id.nav_historial
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    finish()
                    true
                }
                R.id.nav_ordenes -> {
                    if (rol != "Cliente") {
                        startActivity(Intent(this, OrdersActivity::class.java).apply {
                            putExtra("ROL_USUARIO", rol)
                        })
                        finish()
                    }
                    true
                }
                R.id.nav_equipos -> {
                    if (rol == "Administrador") {
                        startActivity(Intent(this, EquipmentActivity::class.java).apply {
                            putExtra("ROL_USUARIO", rol)
                        })
                        finish()
                    }
                    true
                }
                R.id.nav_historial -> true
                else -> false
            }
        }
    }
}