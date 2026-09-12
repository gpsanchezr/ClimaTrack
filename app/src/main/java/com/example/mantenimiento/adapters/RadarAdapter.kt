package com.example.mantenimiento.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mantenimiento.R
import com.google.android.material.button.MaterialButton

class RadarAdapter(
    private var list: List<Map<String, String>>,
    private val onActionClick: (Int, String) -> Unit // id, accion ("ACEPTAR" o "RECHAZAR")
) : RecyclerView.Adapter<RadarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvRadarClientName)
        val tvFault: TextView = view.findViewById(R.id.tvRadarFault)
        val tvAddress: TextView = view.findViewById(R.id.tvRadarAddress)
        val btnAccept: MaterialButton = view.findViewById(R.id.btnAcceptRadar)
        val btnReject: MaterialButton = view.findViewById(R.id.btnRejectRadar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request_radar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = item["cliente"]
        holder.tvFault.text = "Falla: ${item["falla"]}"
        holder.tvAddress.text = "Ubicación: ${item["direccion"]}"
        
        val solicitudId = item["id"]?.toInt() ?: -1

        holder.btnAccept.setOnClickListener {
            onActionClick(solicitudId, "ACEPTAR")
        }

        holder.btnReject.setOnClickListener {
            onActionClick(solicitudId, "RECHAZAR")
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Map<String, String>>) {
        list = newList
        notifyDataSetChanged()
    }
}
