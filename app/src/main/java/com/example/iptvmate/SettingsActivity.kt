package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var etSettingsPlaylistUrl: EditText
    private lateinit var etSettingsEpgUrl: EditText
    private lateinit var btnManagePlaylists: Button
    private lateinit var btnRearrangeChannels: Button
    private lateinit var btnRearrangeCategories: Button
    private lateinit var switchAutoPlayLastChannel: androidx.appcompat.widget.SwitchCompat
    private lateinit var btnSaveSettings: Button
    private lateinit var repository: PlaylistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etSettingsPlaylistUrl = findViewById(R.id.etSettingsPlaylistUrl)
        etSettingsEpgUrl = findViewById(R.id.etSettingsEpgUrl)
        btnManagePlaylists = findViewById(R.id.btnManagePlaylists)
        btnRearrangeChannels = findViewById(R.id.btnRearrangeChannels)
        btnRearrangeCategories = findViewById(R.id.btnRearrangeCategories)
        switchAutoPlayLastChannel = findViewById(R.id.switchAutoPlayLastChannel)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        repository = PlaylistRepository(applicationContext)

        val appSettings = repository.loadAppSettings()
        switchAutoPlayLastChannel.isChecked = appSettings.autoPlayLastChannel

        if (!appSettings.playlistUrl.isNullOrBlank()) {
            etSettingsPlaylistUrl.setText(appSettings.playlistUrl)
            etSettingsPlaylistUrl.setSelection(appSettings.playlistUrl.length)
        }
        if (!appSettings.epgUrl.isNullOrBlank()) {
            etSettingsEpgUrl.setText(appSettings.epgUrl)
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

        val layoutAutoPlayRow = findViewById<android.view.View>(R.id.layoutAutoPlayRow)
        layoutAutoPlayRow.setOnClickListener {
            switchAutoPlayLastChannel.isChecked = !switchAutoPlayLastChannel.isChecked
        }

        btnSaveSettings.setOnClickListener {
            val url = etSettingsPlaylistUrl.text.toString().trim()
            val epgUrl = etSettingsEpgUrl.text.toString().trim()
            lifecycleScope.launch {
                repository.savePlaylistUrl(url)
                repository.saveEpgUrl(epgUrl)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = url,
                    epgUrl = epgUrl,
                    autoPlayLastChannel = switchAutoPlayLastChannel.isChecked
                ))
                Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
