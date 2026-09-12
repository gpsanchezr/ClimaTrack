package com.example.mantenimiento.repositories

import android.content.Context
import com.example.mantenimiento.database.DatabaseHelper
import com.example.mantenimiento.models.Orden
import com.example.mantenimiento.models.OrdenConDetalle
import com.example.mantenimiento.models.Mantenimiento
import com.example.mantenimiento.models.MantenimientoCompleto
import com.example.mantenimiento.models.Evidencia
import com.example.mantenimiento.models.Ubicacion
import android.content.ContentValues

class OrdenRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    /**
     * @param tecnicoId si se indica, cuenta solo las órdenes de ESE técnico
     * (Dashboard de un Técnico); si es null, cuenta las de toda la empresa
     * (Dashboard del Administrador).
     */
    fun getCountByEstado(estado: String, tecnicoId: Int? = null): Int {
        val db = dbHelper.readableDatabase
        val sql = if (tecnicoId != null) "SELECT COUNT(*) FROM ordenes WHERE estado = ? AND tecnico_id = ?"
                  else "SELECT COUNT(*) FROM ordenes WHERE estado = ?"
        val args = if (tecnicoId != null) arrayOf(estado, tecnicoId.toString()) else arrayOf(estado)
        val cursor = db.rawQuery(sql, args)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()

        // Si es PENDIENTE, sumar solicitudes_servicio en estado ASIGNADA
        if (estado == "PENDIENTE") {
            val sqlSol = if (tecnicoId != null) {
                "SELECT COUNT(*) FROM solicitudes_servicio WHERE estado IN ('ASIGNADA', 'ASIGNADO', 'ACEPTADA_TECNICO') AND (tecnico_id = ? OR tecnico_asignado = (SELECT usuario FROM usuarios WHERE id = ?))"
            } else {
                "SELECT COUNT(*) FROM solicitudes_servicio WHERE estado IN ('ASIGNADA', 'ASIGNADO', 'ACEPTADA_TECNICO')"
            }
            val argsSol = if (tecnicoId != null) arrayOf(tecnicoId.toString(), tecnicoId.toString()) else emptyArray()
            val cursorSol = db.rawQuery(sqlSol, argsSol)
            if (cursorSol.moveToFirst()) {
                count += cursorSol.getInt(0)
            }
            cursorSol.close()
        }

        return count
    }

    /**
     * Lista de Órdenes ya combinada con el nombre real del cliente y la
     * descripción real del equipo. Si se consultan órdenes PENDIENTES de un técnico,
     * incluye también las solicitudes en estado 'ASIGNADA' registradas en solicitudes_servicio.
     */
    fun getOrdenesByEstado(estado: String, tecnicoId: Int? = null): List<OrdenConDetalle> {
        val db = dbHelper.readableDatabase
        val sql = StringBuilder("""
            SELECT o.id, o.numero, o.fecha, o.tipo_servicio, o.estado,
                   c.nombre AS cliente_nombre,
                   (e.tipo || ' ' || e.marca) AS equipo_desc
            FROM ordenes o
            LEFT JOIN clientes c ON o.cliente_id = c.id
            LEFT JOIN equipos e ON o.equipo_id = e.id
            WHERE o.estado = ?
        """.trimIndent())
        val args = mutableListOf(estado)
        if (tecnicoId != null) {
            sql.append(" AND o.tecnico_id = ?")
            args.add(tecnicoId.toString())
        }
        sql.append(" ORDER BY o.id DESC")

        val cursor = db.rawQuery(sql.toString(), args.toTypedArray())
        val list = mutableListOf<OrdenConDetalle>()

        if (cursor.moveToFirst()) {
            do {
                list.add(OrdenConDetalle(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    numero = cursor.getString(cursor.getColumnIndexOrThrow("numero")) ?: "",
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: "",
                    clienteNombre = cursor.getString(cursor.getColumnIndexOrThrow("cliente_nombre")) ?: "Sin cliente asignado",
                    equipoDescripcion = cursor.getString(cursor.getColumnIndexOrThrow("equipo_desc")) ?: "Sin equipo asignado",
                    tipoServicio = cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")) ?: "",
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Incluir solicitudes_servicio asignadas al técnico cuando se buscan órdenes PENDIENTES
        if (estado == "PENDIENTE") {
            val sqlSol = StringBuilder("""
                SELECT s.id, s.nombre_cliente, s.marca_equipo, s.falla_reportada, s.estado, s.direccion_exacta, s.direccion
                FROM solicitudes_servicio s
                WHERE s.estado IN ('ASIGNADA', 'ASIGNADO', 'ACEPTADA_TECNICO')
            """.trimIndent())
            val argsSol = mutableListOf<String>()
            if (tecnicoId != null) {
                sqlSol.append(" AND (s.tecnico_id = ? OR s.tecnico_asignado = (SELECT usuario FROM usuarios WHERE id = ?))")
                argsSol.add(tecnicoId.toString())
                argsSol.add(tecnicoId.toString())
            }

            val cursorSol = db.rawQuery(sqlSol.toString(), if (argsSol.isNotEmpty()) argsSol.toTypedArray() else null)
            if (cursorSol.moveToFirst()) {
                do {
                    val sId = cursorSol.getInt(0)
                    val sCliente = cursorSol.getString(1) ?: "Cliente"
                    val sMarca = cursorSol.getString(2) ?: "Aire Acondicionado"
                    val sFalla = cursorSol.getString(3) ?: ""

                    if (list.none { it.id == sId }) {
                        list.add(OrdenConDetalle(
                            id = sId,
                            numero = "OT-SS%04d".format(sId),
                            fecha = "Hoy",
                            clienteNombre = sCliente,
                            equipoDescripcion = "$sMarca - $sFalla",
                            tipoServicio = "MANTENIMIENTO",
                            estado = "PENDIENTE"
                        ))
                    }
                } while (cursorSol.moveToNext())
            }
            cursorSol.close()
        }

        return list
    }

    /**
     * Guarda el formulario de mantenimiento (Módulo 4).
     *
     * OJO: si el técnico entra primero a "Ver repuestos utilizados" desde
     * MaintenanceActivity, obtenerMantenimientoIdPorOrden() YA crea una fila
     * temporal en 'mantenimientos' para poder engancharle los repuestos.
     * Antes, este método hacía un INSERT nuevo sin fijarse en eso, así que
     * terminaban existiendo DOS filas de mantenimiento para la misma orden
     * (una vacía con los repuestos, otra con los datos reales del
     * formulario) y el Historial mostraba el registro duplicado. Ahora
     * siempre reutiliza (o crea, si de verdad no existe) esa única fila y la
     * actualiza con UPDATE, así que solo hay un mantenimiento por orden.
     */
    fun guardarMantenimiento(mantenimiento: Mantenimiento, estadoEquipo: String? = null): Boolean {
        val db = dbHelper.writableDatabase
        val mantenimientoId = obtenerMantenimientoIdPorOrden(mantenimiento.orden_id)
        val values = ContentValues().apply {
            put("orden_id", mantenimiento.orden_id)
            put("fecha", mantenimiento.fecha)
            put("diagnostico", mantenimiento.diagnostico)
            put("trabajo_realizado", mantenimiento.trabajo_realizado)
            put("observaciones", mantenimiento.observaciones)
            put("recomendaciones", mantenimiento.recomendaciones)
            put("tiempo_empleado", mantenimiento.tiempoEmpleado)
            put("nombre_tecnico", mantenimiento.nombreTecnico)
        }
        val filasActualizadas = db.update("mantenimientos", values, "id = ?", arrayOf(mantenimientoId.toString()))
        val result: Long = if (filasActualizadas > 0) mantenimientoId.toLong() else -1L

        if (result != -1L) {
            val updateValues = ContentValues().apply {
                put("estado", "FINALIZADA")
            }
            db.update("ordenes", updateValues, "id = ?", arrayOf(mantenimiento.orden_id.toString()))

            // Módulo 5: el "Estado del equipo" elegido en el formulario de
            // mantenimiento debe reflejarse en la hoja de vida del equipo
            // (antes se pedía en el formulario y se descartaba).
            if (!estadoEquipo.isNullOrEmpty()) {
                val ordenCursor = db.rawQuery(
                    "SELECT equipo_id FROM ordenes WHERE id = ?",
                    arrayOf(mantenimiento.orden_id.toString())
                )
                if (ordenCursor.moveToFirst()) {
                    val equipoId = ordenCursor.getInt(0)
                    val equipoValues = ContentValues().apply { put("estado", estadoEquipo) }
                    db.update("equipos", equipoValues, "id = ?", arrayOf(equipoId.toString()))
                }
                ordenCursor.close()
            }
        }

        return result != -1L
    }

    fun insertarEvidencia(evidencia: Evidencia): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("orden_id", evidencia.orden_id)
            put("ruta_foto", evidencia.ruta_foto)
            put("fecha", evidencia.fecha)
        }
        return db.insert("evidencias", null, values)
    }

    fun obtenerEvidenciasPorOrden(ordenId: Int): List<Evidencia> {
        val list = mutableListOf<Evidencia>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM evidencias WHERE orden_id = ?", arrayOf(ordenId.toString()))

        if (cursor.moveToFirst()) {
            do {
                list.add(Evidencia(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    orden_id = cursor.getInt(cursor.getColumnIndexOrThrow("orden_id")),
                    ruta_foto = cursor.getString(cursor.getColumnIndexOrThrow("ruta_foto")) ?: "",
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun eliminarEvidencia(id: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete("evidencias", "id = ?", arrayOf(id.toString()))
    }

    fun obtenerCatalogoRepuestos(): List<com.example.mantenimiento.models.Repuesto> {
        val list = mutableListOf<com.example.mantenimiento.models.Repuesto>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM repuestos", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(com.example.mantenimiento.models.Repuesto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "",
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo")) ?: "",
                    unidad = cursor.getString(cursor.getColumnIndexOrThrow("unidad")) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun insertarDetalleRepuesto(mantenimientoId: Int, repuestoId: Int, cantidad: Int): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("mantenimiento_id", mantenimientoId)
            put("repuesto_id", repuestoId)
            put("cantidad", cantidad)
        }
        return db.insert("detalle_repuestos", null, values)
    }

    fun obtenerRepuestosPorMantenimiento(mantenimientoId: Int): List<Pair<com.example.mantenimiento.models.Repuesto, Int>> {
        val list = mutableListOf<Pair<com.example.mantenimiento.models.Repuesto, Int>>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT r.*, dr.cantidad 
            FROM repuestos r
            INNER JOIN detalle_repuestos dr ON r.id = dr.repuesto_id
            WHERE dr.mantenimiento_id = ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(mantenimientoId.toString()))
        if (cursor.moveToFirst()) {
            do {
                val repuesto = com.example.mantenimiento.models.Repuesto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "",
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo")) ?: "",
                    unidad = cursor.getString(cursor.getColumnIndexOrThrow("unidad")) ?: ""
                )
                val cantidad = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad"))
                list.add(Pair(repuesto, cantidad))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun eliminarDetalleRepuesto(mantenimientoId: Int, repuestoId: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete("detalle_repuestos", "mantenimiento_id = ? AND repuesto_id = ?",
            arrayOf(mantenimientoId.toString(), repuestoId.toString()))
    }

    fun insertarOActualizarUbicacion(ordenId: Int, lat: Double, lon: Double, fecha: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("orden_id", ordenId)
            put("latitud", lat)
            put("longitud", lon)
            put("fecha", fecha)
        }

        val cursor = db.rawQuery("SELECT id FROM ubicaciones WHERE orden_id = ?", arrayOf(ordenId.toString()))
        val exists = cursor.moveToFirst()
        cursor.close()

        return if (exists) {
            db.update("ubicaciones", values, "orden_id = ?", arrayOf(ordenId.toString())).toLong()
        } else {
            db.insert("ubicaciones", null, values)
        }
    }

    fun obtenerUbicacionPorOrden(ordenId: Int): Ubicacion? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ubicaciones WHERE orden_id = ?", arrayOf(ordenId.toString()))
        var ubicacion: Ubicacion? = null

        if (cursor.moveToFirst()) {
            ubicacion = Ubicacion(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                orden_id = cursor.getInt(cursor.getColumnIndexOrThrow("orden_id")),
                latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: ""
            )
        }
        cursor.close()
        return ubicacion
    }

    fun guardarAprobacion(ordenId: Int, nombreCliente: String, aceptado: Boolean, firmaRuta: String?): Long {
        val db = dbHelper.writableDatabase
        val fechaActual = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val values = ContentValues().apply {
            put("orden_id", ordenId)
            put("cliente", nombreCliente)
            put("aceptado", if (aceptado) 1 else 0)
            put("fecha", fechaActual)
            put("firma_ruta", firmaRuta)
        }

        val result = db.insert("aprobaciones", null, values)

        if (result != -1L) {
            val updateValues = ContentValues().apply {
                put("estado", "FINALIZADA")
            }
            db.update("ordenes", updateValues, "id = ?", arrayOf(ordenId.toString()))
        }

        return result
    }

    fun obtenerMantenimientoResumen(ordenId: Int): Mantenimiento? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM mantenimientos WHERE orden_id = ?", arrayOf(ordenId.toString()))
        var mant: Mantenimiento? = null
        if (cursor.moveToFirst()) {
            mant = Mantenimiento(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                orden_id = cursor.getInt(cursor.getColumnIndexOrThrow("orden_id")),
                fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: "",
                diagnostico = cursor.getString(cursor.getColumnIndexOrThrow("diagnostico")) ?: "",
                trabajo_realizado = cursor.getString(cursor.getColumnIndexOrThrow("trabajo_realizado")) ?: "",
                observaciones = cursor.getString(cursor.getColumnIndexOrThrow("observaciones")) ?: "",
                recomendaciones = cursor.getString(cursor.getColumnIndexOrThrow("recomendaciones")) ?: "",
                tiempoEmpleado = cursor.getString(cursor.getColumnIndexOrThrow("tiempo_empleado")) ?: "",
                nombreTecnico = cursor.getString(cursor.getColumnIndexOrThrow("nombre_tecnico")) ?: ""
            )
        }
        cursor.close()
        return mant
    }

    fun obtenerMantenimientoIdPorOrden(ordenId: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM mantenimientos WHERE orden_id = ?", arrayOf(ordenId.toString()))
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()

        if (id == -1) {
            val dbWrite = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("orden_id", ordenId)
                put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
                put("diagnostico", "")
                put("trabajo_realizado", "")
                put("observaciones", "")
                put("recomendaciones", "")
            }
            id = dbWrite.insert("mantenimientos", null, values).toInt()

            val orderValues = ContentValues().apply {
                put("estado", "EN PROCESO")
            }
            dbWrite.update("ordenes", orderValues, "id = ?", arrayOf(ordenId.toString()))
        }
        return id
    }

    fun obtenerHistorialCompleto(): List<MantenimientoCompleto> {
        val list = mutableListOf<MantenimientoCompleto>()
        val db = dbHelper.readableDatabase

        val sql = """
            SELECT 
                m.id, o.id AS orden_id, o.numero AS orden_num, m.fecha, m.trabajo_realizado, u.nombre AS tecnico,
                ubi.latitud, ubi.longitud, ev.ruta_foto, ap.firma_ruta, o.tipo_servicio
            FROM mantenimientos m
            INNER JOIN ordenes o ON m.orden_id = o.id
            LEFT JOIN usuarios u ON o.tecnico_id = u.id
            LEFT JOIN ubicaciones ubi ON o.id = ubi.orden_id
            LEFT JOIN evidencias ev ON o.id = ev.orden_id
            LEFT JOIN aprobaciones ap ON o.id = ap.orden_id
            GROUP BY m.id
            ORDER BY m.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(MantenimientoCompleto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    ordenId = cursor.getInt(cursor.getColumnIndexOrThrow("orden_id")),
                    ordenNumero = cursor.getString(cursor.getColumnIndexOrThrow("orden_num")) ?: "",
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: "",
                    trabajoRealizado = cursor.getString(cursor.getColumnIndexOrThrow("trabajo_realizado")) ?: "",
                    tecnicoNombre = cursor.getString(cursor.getColumnIndexOrThrow("tecnico")) ?: "N/A",
                    latitud = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitud"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitud"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    fotoRuta = cursor.getString(cursor.getColumnIndexOrThrow("ruta_foto")),
                    firmaRuta = cursor.getString(cursor.getColumnIndexOrThrow("firma_ruta")),
                    tipoServicio = cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")) ?: "SERVICIO"
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    /**
     * Obtiene el historial filtrado por rol y ID de usuario.
     * Para el Cliente, hace un cruce seguro para encontrar sus órdenes.
     */
    fun obtenerHistorialSeguro(rol: String, userId: Int): List<MantenimientoCompleto> {
        val list = mutableListOf<MantenimientoCompleto>()
        val db = dbHelper.readableDatabase

        // Lógica de filtrado dinámico
        val whereClause = when (rol) {
            "Administrador" -> ""
            "Técnico" -> "WHERE o.tecnico_id = $userId"
            // clientes.usuario_id vincula la ficha de negocio con la cuenta de
            // login del Cliente (ver ServiceRequestRepository.confirmarTecnico).
            // Antes esto se intentaba adivinar comparando nombre/email a mano,
            // y hasta se comparaban ids de 'ordenes' contra ids de
            // 'solicitudes_servicio' (dos secuencias de id distintas, nunca
            // debían compararse entre sí).
            "Cliente" -> "WHERE o.cliente_id IN (SELECT id FROM clientes WHERE usuario_id = $userId)"
            else -> "WHERE 1=0"
        }

        val sql = """
            SELECT 
                m.id, o.id AS orden_id, o.numero AS orden_num, m.fecha, m.trabajo_realizado, u.nombre AS tecnico,
                ubi.latitud, ubi.longitud, ev.ruta_foto, ap.firma_ruta, o.tipo_servicio
            FROM mantenimientos m
            INNER JOIN ordenes o ON m.orden_id = o.id
            LEFT JOIN usuarios u ON o.tecnico_id = u.id
            LEFT JOIN ubicaciones ubi ON o.id = ubi.orden_id
            LEFT JOIN evidencias ev ON o.id = ev.orden_id
            LEFT JOIN aprobaciones ap ON o.id = ap.orden_id
            $whereClause
            GROUP BY m.id
            ORDER BY m.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(MantenimientoCompleto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    ordenId = cursor.getInt(cursor.getColumnIndexOrThrow("orden_id")),
                    ordenNumero = cursor.getString(cursor.getColumnIndexOrThrow("orden_num")) ?: "",
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: "",
                    trabajoRealizado = cursor.getString(cursor.getColumnIndexOrThrow("trabajo_realizado")) ?: "",
                    tecnicoNombre = cursor.getString(cursor.getColumnIndexOrThrow("tecnico")) ?: "N/A",
                    latitud = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitud"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitud")),
                    longitud = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitud"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitud")),
                    fotoRuta = cursor.getString(cursor.getColumnIndexOrThrow("ruta_foto")),
                    firmaRuta = cursor.getString(cursor.getColumnIndexOrThrow("firma_ruta")),
                    tipoServicio = cursor.getString(cursor.getColumnIndexOrThrow("tipo_servicio")) ?: "SERVICIO"
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun actualizarEstadoOrden(ordenId: Int, nuevoEstado: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("estado", nuevoEstado)
        }
        return db.update("ordenes", values, "id = ?", arrayOf(ordenId.toString()))
    }

    fun obtenerDetalleOrdenConCliente(ordenId: Int): Map<String, String> {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<String, String>()
        val sql = """
            SELECT o.numero, c.nombre, c.telefono, c.direccion, e.tipo || ' ' || e.capacidad AS equipo_desc, o.tipo_servicio
            FROM ordenes o
            LEFT JOIN clientes c ON o.cliente_id = c.id
            LEFT JOIN equipos e ON o.equipo_id = e.id
            WHERE o.id = ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(ordenId.toString()))
        if (cursor.moveToFirst()) {
            map["numero"] = cursor.getString(0) ?: ""
            map["cliente_nombre"] = cursor.getString(1) ?: "Sin cliente asignado"
            map["cliente_telefono"] = cursor.getString(2) ?: ""
            map["cliente_direccion"] = cursor.getString(3) ?: ""
            map["equipo_info"] = cursor.getString(4) ?: "Sin equipo asignado"
            map["tipo_servicio"] = cursor.getString(5) ?: ""
        }
        cursor.close()
        return map
    }
}