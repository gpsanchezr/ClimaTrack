package com.example.mantenimiento.models

// Entidad que representa el catálogo de Tabla repuestos
data class Repuesto(
    val id: Int = 0,
    val nombre: String,
    val codigo: String,
    val unidad: String
)