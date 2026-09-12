package com.example.mantenimiento.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mantenimiento.R
import com.example.mantenimiento.repositories.ServiceRequestRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class WaitingActivity : AppCompatActivity() {

    private lateinit var serviceRepo: ServiceRequestRepository
    private var solicitudId: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusRunnable: Runnable

    private lateinit var pbSearching: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvTechName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        serviceRepo = ServiceRequestRepository(this)
        solicitudId = intent.getIntExtra("SOLICITUD_ID", -1)

        initViews()
        startPolling()
    }

    private fun initViews() {
        pbSearching = findViewById(R.id.pbSearching)
        tvTitle = findViewById(R.id.tvStatusTitle)
        tvSubtitle = findViewById(R.id.tvStatusSubtitle)
        cardResult = findViewById(R.id.cardResult)
        tvTechName = findViewById(R.id.tvTechnicianName)

        findViewById<MaterialButton>(R.id.btnConfirmService).setOnClickListener {
            val res = serviceRepo.confirmarTecnico(solicitudId)
            if (res > 0) {
                Toast.makeText(this, "Servicio iniciado con éxito", Toast.LENGTH_SHORT).show()
                detenerPolling() // Detenemos el bucle de inmediato
                finish()
            }
        }

        findViewById<MaterialButton>(R.id.btnRefreshStatus).setOnClickListener {
            verificarEstado(manual = true)
        }
    }

    private fun startPolling() {
        statusRunnable = object : Runnable {
            override fun run() {
                verificarEstado(manual = false)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(statusRunnable)
    }

    private fun verificarEstado(manual: Boolean = false) {
        if (solicitudId == -1) return

        val data = serviceRepo.obtenerEstadoSolicitud(solicitudId)
        if (data != null) {
            val estadoActual = data["estado"] ?: ""
            if (estadoActual == "ASIGNADA" || estadoActual == "ASIGNADO" || estadoActual == "ACEPTADA_TECNICO") {
                detenerPolling()
                pbSearching.visibility = View.GONE
                tvTitle.text = "¡Técnico Asignado!"
                tvSubtitle.text = "Un administrador ha asignado tu servicio. El técnico se encuentra listo para atenderte."
                cardResult.visibility = View.VISIBLE
                tvTechName.text = "Técnico: ${data["tecnico"]}"
                if (manual) {
                    Toast.makeText(this, "Estado actualizado: ¡Técnico asignado!", Toast.LENGTH_SHORT).show()
                }
            } else if (manual) {
                Toast.makeText(this, "Estado actual: Tu solicitud sigue en revisión por la administración.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun detenerPolling() {
        if (::statusRunnable.isInitialized) {
            handler.removeCallbacks(statusRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerPolling()
    }
}