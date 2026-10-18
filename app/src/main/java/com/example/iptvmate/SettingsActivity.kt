package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var etSettingsPlaylistUrl: EditText
    private lateinit var etSettingsEpgUrl: EditText
    private lateinit var btnManagePlaylists: Button
    private lateinit var btnRearrangeChannels: Button
    private lateinit var btnRearrangeCategories: Button
    private lateinit var btnAdditionalSettings: Button
    private lateinit var btnAutoPlayToggle: Button
    private lateinit var btnSaveSettings: Button
    private lateinit var repository: PlaylistRepository
    private var autoPlayEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etSettingsPlaylistUrl = findViewById(R.id.etSettingsPlaylistUrl)
        etSettingsEpgUrl = findViewById(R.id.etSettingsEpgUrl)
        btnManagePlaylists = findViewById(R.id.btnManagePlaylists)
        btnRearrangeChannels = findViewById(R.id.btnRearrangeChannels)
        btnRearrangeCategories = findViewById(R.id.btnRearrangeCategories)
        btnAdditionalSettings = findViewById(R.id.btnAdditionalSettings)
        btnAutoPlayToggle = findViewById(R.id.btnAutoPlayToggle)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        repository = PlaylistRepository(applicationContext)

        val appSettings = repository.loadAppSettings()
        autoPlayEnabled = appSettings.autoPlayLastChannel
        updateAutoPlayButtonText()

        lifecycleScope.launch {
            val currentUrl = repository.playlistUrlFlow.first()
            if (!currentUrl.isNullOrBlank()) {
                etSettingsPlaylistUrl.setText(currentUrl)
                etSettingsPlaylistUrl.setSelection(currentUrl.length)
            }
            val currentEpgUrl = repository.epgUrlFlow.first()
            if (!currentEpgUrl.isNullOrBlank()) {
                etSettingsEpgUrl.setText(currentEpgUrl)
            }
        }

        btnManagePlaylists.setOnClickListener {
            startActivity(Intent(this, PlaylistsActivity::class.java))
        }

        btnRearrangeChannels.setOnClickListener {
            startActivity(Intent(this, ChannelOrderActivity::class.java))
        }

        btnRearrangeCategories.setOnClickListener {
            startActivity(Intent(this, CategoryOrderActivity::class.java))
        }

        btnAdditionalSettings.setOnClickListener {
            startActivity(Intent(this, AdditionalSettingsActivity::class.java))
        }

        btnAutoPlayToggle.setOnClickListener {
            autoPlayEnabled = !autoPlayEnabled
            updateAutoPlayButtonText()
            val settings = repository.loadAppSettings()
            repository.saveAppSettings(settings.copy(autoPlayLastChannel = autoPlayEnabled))
            Toast.makeText(this, if (autoPlayEnabled) "Auto-play enabled" else "Auto-play disabled", Toast.LENGTH_SHORT).show()
        }

        btnSaveSettings.setOnClickListener {
            val url = etSettingsPlaylistUrl.text.toString().trim()
            val epgUrl = etSettingsEpgUrl.text.toString().trim()
            lifecycleScope.launch {
                repository.savePlaylistUrl(url)
                repository.saveEpgUrl(epgUrl)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(autoPlayLastChannel = autoPlayEnabled))
                Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateAutoPlayButtonText() {
        btnAutoPlayToggle.text = if (autoPlayEnabled) getString(R.string.auto_play_last_channel_on) else getString(R.string.auto_play_last_channel_off)
    }
}
