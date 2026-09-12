package com.example.mantenimiento.activities

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.adapters.EvidenceAdapter
import com.example.mantenimiento.models.Evidencia
import com.example.mantenimiento.repositories.OrdenRepository
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EvidencesActivity : AppCompatActivity() {

    private lateinit var adapter: EvidenceAdapter
    private lateinit var repository: OrdenRepository
    private var ordenId: Int = -1
    private var currentPhotoPath: String? = null

    // Solicitud de permiso de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            lanzarCamara()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                guardarEvidenciaEnBD(path)
            }
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evidences)

        repository = OrdenRepository(this)
        ordenId = intent.getIntExtra("ORDEN_ID", -1)
        if (ordenId == -1) {
            Toast.makeText(this, "No se pudo identificar la orden.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()
        setupToolbar()

        findViewById<MaterialButton>(R.id.btnTakePhoto).setOnClickListener {
            verificarPermisoYTomarFoto()
        }

        cargarEvidencias()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarEvidences)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvEvidences)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = EvidenceAdapter(emptyList()) { evidencia ->
            eliminarEvidencia(evidencia)
        }
        rv.adapter = adapter
    }

    private fun verificarPermisoYTomarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lanzarCamara() {
        try {
            val photoFile = crearArchivoImagen()
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.example.mantenimiento.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (ex: Exception) {
            Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("CLIMA_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun guardarEvidenciaEnBD(path: String) {
        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val evidencia = Evidencia(
            orden_id = ordenId,
            ruta_foto = path,
            fecha = fechaActual
        )

        if (repository.insertarEvidencia(evidencia) != -1L) {
            cargarEvidencias()
            Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarEvidencias() {
        adapter.updateData(repository.obtenerEvidenciasPorOrden(ordenId))
    }

    private fun eliminarEvidencia(evidencia: Evidencia) {
        repository.eliminarEvidencia(evidencia.id)
        File(evidencia.ruta_foto).let { if (it.exists()) it.delete() }
        cargarEvidencias()
        Toast.makeText(this, "Evidencia eliminada", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
