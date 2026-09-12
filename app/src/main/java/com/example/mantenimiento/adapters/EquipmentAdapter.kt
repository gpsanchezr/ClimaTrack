package com.example.mantenimiento.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.models.Equipo

class EquipmentAdapter(
    private var list: List<Equipo>,
    private val onEditClick: (Equipo) -> Unit
) : RecyclerView.Adapter<EquipmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tvEquipmentCode)
        val tvType: TextView = view.findViewById(R.id.tvEquipmentType)
        val tvBrandModel: TextView = view.findViewById(R.id.tvEquipmentBrandModel)
        val tvSerial: TextView = view.findViewById(R.id.tvEquipmentSerial)
        val tvClient: TextView = view.findViewById(R.id.tvEquipmentClient)
        val tvStatus: TextView = view.findViewById(R.id.tvEquipmentStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_equipment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvCode.text = item.codigo
        holder.tvType.text = "Tipo: ${item.tipo}"
        holder.tvBrandModel.text = "Marca/Mod: ${item.marca} - ${item.modelo}"
        holder.tvSerial.text = "Serie: ${item.serial}"
        holder.tvClient.text = "Cliente ID: ${item.cliente_id}" // Mejorar con Join si es necesario
        holder.tvStatus.text = item.estado

        val context = holder.itemView.context
        when(item.estado) {
            "OPERATIVO" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_finished_bg)
                holder.tvStatus.setTextColor(context.getColor(R.color.status_finished))
            }
            "EN MANTENIMIENTO" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_in_progress_bg)
                holder.tvStatus.setTextColor(context.getColor(R.color.status_in_progress))
            }
            "FUERA DE SERVICIO" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.status_error_bg)
                holder.tvStatus.setTextColor(context.getColor(R.color.status_error))
            }
        }

        holder.itemView.setOnClickListener { onEditClick(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Equipo>) {
        list = newList
        notifyDataSetChanged()
    }
}