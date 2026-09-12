package com.example.mantenimiento.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mantenimiento.utils.SecurityUtils

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ClimaTrack.db"
        // v8: Se agrega columna 'foto_perfil' (TEXT) en la tabla 'usuarios'
        // para guardar la URI de la foto de perfil seleccionada por el usuario.
        private const val DATABASE_VERSION = 8
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabla Usuarios
        db.execSQL("""
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT UNIQUE, 
                password TEXT, 
                nombre TEXT, 
                rol TEXT, 
                genero TEXT,
                foto_perfil TEXT
            )
        """.trimIndent())

        // 2. Tabla Clientes
        db.execSQL("""
            CREATE TABLE clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT, telefono TEXT, direccion TEXT, email TEXT,
                usuario_id INTEGER,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
            )
        """.trimIndent())

        // 3. Tabla Equipos
        db.execSQL("""
            CREATE TABLE equipos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT, tipo TEXT, marca TEXT, modelo TEXT,
                serial TEXT, capacidad TEXT, ubicacion TEXT,
                cliente_id INTEGER, estado TEXT,
                FOREIGN KEY(cliente_id) REFERENCES clientes(id)
            )
        """.trimIndent())

        // 4. Tabla Ordenes
        db.execSQL("""
            CREATE TABLE ordenes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero TEXT, fecha TEXT, cliente_id INTEGER,
                equipo_id INTEGER, tecnico_id INTEGER,
                tipo_servicio TEXT, descripcion TEXT, estado TEXT,
                FOREIGN KEY(cliente_id) REFERENCES clientes(id),
                FOREIGN KEY(equipo_id) REFERENCES equipos(id),
                FOREIGN KEY(tecnico_id) REFERENCES usuarios(id)
            )
        """.trimIndent())

        // 5. Tabla Mantenimientos
        db.execSQL("""
            CREATE TABLE mantenimientos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orden_id INTEGER, fecha TEXT, diagnostico TEXT,
                trabajo_realizado TEXT, observaciones TEXT, recomendaciones TEXT,
                tiempo_empleado TEXT, nombre_tecnico TEXT,
                FOREIGN KEY(orden_id) REFERENCES ordenes(id)
            )
        """.trimIndent())

        // 6. Tabla Repuestos
        db.execSQL("""
            CREATE TABLE repuestos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT, codigo TEXT, unidad TEXT
            )
        """.trimIndent())

        // 7. Tabla Detalle Repuestos
        db.execSQL("""
            CREATE TABLE detalle_repuestos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mantenimiento_id INTEGER, repuesto_id INTEGER, cantidad INTEGER,
                FOREIGN KEY(mantenimiento_id) REFERENCES mantenimientos(id),
                FOREIGN KEY(repuesto_id) REFERENCES repuestos(id)
            )
        """.trimIndent())

        // 8. Tabla Evidencias
        db.execSQL("""
            CREATE TABLE evidencias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orden_id INTEGER, ruta_foto TEXT, fecha TEXT,
                FOREIGN KEY(orden_id) REFERENCES ordenes(id)
            )
        """.trimIndent())

        // 9. Tabla Aprobaciones
        db.execSQL("""
            CREATE TABLE aprobaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orden_id INTEGER, cliente TEXT, aceptado INTEGER, fecha TEXT, firma_ruta TEXT,
                FOREIGN KEY(orden_id) REFERENCES ordenes(id)
            )
        """.trimIndent())

        // 10. Tabla Ubicaciones
        db.execSQL("""
            CREATE TABLE ubicaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orden_id INTEGER, latitud REAL, longitud REAL, fecha TEXT,
                FOREIGN KEY(orden_id) REFERENCES ordenes(id)
            )
        """.trimIndent())

        // 11. Tabla solicitudes_servicio para el flujo de oferta pública controlada
        db.execSQL("""
            CREATE TABLE solicitudes_servicio (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_id INTEGER,
                nombre_cliente TEXT,
                direccion TEXT,
                direccion_exacta TEXT,
                latitud REAL,
                longitud REAL,
                marca_equipo TEXT,
                falla_reportada TEXT,
                tecnico_id INTEGER,
                tecnico_asignado TEXT,
                fecha_programada TEXT,
                hora_programada TEXT,
                estado_evento TEXT,
                estado TEXT DEFAULT 'PENDIENTE_ADMIN',
                FOREIGN KEY(cliente_id) REFERENCES usuarios(id),
                FOREIGN KEY(tecnico_id) REFERENCES usuarios(id)
            )
        """.trimIndent())

        // Inyección de Usuarios Base (Admin, Técnicos y Cliente Semilla)
        insertarUsuariosIniciales(db)
    }

    private fun insertarUsuariosIniciales(db: SQLiteDatabase) {
        val usuarios = listOf(
            arrayOf("Giseella", "giseella01", "123456", "Administrador", "F"),
            arrayOf("Alex", "alex01", "123456", "Técnico", "M"),
            arrayOf("Luis", "luis01", "123456", "Técnico", "M"),
            arrayOf("Daniela", "daniela01", "123456", "Técnico", "F"),
            arrayOf("Carlos", "carlos01", "123456", "Técnico", "M")
        )

        for (u in usuarios) {
            val v = ContentValues().apply {
                put("nombre", u[0])
                put("usuario", u[1])
                put("password", SecurityUtils.hashPassword(u[2]))
                put("rol", u[3])
                put("genero", u[4])
            }
            db.insertWithOnConflict("usuarios", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        }
        
        // Usuario Cliente Semilla
        val cliente = ContentValues().apply {
            put("nombre", "María")
            put("usuario", "cliente01")
            put("password", SecurityUtils.hashPassword("123456"))
            put("rol", "Cliente")
            put("genero", "F")
        }
        db.insertWithOnConflict("usuarios", null, cliente, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * Migración incremental y NO destructiva de versiones de base de datos.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE clientes ADD COLUMN usuario_id INTEGER")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE solicitudes_servicio ADD COLUMN direccion_exacta TEXT")
                db.execSQL("ALTER TABLE solicitudes_servicio ADD COLUMN tecnico_id INTEGER")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN foto_perfil TEXT")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}