package com.example.mantenimiento.models

// Entidad que representa la Tabla aprobaciones
data class Aprobacion(
    val id: Int = 0,
    val orden_id: Int,
    val cliente: String,
    val aceptado: Int, // Usamos Int (1 o 0) porque SQLite no tiene tipo Boolean nativo
    val fecha: String
)