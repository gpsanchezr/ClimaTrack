package com.example.mantenimiento.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.activities.MaintenanceActivity
import com.example.mantenimiento.models.MantenimientoCompleto
import java.util.Locale

class HistoryAdapter(
    private var list: List<MantenimientoCompleto>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>(), Filterable {

    private var fullList: List<MantenimientoCompleto> = ArrayList(list)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val tvOrder: TextView = view.findViewById(R.id.tvHistoryOrder)
        val tvType: TextView = view.findViewById(R.id.tvHistoryType)
        val tvTechnician: TextView = view.findViewById(R.id.tvHistoryTechnician)
        val tvDesc: TextView = view.findViewById(R.id.tvHistoryDesc)
        val ivStatus: ImageView = view.findViewById(R.id.ivHistoryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        
        holder.tvDate.text = item.fecha
        holder.tvTime.text = if (item.latitud != null) "GPS: OK" else "Sin GPS"
        holder.tvOrder.text = "Orden: ${item.ordenNumero}"
        holder.tvType.text = item.tipoServicio.uppercase()
        holder.tvTechnician.text = "Técnico: ${item.tecnicoNombre}"
        holder.tvDesc.text = item.trabajoRealizado

        if (!item.firmaRuta.isNullOrEmpty()) {
            holder.ivStatus.setImageResource(android.R.drawable.checkbox_on_background)
            holder.ivStatus.setColorFilter(context.getColor(R.color.status_finished))
            holder.tvType.setBackgroundResource(R.drawable.status_finished_bg)
            holder.tvType.setTextColor(context.getColor(R.color.status_finished))
        } else {
            holder.ivStatus.setImageResource(android.R.drawable.ic_dialog_alert)
            holder.ivStatus.setColorFilter(context.getColor(R.color.status_pending))
            holder.tvType.setBackgroundResource(R.drawable.status_pending_bg)
            holder.tvType.setTextColor(context.getColor(R.color.status_pending))
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MaintenanceActivity::class.java)
            intent.putExtra("ORDEN_ID", item.ordenId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<MantenimientoCompleto>) {
        fullList = ArrayList(newList)
        list = newList
        notifyDataSetChanged()
    }

    fun ordenarAlfabeticamente(ascendente: Boolean) {
        list = if (ascendente) {
            list.sortedBy { it.ordenNumero }
        } else {
            list.sortedByDescending { it.ordenNumero }
        }
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
                val filtered = if (query.isEmpty()) {
                    fullList
                } else {
                    fullList.filter { item ->
                        item.ordenNumero.lowercase(Locale.ROOT).contains(query) ||
                        item.tecnicoNombre.lowercase(Locale.ROOT).contains(query) ||
                        item.trabajoRealizado.lowercase(Locale.ROOT).contains(query) ||
                        item.tipoServicio.lowercase(Locale.ROOT).contains(query)
                    }
                }
                val results = FilterResults()
                results.values = filtered
                results.count = filtered.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                list = (results?.values as? List<MantenimientoCompleto>) ?: fullList
                notifyDataSetChanged()
            }
        }
    }
}