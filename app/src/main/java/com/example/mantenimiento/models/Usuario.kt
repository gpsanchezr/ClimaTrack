package com.example.mantenimiento.models

// Entidad que representa la Tabla usuarios de la base de datos
data class Usuario(
    val id: Int = 0, // Ponemos = 0 para que SQLite genere el ID automáticamente luego
    val usuario: String,
    val password: String,
    val nombre: String,
    val rol: String,
    val genero: String // "M" para masculino, "F" para femenino
)
