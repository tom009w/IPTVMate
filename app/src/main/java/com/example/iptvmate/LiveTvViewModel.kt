package com.example.iptvmate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed interface LiveTvUiState {
    data class Loading(val message: String = "Loading channels... 0", val count: Int = 0) : LiveTvUiState
    data class Success(
        val channels: List<Channel>,
        val playlistUrl: String?,
        val epgPrograms: Map<Int, Pair<EpgProgram?, EpgProgram?>> = emptyMap()
    ) : LiveTvUiState
    data class Empty(val playlistUrl: String?) : LiveTvUiState
    data class Error(val message: String, val cachedChannels: List<Channel>?, val playlistUrl: String?) : LiveTvUiState
}

class LiveTvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaylistRepository(application)

    private val _uiState = MutableStateFlow<LiveTvUiState>(LiveTvUiState.Loading())
    val uiState: StateFlow<LiveTvUiState> = _uiState.asStateFlow()

    private var currentPlaylistUrl: String? = null
    private var currentEpgUrl: String? = null
    private var cachedChannels: List<Channel> = emptyList()
    var epgMap: Map<String, EpgChannelData> = emptyMap()
        private set
    private var channelEpgMap: Map<Int, Pair<EpgProgram?, EpgProgram?>> = emptyMap()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            cachedChannels = withContext(Dispatchers.IO) {
                repository.loadCachedChannels()
            }
            currentPlaylistUrl = repository.playlistUrlFlow.first()
            currentEpgUrl = repository.epgUrlFlow.first()

            if (!currentEpgUrl.isNullOrBlank()) {
                fetchAndParseEpg(currentEpgUrl!!, isBackground = true)
            }

            if (cachedChannels.isNotEmpty()) {
                channelEpgMap = EpgMatcher.matchEpg(cachedChannels, epgMap)
                _uiState.value = LiveTvUiState.Success(cachedChannels, currentPlaylistUrl, channelEpgMap)
                if (!currentPlaylistUrl.isNullOrBlank()) {
                    refreshPlaylist(currentPlaylistUrl!!, isBackground = true)
                }
            } else if (!currentPlaylistUrl.isNullOrBlank()) {
                fetchAndParsePlaylist(currentPlaylistUrl!!)
            } else {
                _uiState.value = LiveTvUiState.Empty(null)
            }
        }
    }

    fun loadPlaylist(url: String) {
        if (url.isBlank()) {
            _uiState.value = LiveTvUiState.Error("Playlist URL cannot be empty", cachedChannels.takeIf { it.isNotEmpty() }, currentPlaylistUrl)
            return
        }
        viewModelScope.launch {
            repository.savePlaylistUrl(url)
            currentPlaylistUrl = url
            fetchAndParsePlaylist(url)
        }
    }

    fun refreshEpg() {
        viewModelScope.launch {
            val url = repository.epgUrlFlow.first()
            if (!url.isNullOrBlank()) {
                currentEpgUrl = url
                fetchAndParseEpg(url, isBackground = false)
            }
        }
    }

    private fun fetchAndParseEpg(urlStr: String, isBackground: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "IPTVMate/1.0")
                }
                if (connection.responseCode in 200..299) {
                    epgMap = connection.inputStream.use { stream ->
                        XmltvParser.parse(stream)
                    }
                    EpgManager.epgMap = epgMap

                    if (cachedChannels.isNotEmpty()) {
                        channelEpgMap = EpgMatcher.matchEpg(cachedChannels, epgMap)
                        withContext(Dispatchers.Main) {
                            if (_uiState.value is LiveTvUiState.Success) {
                                val current = _uiState.value as LiveTvUiState.Success
                                _uiState.value = current.copy(epgPrograms = channelEpgMap)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("LiveTvViewModel", "Failed to fetch EPG: ${e.message}")
            }
        }
    }

    private fun fetchAndParsePlaylist(urlStr: String, isBackground: Boolean = false) {
        if (!isBackground) {
            _uiState.value = LiveTvUiState.Loading("Connecting to playlist...", 0)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlStr)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "IPTVMate/1.0")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw Exception("HTTP error status: $responseCode")
                }

                var channels = connection.inputStream.use { stream ->
                    M3UParser.parse(stream, urlStr) { count ->
                        if (!isBackground) {
                            if (count % 10 == 1 || count < 20) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    _uiState.value = LiveTvUiState.Loading("Loading channels... $count", count)
                                }
                            }
                        }
                    }
                }

                // Fetch and match JioTV Go /channels API metadata for DRM
                channels = fetchJioChannels(urlStr, channels)

                // Try fetching EPG if not already loaded and epgUrl is configured
                if (epgMap.isEmpty()) {
                    val epgUrl = repository.epgUrlFlow.first()
                    if (!epgUrl.isNullOrBlank()) {
                        try {
                            val epgConn = URL(epgUrl).openConnection() as HttpURLConnection
                            if (epgConn.responseCode in 200..299) {
                                epgMap = epgConn.inputStream.use { XmltvParser.parse(it) }
                                EpgManager.epgMap = epgMap
                            }
                        } catch (_: Exception) {}
                    }
                }

                channelEpgMap = EpgMatcher.matchEpg(channels, epgMap)

                if (channels.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (cachedChannels.isNotEmpty()) {
                            _uiState.value = LiveTvUiState.Error("Parsed playlist is empty. Kept previous channels.", cachedChannels, currentPlaylistUrl)
                        } else {
                            _uiState.value = LiveTvUiState.Empty(currentPlaylistUrl)
                        }
                    }
                } else {
                    cachedChannels = channels
                    repository.saveCachedChannels(channels)
                    withContext(Dispatchers.Main) {
                        _uiState.value = LiveTvUiState.Success(channels, currentPlaylistUrl, channelEpgMap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = e.localizedMessage ?: "Failed to load playlist"
                    if (cachedChannels.isNotEmpty()) {
                        _uiState.value = LiveTvUiState.Error("Import failed ($errorMsg). Showing cached channels.", cachedChannels, currentPlaylistUrl)
                    } else {
                        _uiState.value = LiveTvUiState.Error(errorMsg, null, currentPlaylistUrl)
                    }
                }
            }
        }
    }

    private fun fetchJioChannels(playlistUrl: String, m3uChannels: List<Channel>): List<Channel> {
        try {
            val apiUriStr = getChannelsApiUrl(playlistUrl) ?: return m3uChannels
            val url = URL(apiUriStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "IPTVMate/1.0")
            }

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val response = Gson().fromJson(json, JioChannelsResponse::class.java)
                if (response?.code == 200 && !response.result.isNullOrEmpty()) {
                    Log.d("LiveTvViewModel", "Successfully fetched ${response.result.size} channels from /channels API")
                    return JioChannelMatcher.matchChannels(m3uChannels, response.result)
                }
            }
        } catch (e: Exception) {
            Log.w("LiveTvViewModel", "Failed to fetch /channels API, using M3U channels without DRM: ${e.message}")
        }
        return m3uChannels
    }

    private fun getChannelsApiUrl(playlistUrl: String): String? {
        return try {
            val url = URL(playlistUrl)
            "${url.protocol}://${url.authority}/channels"
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshPlaylist(url: String, isBackground: Boolean) {
        fetchAndParsePlaylist(url, isBackground = isBackground)
    }
}
