package com.example.iptvmate

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
    private lateinit var btnSaveSettings: Button
    private lateinit var repository: PlaylistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etSettingsPlaylistUrl = findViewById(R.id.etSettingsPlaylistUrl)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        repository = PlaylistRepository(applicationContext)

        lifecycleScope.launch {
            val currentUrl = repository.playlistUrlFlow.first()
            if (!currentUrl.isNullOrBlank()) {
                etSettingsPlaylistUrl.setText(currentUrl)
                etSettingsPlaylistUrl.setSelection(currentUrl.length)
            }
        }

        btnSaveSettings.setOnClickListener {
            val url = etSettingsPlaylistUrl.text.toString().trim()
            lifecycleScope.launch {
                repository.savePlaylistUrl(url)
                Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
