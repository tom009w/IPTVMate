package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

    private val viewModel: LiveTvViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[LiveTvViewModel::class.java]
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var layoutLoading: View
    private lateinit var tvLoadingProgress: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvError: TextView
    private lateinit var rvChannels: RecyclerView
    private lateinit var rvGroups: RecyclerView

    // Second-level Main Menu views
    private lateinit var layoutMainMenu: View
    private lateinit var menuSearch: Button
    private lateinit var menuSettings: Button
    private lateinit var menuEpg: Button
    private lateinit var menuExit: Button

    private var currentUrl: String? = null
    private var allChannels: List<Channel> = emptyList()
    private var currentEpgPrograms: Map<Int, Pair<EpgProgram?, EpgProgram?>> = emptyMap()
    private var selectedGroup: String = "All Channels"
    private var lastBackPressTime = 0L
    private var hasPromptedForPlaylist = false
    private var hasAutoPlayedLastChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)

        progressBar = findViewById(R.id.progressBar)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoadingProgress = findViewById(R.id.tvLoadingProgress)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvEmpty.setOnClickListener {
            showMandatoryPlaylistDialog(currentUrl)
        }
        tvError = findViewById(R.id.tvError)
        rvChannels = findViewById(R.id.rvChannels)
        rvGroups = findViewById(R.id.rvGroups)

        rvChannels.layoutManager = LinearLayoutManager(this)
        rvChannels.setHasFixedSize(true)
        rvChannels.setItemViewCacheSize(20)
        rvGroups.layoutManager = LinearLayoutManager(this)
        rvGroups.setHasFixedSize(true)
        rvGroups.setItemViewCacheSize(20)

        // Main Menu init
        layoutMainMenu = findViewById(R.id.layoutMainMenu)
        menuSearch = findViewById(R.id.menuSearch)
        menuSettings = findViewById(R.id.menuSettings)
        menuEpg = findViewById(R.id.menuEpg)
        menuExit = findViewById(R.id.menuExit)

        menuSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        menuSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        menuEpg.setOnClickListener {
            startActivity(Intent(this, EpgActivity::class.java))
        }
        menuExit.setOnClickListener {
            finishAffinity()
        }

        layoutMainMenu.isFocusable = true
        layoutMainMenu.isFocusableInTouchMode = true

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutMainMenu.visibility == View.VISIBLE) {
                    layoutMainMenu.visibility = View.GONE
                    rvGroups.visibility = View.VISIBLE
                    rvGroups.requestFocus()
                } else if (rvGroups.visibility == View.VISIBLE) {
                    rvGroups.visibility = View.GONE
                    rvChannels.requestFocus()
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000) {
                        finishAffinity()
                    } else {
                        lastBackPressTime = currentTime
                        Toast.makeText(this@LiveTvActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadChannels()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (layoutMainMenu.visibility == View.VISIBLE) {
                        return true
                    } else if (rvGroups.visibility == View.VISIBLE) {
                        rvGroups.visibility = View.GONE
                        layoutMainMenu.visibility = View.VISIBLE
                        menuSettings.requestFocus()
                        return true
                    } else if (rvChannels.visibility == View.VISIBLE) {
                        rvGroups.visibility = View.VISIBLE
                        rvGroups.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (layoutMainMenu.visibility == View.VISIBLE) {
                        layoutMainMenu.visibility = View.GONE
                        rvGroups.visibility = View.VISIBLE
                        rvGroups.requestFocus()
                        return true
                    } else if (rvGroups.visibility == View.VISIBLE) {
                        rvGroups.visibility = View.GONE
                        rvChannels.requestFocus()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun renderState(state: LiveTvUiState) {
        when (state) {
            is LiveTvUiState.Loading -> {
                layoutLoading.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                tvLoadingProgress.text = state.message
                tvEmpty.visibility = View.GONE
                tvError.visibility = View.GONE
                rvChannels.visibility = View.GONE
                rvGroups.visibility = View.GONE
                layoutMainMenu.visibility = View.GONE
            }
            is LiveTvUiState.Empty -> {
                layoutLoading.visibility = View.GONE
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvError.visibility = View.GONE
                rvChannels.visibility = View.GONE
                rvGroups.visibility = View.GONE
                layoutMainMenu.visibility = View.GONE
                currentUrl = state.playlistUrl

                if (!hasPromptedForPlaylist) {
                    hasPromptedForPlaylist = true
                    showMandatoryPlaylistDialog(state.playlistUrl)
                }
            }
            is LiveTvUiState.Success -> {
                layoutLoading.visibility = View.GONE
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.GONE
                tvError.visibility = View.GONE
                rvChannels.visibility = View.VISIBLE
                currentUrl = state.playlistUrl
                allChannels = state.channels
                currentEpgPrograms = state.epgPrograms
                EpgManager.epgMap = viewModel.epgMap

                val repo = PlaylistRepository(applicationContext)
                val config = repo.loadCategoryConfig()
                val groups = GroupExtractor.extractGroups(allChannels, config.order, config.hidden)
                rvGroups.adapter = GroupAdapter(groups) { group ->
                    selectedGroup = group
                    val filtered = GroupExtractor.filterChannels(allChannels, selectedGroup)
                    if (filtered.isEmpty()) {
                        Toast.makeText(this, "No channels found in $group", Toast.LENGTH_SHORT).show()
                    } else {
                        rvGroups.visibility = View.GONE
                        rvChannels.requestFocus()
                    }
                    updateChannelList()
                }

                updateChannelList()

                if (!hasAutoPlayedLastChannel) {
                    hasAutoPlayedLastChannel = true
                    val settings = repo.loadAppSettings()
                    if (settings.autoPlayLastChannel) {
                        val targetUrl = settings.lastWatchedStreamUrl
                        val targetChannel = allChannels.find { it.streamUrl == targetUrl } ?: allChannels.firstOrNull()
                        if (targetChannel != null) {
                            val intent = Intent(this, PlayerActivity::class.java).apply {
                                putExtra(PlayerActivity.EXTRA_CHANNEL, targetChannel)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
            is LiveTvUiState.Error -> {
                progressBar.visibility = View.GONE
                currentUrl = state.playlistUrl
                tvError.text = state.message
                tvError.visibility = View.VISIBLE

                val channelsToShow = state.cachedChannels
                if (!channelsToShow.isNullOrEmpty()) {
                    tvEmpty.visibility = View.GONE
                    rvChannels.visibility = View.VISIBLE
                    allChannels = channelsToShow
                    val repo = PlaylistRepository(applicationContext)
                val config = repo.loadCategoryConfig()
                val groups = GroupExtractor.extractGroups(allChannels, config.order, config.hidden)
                    rvGroups.adapter = GroupAdapter(groups) { group ->
                        selectedGroup = group
                        val filtered = GroupExtractor.filterChannels(allChannels, selectedGroup)
                        if (filtered.isEmpty()) {
                            Toast.makeText(this, "No channels found in $group", Toast.LENGTH_SHORT).show()
                        } else {
                            rvGroups.visibility = View.GONE
                            rvChannels.requestFocus()
                        }
                        updateChannelList()
                    }
                    updateChannelList()
                } else {
                    tvEmpty.visibility = View.VISIBLE
                    rvChannels.visibility = View.GONE
                }
            }
        }
    }

    private fun updateChannelList() {
        val filtered = GroupExtractor.filterChannels(allChannels, selectedGroup)
        val adapter = rvChannels.adapter as? ChannelAdapter
        if (adapter != null) {
            adapter.updateData(filtered, currentEpgPrograms)
        } else {
            rvChannels.adapter = ChannelAdapter(filtered, currentEpgPrograms) { channel ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
                }
                startActivity(intent)
            }
        }
    }

    private fun showMandatoryPlaylistDialog(existingUrl: String?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_url, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etPlaylistUrl)
        if (!existingUrl.isNullOrBlank()) {
            etUrl.setText(existingUrl)
            etUrl.setSelection(existingUrl.length)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.playlist_dialog_title))
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Load", null)
            .setNeutralButton("Additional Settings", null)
            .setNegativeButton("Exit", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.decorView?.setBackgroundColor(android.graphics.Color.parseColor("#1E1E2E"))
            val posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            posButton.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            posButton.setOnClickListener {
                val url = etUrl.text.toString().trim()
                if (url.isBlank()) {
                    Toast.makeText(this, "Playlist URL cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.loadPlaylist(url)
                    dialog.dismiss()
                }
            }

            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            neutralButton.setOnClickListener {
                startActivity(Intent(this, AdditionalSettingsActivity::class.java))
            }

            val negButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negButton.setTextColor(android.graphics.Color.parseColor("#B0BEC5"))
            negButton.setOnClickListener {
                finishAffinity()
            }
        }
        dialog.show()
    }
}
