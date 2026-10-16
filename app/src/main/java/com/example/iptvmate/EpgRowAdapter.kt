package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class EpgRowAdapter(
    private val channels: List<Channel>,
    private val epgMap: Map<String, EpgChannelData>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<EpgRowAdapter.RowViewHolder>() {

    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEpgLogo: ImageView = itemView.findViewById(R.id.ivEpgLogo)
        val tvEpgChannelName: TextView = itemView.findViewById(R.id.tvEpgChannelName)
        val layoutChannelInfo: View = itemView.findViewById(R.id.layoutChannelInfo)
        val rvEpgPrograms: RecyclerView = itemView.findViewById(R.id.rvEpgPrograms)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_row, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvEpgChannelName.text = channel.name

        val logoUrl = channel.tvgLogo
        if (!logoUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(logoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivEpgLogo)
        } else {
            holder.ivEpgLogo.setImageResource(R.drawable.default_background)
        }

        val programs = EpgMatcher.getChannelPrograms(channel, epgMap)
        holder.rvEpgPrograms.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.rvEpgPrograms.setHasFixedSize(true)
        holder.rvEpgPrograms.setItemViewCacheSize(10)
        holder.rvEpgPrograms.adapter = EpgProgramAdapter(programs) {
            onChannelClick(channel)
        }

        holder.layoutChannelInfo.setOnClickListener {
            onChannelClick(channel)
        }

        holder.itemView.setOnClickListener {
            onChannelClick(channel)
        }
    }

    override fun getItemCount(): Int = channels.size
}
