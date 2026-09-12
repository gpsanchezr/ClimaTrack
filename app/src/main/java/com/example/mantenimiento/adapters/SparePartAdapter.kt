package com.example.mantenimiento.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.models.Repuesto

class SparePartAdapter(
    private var list: List<Pair<Repuesto, Int>>,
    private val onDeleteClick: (Repuesto) -> Unit
) : RecyclerView.Adapter<SparePartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSparePartName)
        val tvCode: TextView = view.findViewById(R.id.tvSparePartCode)
        val tvQtyUnit: TextView = view.findViewById(R.id.tvSparePartQtyUnit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteSparePart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spare_part, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (repuesto, cantidad) = list[position]
        holder.tvName.text = repuesto.nombre
        holder.tvCode.text = "Cód: ${repuesto.codigo}"
        holder.tvQtyUnit.text = "Cantidad: $cantidad (${repuesto.unidad})"
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(repuesto)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Pair<Repuesto, Int>>) {
        list = newList
        notifyDataSetChanged()
    }
}