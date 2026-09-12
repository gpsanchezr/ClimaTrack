package com.example.mantenimiento.repositories

import android.content.ContentValues
import android.content.Context
import com.example.mantenimiento.database.DatabaseHelper

class ServiceRequestRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    /**
     * Flujo del Cliente: Registra la falla, marca y dirección exacta.
     * Al guardar, ingresa a SQLite con estado 'PENDIENTE_ADMIN' y sin técnico asignado.
     */
    fun crearSolicitud(
        clienteId: Int,
        nombre: String,
        dir: String,
        lat: Double,
        lon: Double,
        marca: String,
        falla: String,
        direccionExacta: String = ""
    ): Long {
        val db = dbHelper.writableDatabase
        val direccionFinal = if (direccionExacta.isNotBlank()) direccionExacta else dir

        val v = ContentValues().apply {
            put("cliente_id", clienteId)
            put("nombre_cliente", nombre)
            put("direccion", dir)
            put("direccion_exacta", direccionFinal)
            put("latitud", lat)
            put("longitud", lon)
            put("marca_equipo", marca)
            put("falla_reportada", falla)
            put("estado", "PENDIENTE_ADMIN")
            put("estado_evento", "POR_AGENDAR")
            putNull("tecnico_id")
            putNull("tecnico_asignado")
        }
        return db.insert("solicitudes_servicio", null, v)
    }

    private fun obtenerDireccionMostrar(cursor: android.database.Cursor): Pair<String, String> {
        val idxExacta = cursor.getColumnIndex("direccion_exacta")
        val dirExacta = if (idxExacta != -1) cursor.getString(idxExacta) ?: "" else ""
        val dirGeneral = cursor.getString(cursor.getColumnIndexOrThrow("direccion")) ?: ""
        val dirMostrar = if (dirExacta.isNotBlank()) dirExacta else dirGeneral
        return Pair(dirMostrar, dirExacta)
    }

    /**
     * Flujo del Administrador (Filtro):
     * Muestra únicamente las solicitudes con estado 'PENDIENTE_ADMIN' (o 'PENDIENTE').
     */
    fun obtenerSolicitudesPendientesAdmin(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM solicitudes_servicio WHERE estado = 'PENDIENTE_ADMIN' OR estado = 'PENDIENTE'",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val (dirMostrar, dirExacta) = obtenerDireccionMostrar(cursor)
                list.add(mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "cliente" to (cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: ""),
                    "direccion" to dirMostrar,
                    "direccion_exacta" to dirExacta,
                    "marca" to (cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")) ?: ""),
                    "falla" to (cursor.getString(cursor.getColumnIndexOrThrow("falla_reportada")) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    /**
     * Para mantener compatibilidad total con vistas existentes:
     * Retorna solicitudes en estado 'PUBLICADA', 'PENDIENTE_ADMIN' o 'PENDIENTE'.
     */
    fun obtenerSolicitudesPendientes(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM solicitudes_servicio WHERE estado IN ('PUBLICADA', 'PENDIENTE_ADMIN', 'PENDIENTE')",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val (dirMostrar, dirExacta) = obtenerDireccionMostrar(cursor)
                list.add(mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "cliente" to (cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: ""),
                    "direccion" to dirMostrar,
                    "direccion_exacta" to dirExacta,
                    "marca" to (cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")) ?: ""),
                    "falla" to (cursor.getString(cursor.getColumnIndexOrThrow("falla_reportada")) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    /**
     * Flujo del Administrador (Publicación):
     * Cambia el estado de una solicitud específica a 'PUBLICADA' para hacerla
     * visible al equipo de campo en el radar.
     */
    fun publicarSolicitud(solicitudId: Int): Int {
        val db = dbHelper.writableDatabase
        val v = ContentValues().apply {
            put("estado", "PUBLICADA")
        }
        return db.update(
            "solicitudes_servicio",
            v,
            "id = ? AND (estado = 'PENDIENTE_ADMIN' OR estado = 'PENDIENTE')",
            arrayOf(solicitudId.toString())
        )
    }

    /**
     * Flujo del Técnico: Muestra en el radar únicamente las solicitudes en estado 'PUBLICADA'.
     */
    fun obtenerSolicitudesPublicadas(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM solicitudes_servicio WHERE estado = 'PUBLICADA'", null)
        if (cursor.moveToFirst()) {
            do {
                val (dirMostrar, dirExacta) = obtenerDireccionMostrar(cursor)
                list.add(mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "cliente" to (cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: ""),
                    "direccion" to dirMostrar,
                    "direccion_exacta" to dirExacta,
                    "marca" to (cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")) ?: ""),
                    "falla" to (cursor.getString(cursor.getColumnIndexOrThrow("falla_reportada")) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    /**
     * El Administrador asigna directamente un técnico a una solicitud con fecha y hora.
     */
    fun asignarTecnico(
        solicitudId: Int,
        tecnicoUsuario: String,
        fecha: String = "",
        hora: String = "",
        estadoEvento: String = "PROGRAMADO"
    ): Int {
        val db = dbHelper.writableDatabase

        var idTecnico: Int? = null
        if (tecnicoUsuario.isNotBlank()) {
            val tecCursor = db.rawQuery("SELECT id FROM usuarios WHERE usuario = ?", arrayOf(tecnicoUsuario))
            if (tecCursor.moveToFirst()) {
                idTecnico = tecCursor.getInt(0)
            }
            tecCursor.close()
        }

        val v = ContentValues().apply {
            put("tecnico_asignado", tecnicoUsuario)
            if (idTecnico != null) put("tecnico_id", idTecnico) else putNull("tecnico_id")
            put("fecha_programada", fecha)
            put("hora_programada", hora)
            put("estado_evento", estadoEvento)
            put("estado", "ASIGNADA")
        }
        return db.update("solicitudes_servicio", v, "id = ?", arrayOf(solicitudId.toString()))
    }

    /**
     * REQUISITO CRÍTICO: Flujo del Técnico (Competencia y Concurrencia).
     *
     * Ejecuta una actualización condicional atómica en SQLite:
     * UPDATE solicitudes_servicio SET estado = 'ASIGNADA', tecnico_id = ?, tecnico_asignado = ?
     * WHERE id = ? AND estado = 'PUBLICADA'
     *
     * - Si afecta > 0 filas: El técnico gana la oferta (devuelve > 0).
     * - Si afecta 0 filas: Otro técnico la ganó antes (devuelve 0).
     */
    fun aceptarSolicitud(solicitudId: Int, tecnicoUsuario: String, tecnicoId: Int? = null): Int {
        val db = dbHelper.writableDatabase

        var idTecnico = tecnicoId
        if (idTecnico == null && tecnicoUsuario.isNotBlank()) {
            val tecCursor = db.rawQuery("SELECT id FROM usuarios WHERE usuario = ?", arrayOf(tecnicoUsuario))
            if (tecCursor.moveToFirst()) {
                idTecnico = tecCursor.getInt(0)
            }
            tecCursor.close()
        }

        val v = ContentValues().apply {
            put("estado", "ASIGNADA")
            put("tecnico_asignado", tecnicoUsuario)
            if (idTecnico != null) put("tecnico_id", idTecnico) else putNull("tecnico_id")
        }

        // UPDATE condicional atómico
        return db.update(
            "solicitudes_servicio",
            v,
            "id = ? AND (estado = 'PUBLICADA' OR estado = 'PENDIENTE_ADMIN' OR estado = 'PENDIENTE')",
            arrayOf(solicitudId.toString())
        )
    }

    fun obtenerEstadoSolicitud(solicitudId: Int): Map<String, String>? {
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT s.estado, u.nombre 
            FROM solicitudes_servicio s
            LEFT JOIN usuarios u ON (s.tecnico_id = u.id OR s.tecnico_asignado = u.usuario)
            WHERE s.id = ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(solicitudId.toString()))
        var data: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            data = mapOf(
                "estado" to (cursor.getString(0) ?: "PENDIENTE_ADMIN"),
                "tecnico" to (cursor.getString(1) ?: "Buscando...")
            )
        }
        cursor.close()
        return data
    }

    fun confirmarTecnico(solicitudId: Int): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Marcar solicitud como confirmada
            val v = ContentValues().apply { put("estado", "CONFIRMADA_CLIENTE") }
            db.update("solicitudes_servicio", v, "id = ?", arrayOf(solicitudId.toString()))

            // 2. Recuperar datos para crear la Orden Real
            val sql = "SELECT * FROM solicitudes_servicio WHERE id = ?"
            val cursor = db.rawQuery(sql, arrayOf(solicitudId.toString()))
            var newOrderId = -1L

            if (cursor.moveToFirst()) {
                val usuarioClienteId = cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id"))
                val nombreCliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: "Cliente"
                val (dirMostrar, _) = obtenerDireccionMostrar(cursor)

                val tecnicoAsignado = cursor.getString(cursor.getColumnIndexOrThrow("tecnico_asignado"))
                val marca = cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")) ?: "Genérica"
                val falla = cursor.getString(cursor.getColumnIndexOrThrow("falla_reportada")) ?: ""

                val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val numeroOrden = "OT-${System.currentTimeMillis().toString().takeLast(5)}"

                var tecnicoIdNumerico: Int? = null
                val idxTecId = cursor.getColumnIndex("tecnico_id")
                if (idxTecId != -1 && !cursor.isNull(idxTecId)) {
                    tecnicoIdNumerico = cursor.getInt(idxTecId)
                }
                if (tecnicoIdNumerico == null && !tecnicoAsignado.isNullOrEmpty()) {
                    val tecCursor = db.rawQuery("SELECT id FROM usuarios WHERE usuario = ?", arrayOf(tecnicoAsignado))
                    if (tecCursor.moveToFirst()) {
                        tecnicoIdNumerico = tecCursor.getInt(0)
                    }
                    tecCursor.close()
                }

                var clienteIdReal: Int
                val clienteCursor = db.rawQuery("SELECT id FROM clientes WHERE usuario_id = ?", arrayOf(usuarioClienteId.toString()))
                if (clienteCursor.moveToFirst()) {
                    clienteIdReal = clienteCursor.getInt(0)
                } else {
                    clienteCursor.close()
                    val nuevoCliente = ContentValues().apply {
                        put("nombre", nombreCliente)
                        put("telefono", "")
                        put("direccion", dirMostrar)
                        put("email", "")
                        put("usuario_id", usuarioClienteId)
                    }
                    clienteIdReal = db.insert("clientes", null, nuevoCliente).toInt()
                }
                clienteCursor.close()

                val nuevoEquipo = ContentValues().apply {
                    put("codigo", "EQ-AUTO-${System.currentTimeMillis().toString().takeLast(6)}")
                    put("tipo", "Aire acondicionado")
                    put("marca", marca)
                    put("modelo", "Por confirmar en sitio")
                    put("serial", "N/A")
                    put("capacidad", "N/A")
                    put("ubicacion", dirMostrar)
                    put("cliente_id", clienteIdReal)
                    put("estado", "OPERATIVO")
                }
                val equipoIdReal = db.insert("equipos", null, nuevoEquipo)

                // 3. Insertar en tabla Ordenes
                val orderValues = ContentValues().apply {
                    put("numero", numeroOrden)
                    put("fecha", fechaActual)
                    put("cliente_id", clienteIdReal)
                    put("equipo_id", equipoIdReal)
                    if (tecnicoIdNumerico != null) put("tecnico_id", tecnicoIdNumerico) else putNull("tecnico_id")
                    put("tipo_servicio", "MANTENIMIENTO")
                    put("descripcion", "Marca: $marca - Falla: $falla")
                    put("estado", "PENDIENTE")
                }
                newOrderId = db.insert("ordenes", null, orderValues)
            }
            cursor.close()
            db.setTransactionSuccessful()
            return newOrderId
        } catch (e: Exception) {
            e.printStackTrace()
            return -1L
        } finally {
            db.endTransaction()
        }
    }

    fun rechazarSolicitud(solicitudId: Int): Int {
        val db = dbHelper.writableDatabase
        val v = ContentValues().apply {
            put("estado", "RECHAZADA")
        }
        return db.update("solicitudes_servicio", v, "id = ?", arrayOf(solicitudId.toString()))
    }

    fun validarHorarioDisponible(usuarioTecnico: String, fecha: String, hora: String): Boolean {
        val db = dbHelper.readableDatabase
        val sql = "SELECT COUNT(*) FROM solicitudes_servicio WHERE (tecnico_asignado = ? OR tecnico_id = (SELECT id FROM usuarios WHERE usuario = ?)) AND fecha_programada = ? AND hora_programada = ? AND estado != 'FINALIZADA'"
        val cursor = db.rawQuery(sql, arrayOf(usuarioTecnico, usuarioTecnico, fecha, hora))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count == 0
    }

    fun obtenerAgendaTecnico(usuarioTecnico: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = dbHelper.readableDatabase
        val sql = "SELECT * FROM solicitudes_servicio WHERE (tecnico_asignado = ? OR tecnico_id = (SELECT id FROM usuarios WHERE usuario = ?)) AND estado IN ('ASIGNADA', 'ASIGNADO') ORDER BY fecha_programada, hora_programada"
        val cursor = db.rawQuery(sql, arrayOf(usuarioTecnico, usuarioTecnico))
        if (cursor.moveToFirst()) {
            do {
                val (dirMostrar, _) = obtenerDireccionMostrar(cursor)

                list.add(mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "cliente" to (cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: ""),
                    "direccion" to dirMostrar,
                    "fecha" to (cursor.getString(cursor.getColumnIndexOrThrow("fecha_programada")) ?: ""),
                    "hora" to (cursor.getString(cursor.getColumnIndexOrThrow("hora_programada")) ?: ""),
                    "marca" to (cursor.getString(cursor.getColumnIndexOrThrow("marca_equipo")) ?: ""),
                    "falla" to (cursor.getString(cursor.getColumnIndexOrThrow("falla_reportada")) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}