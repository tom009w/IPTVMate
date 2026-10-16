package com.example.iptvmate

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaylistsActivity : AppCompatActivity() {

    private lateinit var btnAddPlaylist: Button
    private lateinit var rvPlaylists: RecyclerView
    private lateinit var repository: PlaylistRepository

    private val playlists = mutableListOf<Playlist>()
    private var activePlaylistId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlists)

        btnAddPlaylist = findViewById(R.id.btnAddPlaylist)
        rvPlaylists = findViewById(R.id.rvPlaylists)
        repository = PlaylistRepository(applicationContext)

        rvPlaylists.layoutManager = LinearLayoutManager(this)
        rvPlaylists.setHasFixedSize(true)
        rvPlaylists.setItemViewCacheSize(20)

        btnAddPlaylist.setOnClickListener {
            showEditPlaylistDialog()
        }

        loadPlaylists()
    }

    private fun loadPlaylists() {
        lifecycleScope.launch {
            val list = repository.playlistsFlow.first()
            val activeId = repository.activePlaylistIdFlow.first()
            playlists.clear()
            playlists.addAll(list)
            activePlaylistId = activeId ?: list.firstOrNull()?.id

            rvPlaylists.adapter = PlaylistsAdapter(
                playlists = playlists,
                activePlaylistId = activePlaylistId,
                onSelect = { playlist ->
                    lifecycleScope.launch {
                        repository.setActivePlaylistId(playlist.id)
                        Toast.makeText(this@PlaylistsActivity, "Active playlist: ${playlist.name}", Toast.LENGTH_SHORT).show()
                        loadPlaylists()
                    }
                },
                onDelete = { playlist ->
                    lifecycleScope.launch {
                        val updated = playlists.filter { it.id != playlist.id }
                        repository.savePlaylists(updated)
                        if (activePlaylistId == playlist.id && updated.isNotEmpty()) {
                            repository.setActivePlaylistId(updated[0].id)
                        }
                        Toast.makeText(this@PlaylistsActivity, "Playlist deleted", Toast.LENGTH_SHORT).show()
                        loadPlaylists()
                    }
                }
            )
        }
    }

    private fun showEditPlaylistDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_edit, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPlaylistName)
        val etM3u = dialogView.findViewById<EditText>(R.id.etPlaylistM3uUrl)
        val etEpg = dialogView.findViewById<EditText>(R.id.etPlaylistEpgUrl)

        AlertDialog.Builder(this)
            .setTitle("Add Playlist")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim().ifBlank { "Playlist" }
                val m3u = etM3u.text.toString().trim()
                val epg = etEpg.text.toString().trim().ifBlank { null }

                if (m3u.isBlank()) {
                    Toast.makeText(this, "Playlist URL cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val currentList = repository.playlistsFlow.first().toMutableList()
                    val newPl = Playlist(name = name, url = m3u, epgUrl = epg)
                    currentList.add(newPl)
                    repository.savePlaylists(currentList)
                    if (currentList.size == 1) {
                        repository.setActivePlaylistId(newPl.id)
                    }
                    Toast.makeText(this@PlaylistsActivity, "Playlist saved", Toast.LENGTH_SHORT).show()
                    loadPlaylists()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
