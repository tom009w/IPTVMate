package com.example.iptvmate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChannelOrderAdapter(
    private val channels: MutableList<Channel>,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onFavoriteToggle: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<ChannelOrderAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderNumber: TextView = itemView.findViewById(R.id.tvOrderNumber)
        val ivFavoriteToggle: ImageView = itemView.findViewById(R.id.ivFavoriteToggle)
        val tvOrderChannelName: TextView = itemView.findViewById(R.id.tvOrderChannelName)
        val btnMoveUp: Button = itemView.findViewById(R.id.btnMoveUp)
        val btnMoveDown: Button = itemView.findViewById(R.id.btnMoveDown)
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
            holder.ivFavoriteToggle.setImageResource(android.R.drawable.btn_star_big_on)
            holder.ivFavoriteToggle.setColorFilter(Color.parseColor("#FFB300"))
        } else {
            holder.ivFavoriteToggle.setImageResource(android.R.drawable.btn_star_big_off)
            holder.ivFavoriteToggle.setColorFilter(Color.parseColor("#80FFFFFF"))
        }

        holder.ivFavoriteToggle.setOnClickListener {
            val newFav = !channel.isFavorite
            channels[position] = channel.copy(isFavorite = newFav)
            if (newFav) {
                holder.ivFavoriteToggle.setImageResource(android.R.drawable.btn_star_big_on)
                holder.ivFavoriteToggle.setColorFilter(Color.parseColor("#FFB300"))
            } else {
                holder.ivFavoriteToggle.setImageResource(android.R.drawable.btn_star_big_off)
                holder.ivFavoriteToggle.setColorFilter(Color.parseColor("#80FFFFFF"))
            }
            onFavoriteToggle(position, newFav)
        }

        holder.btnMoveUp.setOnClickListener {
            onMoveUp(position)
        }

        holder.btnMoveDown.setOnClickListener {
            onMoveDown(position)
        }
    }

    override fun getItemCount(): Int = channels.size
}
