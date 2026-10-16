package com.example.iptvmate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelOrderAdapter(
    private val channels: MutableList<Channel>,
    private val onChannelClick: (Int) -> Unit
) : RecyclerView.Adapter<ChannelOrderAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        val ivFavoriteIndicator: ImageView = itemView.findViewById(R.id.ivFavoriteIndicator)
        val tvOrderChannelName: TextView = itemView.findViewById(R.id.tvOrderChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvOrderNumber.text = (position + 1).toString()
        holder.tvOrderChannelName.text = channel.name

        if (channel.isFavorite) {
            holder.ivFavoriteIndicator.setImageResource(android.R.drawable.btn_star_big_on)
            holder.ivFavoriteIndicator.setColorFilter(Color.parseColor("#FFB300"))
        } else {
            holder.ivFavoriteIndicator.setImageResource(android.R.drawable.btn_star_big_off)
            holder.ivFavoriteIndicator.setColorFilter(Color.parseColor("#80FFFFFF"))
        }

        holder.itemView.setOnClickListener {
            onChannelClick(position)
        }
    }

    override fun getItemCount(): Int = channels.size
}
