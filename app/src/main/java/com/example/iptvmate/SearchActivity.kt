package com.example.iptvmate

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearchQuery: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var repository: PlaylistRepository
    private val allChannels = mutableListOf<Channel>()
    private val filteredChannels = mutableListOf<Channel>()
    private lateinit var adapter: ChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etSearchQuery = findViewById(R.id.etSearchQuery)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        repository = PlaylistRepository(applicationContext)

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.setHasFixedSize(true)
        rvSearchResults.setItemViewCacheSize(20)

        allChannels.addAll(repository.loadCachedChannels())
        filteredChannels.addAll(allChannels)

        adapter = ChannelAdapter(filteredChannels) { channel ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
            }
            startActivity(intent)
        }
        rvSearchResults.adapter = adapter

        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearchQuery.requestFocus()
    }

    private fun filter(query: String) {
        filteredChannels.clear()
        if (query.isBlank()) {
            filteredChannels.addAll(allChannels)
        } else {
            val q = query.trim().lowercase()
            filteredChannels.addAll(allChannels.filter {
                it.name.lowercase().contains(q) || (it.groupTitle?.lowercase()?.contains(q) == true)
            })
        }
        adapter.notifyDataSetChanged()
    }
}
