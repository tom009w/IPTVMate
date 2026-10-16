package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChannelAdapter(
    private val channels: List<Channel>,
    private val epgPrograms: Map<Int, Pair<EpgProgram?, EpgProgram?>> = emptyMap(),
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChannelNumber: TextView = itemView.findViewById(R.id.tvChannelNumber)
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvProgramInfo: TextView = itemView.findViewById(R.id.tvProgramInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvChannelNumber.text = (position + 1).toString()
        holder.tvChannelName.text = channel.name

        val programPair = epgPrograms[channel.orderIndex]
        if (programPair != null && (programPair.first != null || programPair.second != null)) {
            val curr = programPair.first
            val next = programPair.second
            val sb = StringBuilder()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            if (curr != null) {
                val startStr = timeFormat.format(Date(curr.startTime))
                val stopStr = timeFormat.format(Date(curr.stopTime))
                sb.append("Now: ").append(curr.title).append(" ($startStr - $stopStr)")
            } else if (next != null) {
                val startStr = timeFormat.format(Date(next.startTime))
                sb.append("Next: ").append(next.title).append(" ($startStr)")
            }
            holder.tvProgramInfo.text = sb.toString()
            holder.tvProgramInfo.visibility = View.VISIBLE
        } else {
            if (!channel.groupTitle.isNullOrBlank()) {
                holder.tvProgramInfo.text = channel.groupTitle
                holder.tvProgramInfo.visibility = View.VISIBLE
            } else {
                holder.tvProgramInfo.visibility = View.GONE
            }
        }

        val logoUrl = channel.tvgLogo
        if (!logoUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(logoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivLogo)
        } else {
            holder.ivLogo.setImageResource(R.drawable.default_background)
        }

        holder.itemView.setOnClickListener {
            onChannelClick(channel)
        }
    }

    override fun getItemCount(): Int = channels.size
}
