package com.example.mantenimiento.models

// Entidad que representa la Tabla ordenes
data class Orden(
    val id: Int = 0,
    val numero: String,
    val fecha: String,
    val cliente_id: Int,  // Conecta con la tabla Clientes
    val equipo_id: Int,   // Conecta con la tabla Equipos
    val tecnico_id: Int,  // Conecta con la tabla Usuarios (Técnicos)
    val tipo_servicio: String,
    val descripcion: String,
    val estado: String
)
