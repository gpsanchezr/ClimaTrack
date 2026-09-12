package com.example.mantenimiento.repositories

import android.content.ContentValues
import android.content.Context
import com.example.mantenimiento.database.DatabaseHelper
import com.example.mantenimiento.models.Usuario
import com.example.mantenimiento.utils.SecurityUtils

class UsuarioRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insertarUsuarioPrueba() {
        val db = dbHelper.writableDatabase

        val usuariosDefault = listOf(
            Triple("giseella01", "Giseella Patricia Sanchez Rico", "Administrador"),
            Triple("alex01", "Alex", "Técnico"),
            Triple("cliente01", "María", "Cliente")
        )

        for (u in usuariosDefault) {
            val hashedPass = SecurityUtils.hashPassword("123456")
            val values = ContentValues().apply {
                put("usuario", u.first)
                put("password", hashedPass)
                put("nombre", u.second)
                put("rol", u.third)
                put("genero", if (u.first == "giseella01") "F" else "M")
            }
            
            val exists = db.rawQuery("SELECT id FROM usuarios WHERE usuario = ?", arrayOf(u.first))
            if (exists.moveToFirst()) {
                db.update("usuarios", values, "usuario = ?", arrayOf(u.first))
            } else {
                db.insert("usuarios", null, values)
            }
            exists.close()
        }

        val cursor = db.rawQuery("SELECT COUNT(*) FROM clientes", null)
        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
            insertarDatosPruebaCompleto(db)
        }
        cursor.close()
    }

    private fun insertarDatosPruebaCompleto(db: android.database.sqlite.SQLiteDatabase) {
        val clienteValues = ContentValues().apply {
            put("nombre", "ACME S.A.S.")
            put("telefono", "3001234567")
            put("direccion", "Calle 100 #15-30, Bogotá")
            put("email", "contacto@acme.com")
        }
        val clienteId = db.insert("clientes", null, clienteValues)

        val equipoValues = ContentValues().apply {
            put("codigo", "EQ-00015")
            put("tipo", "Split Inverter 24K")
            put("marca", "LG")
            put("cliente_id", clienteId)
            put("estado", "OPERATIVO")
        }
        val equipoId = db.insert("equipos", null, equipoValues)

        val ordenes = listOf(
            arrayOf("OT-00025", "25/08/2026", "PENDIENTE", "Preventivo"),
            arrayOf("OT-00026", "25/08/2026", "EN PROCESO", "Correctivo"),
            arrayOf("OT-00027", "26/08/2026", "FINALIZADA", "Inspección")
        )

        for (o in ordenes) {
            val v = ContentValues().apply {
                put("numero", o[0])
                put("fecha", o[1])
                put("estado", o[2])
                put("tipo_servicio", o[3])
                put("cliente_id", clienteId)
                put("equipo_id", equipoId)
                put("tecnico_id", 2) 
            }
            db.insert("ordenes", null, v)
        }

        val repuestos = listOf(
            arrayOf("Filtro de aire lavable", "RPT-0007", "Unidad"),
            arrayOf("Capacitor 35 + 5 uF", "RPT-0012", "Unidad"),
            arrayOf("Contactor 24V 40A", "RPT-0021", "Unidad"),
            arrayOf("Gas Refrigerante R410A", "RPT-0030", "Gramos")
        )

        for (r in repuestos) {
            val v = ContentValues().apply {
                put("nombre", r[0])
                put("codigo", r[1])
                put("unidad", r[2])
            }
            db.insert("repuestos", null, v)
        }
    }

    fun validarLogin(usuario: String, pass: String): Usuario? {
        val db = dbHelper.readableDatabase
        val hashedPass = SecurityUtils.hashPassword(pass)
        val cursor = db.rawQuery(
            "SELECT * FROM usuarios WHERE usuario = ? AND password = ?",
            arrayOf(usuario.trim().lowercase(), hashedPass)
        )

        var user: Usuario? = null
        if (cursor.moveToFirst()) {
            user = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")) ?: "",
                password = cursor.getString(cursor.getColumnIndexOrThrow("password")) ?: "",
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "",
                rol = cursor.getString(cursor.getColumnIndexOrThrow("rol")) ?: "",
                genero = cursor.getString(cursor.getColumnIndexOrThrow("genero")) ?: "M"
            )
        }
        cursor.close()
        return user
    }

    /**
     * Registra un nuevo Cliente real en SQLite.
     * Inserta en 'usuarios' con rol = 'Cliente' y crea la ficha en 'clientes' vinculada.
     */
    fun registrarCliente(nombre: String, telefono: String, email: String, pass: String): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val userEmail = email.trim().lowercase()
            val hashedPass = SecurityUtils.hashPassword(pass)

            val userValues = ContentValues().apply {
                put("usuario", userEmail)
                put("password", hashedPass)
                put("nombre", nombre.trim())
                put("rol", "Cliente")
                put("genero", "M")
            }

            val userId = db.insertWithOnConflict("usuarios", null, userValues, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
            if (userId != -1L) {
                val clientValues = ContentValues().apply {
                    put("nombre", nombre.trim())
                    put("telefono", telefono.trim())
                    put("direccion", "Sin registrar")
                    put("email", userEmail)
                    put("usuario_id", userId.toInt())
                }
                db.insert("clientes", null, clientValues)
                db.setTransactionSuccessful()
            }
            return userId
        } catch (e: Exception) {
            e.printStackTrace()
            return -1L
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Actualiza la URI de la foto de perfil de un usuario en SQLite.
     */
    fun actualizarFotoPerfil(usuarioId: Int, uriFoto: String): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("foto_perfil", uriFoto)
        }
        return db.update("usuarios", values, "id = ?", arrayOf(usuarioId.toString()))
    }

    /**
     * Obtiene la URI de la foto de perfil almacenada para un usuario.
     */
    fun obtenerFotoPerfil(usuarioId: Int): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT foto_perfil FROM usuarios WHERE id = ?", arrayOf(usuarioId.toString()))
        var foto: String? = null
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex("foto_perfil")
            if (idx != -1 && !cursor.isNull(idx)) {
                foto = cursor.getString(idx)
            }
        }
        cursor.close()
        return foto
    }

    /**
     * Permite al Administrador registrar nuevos Técnicos en SQLite.
     * Encripta la contraseña usando SHA-256 y fuerza el rol a 'Técnico'.
     */
    fun registrarTecnico(usuario: String, pass: String, nombre: String, genero: String = "M"): Long {
        val db = dbHelper.writableDatabase
        val hashedPass = SecurityUtils.hashPassword(pass)
        val values = ContentValues().apply {
            put("usuario", usuario.trim().lowercase())
            put("password", hashedPass)
            put("nombre", nombre.trim())
            put("rol", "Técnico")
            put("genero", genero)
        }
        return db.insertWithOnConflict("usuarios", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * Obtiene la lista de técnicos disponibles para asignar.
     */
    fun obtenerTecnicos(): List<Usuario> {
        val list = mutableListOf<Usuario>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE rol = 'Técnico'", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Usuario(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")) ?: "",
                    password = "",
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")) ?: "",
                    rol = "Técnico",
                    genero = cursor.getString(cursor.getColumnIndexOrThrow("genero")) ?: "M"
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}