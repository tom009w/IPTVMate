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
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "channels_cache.json")

    val playlistUrlFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLAYLIST_URL_KEY]
    }

    suspend fun savePlaylistUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYLIST_URL_KEY] = url
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
}
