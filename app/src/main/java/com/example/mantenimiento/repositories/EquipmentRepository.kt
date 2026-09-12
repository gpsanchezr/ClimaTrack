package com.example.mantenimiento.repositories

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import com.example.mantenimiento.database.DatabaseHelper
import com.example.mantenimiento.models.Equipo

class EquipmentRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun getAllEquipment(): List<Equipo> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM equipos", null)
        return cursorToList(cursor)
    }

    fun buscarEquipos(criterio: String): List<Equipo> {
        val db = dbHelper.readableDatabase
        val sql = "SELECT * FROM equipos WHERE codigo LIKE ? OR marca LIKE ? OR modelo LIKE ?"
        val pattern = "%$criterio%"
        val cursor = db.rawQuery(sql, arrayOf(pattern, pattern, pattern))
        return cursorToList(cursor)
    }

    fun insertarEquipo(e: Equipo): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("codigo", e.codigo)
            put("tipo", e.tipo)
            put("marca", e.marca)
            put("modelo", e.modelo)
            put("serial", e.serial)
            put("capacidad", e.capacidad)
            put("ubicacion", e.ubicacion)
            put("cliente_id", e.cliente_id)
            put("estado", e.estado)
        }
        return db.insert("equipos", null, values)
    }

    fun actualizarEquipo(e: Equipo): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("codigo", e.codigo)
            put("tipo", e.tipo)
            put("marca", e.marca)
            put("modelo", e.modelo)
            put("serial", e.serial)
            put("capacidad", e.capacidad)
            put("ubicacion", e.ubicacion)
            put("cliente_id", e.cliente_id)
            put("estado", e.estado)
        }
        return db.update("equipos", values, "id = ?", arrayOf(e.id.toString()))
    }

    fun eliminarEquipo(id: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete("equipos", "id = ?", arrayOf(id.toString()))
    }

    fun obtenerClientes(): Map<Int, String> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, nombre FROM clientes", null)
        val clientes = mutableMapOf<Int, String>()
        if (cursor.moveToFirst()) {
            do {
                clientes[cursor.getInt(0)] = cursor.getString(1) ?: ""
            } while (cursor.moveToNext())
        }
        cursor.close()
        return clientes
    }

    fun obtenerOCrearCliente(nombreCliente: String, direccion: String = ""): Int {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery("SELECT id FROM clientes WHERE nombre = ?", arrayOf(nombreCliente))
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()

        if (id == -1) {
            val values = ContentValues().apply {
                put("nombre", nombreCliente)
                put("telefono", "")
                put("direccion", direccion)
                put("email", "")
            }
            id = db.insert("clientes", null, values).toInt()
        }
        return if (id != -1) id else 1
    }

    private fun cursorToList(cursor: Cursor): List<Equipo> {
        val list = mutableListOf<Equipo>()
        if (cursor.moveToFirst()) {
            do {
                list.add(Equipo(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo")) ?: "",
                    tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")) ?: "",
                    marca = cursor.getString(cursor.getColumnIndexOrThrow("marca")) ?: "",
                    modelo = cursor.getString(cursor.getColumnIndexOrThrow("modelo")) ?: "",
                    serial = cursor.getString(cursor.getColumnIndexOrThrow("serial")) ?: "",
                    capacidad = cursor.getString(cursor.getColumnIndexOrThrow("capacidad")) ?: "",
                    ubicacion = cursor.getString(cursor.getColumnIndexOrThrow("ubicacion")) ?: "",
                    cliente_id = cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}