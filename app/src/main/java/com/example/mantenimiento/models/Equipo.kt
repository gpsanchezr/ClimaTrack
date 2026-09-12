package com.example.mantenimiento.models

// Entidad que representa la Tabla equipos
data class Equipo(
    val id: Int = 0,
    val codigo: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val serial: String,
    val capacidad: String,
    val ubicacion: String,
    val cliente_id: Int, // Llave foránea (FK) para saber de qué cliente es el equipo
    val estado: String
)