package com.example.mantenimiento.utils

import android.content.Context
import com.example.mantenimiento.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {
    /**
     * Aplica reglas dinámicas de visibilidad al menú inferior según el rol en SharedPreferences.
     */
    fun BottomNavigationView.configurarPorRol(context: Context) {
        val prefs = context.getSharedPreferences("ClimaTrackPrefs", Context.MODE_PRIVATE)
        val rol = prefs.getString("ROL_USUARIO", "Cliente") ?: "Cliente"
        
        val menu = this.menu
        when (rol) {
            "Administrador" -> {
                // Ve todo: Inicio, Órdenes, Equipos, Historial
                menu.findItem(R.id.nav_ordenes)?.isVisible = true
                menu.findItem(R.id.nav_equipos)?.isVisible = true
            }
            "Técnico" -> {
                // Oculta Equipos/Gestión
                menu.findItem(R.id.nav_ordenes)?.isVisible = true
                menu.findItem(R.id.nav_equipos)?.isVisible = false
            }
            "Cliente" -> {
                // Oculta Órdenes Globales y Equipos
                menu.findItem(R.id.nav_ordenes)?.isVisible = false
                menu.findItem(R.id.nav_equipos)?.isVisible = false
            }
        }
    }
}
