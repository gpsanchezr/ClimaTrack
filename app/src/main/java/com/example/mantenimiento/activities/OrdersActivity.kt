package com.example.mantenimiento.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.OrdenAdapter
import com.example.mantenimiento.repositories.OrdenRepository
import com.example.mantenimiento.utils.NavigationUtils.configurarPorRol
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class OrdersActivity : AppCompatActivity() {

    private lateinit var adapter: OrdenAdapter
    private lateinit var ordenRepository: OrdenRepository
    private var currentStatusFilter = "PENDIENTE"
    private var rolUsuario: String = "Técnico"
    private var tecnicoIdFiltro: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_orders)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ordenRepository = OrdenRepository(this)

        // Módulo 3: "consultar las órdenes asignadas AL TÉCNICO" -- antes esta
        // pantalla mostraba siempre TODAS las órdenes de la empresa sin
        // importar quién iniciara sesión. Ahora un Técnico solo ve las suyas;
        // el Administrador conserva la vista completa.
        val prefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
        rolUsuario = intent.getStringExtra("ROL_USUARIO") ?: prefs.getString("ROL_USUARIO", "Técnico") ?: "Técnico"
        val userId = prefs.getInt("ID_USUARIO", -1)
        tecnicoIdFiltro = if (rolUsuario == "Técnico" && userId != -1) userId else null

        val rvOrders = findViewById<RecyclerView>(R.id.rvOrders)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Aplicar seguridad de menú
        bottomNavigation.configurarPorRol(this)

        rvOrders.layoutManager = LinearLayoutManager(this)
        
        // Inicializar adaptador con callback de cambio de estado
        adapter = OrdenAdapter(emptyList()) { ordenId, nuevoEstado ->
            val rows = ordenRepository.actualizarEstadoOrden(ordenId, nuevoEstado)
            if (rows > 0) {
                Toast.makeText(this, "Estado actualizado: $nuevoEstado", Toast.LENGTH_SHORT).show()
                loadOrders(currentStatusFilter)
            }
        }
        rvOrders.adapter = adapter

        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchViewOrders)
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

        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = when (tab?.position) {
                    0 -> "PENDIENTE"
                    1 -> "EN PROCESO"
                    2 -> "FINALIZADA"
                    else -> "PENDIENTE"
                }
                loadOrders(currentStatusFilter)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        bottomNavigation.selectedItemId = R.id.nav_ordenes
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_ordenes -> true
                R.id.nav_equipos -> {
                    startActivity(Intent(this, EquipmentActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_historial -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        loadOrders(currentStatusFilter)
    }

    private fun loadOrders(estado: String) {
        val orders = ordenRepository.getOrdenesByEstado(estado, tecnicoIdFiltro)
        adapter.updateList(orders)
    }
}
