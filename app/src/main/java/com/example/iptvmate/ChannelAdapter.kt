package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ChannelAdapter(
    private val channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvChannelName.text = channel.name

        val logoUrl = channel.tvgLogo
        if (!logoUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(logoUrl)
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
