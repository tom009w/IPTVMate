package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EpgActivity : AppCompatActivity() {

    private lateinit var rvEpgRows: RecyclerView
    private lateinit var repository: PlaylistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg)

        rvEpgRows = findViewById(R.id.rvEpgRows)
        rvEpgRows.layoutManager = LinearLayoutManager(this)
        rvEpgRows.setHasFixedSize(true)
        rvEpgRows.setItemViewCacheSize(20)
        repository = PlaylistRepository(applicationContext)

        val channels = repository.loadCachedChannels()
        val epgMap = EpgManager.epgMap

        rvEpgRows.adapter = EpgRowAdapter(channels, epgMap) { channel ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
            }
            startActivity(intent)
        }
    }
}
