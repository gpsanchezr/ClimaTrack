package com.example.mantenimiento.models

// Entidad que representa la Tabla clientes
data class Cliente(
    val id: Int = 0,
    val nombre: String,
    val telefono: String,
    val direccion: String,
    val email: String
)