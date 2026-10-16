package com.example.iptvmate

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URL

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    // EPG Panel views
    private lateinit var layoutEpgPanel: View
    private lateinit var tvEpgPanelTitle: TextView
    private lateinit var tvEpgPanelEmpty: TextView
    private lateinit var rvPlayerEpg: RecyclerView

    // Channel List Panel views
    private lateinit var layoutChannelListPanel: View
    private lateinit var rvPlayerChannels: RecyclerView

    private var exoPlayer: ExoPlayer? = null
    private var channel: Channel? = null
    private var allChannels: List<Channel> = emptyList()
    private var currentChannelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        playerView.useController = false
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        // EPG Panel init
        layoutEpgPanel = findViewById(R.id.layoutEpgPanel)
        tvEpgPanelTitle = findViewById(R.id.tvEpgPanelTitle)
        tvEpgPanelEmpty = findViewById(R.id.tvEpgPanelEmpty)
        rvPlayerEpg = findViewById(R.id.rvPlayerEpg)
        rvPlayerEpg.layoutManager = LinearLayoutManager(this)

        // Channel List Panel init
        layoutChannelListPanel = findViewById(R.id.layoutChannelListPanel)
        rvPlayerChannels = findViewById(R.id.rvPlayerChannels)
        rvPlayerChannels.layoutManager = LinearLayoutManager(this)

        channel = intent.getSerializableExtra(EXTRA_CHANNEL) as? Channel
        val repository = PlaylistRepository(applicationContext)
        allChannels = repository.loadCachedChannels()

        if (channel != null) {
            currentChannelIndex = allChannels.indexOfFirst { it.streamUrl == channel?.streamUrl }.let { if (it != -1) it else 0 }
            logChannelDiagnostics(channel!!)
            initPlayer(channel!!)

            val settings = repository.loadAppSettings()
            repository.saveAppSettings(settings.copy(lastWatchedStreamUrl = channel!!.streamUrl))
        } else {
            Log.e(TAG, "No channel specified in intent extra")
            tvError.text = "No channel specified"
            tvError.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutChannelListPanel.visibility == View.VISIBLE) {
                    layoutChannelListPanel.visibility = View.GONE
                    playerView.requestFocus()
                } else if (layoutEpgPanel.visibility == View.VISIBLE) {
                    layoutEpgPanel.visibility = View.GONE
                    playerView.requestFocus()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (layoutChannelListPanel.visibility == View.VISIBLE) {
                    layoutChannelListPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                } else if (layoutEpgPanel.visibility == View.VISIBLE) {
                    layoutEpgPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                } else {
                    openChannelListPanel()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (layoutChannelListPanel.visibility == View.VISIBLE) {
                    layoutChannelListPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                } else if (layoutEpgPanel.visibility == View.VISIBLE) {
                    layoutEpgPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                } else {
                    openEpgPanel()
                    return true
                }
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (layoutChannelListPanel.visibility == View.VISIBLE) {
                    layoutChannelListPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                } else if (layoutEpgPanel.visibility == View.VISIBLE) {
                    layoutEpgPanel.visibility = View.GONE
                    playerView.requestFocus()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (layoutChannelListPanel.visibility != View.VISIBLE && layoutEpgPanel.visibility != View.VISIBLE) {
                    switchChannel(-1)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (layoutChannelListPanel.visibility != View.VISIBLE && layoutEpgPanel.visibility != View.VISIBLE) {
                    switchChannel(1)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getCurrentCategoryChannels(): List<Channel> {
        val currentGroup = channel?.groupTitle?.trim().let { if (it.isNullOrBlank()) "Uncategorized" else it }
        val categoryChannels = allChannels.filter { (it.groupTitle?.trim().let { t -> if (t.isNullOrBlank()) "Uncategorized" else t }) == currentGroup }
        return if (categoryChannels.isNotEmpty()) categoryChannels else allChannels
    }

    private fun openChannelListPanel() {
        val categoryChannels = getCurrentCategoryChannels()
        val ch = channel
        val targetIndex = if (ch != null) {
            categoryChannels.indexOfFirst { it.streamUrl == ch.streamUrl }.let { if (it != -1) it else 0 }
        } else {
            0
        }

        rvPlayerChannels.adapter = ChannelAdapter(categoryChannels, emptyMap(), showChannelNumber = false) { selectedChannel ->
            layoutChannelListPanel.visibility = View.GONE
            channel = selectedChannel
            val idx = allChannels.indexOfFirst { it.streamUrl == selectedChannel.streamUrl }
            if (idx != -1) currentChannelIndex = idx
            logChannelDiagnostics(selectedChannel)
            playChannel(selectedChannel)

            val repository = PlaylistRepository(applicationContext)
            val settings = repository.loadAppSettings()
            repository.saveAppSettings(settings.copy(lastWatchedStreamUrl = selectedChannel.streamUrl))

            playerView.requestFocus()
        }
        layoutChannelListPanel.visibility = View.VISIBLE
        rvPlayerChannels.scrollToPosition(targetIndex)
        rvPlayerChannels.post {
            val viewHolder = rvPlayerChannels.findViewHolderForAdapterPosition(targetIndex)
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus()
            } else {
                rvPlayerChannels.requestFocus()
            }
        }
    }

    private fun openEpgPanel() {
        val ch = channel ?: return
        tvEpgPanelTitle.text = ch.name
        val programs = EpgMatcher.getChannelPrograms(ch, EpgManager.epgMap)

        if (programs.isEmpty()) {
            tvEpgPanelEmpty.text = "No programme information available"
            tvEpgPanelEmpty.visibility = View.VISIBLE
            rvPlayerEpg.visibility = View.GONE
        } else {
            tvEpgPanelEmpty.visibility = View.GONE
            rvPlayerEpg.visibility = View.VISIBLE
            rvPlayerEpg.adapter = PlayerEpgAdapter(programs)
        }

        layoutEpgPanel.visibility = View.VISIBLE
        if (programs.isNotEmpty()) {
            rvPlayerEpg.requestFocus()
        } else {
            layoutEpgPanel.requestFocus()
        }
    }

    private fun switchChannel(direction: Int) {
        val categoryChannels = getCurrentCategoryChannels()
        if (categoryChannels.isEmpty()) return

        val currentIndex = categoryChannels.indexOfFirst { it.streamUrl == channel?.streamUrl }.let { if (it != -1) it else 0 }
        val nextIndex = (currentIndex + direction + categoryChannels.size) % categoryChannels.size
        channel = categoryChannels[nextIndex]

        currentChannelIndex = allChannels.indexOfFirst { it.streamUrl == channel?.streamUrl }.let { if (it != -1) it else 0 }

        channel?.let { ch ->
            logChannelDiagnostics(ch)
            playChannel(ch)

            val repository = PlaylistRepository(applicationContext)
            val settings = repository.loadAppSettings()
            repository.saveAppSettings(settings.copy(lastWatchedStreamUrl = ch.streamUrl))

            if (layoutEpgPanel.visibility == View.VISIBLE) {
                openEpgPanel()
            }
        }
    }

    private fun buildMediaItem(ch: Channel): MediaItem {
        val mediaItemBuilder = MediaItem.Builder().setUri(ch.streamUrl)

        if (ch.streamUrl.contains("/mpd/", ignoreCase = true) || ch.streamUrl.endsWith(".mpd", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        } else if (ch.streamUrl.contains("/m3u8/", ignoreCase = true) || ch.streamUrl.endsWith(".m3u8", ignoreCase = true)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        if (!ch.licenseUrl.isNullOrBlank()) {
            val schemeUuid = when (ch.drmScheme?.lowercase()) {
                "widevine", "com.widevine.alpha" -> C.WIDEVINE_UUID
                "playready", "com.microsoft.playready" -> C.PLAYREADY_UUID
                else -> C.WIDEVINE_UUID
            }
            val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(schemeUuid)
                .setLicenseUri(ch.licenseUrl)
            if (!ch.licenseHeaders.isNullOrEmpty()) {
                drmConfigBuilder.setLicenseRequestHeaders(ch.licenseHeaders)
            }
            mediaItemBuilder.setDrmConfiguration(drmConfigBuilder.build())
        }
        return mediaItemBuilder.build()
    }

    private fun playChannel(ch: Channel) {
        val mediaItem = buildMediaItem(ch)
        exoPlayer?.apply {
            stop()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    private fun logChannelDiagnostics(ch: Channel) {
        val streamUri = Uri.parse(ch.streamUrl)
        val scheme = streamUri.scheme ?: "unknown"
        val host = streamUri.host ?: "unknown"
        val port = if (streamUri.port != -1) ":${streamUri.port}" else ""
        val path = streamUri.path ?: ""
        val redactedStreamUrl = "$scheme://$host$port$path"

        val isRelative = !ch.streamUrl.startsWith("http://", ignoreCase = true) && !ch.streamUrl.startsWith("https://", ignoreCase = true)

        val hasDrm = !ch.licenseUrl.isNullOrBlank()
        val licenseHost = if (hasDrm) {
            try {
                URL(ch.licenseUrl).host
            } catch (e: Exception) {
                "invalid"
            }
        } else {
            "none"
        }

        Log.d(TAG, "=== Channel Playback Diagnostics ===")
        Log.d(TAG, "Channel ID: ${ch.id}, Name: ${ch.name}")
        Log.d(TAG, "Stream URL (redacted): $redactedStreamUrl")
        Log.d(TAG, "Is Stream URL Relative?: $isRelative")
        Log.d(TAG, "DRM Enabled?: $hasDrm")
        Log.d(TAG, "DRM Scheme: ${ch.drmScheme ?: "none"}")
        Log.d(TAG, "License URL Host: $licenseHost (Full license URL redacted)")
    }

    private fun initPlayer(ch: Channel) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, "IPTVMate"))
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        if (!ch.licenseHeaders.isNullOrEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(ch.licenseHeaders)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val mediaItem = buildMediaItem(ch)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(1000, 30000, 500, 1000)
            .build()

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(3840, 2160)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
            )
        }

        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playerView.player = this
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                progressBar.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                progressBar.visibility = View.GONE
                                tvError.visibility = View.GONE
                            }
                            Player.STATE_ENDED -> {
                                progressBar.visibility = View.GONE
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        progressBar.visibility = View.GONE

                        val errorCode = error.errorCode
                        val errorName = error.errorCodeName
                        val message = error.localizedMessage ?: "Unable to play stream"

                        Log.e(TAG, "Playback error code: $errorCode ($errorName), message: $message")

                        var cause: Throwable? = error.cause
                        while (cause != null) {
                            Log.e(TAG, "Caused by: ${cause.javaClass.name}: ${cause.message}")
                            if (cause is ParserException) {
                                Log.e(TAG, "ParserException details: ${cause.message}")
                            }
                            if (cause is HttpDataSource.InvalidResponseCodeException) {
                                Log.e(TAG, "InvalidResponseCodeException status: ${cause.responseCode}, message: ${cause.message}")
                            }
                            cause = cause.cause
                        }

                        tvError.text = "Playback error [$errorName]: $message"
                        tvError.visibility = View.VISIBLE
                    }
                })
            }
    }

    override fun onStart() {
        super.onStart()
        exoPlayer?.play()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_CHANNEL = "extra_channel"
    }
}
