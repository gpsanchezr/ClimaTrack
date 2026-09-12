package com.example.mantenimiento.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.UsuarioRepository
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var usuarioRepo: UsuarioRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usuarioRepo = UsuarioRepository(this)

        val btnRegister = findViewById<Button>(R.id.btnDoRegister)

        btnRegister.setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.etRegName)?.text.toString().trim()
            val phone = findViewById<TextInputEditText>(R.id.etRegPhone)?.text.toString().trim()
            val email = findViewById<TextInputEditText>(R.id.etRegEmail)?.text.toString().trim()
            val pass = findViewById<TextInputEditText>(R.id.etRegPass)?.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                registrarUsuario(name, phone, email, pass)
            } else {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registrarUsuario(name: String, phone: String, email: String, pass: String) {
        val newRowId = usuarioRepo.registrarCliente(
            nombre = name,
            telefono = phone,
            email = email,
            pass = pass
        )

        if (newRowId != -1L) {
            Toast.makeText(this, "¡Registro exitoso! Ya puedes iniciar sesión", Toast.LENGTH_LONG).show()
            finish() // Regresar al Login
        } else {
            Toast.makeText(this, "Error: El correo ya existe o hubo un fallo en BD", Toast.LENGTH_SHORT).show()
        }
    }
}