package com.example.iptvmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaylistsAdapter(
    private val playlists: List<Playlist>,
    private val activePlaylistId: String?,
    private val onSelect: (Playlist) -> Unit,
    private val onDelete: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlaylistName: TextView = itemView.findViewById(R.id.tvPlaylistName)
        val tvPlaylistUrl: TextView = itemView.findViewById(R.id.tvPlaylistUrl)
        val btnSelectPlaylist: Button = itemView.findViewById(R.id.btnSelectPlaylist)
        val btnDeletePlaylist: Button = itemView.findViewById(R.id.btnDeletePlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        val isActive = playlist.id == activePlaylistId
        holder.tvPlaylistName.text = if (isActive) "${playlist.name} (Active)" else playlist.name
        holder.tvPlaylistUrl.text = playlist.url

        holder.btnSelectPlaylist.setOnClickListener {
            onSelect(playlist)
        }

        holder.btnDeletePlaylist.setOnClickListener {
            onDelete(playlist)
        }
    }

    override fun getItemCount(): Int = playlists.size
}
