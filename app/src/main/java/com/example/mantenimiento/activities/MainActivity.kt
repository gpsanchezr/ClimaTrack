package com.example.mantenimiento.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.UsuarioRepository

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usuarioRepository = UsuarioRepository(this)
        usuarioRepository.insertarUsuarioPrueba()

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegister = findViewById<Button>(R.id.btnRegister) // Asumiendo que el ID es btnRegister
        val cbRemember = findViewById<CheckBox>(R.id.cbRemember)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // "Recordar usuario": el checkbox existía en el layout pero no hacía
        // nada. Si la última sesión lo dejó marcado, precargamos el usuario.
        val loginPrefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
        if (loginPrefs.getBoolean("RECORDAR_USUARIO", false)) {
            etUsuario.setText(loginPrefs.getString("USUARIO_RECORDADO", ""))
            cbRemember.isChecked = true
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Contacta a tu administrador para restablecer tu contraseña.", Toast.LENGTH_LONG).show()
        }

        btnIngresar.setOnClickListener {
            val user = etUsuario.text.toString()
            val pass = etPassword.text.toString()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val usuarioValidado = usuarioRepository.validarLogin(user, pass)

                if (usuarioValidado != null) {
                    loginPrefs.edit().apply {
                        putBoolean("RECORDAR_USUARIO", cbRemember.isChecked)
                        putString("USUARIO_RECORDADO", if (cbRemember.isChecked) user else "")
                        apply()
                    }

                    // --- PERSISTENCIA DE SESIÓN ---
                    // ID_USUARIO y USUARIO_LOGIN faltaban aquí: HistoryActivity y
                    // ClientConfirmationActivity ya los leen de estos mismos prefs
                    // (con -1 / 0 de respaldo), pero nunca se guardaban al iniciar
                    // sesión. Por eso el Historial quedaba vacío (o crasheaba) para
                    // Técnico y Cliente, y las solicitudes del cliente se guardaban
                    // con cliente_id = 0 en vez del id real.
                    val prefs = getSharedPreferences("ClimaTrackPrefs", MODE_PRIVATE)
                    prefs.edit().apply {
                        putInt("ID_USUARIO", usuarioValidado.id)
                        putString("USUARIO_LOGIN", usuarioValidado.usuario)
                        putString("NOMBRE_USUARIO", usuarioValidado.nombre)
                        putString("GENERO_USUARIO", usuarioValidado.genero)
                        putString("ROL_USUARIO", usuarioValidado.rol)
                        apply()
                    }

                    Toast.makeText(this, "Bienvenido ${usuarioValidado.nombre}", Toast.LENGTH_SHORT).show()
                    
                    // Siempre enviamos al DashboardActivity para centralizar la navegación y el logout
                    val intent = Intent(this, DashboardActivity::class.java)
                    
                    // Pasamos datos por Intent para el primer ingreso
                    intent.putExtra("ID_USUARIO", usuarioValidado.id)
                    intent.putExtra("USUARIO_LOGIN", usuarioValidado.usuario)
                    intent.putExtra("NOMBRE_USUARIO", usuarioValidado.nombre)
                    intent.putExtra("GENERO_USUARIO", usuarioValidado.genero)
                    intent.putExtra("ROL_USUARIO", usuarioValidado.rol)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Navegar a la pantalla de registro
        btnRegister?.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
