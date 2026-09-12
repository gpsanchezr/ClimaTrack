package com.example.mantenimiento.models

// Entidad que representa la Tabla detalle_repuestos
data class DetalleRepuesto(
    val id: Int = 0,
    val mantenimiento_id: Int,
    val repuesto_id: Int,
    val cantidad: Int
)