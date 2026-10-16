package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpgProgramAdapter(
    private val programs: List<EpgProgram>,
    private val onProgramClick: (EpgProgram) -> Unit
) : RecyclerView.Adapter<EpgProgramAdapter.ProgramViewHolder>() {

    class ProgramViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProgramTitle: TextView = itemView.findViewById(R.id.tvProgramTitle)
        val tvProgramTime: TextView = itemView.findViewById(R.id.tvProgramTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_program, parent, false)
        return ProgramViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        val program = programs[position]
        holder.tvProgramTitle.text = program.title
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startStr = timeFormat.format(Date(program.startTime))
        val stopStr = timeFormat.format(Date(program.stopTime))
        holder.tvProgramTime.text = "$startStr - $stopStr"

        holder.itemView.setOnClickListener {
            onProgramClick(program)
        }
    }

    override fun getItemCount(): Int = programs.size
}
