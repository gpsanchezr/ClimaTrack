package com.example.mantenimiento.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.models.Evidencia
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

/**
 * Adaptador para mostrar la grilla de evidencias fotográficas.
 */
class EvidenceAdapter(
    private var list: List<Evidencia>,
    private val onDeleteClick: (Evidencia) -> Unit
) : RecyclerView.Adapter<EvidenceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivEvidencePhoto)
        val tvDate: TextView = view.findViewById(R.id.tvEvidenceDate)
        val btnDelete: FloatingActionButton = view.findViewById(R.id.btnDeleteEvidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evidence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvDate.text = item.fecha

        // Cargar imagen local de forma eficiente
        val imgFile = File(item.ruta_foto)
        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            holder.ivPhoto.setImageBitmap(myBitmap)
        } else {
            holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = list.size

    /**
     * Actualiza el listado de evidencias en el RecyclerView.
     */
    fun updateData(newList: List<Evidencia>) {
        list = newList
        notifyDataSetChanged()
    }
}