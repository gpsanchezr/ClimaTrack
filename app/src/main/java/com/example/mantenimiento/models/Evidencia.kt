package com.example.mantenimiento.models

// Entidad que representa la Tabla evidencias
data class Evidencia(
    val id: Int = 0,
    val orden_id: Int,
    val ruta_foto: String,
    val fecha: String
)