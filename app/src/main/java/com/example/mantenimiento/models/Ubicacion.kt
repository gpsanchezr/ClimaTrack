package com.example.mantenimiento.models

// Entidad que representa la Tabla ubicaciones
data class Ubicacion(
    val id: Int = 0,
    val orden_id: Int,
    val latitud: Double, // Usamos Double para coordenadas precisas
    val longitud: Double,
    val fecha: String
)