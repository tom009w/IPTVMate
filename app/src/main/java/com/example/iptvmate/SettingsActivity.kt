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

    private lateinit var etXtreamServer: EditText
    private lateinit var etXtreamUsername: EditText
    private lateinit var etXtreamPassword: EditText
    private lateinit var btnSaveXtream: Button

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

        etXtreamServer = findViewById(R.id.etXtreamServer)
        etXtreamUsername = findViewById(R.id.etXtreamUsername)
        etXtreamPassword = findViewById(R.id.etXtreamPassword)
        btnSaveXtream = findViewById(R.id.btnSaveXtream)

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
        if (!appSettings.xtreamServer.isNullOrBlank()) {
            etXtreamServer.setText(appSettings.xtreamServer)
        }
        if (!appSettings.xtreamUsername.isNullOrBlank()) {
            etXtreamUsername.setText(appSettings.xtreamUsername)
        }
        if (!appSettings.xtreamPassword.isNullOrBlank()) {
            etXtreamPassword.setText(appSettings.xtreamPassword)
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

        btnSaveXtream.setOnClickListener {
            val server = etXtreamServer.text.toString().trim().removeSuffix("/")
            val username = etXtreamUsername.text.toString().trim()
            val password = etXtreamPassword.text.toString().trim()

            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in all Xtream Codes fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val m3uUrl = "$server/get.php?username=$username&password=$password&type=m3u_plus"
            lifecycleScope.launch {
                repository.savePlaylistUrl(m3uUrl)
                val settings = repository.loadAppSettings()
                repository.saveAppSettings(settings.copy(
                    playlistUrl = m3uUrl,
                    xtreamServer = server,
                    xtreamUsername = username,
                    xtreamPassword = password
                ))
                Toast.makeText(this@SettingsActivity, "Xtream Codes connected successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
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
