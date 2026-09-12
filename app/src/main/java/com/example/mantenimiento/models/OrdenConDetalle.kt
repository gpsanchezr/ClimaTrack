package com.example.mantenimiento.models

/**
 * Fila de la lista de Órdenes (Módulo 3) ya combinada con el nombre real del
 * cliente y la descripción real del equipo.
 *
 * Antes, OrdenAdapter mostraba un texto fijo ("Cliente: ACME S.A.S.") para
 * todas las filas sin importar la orden real, y "Equipo:" en realidad
 * mostraba el tipo de servicio (Preventivo/Correctivo/...). Este modelo se
 * llena con un JOIN real (ver OrdenRepository.getOrdenesByEstado) para que
 * cada fila muestre el cliente y el equipo que de verdad le corresponden.
 */
data class OrdenConDetalle(
    val id: Int,
    val numero: String,
    val fecha: String,
    val clienteNombre: String,
    val equipoDescripcion: String,
    val tipoServicio: String,
    val estado: String
)
