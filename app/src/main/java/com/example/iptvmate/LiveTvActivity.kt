package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveTvActivity : AppCompatActivity() {

    private val viewModel: LiveTvViewModel by viewModels()

    private lateinit var btnChangePlaylist: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var rvChannels: RecyclerView

    private var currentUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)

        btnChangePlaylist = findViewById(R.id.btnChangePlaylist)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvError = findViewById(R.id.tvError)
        rvChannels = findViewById(R.id.rvChannels)

        rvChannels.layoutManager = LinearLayoutManager(this)

        btnChangePlaylist.setOnClickListener {
            showPlaylistUrlDialog(currentUrl)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: LiveTvUiState) {
        when (state) {
            is LiveTvUiState.Loading -> {
                progressBar.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                tvError.visibility = android.view.View.GONE
                rvChannels.visibility = android.view.View.GONE
            }
            is LiveTvUiState.Empty -> {
                progressBar.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.VISIBLE
                tvError.visibility = android.view.View.GONE
                rvChannels.visibility = android.view.View.GONE
                currentUrl = state.playlistUrl
            }
            is LiveTvUiState.Success -> {
                progressBar.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.GONE
                tvError.visibility = android.view.View.GONE
                rvChannels.visibility = android.view.View.VISIBLE
                currentUrl = state.playlistUrl
                rvChannels.adapter = ChannelAdapter(state.channels) { channel ->
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
                    }
                    startActivity(intent)
                }
            }
            is LiveTvUiState.Error -> {
                progressBar.visibility = android.view.View.GONE
                currentUrl = state.playlistUrl
                tvError.text = state.message
                tvError.visibility = android.view.View.VISIBLE

                if (state.cachedChannels != null && state.cachedChannels.isNotEmpty()) {
                    tvEmpty.visibility = android.view.View.GONE
                    rvChannels.visibility = android.view.View.VISIBLE
                    rvChannels.adapter = ChannelAdapter(state.cachedChannels) { channel ->
                        val intent = Intent(this, PlayerActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
                        }
                        startActivity(intent)
                    }
                } else {
                    tvEmpty.visibility = android.view.View.VISIBLE
                    rvChannels.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun showPlaylistUrlDialog(existingUrl: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_url, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etPlaylistUrl)
        if (!existingUrl.isNullOrBlank()) {
            etUrl.setText(existingUrl)
            etUrl.setSelection(existingUrl.length)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.playlist_dialog_title))
            .setView(dialogView)
            .setPositiveButton("Load") { _, _ ->
                val url = etUrl.text.toString().trim()
                viewModel.loadPlaylist(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
