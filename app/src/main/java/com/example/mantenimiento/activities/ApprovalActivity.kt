package com.example.mantenimiento.activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.OrdenRepository
import com.example.mantenimiento.utils.SignatureView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * ApprovalActivity - Módulo 8: Aprobación del cliente (Refactorizado Senior)
 */
class ApprovalActivity : AppCompatActivity() {

    private lateinit var repository: OrdenRepository
    private var ordenId: Int = -1

    private lateinit var signatureView: SignatureView
    private lateinit var etClientName: TextInputEditText
    private lateinit var tilClientName: TextInputLayout
    private lateinit var cbAcceptance: MaterialCheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approval)

        repository = OrdenRepository(this)
        ordenId = intent.getIntExtra("ORDEN_ID", -1)
        if (ordenId == -1) {
            Toast.makeText(this, "No se pudo identificar la orden.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        cargarResumen()

        findViewById<MaterialButton>(R.id.btnClearSignature).setOnClickListener {
            signatureView.clear()
        }

        findViewById<MaterialButton>(R.id.btnSaveApproval).setOnClickListener {
            if (validarAprobacion()) {
                ejecutarGuardado()
            }
        }
    }

    private fun initViews() {
        signatureView = findViewById(R.id.signatureView)
        etClientName = findViewById(R.id.etClientName)
        tilClientName = findViewById(R.id.tilClientName)
        cbAcceptance = findViewById(R.id.cbAcceptance)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarApproval)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun cargarResumen() {
        repository.obtenerMantenimientoResumen(ordenId)?.let { mant ->
            findViewById<TextView>(R.id.tvSummaryDate).text = mant.fecha
            findViewById<TextView>(R.id.tvSummaryWork).text = mant.trabajo_realizado
            findViewById<TextView>(R.id.tvSummaryObservations).text = 
                if (mant.observaciones.isEmpty()) "Sin observaciones" else mant.observaciones
            findViewById<TextView>(R.id.tvSummaryRecommendations).text =
                if (mant.recomendaciones.isEmpty()) "--" else mant.recomendaciones
            findViewById<TextView>(R.id.tvSummaryTech).text =
                mant.nombreTecnico.ifEmpty { "--" }

            // El tipo de servicio real vive en 'ordenes', no en 'mantenimientos'
            val datosOrden = repository.obtenerDetalleOrdenConCliente(ordenId)
            findViewById<TextView>(R.id.tvSummaryType).text = datosOrden["tipo_servicio"]?.ifEmpty { "--" } ?: "--"
        }
    }

    private fun validarAprobacion(): Boolean {
        var esValido = true
        if (etClientName.text.isNullOrEmpty()) {
            tilClientName.error = "Ingrese el nombre del cliente"
            esValido = false
        } else tilClientName.error = null

        if (!cbAcceptance.isChecked) {
            Toast.makeText(this, "El cliente debe aceptar el servicio", Toast.LENGTH_SHORT).show()
            esValido = false
        }

        if (signatureView.isEmpty()) {
            Toast.makeText(this, "La firma es obligatoria", Toast.LENGTH_SHORT).show()
            esValido = false
        }
        return esValido
    }

    private fun ejecutarGuardado() {
        val signaturePath = guardarFirmaLocal(signatureView.getSignatureBitmap())
        val exito = repository.guardarAprobacion(
            ordenId,
            etClientName.text.toString(),
            cbAcceptance.isChecked,
            signaturePath
        )

        if (exito != -1L) {
            Toast.makeText(this, "Servicio finalizado con éxito", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }
    }

    private fun guardarFirmaLocal(bitmap: Bitmap): String? {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, "SIGN_${ordenId}_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
