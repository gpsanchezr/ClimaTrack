package com.example.mantenimiento.models

// Entidad que representa la Tabla mantenimientos
data class Mantenimiento(
    val id: Int = 0,
    val orden_id: Int, // Conecta con la Orden a la que pertenece
    val fecha: String,
    val diagnostico: String,
    val trabajo_realizado: String,
    val observaciones: String,
    val recomendaciones: String,
    val tiempoEmpleado: String = "",
    val nombreTecnico: String = ""
)