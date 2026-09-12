package com.example.mantenimiento.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.example.mantenimiento.activities.MaintenanceActivity
import com.example.mantenimiento.models.OrdenConDetalle
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * OrdenAdapter - Gestión de Órdenes con filtrado en tiempo real (Filterable)
 * y flujo de Aceptación/Rechazo.
 */
class OrdenAdapter(
    private var list: List<OrdenConDetalle>,
    private val onStatusChange: (Int, String) -> Unit
) : RecyclerView.Adapter<OrdenAdapter.ViewHolder>(), Filterable {

    private var fullList: List<OrdenConDetalle> = ArrayList(list)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderNumber: TextView = view.findViewById(R.id.tvOrderNumber)
        val tvOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvEquipmentName: TextView = view.findViewById(R.id.tvEquipmentName)
        val tvStatusLabel: TextView = view.findViewById(R.id.tvStatusLabel)
        
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutOrderActions)
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAcceptOrder)
        val btnReject: MaterialButton = view.findViewById(R.id.btnRejectOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_orden, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        
        holder.tvOrderNumber.text = item.numero
        holder.tvOrderDate.text = item.fecha
        holder.tvClientName.text = "Cliente: ${item.clienteNombre}"
        holder.tvEquipmentName.text = "Equipo: ${item.equipoDescripcion}"
        holder.tvStatusLabel.text = item.estado
        
        when(item.estado) {
            "PENDIENTE" -> {
                holder.tvStatusLabel.setBackgroundResource(R.drawable.status_pending_bg)
                holder.tvStatusLabel.setTextColor(context.getColor(R.color.status_pending))
                holder.layoutActions.visibility = View.VISIBLE
            }
            "EN PROCESO" -> {
                holder.tvStatusLabel.setBackgroundResource(R.drawable.status_in_progress_bg)
                holder.tvStatusLabel.setTextColor(context.getColor(R.color.status_in_progress))
                holder.layoutActions.visibility = View.GONE
            }
            "FINALIZADA" -> {
                holder.tvStatusLabel.setBackgroundResource(R.drawable.status_finished_bg)
                holder.tvStatusLabel.setTextColor(context.getColor(R.color.status_finished))
                holder.layoutActions.visibility = View.GONE
            }
            else -> {
                holder.layoutActions.visibility = View.GONE
            }
        }

        holder.btnAccept.setOnClickListener {
            onStatusChange(item.id, "EN PROCESO")
        }

        holder.btnReject.setOnClickListener {
            onStatusChange(item.id, "RECHAZADA")
        }

        holder.itemView.setOnClickListener {
            if (item.estado != "RECHAZADA") {
                val intent = Intent(context, MaintenanceActivity::class.java)
                intent.putExtra("ORDEN_ID", item.id)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<OrdenConDetalle>) {
        fullList = ArrayList(newList)
        list = newList
        notifyDataSetChanged()
    }

    /**
     * Implementación de Filterable para búsqueda en tiempo real por
     * clienteNombre, numero o equipoDescripcion.
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
                val filtered = if (query.isEmpty()) {
                    fullList
                } else {
                    fullList.filter { item ->
                        item.clienteNombre.lowercase(Locale.ROOT).contains(query) ||
                        item.numero.lowercase(Locale.ROOT).contains(query) ||
                        item.equipoDescripcion.lowercase(Locale.ROOT).contains(query) ||
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
                list = (results?.values as? List<OrdenConDetalle>) ?: fullList
                notifyDataSetChanged()
            }
        }
    }
}