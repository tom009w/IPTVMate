package com.example.iptvmate

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileReader
import java.io.FileWriter

private val Context.dataStore by preferencesDataStore(name = "settings")

class PlaylistRepository(private val context: Context) {

    private val PLAYLIST_URL_KEY = stringPreferencesKey("playlist_url")
    private val EPG_URL_KEY = stringPreferencesKey("epg_url")
    private val PLAYLISTS_JSON_KEY = stringPreferencesKey("playlists_json")
    private val ACTIVE_PLAYLIST_ID_KEY = stringPreferencesKey("active_playlist_id")

    private val gson = Gson()
    val cacheFile = File(context.filesDir, "channels_cache.json")

    val playlistUrlFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val activeId = preferences[ACTIVE_PLAYLIST_ID_KEY]
        val json = preferences[PLAYLISTS_JSON_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Playlist>>() {}.type
                val list = gson.fromJson<List<Playlist>>(json, type) ?: emptyList()
                val active = list.find { it.id == activeId } ?: list.firstOrNull()
                if (active != null) return@map active.url
            } catch (_: Exception) {}
        }
        preferences[PLAYLIST_URL_KEY]
    }

    val epgUrlFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val activeId = preferences[ACTIVE_PLAYLIST_ID_KEY]
        val json = preferences[PLAYLISTS_JSON_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Playlist>>() {}.type
                val list = gson.fromJson<List<Playlist>>(json, type) ?: emptyList()
                val active = list.find { it.id == activeId } ?: list.firstOrNull()
                if (active != null) return@map active.epgUrl
            } catch (_: Exception) {}
        }
        preferences[EPG_URL_KEY]
    }

    val playlistsFlow: Flow<List<Playlist>> = context.dataStore.data.map { preferences ->
        val json = preferences[PLAYLISTS_JSON_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Playlist>>() {}.type
                val list = gson.fromJson<List<Playlist>>(json, type) ?: emptyList()
                if (list.isNotEmpty()) return@map list
            } catch (_: Exception) {}
        }
        // Migration from legacy single playlist
        val legacyUrl = preferences[PLAYLIST_URL_KEY]
        val legacyEpg = preferences[EPG_URL_KEY]
        if (!legacyUrl.isNullOrBlank()) {
            listOf(Playlist(name = "Default Playlist", url = legacyUrl, epgUrl = legacyEpg))
        } else {
            emptyList()
        }
    }

    val activePlaylistIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_PLAYLIST_ID_KEY]
    }

    suspend fun savePlaylistUrl(url: String) {
        val settings = loadAppSettings()
        saveAppSettings(settings.copy(playlistUrl = url))
        context.dataStore.edit { preferences ->
            preferences[PLAYLIST_URL_KEY] = url
            val json = preferences[PLAYLISTS_JSON_KEY]
            val type = object : TypeToken<List<Playlist>>() {}.type
            val list = if (!json.isNullOrBlank()) {
                try {
                    (gson.fromJson<List<Playlist>>(json, type) ?: emptyList()).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            val activeId = preferences[ACTIVE_PLAYLIST_ID_KEY]
            val existing = list.find { it.id == activeId }
            if (existing != null) {
                val updated = existing.copy(url = url)
                val idx = list.indexOf(existing)
                list[idx] = updated
            } else if (list.isNotEmpty()) {
                val updated = list[0].copy(url = url)
                list[0] = updated
                preferences[ACTIVE_PLAYLIST_ID_KEY] = updated.id
            } else {
                val newPl = Playlist(name = "Default Playlist", url = url)
                list.add(newPl)
                preferences[ACTIVE_PLAYLIST_ID_KEY] = newPl.id
            }
            preferences[PLAYLISTS_JSON_KEY] = gson.toJson(list)
        }
    }

    suspend fun saveEpgUrl(url: String) {
        val settings = loadAppSettings()
        saveAppSettings(settings.copy(epgUrl = url.takeIf { it.isNotBlank() }))
        context.dataStore.edit { preferences ->
            if (url.isBlank()) {
                preferences.remove(EPG_URL_KEY)
            } else {
                preferences[EPG_URL_KEY] = url
            }
            val json = preferences[PLAYLISTS_JSON_KEY]
            val type = object : TypeToken<List<Playlist>>() {}.type
            val list = if (!json.isNullOrBlank()) {
                try {
                    (gson.fromJson<List<Playlist>>(json, type) ?: emptyList()).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            val activeId = preferences[ACTIVE_PLAYLIST_ID_KEY]
            val existing = list.find { it.id == activeId }
            if (existing != null) {
                val updated = existing.copy(epgUrl = url.takeIf { it.isNotBlank() })
                val idx = list.indexOf(existing)
                list[idx] = updated
                preferences[PLAYLISTS_JSON_KEY] = gson.toJson(list)
            }
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) {
        context.dataStore.edit { preferences ->
            preferences[PLAYLISTS_JSON_KEY] = gson.toJson(playlists)
        }
    }

    suspend fun setActivePlaylistId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PLAYLIST_ID_KEY] = id
            val json = preferences[PLAYLISTS_JSON_KEY]
            if (!json.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<List<Playlist>>() {}.type
                    val list = gson.fromJson<List<Playlist>>(json, type) ?: emptyList()
                    val target = list.find { it.id == id }
                    if (target != null) {
                        preferences[PLAYLIST_URL_KEY] = target.url
                        if (target.epgUrl != null) {
                            preferences[EPG_URL_KEY] = target.epgUrl
                        } else {
                            preferences.remove(EPG_URL_KEY)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun saveCachedChannels(channels: List<Channel>) {
        try {
            val json = gson.toJson(channels)
            FileWriter(cacheFile).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCachedChannels(): List<Channel> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            FileReader(cacheFile).use { reader ->
                val type = object : TypeToken<List<Channel>>() {}.type
                gson.fromJson<List<Channel>>(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val categoryOrderCacheFile = File(context.filesDir, "category_order_cache.json")

    data class CategoryConfig(
        val order: List<String> = emptyList(),
        val hidden: List<String> = emptyList()
    )

    fun saveCategoryConfig(config: CategoryConfig) {
        try {
            val json = gson.toJson(config)
            FileWriter(categoryOrderCacheFile).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCategoryConfig(): CategoryConfig {
        if (!categoryOrderCacheFile.exists()) return CategoryConfig()
        return try {
            FileReader(categoryOrderCacheFile).use { reader ->
                val jsonStr = reader.readText()
                if (jsonStr.trim().startsWith("[")) {
                    val listType = object : TypeToken<List<String>>() {}.type
                    val list = gson.fromJson<List<String>>(jsonStr, listType) ?: emptyList()
                    CategoryConfig(order = list, hidden = emptyList())
                } else {
                    gson.fromJson(jsonStr, CategoryConfig::class.java) ?: CategoryConfig()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CategoryConfig()
        }
    }

    fun saveCategoryOrder(order: List<String>) {
        val current = loadCategoryConfig()
        saveCategoryConfig(current.copy(order = order))
    }

    fun loadCategoryOrder(): List<String> {
        return loadCategoryConfig().order
    }

    fun saveHiddenCategories(hidden: List<String>) {
        val current = loadCategoryConfig()
        saveCategoryConfig(current.copy(hidden = hidden))
    }

    fun loadHiddenCategories(): List<String> {
        return loadCategoryConfig().hidden
    }

    private val settingsCacheFile = File(context.filesDir, "settings_cache.json")

    data class AppSettings(
        val autoPlayLastChannel: Boolean = false,
        val lastWatchedStreamUrl: String? = null,
        val playlistUrl: String? = null,
        val epgUrl: String? = null,
        val xtreamServer: String? = null,
        val xtreamUsername: String? = null,
        val xtreamPassword: String? = null
    )

    fun saveAppSettings(settings: AppSettings) {
        try {
            val json = gson.toJson(settings)
            FileWriter(settingsCacheFile).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAppSettings(): AppSettings {
        if (!settingsCacheFile.exists()) return AppSettings()
        return try {
            FileReader(settingsCacheFile).use { reader ->
                gson.fromJson(reader, AppSettings::class.java) ?: AppSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSettings()
        }
    }
}
