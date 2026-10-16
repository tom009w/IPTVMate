package com.example.iptvmate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlayerEpgAdapter(
    private val programs: List<EpgProgram>
) : RecyclerView.Adapter<PlayerEpgAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvPlayerEpgTime)
        val tvTitle: TextView = itemView.findViewById(R.id.tvPlayerEpgTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_epg, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programs[position]
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = timeFormat.format(Date(program.startTime))
        val stopStr = timeFormat.format(Date(program.stopTime))
        holder.tvTime.text = "$startStr - $stopStr"
        holder.tvTitle.text = program.title

        val now = System.currentTimeMillis()
        if (now in program.startTime..program.stopTime) {
            holder.itemView.setBackgroundColor(Color.parseColor("#443700B3"))
        } else {
            holder.itemView.setBackgroundResource(R.drawable.tv_focusable_background)
        }
    }

    override fun getItemCount(): Int = programs.size
}
