package com.example.mantenimiento.models

/**
 * Modelo extendido para el historial que incluye datos de hardware y el ID de la orden.
 */
data class MantenimientoCompleto(
    val id: Int,             // ID del Mantenimiento
    val ordenId: Int,        // ID de la Orden vinculada
    val ordenNumero: String,
    val fecha: String,
    val trabajoRealizado: String,
    val tecnicoNombre: String,
    val latitud: Double?,
    val longitud: Double?,
    val fotoRuta: String?,
    val firmaRuta: String?,
    val tipoServicio: String
)
