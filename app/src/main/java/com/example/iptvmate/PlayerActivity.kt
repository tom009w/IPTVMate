package com.example.iptvmate

import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import java.net.URL

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvChannelTitle: TextView

    private var exoPlayer: ExoPlayer? = null
    private var channel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        tvChannelTitle = findViewById(R.id.tvChannelTitle)

        channel = intent.getSerializableExtra(EXTRA_CHANNEL) as? Channel

        if (channel != null) {
            tvChannelTitle.text = channel?.name
            logChannelDiagnostics(channel!!)
            initPlayer(channel!!)
        } else {
            Log.e(TAG, "No channel specified in intent extra")
            tvError.text = "No channel specified"
            tvError.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
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
            .setUserAgent("IPTVMate/1.0")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val mediaItemBuilder = MediaItem.Builder().setUri(ch.streamUrl)

        // Explicitly set MIME type for DASH (.mpd or /mpd/) and HLS (.m3u8 or /m3u8/)
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

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playerView.player = this
                setMediaItem(mediaItemBuilder.build())
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
