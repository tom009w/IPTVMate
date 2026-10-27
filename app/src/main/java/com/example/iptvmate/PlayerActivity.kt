package com.example.iptvmate

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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

@OptIn(UnstableApi::class)
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
    private var isRetryingLocal = false

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
            repository.addRecentChannel(channel!!)
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
            repository.addRecentChannel(selectedChannel)

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
            repository.addRecentChannel(ch)

            if (layoutEpgPanel.visibility == View.VISIBLE) {
                openEpgPanel()
            }
        }
    }

    private fun buildMediaItem(ch: Channel): MediaItem {
        val mediaItemBuilder = MediaItem.Builder().setUri(ch.streamUrl)

        val type = ch.manifestType?.lowercase()?.trim()
        val urlLower = ch.streamUrl.lowercase()

        when {
            type == "mpd" || urlLower.contains("/mpd/") || urlLower.endsWith(".mpd") || urlLower.contains(".mpd?") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            }
            type == "hls" || type == "m3u8" || urlLower.contains("/m3u8/") || urlLower.endsWith(".m3u8") || urlLower.contains(".m3u8?") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            type == "ism" || type == "ss" || urlLower.contains(".ism") || urlLower.contains("/manifest") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_SS)
            }
            type == "ts" || urlLower.endsWith(".ts") || urlLower.contains(".ts?") -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }

        if (!ch.licenseUrl.isNullOrBlank()) {
            val schemeUuid = when (ch.drmScheme?.lowercase()?.trim()) {
                "widevine", "com.widevine.alpha" -> C.WIDEVINE_UUID
                "playready", "com.microsoft.playready" -> C.PLAYREADY_UUID
                "clearkey", "com.clearkey", "org.w3.clearkey", "cenc" -> C.CLEARKEY_UUID
                else -> {
                    val url = ch.licenseUrl.lowercase()
                    when {
                        url.contains("clearkey") || isDirectKeyString(ch.licenseUrl) -> C.CLEARKEY_UUID
                        url.contains("playready") -> C.PLAYREADY_UUID
                        else -> C.WIDEVINE_UUID
                    }
                }
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

    private fun playChannel(ch: Channel, isFallbackRetry: Boolean = false) {
        if (!isFallbackRetry) {
            isRetryingLocal = false
        }
        val mediaItem = buildMediaItem(ch)
        exoPlayer?.apply {
            stop()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    private fun convertToLocalUrl(urlStr: String): String {
        return try {
            val uri = Uri.parse(urlStr)
            val path = uri.path ?: ""
            when {
                path.contains("/mpd/") -> {
                    val id = path.substringAfter("/mpd/").substringBefore("/")
                    "http://localhost:5350/live/mpd/$id"
                }
                path.contains("/m3u8/") -> {
                    val id = path.substringAfter("/m3u8/").substringBefore("/")
                    "http://localhost:5350/live/m3u8/$id"
                }
                path.contains("/key/") || path.contains("/clearkey/") -> {
                    val id = path.substringAfterLast("/")
                    "http://localhost:5350/live/key/$id"
                }
                else -> urlStr
            }
        } catch (e: Exception) {
            urlStr
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
        Log.d(TAG, "Is Stream URL Relative?: false")
        Log.d(TAG, "DRM Enabled?: $hasDrm")
        Log.d(TAG, "DRM Scheme: ${ch.drmScheme ?: "none"}")
        Log.d(TAG, "License URL Host: $licenseHost (Full license URL redacted)")
    }

    private fun isDirectKeyString(licenseStr: String): Boolean {
        val trimmed = licenseStr.trim()
        if (trimmed.startsWith("{") || trimmed.contains("\"keys\"")) return true
        if (trimmed.contains(":") && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            val parts = trimmed.split(":", limit = 2)
            val p0 = parts[0].trim().replace("-", "")
            val p1 = parts[1].trim().replace("-", "")
            if (p0.length == 32 || p1.length == 32 || (p0.length in 20..24 && p1.length in 20..24)) {
                return true
            }
        }
        return false
    }

    private fun formatClearKeyJson(jsonOrKidKeyStr: String): String {
        val trimmed = jsonOrKidKeyStr.trim()
        if (trimmed.startsWith("{") || trimmed.contains("\"keys\"")) {
            return try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val map: MutableMap<String, Any>? = gson.fromJson(trimmed, type)
                if (map != null && map.containsKey("keys")) {
                    val keysList = map["keys"] as? List<*>
                    if (keysList != null) {
                        val formattedKeys = keysList.mapNotNull { keyObj ->
                            val keyMap = keyObj as? Map<*, *> ?: return@mapNotNull null
                            val newObj = keyMap.mapKeys { it.key.toString() }.mapValues { it.value }.toMutableMap()
                            val kid = newObj["kid"]?.toString()
                            val k = newObj["k"]?.toString()
                            if (!kid.isNullOrBlank()) newObj["kid"] = formatBase64UrlKey(kid)
                            if (!k.isNullOrBlank()) newObj["k"] = formatBase64UrlKey(k)
                            if (!newObj.containsKey("kty")) newObj["kty"] = "oct"
                            newObj
                        }
                        // Android 9 ClearKey plugin expects ONLY "keys" at top level
                        val cleanMap = mapOf("keys" to formattedKeys)
                        gson.toJson(cleanMap)
                    } else {
                        trimmed
                    }
                } else {
                    trimmed
                }
            } catch (e: Exception) {
                trimmed
            }
        }

        val keysList = mutableListOf<Map<String, String>>()
        val pairs = trimmed.split(Regex("[,;\\s]+"))
        for (pair in pairs) {
            val p = pair.trim()
            if (p.contains(":")) {
                val colonIdx = p.indexOf(':')
                val rawKid = p.substring(0, colonIdx).trim()
                val rawKey = p.substring(colonIdx + 1).trim()
                if (rawKid.isNotEmpty() && rawKey.isNotEmpty()) {
                    val formattedKid = formatBase64UrlKey(rawKid)
                    val formattedKey = formatBase64UrlKey(rawKey)
                    keysList.add(
                        mapOf(
                            "kty" to "oct",
                            "kid" to formattedKid,
                            "k" to formattedKey
                        )
                    )
                }
            }
        }

        if (keysList.isNotEmpty()) {
            val resultMap = mapOf("keys" to keysList)
            return com.google.gson.Gson().toJson(resultMap)
        }

        return trimmed
    }

    private fun formatBase64UrlKey(str: String): String {
        val trimmed = str.trim().removeSurrounding("\"")
        val cleanHex = trimmed.replace("-", "").replace(":", "").replace(" ", "")
        if (cleanHex.length == 32 && cleanHex.all { it in "0123456789abcdefABCDEF" }) {
            return hexToBase64Url(cleanHex)
        }

        try {
            val base64UrlStandard = trimmed.replace("+", "-").replace("/", "_").removeSuffix("=").removeSuffix("=")
            val bytes = android.util.Base64.decode(base64UrlStandard, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
            if (bytes.size == 16) {
                return android.util.Base64.encodeToString(
                    bytes,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                ).trim()
            }
            if (bytes.size == 32) {
                val hexStr = String(bytes, Charsets.UTF_8).trim()
                if (hexStr.length == 32 && hexStr.all { it in "0123456789abcdefABCDEF" }) {
                    return hexToBase64Url(hexStr)
                }
            }
        } catch (_: Exception) {}

        return trimmed.replace("+", "-").replace("/", "_").removeSuffix("=").removeSuffix("=")
    }

    private fun hexToBase64Url(hex32: String): String {
        return try {
            val cleanHex = hex32.replace("-", "").replace(":", "").trim()
            if (cleanHex.length != 32) return hex32
            val bytes = ByteArray(16)
            for (i in 0 until 16) {
                bytes[i] = cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            ).trim()
        } catch (e: Exception) {
            hex32
        }
    }

    private fun initPlayer(ch: Channel) {
        val userAgentHeader = ch.licenseHeaders?.entries?.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
        val userAgent = if (!userAgentHeader.isNullOrBlank()) userAgentHeader else Util.getUserAgent(this, "IPTVMate")

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)

        if (!ch.licenseHeaders.isNullOrEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(ch.licenseHeaders)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)
            .setDrmSessionManagerProvider { mediaItem ->
                val drmConfig = mediaItem.localConfiguration?.drmConfiguration
                if (drmConfig != null) {
                    val licenseUriStr = drmConfig.licenseUri?.toString() ?: ""
                    val isDirectKey = isDirectKeyString(licenseUriStr)

                    val httpCallback = if (!isDirectKey && licenseUriStr.startsWith("http", ignoreCase = true)) {
                        androidx.media3.exoplayer.drm.HttpMediaDrmCallback(licenseUriStr, dataSourceFactory)
                    } else {
                        null
                    }

                    val customDrmCallback = object : androidx.media3.exoplayer.drm.MediaDrmCallback {
                        override fun executeProvisionRequest(
                            uuid: java.util.UUID,
                            request: androidx.media3.exoplayer.drm.ExoMediaDrm.ProvisionRequest
                        ): ByteArray {
                            return httpCallback?.executeProvisionRequest(uuid, request) ?: ByteArray(0)
                        }

                        override fun executeKeyRequest(
                            uuid: java.util.UUID,
                            request: androidx.media3.exoplayer.drm.ExoMediaDrm.KeyRequest
                        ): ByteArray {
                            val rawResponseBytes = if (isDirectKey) {
                                licenseUriStr.toByteArray(Charsets.UTF_8)
                            } else if (httpCallback != null) {
                                httpCallback.executeKeyRequest(uuid, request)
                            } else {
                                licenseUriStr.toByteArray(Charsets.UTF_8)
                            }

                            if (drmConfig.scheme == C.CLEARKEY_UUID) {
                                return try {
                                    val responseString = String(rawResponseBytes, Charsets.UTF_8)
                                    if (responseString.contains("{") || responseString.contains(":") || isDirectKey) {
                                        val sanitizedJson = formatClearKeyJson(responseString)
                                        Log.d(TAG, "Sanitized ClearKey DRM payload: $sanitizedJson")
                                        sanitizedJson.toByteArray(Charsets.UTF_8)
                                    } else {
                                        rawResponseBytes
                                    }
                                } catch (e: Exception) {
                                    rawResponseBytes
                                }
                            }

                            return rawResponseBytes
                        }
                    }

                    androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                        .build(customDrmCallback)
                } else {
                    androidx.media3.exoplayer.drm.DrmSessionManager.DRM_UNSUPPORTED
                }
            }

        val mediaItem = buildMediaItem(ch)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(800, 20000, 400, 800)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(3840, 2160)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
            )
        }

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunneling ->
                val nonSecureDecoders = androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, false, requiresTunneling)
                if (nonSecureDecoders.isNotEmpty()) nonSecureDecoders else androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunneling)
            }

        exoPlayer = ExoPlayer.Builder(this, renderersFactory)
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
                        val currentCh = channel
                        if (!isRetryingLocal && currentCh != null) {
                            val localStreamUrl = convertToLocalUrl(currentCh.streamUrl)
                            val localLicenseUrl = currentCh.licenseUrl?.let { convertToLocalUrl(it) }
                            if (localStreamUrl != currentCh.streamUrl) {
                                isRetryingLocal = true
                                Log.d(TAG, "External playback failed. Retrying with local server: $localStreamUrl")
                                val fallbackCh = currentCh.copy(
                                    streamUrl = localStreamUrl,
                                    licenseUrl = localLicenseUrl
                                )
                                channel = fallbackCh
                                playChannel(fallbackCh, isFallbackRetry = true)
                                return
                            }
                        }

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
