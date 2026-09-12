package com.example.mantenimiento.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.google.android.material.button.MaterialButton

class TechAgendaAdapter(
    private var list: List<Map<String, String>>,
    private val onStartClick: (Int) -> Unit
) : RecyclerView.Adapter<TechAgendaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvAgendaDate)
        val tvTime: TextView = view.findViewById(R.id.tvAgendaTime)
        val tvClient: TextView = view.findViewById(R.id.tvAgendaClient)
        val tvEquipment: TextView = view.findViewById(R.id.tvAgendaEquipment)
        val tvFault: TextView = view.findViewById(R.id.tvAgendaFault)
        val btnStart: MaterialButton = view.findViewById(R.id.btnStartFromAgenda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_agenda_tech, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvDate.text = item["fecha"]
        holder.tvTime.text = item["hora"]
        holder.tvClient.text = item["cliente"]
        holder.tvEquipment.text = "Marca: ${item["marca"]}"
        holder.tvFault.text = "Falla: ${item["falla"]}"
        
        holder.btnStart.setOnClickListener {
            val id = item["id"]?.toInt() ?: -1
            onStartClick(id)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Map<String, String>>) {
        list = newList
        notifyDataSetChanged()
    }
}
